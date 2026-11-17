package com.billing.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.util.Money
import com.billing.app.domain.repository.CustomerRepository
import com.billing.app.domain.repository.InvoiceRepository
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.ShopRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

enum class OverviewPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

class HomeViewModel(
    private val productRepository: ProductRepository,
    private val invoiceRepository: InvoiceRepository,
    private val customerRepository: CustomerRepository,
    private val shopRepository: ShopRepository? = null
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(OverviewPeriod.TODAY)
    val selectedPeriod: StateFlow<OverviewPeriod> = _selectedPeriod.asStateFlow()

    val storeName: StateFlow<String> = shopRepository?.getShopProfile()
        ?.map { it.name.ifBlank { "My Store" } }
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, "My Store")
        ?: MutableStateFlow("My Store").asStateFlow()

    val totalProducts: StateFlow<Int> = productRepository.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val totalCustomers: StateFlow<Int> = customerRepository.getCustomerCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val lowStockCount: StateFlow<Int> = productRepository.getLowStockCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalSales: StateFlow<Money> = _selectedPeriod.flatMapLatest { period ->
        val (start, end) = getTimestampsForPeriod(period)
        invoiceRepository.getTotalSalesBetween(start, end).map { Money(it) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Money.ZERO)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalInvoices: StateFlow<Int> = _selectedPeriod.flatMapLatest { period ->
        val (start, end) = getTimestampsForPeriod(period)
        invoiceRepository.getInvoiceCountBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun setPeriod(period: OverviewPeriod) {
        _selectedPeriod.value = period
    }

    private fun getTimestampsForPeriod(period: OverviewPeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        return when (period) {
            OverviewPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            OverviewPeriod.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            OverviewPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            OverviewPeriod.ALL_TIME -> {
                Pair(0L, now)
            }
        }
    }
}
