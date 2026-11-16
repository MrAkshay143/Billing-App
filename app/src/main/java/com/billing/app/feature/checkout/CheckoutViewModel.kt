package com.billing.app.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.core.datastore.SettingsDataStore
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.ShopProfile
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
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore? = null
) : ViewModel() {

    val shopProfile: StateFlow<ShopProfile> = shopRepository.getShopProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShopProfile())

    val selectedPaymentMethod = MutableStateFlow(PaymentMethodMode.UPI)
    val discountPaise = MutableStateFlow(0L)
    val cashReceivedPaise = MutableStateFlow(0L)
    val customerName = MutableStateFlow("")
    val customerPhone = MutableStateFlow("")

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

    init {
        // Initialize default payment method from settings if available
        if (settingsDataStore != null) {
            viewModelScope.launch {
                val defaultMethod = settingsDataStore.defaultPaymentMethodFlow.firstOrNull() ?: "UPI"
                selectedPaymentMethod.value = when (defaultMethod.uppercase()) {
                    "CASH" -> PaymentMethodMode.CASH
                    "OTHER" -> PaymentMethodMode.OTHER
                    else -> PaymentMethodMode.UPI
                }
            }
        }
    }

    fun setPaymentMethod(method: PaymentMethodMode) {
        selectedPaymentMethod.value = method
    }

    fun setDiscount(amountPaise: Long) {
        discountPaise.value = amountPaise.coerceAtLeast(0L)
    }

    fun setCashReceived(amountPaise: Long) {
        cashReceivedPaise.value = amountPaise.coerceAtLeast(0L)
    }

    fun completeSale(items: List<CartItem>, subtotal: Money, onSuccess: () -> Unit) {
        if (_isCompletingSale.value) return // Prevent multi-click race condition
        if (items.isEmpty()) {
            _errorMessage.value = "Cart is empty"
            return
        }

        _isCompletingSale.value = true

        viewModelScope.launch {
            try {
                // Stock validation check before completing sale
                for (item in items) {
                    val dbProduct = database.productDao().getProductById(item.product.id)
                    if (dbProduct != null && dbProduct.stock < item.quantity) {
                        _errorMessage.value = "Insufficient stock for '${item.product.name}'. Available: ${dbProduct.stock}, requested: ${item.quantity}"
                        _isCompletingSale.value = false
                        return@launch
                    }
                }

                val discount = discountPaise.value
                val grandTotalPaise = (subtotal.paise - discount).coerceAtLeast(0L)
                val methodStr = selectedPaymentMethod.value.name

                val invoiceNumber = "INV-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val changeReturned = if (selectedPaymentMethod.value == PaymentMethodMode.CASH) {
                    (cashReceivedPaise.value - grandTotalPaise).coerceAtLeast(0L)
                } else 0L

                val invoice = InvoiceEntity(
                    invoiceNumber = invoiceNumber,
                    subtotalPaise = subtotal.paise,
                    discountPaise = discount,
                    grandTotalPaise = grandTotalPaise,
                    status = InvoiceStatus.COMPLETED,
                    paymentMethod = methodStr,
                    customerName = customerName.value.ifBlank { null },
                    customerPhone = customerPhone.value.ifBlank { null },
                    cashReceivedPaise = cashReceivedPaise.value,
                    changeReturnedPaise = changeReturned
                )

                val invoiceItems = items.map { item ->
                    InvoiceItemEntity(
                        invoiceNumber = invoiceNumber,
                        productId = item.product.id,
                        productName = item.product.name,
                        barcode = item.product.barcode,
                        unitPricePaise = item.product.price.paise,
                        quantity = item.quantity,
                        lineTotalPaise = item.total.paise
                    )
                }

                database.withTransaction {
                    database.invoiceDao().insertInvoice(invoice)
                    database.invoiceDao().insertInvoiceItems(invoiceItems)

                    for (item in items) {
                        database.productDao().decrementProductStock(item.product.id, item.quantity)
                    }
                }

                _saleSuccess.value = true
                _isCompletingSale.value = false
                onSuccess()

                // Offline Notification Check: Low Stock
                if (settingsDataStore != null && settingsDataStore.areNotificationsEnabled()) {
                    try {
                        val lowStockProducts = database.productDao().getLowAndOutOfStockProducts()
                        if (lowStockProducts.isNotEmpty()) {
                            val sample = lowStockProducts.take(3).joinToString(", ") { "${it.name} (${it.stock})" }
                            com.billing.app.core.notification.DokandaarNotificationManager.showLowStockNotification(
                                context = settingsDataStore.appContext,
                                productCount = lowStockProducts.size,
                                sampleNames = sample
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CheckoutViewModel", "Error checking low stock notification", e)
                    }
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
                val discount = discountPaise.value
                val grandTotalPaise = (subtotal.paise - discount).coerceAtLeast(0L)
                val methodStr = selectedPaymentMethod.value.name

                val invoiceNumber = "DRAFT-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

                val invoice = InvoiceEntity(
                    invoiceNumber = invoiceNumber,
                    subtotalPaise = subtotal.paise,
                    discountPaise = discount,
                    grandTotalPaise = grandTotalPaise,
                    status = InvoiceStatus.DRAFT,
                    paymentMethod = methodStr,
                    customerName = customerName.value.ifBlank { null },
                    customerPhone = customerPhone.value.ifBlank { null },
                    cashReceivedPaise = 0L,
                    changeReturnedPaise = 0L
                )

                val invoiceItems = items.map { item ->
                    InvoiceItemEntity(
                        invoiceNumber = invoiceNumber,
                        productId = item.product.id,
                        productName = item.product.name,
                        barcode = item.product.barcode,
                        unitPricePaise = item.product.price.paise,
                        quantity = item.quantity,
                        lineTotalPaise = item.total.paise
                    )
                }

                database.withTransaction {
                    database.invoiceDao().insertInvoice(invoice)
                    database.invoiceDao().insertInvoiceItems(invoiceItems)
                }

                _isCompletingSale.value = false
                onSuccess()
            } catch (e: Exception) {
                _isCompletingSale.value = false
                _errorMessage.value = "Failed to hold draft: ${e.message}"
            }
        }
    }

    fun printReceipt(items: List<CartItem>, total: Money) {
        if (_isPrinting.value) return
        _isPrinting.value = true
        _printSuccess.value = false
        _errorMessage.value = null

        viewModelScope.launch {
            val shop = shopRepository.getShopProfileSync()
            val result = printerRepository.printReceipt(shop, items)

            if (result.isSuccess) {
                _printSuccess.value = true
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Printer not connected. Please pair via Settings."
            }
            _isPrinting.value = false
        }
    }

    fun resetSaleState() {
        _saleSuccess.value = false
        discountPaise.value = 0L
        cashReceivedPaise.value = 0L
        customerName.value = ""
        customerPhone.value = ""
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
