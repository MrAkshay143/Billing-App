package com.billing.app.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.util.Money
import com.billing.app.data.local.dao.CategorySalesSummary
import com.billing.app.data.local.dao.TopProductSummary
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.repository.InvoiceRepository
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportPeriod {
    TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, ALL
}

enum class SalesTrendMode {
    SALES, INVOICES
}

data class DailyTrendPoint(
    val dayLabel: String,
    val salesPaise: Long,
    val invoiceCount: Int
)

class ReportsViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    val selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    val trendMode = MutableStateFlow(SalesTrendMode.SALES)

    // KPI Metrics
    val totalSalesPaise = MutableStateFlow(0L)
    val totalInvoicesCount = MutableStateFlow(0)
    val pendingBillsPaise = MutableStateFlow(0L)
    val totalItemsCount = MutableStateFlow(0)

    val topSellingItems = MutableStateFlow<List<TopProductSummary>>(emptyList())
    val categorySales = MutableStateFlow<List<CategorySalesSummary>>(emptyList())
    val trendData = MutableStateFlow<List<DailyTrendPoint>>(emptyList())

    init {
        loadReportData(ReportPeriod.THIS_MONTH)
    }

    fun setPeriod(period: ReportPeriod) {
        selectedPeriod.value = period
        loadReportData(period)
    }

    fun setTrendMode(mode: SalesTrendMode) {
        trendMode.value = mode
    }

    private fun loadReportData(period: ReportPeriod) {
        viewModelScope.launch {
            val (startTime, endTime) = getPeriodRange(period)

            launch {
                invoiceRepository.getInvoicesSince(startTime).collect { list ->
                    val completed = list.filter { it.status == InvoiceStatus.COMPLETED }
                    totalSalesPaise.value = completed.sumOf { it.grandTotalPaise }
                    totalInvoicesCount.value = list.size
                    totalItemsCount.value = completed.sumOf { it.itemsCount }
                    pendingBillsPaise.value = completed.filter { it.duePaise > 0 }.sumOf { it.duePaise }

                    // Group by days for trend
                    val dayMap = mutableMapOf<String, Pair<Long, Int>>()
                    val calendar = Calendar.getInstance()
                    completed.forEach { inv ->
                        calendar.timeInMillis = inv.timestamp
                        val day = "${calendar.get(Calendar.DAY_OF_MONTH)} ${calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, java.util.Locale.getDefault())}"
                        val prev = dayMap.getOrDefault(day, Pair(0L, 0))
                        dayMap[day] = Pair(prev.first + inv.grandTotalPaise, prev.second + 1)
                    }

                    if (dayMap.isEmpty()) {
                        trendData.value = listOf(
                            DailyTrendPoint("1", 0L, 0),
                            DailyTrendPoint("5", 0L, 0),
                            DailyTrendPoint("10", 0L, 0),
                            DailyTrendPoint("15", 0L, 0),
                            DailyTrendPoint("20", 0L, 0),
                            DailyTrendPoint("25", 0L, 0),
                            DailyTrendPoint("30", 0L, 0)
                        )
                    } else {
                        trendData.value = dayMap.entries.map { (day, pair) ->
                            DailyTrendPoint(day, pair.first, pair.second)
                        }
                    }
                }
            }

            launch {
                invoiceRepository.getTopSellingProducts(startTime, endTime, 5).collect {
                    topSellingItems.value = it
                }
            }

            launch {
                invoiceRepository.getSalesByCategory(startTime, endTime).collect {
                    categorySales.value = it
                }
            }
        }
    }

    private fun getPeriodRange(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis

        val startTime = when (period) {
            ReportPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            ReportPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.timeInMillis
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.timeInMillis
            }
            ReportPeriod.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.timeInMillis
            }
            ReportPeriod.ALL -> 0L
        }
        return Pair(startTime, endTime)
    }
}
