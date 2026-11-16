package com.billing.app.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.util.Money
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class InvoiceDateFilter {
    ALL, TODAY, THIS_WEEK, THIS_MONTH
}

enum class InvoiceStatusTab {
    COMPLETED, DRAFT, ALL
}

class InvoiceViewModel(
    private val invoiceDao: InvoiceDao,
    private val printerRepository: PrinterRepository,
    private val shopRepository: ShopRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val dateFilter = MutableStateFlow(InvoiceDateFilter.ALL)
    val statusTab = MutableStateFlow(InvoiceStatusTab.COMPLETED)

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    private val _printMessage = MutableStateFlow<String?>(null)
    val printMessage: StateFlow<String?> = _printMessage.asStateFlow()

    private val _selectedInvoice = MutableStateFlow<Pair<InvoiceEntity, List<InvoiceItemEntity>>?>(null)
    val selectedInvoice: StateFlow<Pair<InvoiceEntity, List<InvoiceItemEntity>>?> = _selectedInvoice.asStateFlow()

    // Base flow of invoices by date filter
    val filteredInvoices: StateFlow<List<InvoiceEntity>> = combine(
        searchQuery,
        dateFilter
    ) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        val startTime = getFilterStartTime(filter)
        if (query.isBlank()) {
            if (startTime == 0L) {
                invoiceDao.getAllInvoices()
            } else {
                invoiceDao.getInvoicesSince(startTime)
            }
        } else {
            invoiceDao.searchInvoices(query.trim())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Invoices filtered by active status tab (Completed, Draft, All)
    val displayedInvoices: StateFlow<List<InvoiceEntity>> = combine(
        filteredInvoices,
        statusTab
    ) { invoices, tab ->
        when (tab) {
            InvoiceStatusTab.COMPLETED -> invoices.filter { it.status == InvoiceStatus.COMPLETED }
            InvoiceStatusTab.DRAFT -> invoices.filter { it.status == InvoiceStatus.DRAFT }
            InvoiceStatusTab.ALL -> invoices
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val draftCount: StateFlow<Int> = filteredInvoices.map { list ->
        list.count { it.status == InvoiceStatus.DRAFT }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedInvoices: StateFlow<List<InvoiceEntity>> = filteredInvoices.map { list ->
        list.filter { it.status == InvoiceStatus.COMPLETED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // KPI Metrics dynamically computed for COMPLETED sales only
    val totalSalesCount: StateFlow<Int> = completedInvoices.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSalesAmountPaise: StateFlow<Long> = completedInvoices.map { list ->
        list.sumOf { it.grandTotalPaise }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val avgBillAmountPaise: StateFlow<Long> = combine(totalSalesCount, totalSalesAmountPaise) { count, total ->
        if (count > 0) total / count else 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setDateFilter(filter: InvoiceDateFilter) {
        dateFilter.value = filter
    }

    fun setStatusTab(tab: InvoiceStatusTab) {
        statusTab.value = tab
    }

    suspend fun getCartItemsForInvoice(invoiceNumber: String): List<CartItem> {
        val items = invoiceDao.getInvoiceItems(invoiceNumber)
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

    fun discardDraft(invoiceNumber: String) {
        viewModelScope.launch {
            invoiceDao.deleteInvoice(invoiceNumber)
            _selectedInvoice.value = null
        }
    }

    fun viewInvoiceDetails(invoice: InvoiceEntity) {
        viewModelScope.launch {
            val items = invoiceDao.getInvoiceItems(invoice.invoiceNumber)
            _selectedInvoice.value = Pair(invoice, items)
        }
    }

    fun dismissInvoiceDetails() {
        _selectedInvoice.value = null
    }

    fun reprintInvoice(invoiceNumber: String) {
        if (_isPrinting.value) return
        _isPrinting.value = true
        _printMessage.value = null

        viewModelScope.launch {
            try {
                val invoice = invoiceDao.getInvoiceByNumber(invoiceNumber)
                if (invoice == null) {
                    _printMessage.value = "Invoice not found"
                    _isPrinting.value = false
                    return@launch
                }
                val items = invoiceDao.getInvoiceItems(invoiceNumber)
                val shop = shopRepository.getShopProfileSync()

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

                val result = printerRepository.printReceipt(shop, cartItems)
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
