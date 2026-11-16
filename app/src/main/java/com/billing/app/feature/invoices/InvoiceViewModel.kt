package com.billing.app.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.datastore.SettingsDataStore
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.InvoiceRepository
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class InvoiceDateFilter {
    ALL, TODAY, THIS_WEEK, THIS_MONTH
}

enum class InvoiceStatusTab {
    COMPLETED, DRAFT, DUES, ALL
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InvoiceViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val printerRepository: PrinterRepository,
    private val shopRepository: ShopRepository,
    private val settingsDataStore: SettingsDataStore? = null
) : ViewModel() {

    val isPinEnabled: StateFlow<Boolean> = (settingsDataStore?.pinEnabledFlow ?: flowOf(false))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    suspend fun verifySecurityPin(pin: String): Boolean {
        return settingsDataStore?.verifyPin(pin) ?: true
    }

    val searchQuery = MutableStateFlow("")
    val dateFilter = MutableStateFlow(InvoiceDateFilter.ALL)
    val statusTab = MutableStateFlow(InvoiceStatusTab.COMPLETED)

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    private val _printMessage = MutableStateFlow<String?>(null)
    val printMessage: StateFlow<String?> = _printMessage.asStateFlow()

    private val _selectedInvoice = MutableStateFlow<Pair<InvoiceEntity, List<InvoiceItemEntity>>?>(null)
    val selectedInvoice: StateFlow<Pair<InvoiceEntity, List<InvoiceItemEntity>>?> = _selectedInvoice.asStateFlow()

    // Base flow of invoices by search query, date filter, and tab status
    val filteredInvoices: StateFlow<List<InvoiceEntity>> = combine(
        searchQuery,
        dateFilter,
        statusTab
    ) { query, filter, tab ->
        Triple(query, filter, tab)
    }.flatMapLatest { (query, filter, tab) ->
        val startTime = getFilterStartTime(filter)
        if (tab == InvoiceStatusTab.DUES) {
            invoiceRepository.getCreditDuesInvoices().map { list ->
                if (query.isBlank()) list
                else list.filter {
                    it.invoiceNumber.contains(query.trim(), ignoreCase = true) ||
                            (it.customerName?.contains(query.trim(), ignoreCase = true) == true)
                }
            }
        } else {
            val status = when (tab) {
                InvoiceStatusTab.COMPLETED -> InvoiceStatus.COMPLETED
                InvoiceStatusTab.DRAFT -> InvoiceStatus.DRAFT
                else -> null
            }

            if (query.isBlank()) {
                if (status != null) {
                    invoiceRepository.getInvoicesByStatus(status)
                } else if (startTime > 0L) {
                    invoiceRepository.getInvoicesSince(startTime)
                } else {
                    invoiceRepository.getAllInvoices()
                }
            } else {
                invoiceRepository.searchInvoices(query.trim(), status)
            }
        }
    }.map { invoices ->
        val startTime = getFilterStartTime(dateFilter.value)
        if (startTime > 0L) {
            invoices.filter { it.timestamp >= startTime }
        } else {
            invoices
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayedInvoices: StateFlow<List<InvoiceEntity>> = filteredInvoices

    val draftCount: StateFlow<Int> = invoiceRepository.getInvoicesByStatus(InvoiceStatus.DRAFT)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dueInvoicesCount: StateFlow<Int> = invoiceRepository.getCreditDuesInvoices()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedInvoices: StateFlow<List<InvoiceEntity>> = filteredInvoices.map { list ->
        list.filter { it.status == InvoiceStatus.COMPLETED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // KPI Metrics dynamically computed for COMPLETED sales only (excluding CANCELLED)
    val totalSalesCount: StateFlow<Int> = completedInvoices.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSalesAmountPaise: StateFlow<Long> = completedInvoices.map { list ->
        list.sumOf { it.grandTotalPaise }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val avgBillAmountPaise: StateFlow<Long> = combine(totalSalesCount, totalSalesAmountPaise) { count, total ->
        if (count > 0) total / count else 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setDateFilter(filter: InvoiceDateFilter) {
        dateFilter.value = filter
    }

    fun setStatusTab(tab: InvoiceStatusTab) {
        statusTab.value = tab
    }

    fun selectInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            val items = invoiceRepository.getInvoiceItems(invoice.invoiceNumber)
            _selectedInvoice.value = Pair(invoice, items)
        }
    }

    fun viewInvoiceDetails(invoice: InvoiceEntity) = selectInvoice(invoice)

    fun clearSelectedInvoice() {
        _selectedInvoice.value = null
    }

    fun dismissInvoiceDetails() = clearSelectedInvoice()

    suspend fun getCartItemsForInvoice(invoiceNumber: String): List<CartItem> {
        val items = invoiceRepository.getInvoiceItems(invoiceNumber)
        return items.map { item ->
            CartItem(
                product = Product(
                    id = item.productId,
                    name = item.productName,
                    barcode = item.barcode,
                    price = Money.fromPaise(item.unitPricePaise)
                ),
                quantity = item.quantity
            )
        }
    }

    fun reprintInvoice(invoiceNumber: String) = printInvoice(invoiceNumber)

    fun discardDraft(invoiceNumber: String) {
        viewModelScope.launch {
            invoiceRepository.deleteInvoice(invoiceNumber)
            if (_selectedInvoice.value?.first?.invoiceNumber == invoiceNumber) {
                _selectedInvoice.value = null
            }
        }
    }

    /**
     * Voids a completed invoice, changes status to CANCELLED and restores product stock (H6)
     */
    fun voidInvoice(invoiceNumber: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = invoiceRepository.voidInvoice(invoiceNumber)
            if (result.isSuccess) {
                _printMessage.value = "Invoice $invoiceNumber voided. Stock returned to inventory."
                if (_selectedInvoice.value?.first?.invoiceNumber == invoiceNumber) {
                    _selectedInvoice.value = null
                }
                onSuccess()
            } else {
                _printMessage.value = result.exceptionOrNull()?.message ?: "Failed to void invoice"
            }
        }
    }

    fun settleDue(invoiceNumber: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = invoiceRepository.settleInvoiceDue(invoiceNumber)
            if (result.isSuccess) {
                _printMessage.value = "Due balance settled for invoice $invoiceNumber"
                _selectedInvoice.value?.let { pair ->
                    if (pair.first.invoiceNumber == invoiceNumber) {
                        _selectedInvoice.value = Pair(pair.first.copy(duePaise = 0L), pair.second)
                    }
                }
                onSuccess()
            } else {
                _printMessage.value = result.exceptionOrNull()?.message ?: "Failed to settle due"
            }
        }
    }

    fun resumeDraft(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        onResumeDraft: (List<CartItem>, String) -> Unit
    ) {
        val cartItems = items.map { item ->
            CartItem(
                product = Product(
                    id = item.productId,
                    name = item.productName,
                    barcode = item.barcode,
                    price = Money.fromPaise(item.unitPricePaise)
                ),
                quantity = item.quantity
            )
        }
        onResumeDraft(cartItems, invoice.invoiceNumber)
    }

    /**
     * Accurate invoice reprint using saved invoice entity and exact timestamp (C4 & M2)
     */
    fun printInvoice(invoiceNumber: String) {
        if (_isPrinting.value) return
        _isPrinting.value = true
        _printMessage.value = null

        viewModelScope.launch {
            try {
                val invoice = invoiceRepository.getInvoiceByNumber(invoiceNumber)
                if (invoice == null) {
                    _printMessage.value = "Invoice not found"
                    _isPrinting.value = false
                    return@launch
                }
                val items = invoiceRepository.getInvoiceItems(invoiceNumber)
                val shop = shopRepository.getShopProfileSync()

                val result = printerRepository.printInvoice(invoice, items, shop)
                if (result.isSuccess) {
                    _printMessage.value = "Reprinted ${invoice.invoiceNumber} successfully"
                } else {
                    _printMessage.value = result.exceptionOrNull()?.message ?: "Reprint failed: printer offline"
                }
            } catch (e: Exception) {
                _printMessage.value = "Print error: ${e.message}"
            } finally {
                _isPrinting.value = false
            }
        }
    }

    fun clearPrintMessage() {
        _printMessage.value = null
    }

    private fun getFilterStartTime(filter: InvoiceDateFilter): Long {
        val calendar = Calendar.getInstance()
        return when (filter) {
            InvoiceDateFilter.ALL -> 0L
            InvoiceDateFilter.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            InvoiceDateFilter.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            InvoiceDateFilter.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
        }
    }
}
