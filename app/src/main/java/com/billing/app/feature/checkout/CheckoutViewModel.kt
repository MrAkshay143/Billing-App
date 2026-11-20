package com.billing.app.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.BillingApplication
import com.billing.app.core.datastore.SettingsDataStore
import com.billing.app.core.notification.DokandaarNotificationManager
import com.billing.app.core.util.BillCalculationResult
import com.billing.app.core.util.Money
import com.billing.app.core.util.Quantity
import com.billing.app.core.util.TaxEngine
import com.billing.app.core.util.UnitRegistry
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
import com.billing.app.domain.repository.AuditRepository
import com.billing.app.domain.repository.CategoryRepository
import com.billing.app.domain.repository.CustomerRepository
import com.billing.app.domain.repository.InvoiceRepository
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.ShiftRepository
import com.billing.app.domain.repository.ShopRepository
import com.billing.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PaymentMethodMode {
    CASH, UPI, OTHER, SPLIT
}

class CheckoutViewModel(
    private val shopRepository: ShopRepository,
    private val printerRepository: PrinterRepository,
    private val invoiceRepository: InvoiceRepository,
    private val settingsDataStore: SettingsDataStore? = null,
    private val productRepository: ProductRepository? = null,
    private val customerRepository: CustomerRepository? = null,
    private val categoryRepository: CategoryRepository? = null,
    private val auditRepository: AuditRepository? = null,
    private val userRepository: UserRepository? = null,
    private val shiftRepository: ShiftRepository? = null
) : ViewModel() {

    val shopProfile: StateFlow<ShopProfile> = shopRepository.getShopProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShopProfile())

    val selectedPaymentMethod = MutableStateFlow(PaymentMethodMode.UPI)
    val discountPaise = MutableStateFlow(0L)
    val cashReceivedPaise = MutableStateFlow(0L)
    val splitCashPaise = MutableStateFlow(0L)
    val splitUpiPaise = MutableStateFlow(0L)
    val isCompositionScheme = MutableStateFlow(false)
    val selectedCustomerId = MutableStateFlow<String?>(null)

    val activeShift = shiftRepository?.getActiveShiftFlow()
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, null)
        ?: MutableStateFlow(null).asStateFlow()

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
                        "SPLIT" -> PaymentMethodMode.SPLIT
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

    fun setCustomer(name: String, phone: String, id: String? = null) {
        customerName.value = name.ifBlank { "Walk-in Customer" }
        customerPhone.value = phone
        selectedCustomerId.value = id
    }

    fun setSplitPayment(cashPaise: Long, upiPaise: Long) {
        splitCashPaise.value = cashPaise.coerceAtLeast(0L)
        splitUpiPaise.value = upiPaise.coerceAtLeast(0L)
    }

    fun addNewCustomer(name: String, phone: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newCustomer = CustomerEntity(name = name.trim(), phone = phone.trim())
            customerRepository?.addCustomer(newCustomer)
            setCustomer(name.trim(), phone.trim(), newCustomer.id)
        }
    }

    fun logPriceOverride(productId: String, productName: String, oldPricePaise: Long, newPricePaise: Long) {
        if (oldPricePaise == newPricePaise) return
        viewModelScope.launch {
            val currentUserId = userRepository?.currentUser?.value?.id ?: "SYSTEM"
            auditRepository?.log(
                action = "PRICE_OVERRIDE",
                details = "Price changed for $productName from $oldPricePaise to $newPricePaise",
                userId = currentUserId,
                entityType = "PRODUCT",
                entityId = productId
            )
        }
    }

    fun reactivateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            customerRepository?.updateCustomer(customer.copy(isActive = true))
        }
    }

    fun reactivateProduct(product: Product) {
        viewModelScope.launch {
            productRepository?.updateProduct(product.copy(isActive = true))
        }
    }

    fun setDiscount(amountPaise: Long, subtotalPaise: Long) {
        discountPaise.value = amountPaise.coerceIn(0L, subtotalPaise.coerceAtLeast(0L))
    }

    fun setCashReceived(amountPaise: Long) {
        cashReceivedPaise.value = amountPaise.coerceAtLeast(0L)
    }

    fun calculateTax(subtotalPaise: Long, discountPaise: Long): Long {
        if (!isGstEnabled || isCompositionScheme.value || taxRatePercent <= 0.0) return 0L
        val taxablePaise = (subtotalPaise - discountPaise).coerceAtLeast(0L)
        return kotlin.math.round(taxablePaise * (taxRatePercent / 100.0)).toLong()
    }

    fun calculateBillResult(items: List<CartItem>, billDiscountPaise: Long): BillCalculationResult {
        val isGstActive = isGstEnabled && !isCompositionScheme.value
        val lineInputs = items.map { item ->
            val effectiveRateBp = if (isGstActive) {
                if (item.product.gstRateBp > 0) item.product.gstRateBp else (taxRatePercent * 100).toInt()
            } else {
                0
            }
            TaxEngine.LineItem(
                lineTotalPaise = item.total.paise,
                gstRateBp = effectiveRateBp
            )
        }
        return TaxEngine.calculateBill(
            items = lineInputs,
            billDiscountPaise = billDiscountPaise.coerceAtLeast(0L),
            isGstEnabled = isGstActive
        )
    }

    fun calculateGrandTotal(subtotalPaise: Long): Long {
        val discount = discountPaise.value.coerceIn(0L, subtotalPaise)
        val tax = calculateTax(subtotalPaise, discount)
        val unrounded = (subtotalPaise - discount + tax).coerceAtLeast(0L)
        return TaxEngine.roundToNearestRupee(unrounded).first
    }

    fun calculateDueAndChange(grandTotalPaise: Long): Pair<Long, Long> {
        return when (selectedPaymentMethod.value) {
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
            PaymentMethodMode.SPLIT -> {
                val totalPaid = splitCashPaise.value + splitUpiPaise.value
                if (totalPaid < grandTotalPaise) {
                    Pair(grandTotalPaise - totalPaid, 0L)
                } else {
                    Pair(0L, totalPaid - grandTotalPaise)
                }
            }
        }
    }

    suspend fun getTargetCustomer(): CustomerEntity? {
        val custId = selectedCustomerId.value
        val custPhone = customerPhone.value.trim()
        return if (!custId.isNullOrBlank()) {
            customerRepository?.getCustomerById(custId)
        } else if (custPhone.isNotBlank()) {
            customerRepository?.getCustomerByPhone(custPhone)
        } else null
    }

    suspend fun isCreditLimitExceeded(duePaise: Long): Boolean {
        if (duePaise <= 0L) return false
        val customer = getTargetCustomer() ?: return false
        val limit = customer.creditLimitPaise
        if (limit <= 0L) return false
        val currentBalance = customer.openingBalancePaise
        return (currentBalance + duePaise) > limit
    }

    fun completeSale(
        items: List<CartItem>,
        subtotal: Money,
        managerApprovedCreditLimit: Boolean = false,
        onSuccess: () -> Unit
    ) {
        if (_isCompletingSale.value) return
        if (items.isEmpty()) {
            _errorMessage.value = "Cart is empty. Scan or pick items first."
            return
        }

        _isCompletingSale.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // 1. Mandatory Active Shift validation
                val activeShift = shiftRepository?.getActiveShift()
                if (shiftRepository != null && activeShift == null) {
                    _errorMessage.value = "Active shift required before completing sale. Please open shift."
                    _isCompletingSale.value = false
                    return@launch
                }

                // 2. TaxEngine Bill Calculation
                val billResult = calculateBillResult(items, discountPaise.value)
                val grandTotalPaise = billResult.grandTotalPaise

                val selectedMode = selectedPaymentMethod.value
                val (duePaise, changeReturnedPaise) = calculateDueAndChange(grandTotalPaise)

                val cashReceived = when (selectedMode) {
                    PaymentMethodMode.CASH, PaymentMethodMode.OTHER -> cashReceivedPaise.value
                    PaymentMethodMode.SPLIT -> splitCashPaise.value
                    PaymentMethodMode.UPI -> 0L
                }

                val upiPaid = when (selectedMode) {
                    PaymentMethodMode.UPI -> grandTotalPaise
                    PaymentMethodMode.SPLIT -> splitUpiPaise.value
                    PaymentMethodMode.CASH, PaymentMethodMode.OTHER -> 0L
                }

                val custName = customerName.value.trim()
                val custPhone = customerPhone.value.trim()

                if (duePaise > 0 && (custName.isBlank() || custName.equals("Walk-in Customer", ignoreCase = true))) {
                    _errorMessage.value = "Customer name required for Udhaar (Due) bill"
                    _isCompletingSale.value = false
                    return@launch
                }

                val targetCustomer = getTargetCustomer()

                // 3. Customer Credit Limit Check
                if (duePaise > 0L && targetCustomer != null && targetCustomer.creditLimitPaise > 0L) {
                    val currentBal = targetCustomer.openingBalancePaise
                    if ((currentBal + duePaise) > targetCustomer.creditLimitPaise && !managerApprovedCreditLimit) {
                        _errorMessage.value = "Credit limit exceeded for ${targetCustomer.name}. Manager PIN authorization required."
                        _isCompletingSale.value = false
                        return@launch
                    }
                }

                val currentUser = userRepository?.currentUser?.value
                val invoiceNumber = invoiceRepository.generateNextInvoiceNumber()

                val paymentMethodStr = when {
                    selectedMode == PaymentMethodMode.SPLIT -> "SPLIT"
                    selectedMode == PaymentMethodMode.UPI -> "UPI"
                    duePaise > 0L && cashReceived == 0L && upiPaid == 0L -> "CREDIT"
                    else -> "CASH"
                }

                val isGstActive = isGstEnabled && !isCompositionScheme.value

                val invoice = InvoiceEntity(
                    id = 0L,
                    invoiceNumber = invoiceNumber,
                    timestamp = System.currentTimeMillis(),
                    subtotalPaise = billResult.subtotalPaise,
                    discountPaise = billResult.billDiscountPaise,
                    taxPaise = billResult.totalTaxPaise,
                    roundOffPaise = billResult.roundOffPaise,
                    grandTotalPaise = billResult.grandTotalPaise,
                    duePaise = duePaise,
                    originalDuePaise = duePaise,
                    status = InvoiceStatus.COMPLETED,
                    paymentMethod = paymentMethodStr,
                    customerName = custName.ifBlank { null },
                    customerPhone = custPhone.ifBlank { null },
                    customerId = targetCustomer?.id,
                    cashReceivedPaise = cashReceived,
                    changeReturnedPaise = changeReturnedPaise,
                    upiPaidPaise = upiPaid,
                    shiftId = activeShift?.id,
                    createdByUserId = currentUser?.id,
                    invoiceType = "SALE",
                    itemsCount = items.sumOf { it.quantity }.coerceAtLeast(1)
                )

                val invoiceItems = items.mapIndexed { index, item ->
                    val lineCalc = billResult.lineCalculations.getOrNull(index)
                    val effectiveRateBp = if (isGstActive) {
                        if (item.product.gstRateBp > 0) item.product.gstRateBp else (taxRatePercent * 100).toInt()
                    } else {
                        0
                    }
                    InvoiceItemEntity(
                        id = 0L,
                        invoiceId = 0L,
                        productId = if (item.isCustomItem) null else item.product.id,
                        productName = item.displayName,
                        barcode = if (item.isCustomItem) "" else item.product.barcode,
                        unitPricePaise = item.effectiveUnitPricePaise,
                        quantityMilli = item.quantityMilli,
                        lineTotalPaise = item.total.paise,
                        discountPaise = item.discountPaise,
                        billDiscountSharePaise = lineCalc?.billDiscountSharePaise ?: 0L,
                        taxableValuePaise = lineCalc?.taxableValuePaise ?: item.total.paise,
                        taxPaise = lineCalc?.taxPaise ?: 0L,
                        gstRateBp = effectiveRateBp,
                        unitCostPaise = if (item.isCustomItem) 0L else item.product.purchasePrice.paise,
                        unit = UnitRegistry.resolve(if (item.isCustomItem) "PCS" else item.product.unit).code,
                        isCustomItem = item.isCustomItem,
                        isCostUnknown = item.isCostUnknown
                    ).apply {
                        this.invoiceNumber = invoiceNumber
                    }
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
                            if (item.isCustomItem) continue
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

    fun completeSale(items: List<CartItem>, subtotal: Money, onSuccess: () -> Unit) {
        completeSale(items, subtotal, managerApprovedCreditLimit = false, onSuccess = onSuccess)
    }

    fun holdDraft(items: List<CartItem>, subtotal: Money, onSuccess: () -> Unit) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            try {
                val invoiceNumber = resumedDraftInvoiceNumber
                    ?: String.format(Locale.US, "DFT-%06d", (System.currentTimeMillis() / 1000) % 1000000)
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
                    customerId = selectedCustomerId.value,
                    invoiceType = "SALE",
                    itemsCount = items.sumOf { it.quantity }.coerceAtLeast(1)
                )
                val invoiceItems = items.map { item ->
                    InvoiceItemEntity(
                        id = 0L,
                        invoiceId = 0L,
                        productId = if (item.isCustomItem) null else item.product.id,
                        productName = item.displayName,
                        barcode = if (item.isCustomItem) "" else item.product.barcode,
                        unitPricePaise = item.effectiveUnitPricePaise,
                        quantityMilli = item.quantityMilli,
                        lineTotalPaise = item.total.paise,
                        discountPaise = item.discountPaise,
                        unit = UnitRegistry.resolve(if (item.isCustomItem) "PCS" else item.product.unit).code,
                        isCustomItem = item.isCustomItem,
                        isCostUnknown = item.isCostUnknown
                    ).apply {
                        this.invoiceNumber = invoiceNumber
                    }
                }
                invoiceRepository.saveDraft(invoice, invoiceItems)
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Could not park draft bill. Please try again."
            }
        }
    }

    fun getLastCompletedInvoice(): InvoiceEntity? = lastCompletedInvoice
    fun getLastCompletedItems(): List<InvoiceItemEntity> = lastCompletedItems

    fun getLastInvoiceReceipt(): InvoiceReceipt? {
        val invoice = lastCompletedInvoice ?: return null
        val shop = shopProfile.value
        val receiptItems = lastCompletedItems.map {
            ReceiptItem(
                name = it.productName,
                quantityMilli = it.quantityMilli,
                unitCode = it.unit,
                unitPricePaise = it.unitPricePaise,
                lineTotalPaise = it.lineTotalPaise
            )
        }
        val receiptType = if (isCompositionScheme.value) "Bill of Supply" else if (invoice.taxPaise > 0L || isGstEnabled) "Tax Invoice" else "Bill of Supply"
        val currentUser = userRepository?.currentUser?.value
        val cashier = currentUser?.displayName ?: currentUser?.username ?: ""

        return InvoiceReceipt(
            shopName = shop.name,
            shopAddressLine1 = shop.addressLine1,
            shopAddressLine2 = shop.addressLine2,
            shopPhone = shop.phoneNumber,
            shopGstin = gstin,
            footerText = shop.footerText,
            invoiceNumber = invoice.invoiceNumber,
            timestamp = invoice.timestamp,
            items = receiptItems,
            subtotalPaise = invoice.subtotalPaise,
            discountPaise = invoice.discountPaise,
            taxPaise = invoice.taxPaise,
            grandTotalPaise = invoice.grandTotalPaise,
            paymentMethod = invoice.paymentMethod,
            cashReceivedPaise = invoice.cashReceivedPaise,
            changeReturnedPaise = invoice.changeReturnedPaise,
            duePaise = invoice.duePaise,
            customerName = invoice.customerName,
            customerPhone = invoice.customerPhone,
            receiptType = receiptType,
            cashierName = cashier,
            roundOffPaise = invoice.roundOffPaise,
            upiPaidPaise = invoice.upiPaidPaise
        )
    }

    fun getLastInvoiceWhatsAppUrl(): String? {
        val invoice = lastCompletedInvoice ?: return null
        val rawPhone = invoice.customerPhone ?: return null
        val normalized = Quantity.normalizeInputDigits(rawPhone)
        val digits = normalized.filter { it.isDigit() }
        val phone10 = if (digits.length >= 10) digits.takeLast(10) else return null

        val shop = shopProfile.value
        val dateStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.ROOT).format(Date(invoice.timestamp))
        val receiptType = if (isCompositionScheme.value) "Bill of Supply" else "Tax Invoice"

        val summary = buildString {
            appendLine("*${shop.name.ifBlank { "Dokandaar Store" }}*")
            if (shop.addressLine1.isNotBlank()) appendLine(shop.addressLine1)
            if (shop.phoneNumber.isNotBlank()) appendLine("Ph: ${shop.phoneNumber}")
            if (gstin.isNotBlank()) appendLine("GSTIN: $gstin")
            appendLine("--------------------------------")
            appendLine("*$receiptType*")
            if (isCompositionScheme.value) {
                appendLine("_Composition Taxable Person, not eligible to collect tax on supplies_")
            }
            appendLine("Invoice: ${invoice.invoiceNumber}")
            appendLine("Date: $dateStr")
            if (!invoice.customerName.isNullOrBlank()) {
                appendLine("Customer: ${invoice.customerName}")
            }
            appendLine("--------------------------------")
            for (item in lastCompletedItems) {
                val totalStr = Money(item.lineTotalPaise).format()
                val qtyStr = Quantity.formatReceipt(item.quantityMilli, item.unit)
                val priceUnitStr = Quantity.formatPricePerUnit(item.unitPricePaise, item.unit)
                appendLine("${item.productName}: $qtyStr x $priceUnitStr = $totalStr")
            }
            appendLine("--------------------------------")
            appendLine("Subtotal: ${Money(invoice.subtotalPaise).format()}")
            if (invoice.discountPaise > 0L) {
                appendLine("Discount: -${Money(invoice.discountPaise).format()}")
            }
            if (invoice.taxPaise > 0L) {
                appendLine("Tax / GST: ${Money(invoice.taxPaise).format()}")
            }
            if (invoice.roundOffPaise != 0L) {
                val sign = if (invoice.roundOffPaise > 0) "+" else "-"
                appendLine("Round Off: $sign${Money(kotlin.math.abs(invoice.roundOffPaise)).format()}")
            }
            appendLine("*TOTAL: ${Money(invoice.grandTotalPaise).format()}*")
            appendLine("--------------------------------")
            appendLine("Payment: ${invoice.paymentMethod}")
            if (invoice.cashReceivedPaise > 0L) {
                appendLine("Cash Received: ${Money(invoice.cashReceivedPaise).format()}")
                if (invoice.changeReturnedPaise > 0L) {
                    appendLine("Change Returned: ${Money(invoice.changeReturnedPaise).format()}")
                }
            }
            if (invoice.upiPaidPaise > 0L) {
                appendLine("UPI Paid: ${Money(invoice.upiPaidPaise).format()}")
            }
            if (invoice.duePaise > 0L) {
                appendLine("*Balance Due: ${Money(invoice.duePaise).format()}*")
            }
            appendLine("--------------------------------")
            if (shop.footerText.isNotBlank()) {
                appendLine(shop.footerText)
            } else {
                appendLine("Thank you! Visit again.")
            }
        }

        val encodedText = URLEncoder.encode(summary, "UTF-8")
        return "https://wa.me/91$phone10?text=$encodedText"
    }

    fun printLastReceipt(onDone: () -> Unit = {}) {
        val receipt = getLastInvoiceReceipt()
        if (receipt == null) {
            onDone()
            return
        }
        _isPrinting.value = true
        viewModelScope.launch {
            val result = printerRepository.printInvoiceReceipt(receipt)
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
