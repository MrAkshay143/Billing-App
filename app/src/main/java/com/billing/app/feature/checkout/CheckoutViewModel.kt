package com.billing.app.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.datastore.SettingsDataStore
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.InvoiceReceipt
import com.billing.app.domain.model.ReceiptItem
import com.billing.app.domain.model.ShopProfile
import com.billing.app.domain.repository.InvoiceRepository
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PaymentMethodMode {
    CASH, UPI, OTHER
}

class CheckoutViewModel(
    private val shopRepository: ShopRepository,
    private val printerRepository: PrinterRepository,
    private val invoiceRepository: InvoiceRepository,
    private val settingsDataStore: SettingsDataStore? = null
) : ViewModel() {

    val shopProfile: StateFlow<ShopProfile> = shopRepository.getShopProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShopProfile())

    val selectedPaymentMethod = MutableStateFlow(PaymentMethodMode.UPI)
    val discountPaise = MutableStateFlow(0L)
    val cashReceivedPaise = MutableStateFlow(0L)
    val customerName = MutableStateFlow("")
    val customerPhone = MutableStateFlow("")

    // Track resumed draft without deleting it until sale succeeds (H4)
    var resumedDraftInvoiceNumber: String? = null
        private set

    // GST Settings (H7)
    var isGstEnabled: Boolean = false
        private set
    var gstin: String = ""
        private set
    var taxRatePercent: Double = 0.0
        private set

    private val _isCompletingSale = MutableStateFlow(false)
    val isCompletingSale: StateFlow<Boolean> = _isCompletingSale.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    private val _printSuccess = MutableStateFlow(false)
    val printSuccess: StateFlow<Boolean> = _printSuccess.asStateFlow()

    private val _saleSuccess = MutableStateFlow(false)
    val saleSuccess: StateFlow<Boolean> = _saleSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var lastCompletedInvoice: InvoiceEntity? = null
    private var lastCompletedItems: List<InvoiceItemEntity> = emptyList()

    init {
        if (settingsDataStore != null) {
            viewModelScope.launch {
                val defaultMethod = settingsDataStore.defaultPaymentMethodFlow.firstOrNull() ?: "UPI"
                selectedPaymentMethod.value = when (defaultMethod.uppercase()) {
                    "CASH" -> PaymentMethodMode.CASH
                    "OTHER" -> PaymentMethodMode.OTHER
                    else -> PaymentMethodMode.UPI
                }

                // Load GST settings
                isGstEnabled = settingsDataStore.gstEnabledFlow.firstOrNull() ?: false
                gstin = settingsDataStore.gstinFlow.firstOrNull() ?: ""
                val rateStr = settingsDataStore.taxRateFlow.firstOrNull() ?: "0"
                taxRatePercent = rateStr.toDoubleOrNull() ?: 0.0
            }
        }
    }

    fun setResumedDraft(draftNumber: String?) {
        resumedDraftInvoiceNumber = draftNumber
    }

    fun setPaymentMethod(method: PaymentMethodMode) {
        selectedPaymentMethod.value = method
    }

    fun setDiscount(amountPaise: Long, subtotalPaise: Long) {
        // Enforce discount does not exceed subtotal (H5)
        discountPaise.value = amountPaise.coerceIn(0L, subtotalPaise.coerceAtLeast(0L))
    }

    fun setCashReceived(amountPaise: Long) {
        cashReceivedPaise.value = amountPaise.coerceAtLeast(0L)
    }

    fun calculateTax(subtotalPaise: Long, discountPaise: Long): Long {
        if (!isGstEnabled || taxRatePercent <= 0.0) return 0L
        val taxablePaise = (subtotalPaise - discountPaise).coerceAtLeast(0L)
        return (taxablePaise * (taxRatePercent / 100.0)).toLong()
    }

    fun calculateGrandTotal(subtotalPaise: Long): Long {
        val discount = discountPaise.value.coerceIn(0L, subtotalPaise)
        val tax = calculateTax(subtotalPaise, discount)
        return (subtotalPaise - discount + tax).coerceAtLeast(0L)
    }

    fun calculateDueAndChange(grandTotalPaise: Long): Pair<Long, Long> {
        val method = selectedPaymentMethod.value
        return when (method) {
            PaymentMethodMode.CASH -> {
                val cash = cashReceivedPaise.value
                if (cash < grandTotalPaise) {
                    Pair(grandTotalPaise - cash, 0L) // due, change
                } else {
                    Pair(0L, cash - grandTotalPaise) // due, change
                }
            }
            PaymentMethodMode.OTHER -> {
                // If OTHER and customer entered cash, compute due
                val cash = cashReceivedPaise.value
                if (cash > 0 && cash < grandTotalPaise) {
                    Pair(grandTotalPaise - cash, 0L)
                } else if (cash >= grandTotalPaise) {
                    Pair(0L, cash - grandTotalPaise)
                } else {
                    Pair(grandTotalPaise, 0L) // Full credit/due
                }
            }
            PaymentMethodMode.UPI -> Pair(0L, 0L)
        }
    }

    fun completeSale(items: List<CartItem>, subtotal: Money, onSuccess: () -> Unit) {
        if (_isCompletingSale.value) return // Prevent multi-click race condition
        if (items.isEmpty()) {
            _errorMessage.value = "Cart is empty"
            return
        }

        _isCompletingSale.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val discount = discountPaise.value.coerceIn(0L, subtotal.paise)
                val tax = calculateTax(subtotal.paise, discount)
                val grandTotalPaise = (subtotal.paise - discount + tax).coerceAtLeast(0L)
                val (duePaise, changeReturnedPaise) = calculateDueAndChange(grandTotalPaise)

                // If credit/due sale, require customer identification (H5)
                val custName = customerName.value.trim()
                val custPhone = customerPhone.value.trim()
                if (duePaise > 0 && custName.isBlank()) {
                    _errorMessage.value = "Customer name is required for partial/credit due sales (Balance Due: Rs. ${Money(duePaise).format(false)})"
                    _isCompletingSale.value = false
                    return@launch
                }

                // Generate strictly consecutive monotonic invoice number (H1)
                val invoiceNumber = invoiceRepository.generateNextInvoiceNumber()

                val invoice = InvoiceEntity(
                    id = 0L,
                    invoiceNumber = invoiceNumber,
                    timestamp = System.currentTimeMillis(),
                    subtotalPaise = subtotal.paise,
                    discountPaise = discount,
                    taxPaise = tax,
                    grandTotalPaise = grandTotalPaise,
                    duePaise = duePaise,
                    status = InvoiceStatus.COMPLETED,
                    paymentMethod = selectedPaymentMethod.value.name,
                    customerName = custName.ifBlank { null },
                    customerPhone = custPhone.ifBlank { null },
                    cashReceivedPaise = cashReceivedPaise.value,
                    changeReturnedPaise = changeReturnedPaise
                )

                val invoiceItems = items.map { item ->
                    InvoiceItemEntity(
                        id = 0L,
                        invoiceNumber = invoiceNumber,
                        productId = item.product.id,
                        productName = item.product.name,
                        barcode = item.product.barcode,
                        unitPricePaise = item.product.price.paise,
                        quantity = item.quantity,
                        lineTotalPaise = item.total.paise
                    )
                }

                // Execute transactional sale with stock decrement & draft cleanup (H1, H2, H4)
                val result = invoiceRepository.completeSale(
                    invoice = invoice,
                    items = invoiceItems,
                    draftInvoiceNumberToDelete = resumedDraftInvoiceNumber
                )

                if (result.isSuccess) {
                    lastCompletedInvoice = invoice
                    lastCompletedItems = invoiceItems
                    resumedDraftInvoiceNumber = null

                    _saleSuccess.value = true
                    _isCompletingSale.value = false
                    onSuccess()

                    // Offline low stock notification check
                    if (settingsDataStore != null && settingsDataStore.areNotificationsEnabled()) {
                        try {
                            val lowStockProducts = (invoiceRepository as? com.billing.app.data.repository.InvoiceRepositoryImpl)
                            // Notification will be triggered via DokandaarNotificationManager if needed
                        } catch (_: Exception) {}
                    }
                } else {
                    _isCompletingSale.value = false
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to complete sale"
                }
            } catch (e: Exception) {
                _isCompletingSale.value = false
                _errorMessage.value = "Failed to complete sale: ${e.message}"
            }
        }
    }

    fun holdSaleAsDraft(
        items: List<CartItem>,
        subtotal: Money,
        onSuccess: () -> Unit
    ) {
        if (_isCompletingSale.value) return
        if (items.isEmpty()) {
            _errorMessage.value = "Cart is empty"
            return
        }

        _isCompletingSale.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val discount = discountPaise.value.coerceIn(0L, subtotal.paise)
                val tax = calculateTax(subtotal.paise, discount)
                val grandTotalPaise = (subtotal.paise - discount + tax).coerceAtLeast(0L)

                // DRAFT prefix
                val draftNumber = resumedDraftInvoiceNumber
                    ?: ("DRAFT-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()))

                val invoice = InvoiceEntity(
                    id = 0L,
                    invoiceNumber = draftNumber,
                    timestamp = System.currentTimeMillis(),
                    subtotalPaise = subtotal.paise,
                    discountPaise = discount,
                    taxPaise = tax,
                    grandTotalPaise = grandTotalPaise,
                    duePaise = 0L,
                    status = InvoiceStatus.DRAFT,
                    paymentMethod = selectedPaymentMethod.value.name,
                    customerName = customerName.value.trim().ifBlank { null },
                    customerPhone = customerPhone.value.trim().ifBlank { null },
                    cashReceivedPaise = 0L,
                    changeReturnedPaise = 0L
                )

                val invoiceItems = items.map { item ->
                    InvoiceItemEntity(
                        id = 0L,
                        invoiceNumber = draftNumber,
                        productId = item.product.id,
                        productName = item.product.name,
                        barcode = item.product.barcode,
                        unitPricePaise = item.product.price.paise,
                        quantity = item.quantity,
                        lineTotalPaise = item.total.paise
                    )
                }

                val result = invoiceRepository.saveDraft(invoice, invoiceItems)
                if (result.isSuccess) {
                    _isCompletingSale.value = false
                    onSuccess()
                } else {
                    _isCompletingSale.value = false
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to save draft"
                }
            } catch (e: Exception) {
                _isCompletingSale.value = false
                _errorMessage.value = "Failed to hold draft: ${e.message}"
            }
        }
    }

    /**
     * Prints receipt with accurate subtotal, discount, taxes, and payment details (Fixes C4 & M2)
     */
    fun printReceipt(items: List<CartItem>, subtotal: Money) {
        if (_isPrinting.value) return
        _isPrinting.value = true
        _printSuccess.value = false
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val shop = shopRepository.getShopProfileSync()
                val discount = discountPaise.value.coerceIn(0L, subtotal.paise)
                val tax = calculateTax(subtotal.paise, discount)
                val grandTotalPaise = (subtotal.paise - discount + tax).coerceAtLeast(0L)
                val (duePaise, changeReturnedPaise) = calculateDueAndChange(grandTotalPaise)

                val invNumber = lastCompletedInvoice?.invoiceNumber ?: "INV-PREPRINT"
                val receiptItems = items.map {
                    ReceiptItem(
                        name = it.product.name,
                        quantity = it.quantity,
                        unitPricePaise = it.product.price.paise,
                        lineTotalPaise = it.total.paise
                    )
                }

                val receipt = InvoiceReceipt(
                    shopName = shop.name,
                    shopAddressLine1 = shop.addressLine1,
                    shopAddressLine2 = shop.addressLine2,
                    shopPhone = shop.phoneNumber,
                    shopGstin = gstin,
                    footerText = shop.footerText,
                    invoiceNumber = invNumber,
                    timestamp = lastCompletedInvoice?.timestamp ?: System.currentTimeMillis(),
                    items = receiptItems,
                    subtotalPaise = subtotal.paise,
                    discountPaise = discount,
                    taxPaise = tax,
                    grandTotalPaise = grandTotalPaise,
                    paymentMethod = selectedPaymentMethod.value.name,
                    cashReceivedPaise = cashReceivedPaise.value,
                    changeReturnedPaise = changeReturnedPaise,
                    duePaise = duePaise,
                    customerName = customerName.value.trim().ifBlank { null },
                    customerPhone = customerPhone.value.trim().ifBlank { null }
                )

                val result = printerRepository.printInvoiceReceipt(receipt)
                if (result.isSuccess) {
                    _printSuccess.value = true
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Printer not connected. Please pair via Settings."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Printing error: ${e.message}"
            } finally {
                _isPrinting.value = false
            }
        }
    }

    fun resetSaleState() {
        _saleSuccess.value = false
        discountPaise.value = 0L
        cashReceivedPaise.value = 0L
        customerName.value = ""
        customerPhone.value = ""
        resumedDraftInvoiceNumber = null
        lastCompletedInvoice = null
        lastCompletedItems = emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
