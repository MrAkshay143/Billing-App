package com.billing.app.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.BillingApplication
import com.billing.app.core.datastore.SettingsDataStore
import com.billing.app.core.notification.DokandaarNotificationManager
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.CategoryEntity
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.InvoiceReceipt
import com.billing.app.domain.model.Product
import com.billing.app.domain.model.ReceiptItem
import com.billing.app.domain.model.ShopProfile
import com.billing.app.domain.repository.CategoryRepository
import com.billing.app.domain.repository.CustomerRepository
import com.billing.app.domain.repository.InvoiceRepository
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val settingsDataStore: SettingsDataStore? = null,
    private val productRepository: ProductRepository? = null,
    private val customerRepository: CustomerRepository? = null,
    private val categoryRepository: CategoryRepository? = null
) : ViewModel() {

    val shopProfile: StateFlow<ShopProfile> = shopRepository.getShopProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShopProfile())

    val selectedPaymentMethod = MutableStateFlow(PaymentMethodMode.UPI)
    val discountPaise = MutableStateFlow(0L)
    val cashReceivedPaise = MutableStateFlow(0L)
    val customerName = MutableStateFlow("Walk-in Customer")
    val customerPhone = MutableStateFlow("")

    val selectedCategory = MutableStateFlow("All")
    val searchQuery = MutableStateFlow("")

    // Customers stream
    val customers: StateFlow<List<CustomerEntity>> = customerRepository?.getAllCustomers()
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        ?: MutableStateFlow(emptyList<CustomerEntity>()).asStateFlow()

    // Categories stream
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository?.getAllCategories()
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        ?: MutableStateFlow(emptyList<CategoryEntity>()).asStateFlow()

    // Filtered Products stream for POS
    val products: StateFlow<List<Product>> = if (productRepository != null) {
        combine(
            productRepository.getProducts(),
            selectedCategory,
            searchQuery
        ) { allProducts, cat, query ->
            allProducts.filter { prod ->
                val matchesCat = cat.equals("All", ignoreCase = true) || prod.category.equals(cat, ignoreCase = true)
                val matchesQuery = query.isBlank() ||
                        prod.name.contains(query, ignoreCase = true) ||
                        prod.barcode.contains(query, ignoreCase = true) ||
                        prod.sku.contains(query, ignoreCase = true)
                matchesCat && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    } else {
        MutableStateFlow(emptyList<Product>()).asStateFlow()
    }

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
                settingsDataStore.defaultPaymentMethodFlow.collect { defaultMethod ->
                    selectedPaymentMethod.value = when (defaultMethod.uppercase()) {
                        "CASH" -> PaymentMethodMode.CASH
                        "OTHER" -> PaymentMethodMode.OTHER
                        else -> PaymentMethodMode.UPI
                    }
                }
            }
            viewModelScope.launch {
                settingsDataStore.gstEnabledFlow.collect { enabled ->
                    isGstEnabled = enabled
                }
            }
            viewModelScope.launch {
                settingsDataStore.gstinFlow.collect { gstinValue ->
                    gstin = gstinValue
                }
            }
            viewModelScope.launch {
                settingsDataStore.taxRateFlow.collect { rateStr ->
                    taxRatePercent = rateStr.toDoubleOrNull() ?: 0.0
                }
            }
        }
    }

    fun setResumedDraft(draftNumber: String?) {
        resumedDraftInvoiceNumber = draftNumber
    }

    fun setPaymentMethod(method: PaymentMethodMode) {
        selectedPaymentMethod.value = method
    }

    fun setCustomer(name: String, phone: String) {
        customerName.value = name.ifBlank { "Walk-in Customer" }
        customerPhone.value = phone
    }

    fun addNewCustomer(name: String, phone: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            customerRepository?.addCustomer(
                CustomerEntity(name = name.trim(), phone = phone.trim())
            )
            setCustomer(name.trim(), phone.trim())
        }
    }

    fun setDiscount(amountPaise: Long, subtotalPaise: Long) {
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
                    Pair(grandTotalPaise - cash, 0L)
                } else {
                    Pair(0L, cash - grandTotalPaise)
                }
            }
            PaymentMethodMode.OTHER -> {
                val cash = cashReceivedPaise.value
                if (cash > 0 && cash < grandTotalPaise) {
                    Pair(grandTotalPaise - cash, 0L)
                } else if (cash >= grandTotalPaise) {
                    Pair(0L, cash - grandTotalPaise)
                } else {
                    Pair(grandTotalPaise, 0L)
                }
            }
            PaymentMethodMode.UPI -> Pair(0L, 0L)
        }
    }

    fun completeSale(items: List<CartItem>, subtotal: Money, onSuccess: () -> Unit) {
        if (_isCompletingSale.value) return
        if (items.isEmpty()) {
            _errorMessage.value = "Cart is empty. Scan or pick items first."
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

                val custName = customerName.value.trim()
                val custPhone = customerPhone.value.trim()
                if (duePaise > 0 && (custName.isBlank() || custName.equals("Walk-in Customer", ignoreCase = true))) {
                    _errorMessage.value = "Customer name required for Udhaar (Due) bill"
                    _isCompletingSale.value = false
                    return@launch
                }

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
                    changeReturnedPaise = changeReturnedPaise,
                    invoiceType = "SALE",
                    itemsCount = items.sumOf { it.quantity }.coerceAtLeast(1)
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

                    // Check low stock trigger
                    productRepository?.let { repo ->
                        for (item in items) {
                            val updatedProd = repo.getProductById(item.product.id)
                            if (updatedProd != null && updatedProd.stock <= 5) {
                                DokandaarNotificationManager.showLowStockNotification(
                                    BillingApplication.instance,
                                    1,
                                    "${updatedProd.name} (${updatedProd.stock} left)"
                                )
                            }
                        }
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Could not complete sale. Please try again."
                    _isCompletingSale.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not complete sale. Please try again."
                _isCompletingSale.value = false
            }
        }
    }

    fun holdDraft(items: List<CartItem>, subtotal: Money, onSuccess: () -> Unit) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            try {
                val invoiceNumber = resumedDraftInvoiceNumber ?: invoiceRepository.generateNextInvoiceNumber()
                val invoice = InvoiceEntity(
                    id = 0L,
                    invoiceNumber = invoiceNumber,
                    timestamp = System.currentTimeMillis(),
                    subtotalPaise = subtotal.paise,
                    discountPaise = discountPaise.value,
                    taxPaise = 0L,
                    grandTotalPaise = subtotal.paise - discountPaise.value,
                    status = InvoiceStatus.DRAFT,
                    paymentMethod = selectedPaymentMethod.value.name,
                    customerName = customerName.value.ifBlank { null },
                    customerPhone = customerPhone.value.ifBlank { null },
                    invoiceType = "SALE",
                    itemsCount = items.sumOf { it.quantity }.coerceAtLeast(1)
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
                invoiceRepository.saveDraft(invoice, invoiceItems)
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Could not park draft bill. Please try again."
            }
        }
    }

    fun printLastReceipt(onDone: () -> Unit = {}) {
        val invoice = lastCompletedInvoice ?: return
        val items = lastCompletedItems
        val profile = shopProfile.value

        _isPrinting.value = true
        viewModelScope.launch {
            val result = printerRepository.printInvoice(invoice, items, profile)
            _isPrinting.value = false
            _printSuccess.value = result.isSuccess
            onDone()
        }
    }

    fun printReceipt(cartItems: List<CartItem>, subtotal: Money) {
        printLastReceipt()
    }

    fun holdSaleAsDraft(cartItems: List<CartItem>, subtotal: Money, onSuccess: () -> Unit) {
        holdDraft(cartItems, subtotal, onSuccess)
    }

    fun clearError() {
        clearMessages()
    }

    fun clearMessages() {
        _errorMessage.value = null
        _saleSuccess.value = false
        _printSuccess.value = false
    }
}
