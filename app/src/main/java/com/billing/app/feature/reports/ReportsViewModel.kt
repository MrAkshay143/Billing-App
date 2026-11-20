package com.billing.app.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.database.AppDatabase
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
    private val productRepository: ProductRepository,
    private val database: AppDatabase? = null
) : ViewModel() {

    val selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    val trendMode = MutableStateFlow(SalesTrendMode.SALES)

    // KPI Metrics
    val totalSalesPaise = MutableStateFlow(0L)
    val totalInvoicesCount = MutableStateFlow(0)
    val pendingBillsPaise = MutableStateFlow(0L)
    val totalItemsCount = MutableStateFlow(0)

    // Real Accounting & Profit Metrics
    val taxableSalesPaise = MutableStateFlow(0L)
    val cogsPaise = MutableStateFlow(0L)
    val realGrossProfitPaise = MutableStateFlow(0L)
    val grossMarginPercent = MutableStateFlow(0.0)
    val totalGstPaise = MutableStateFlow(0L)
    val totalDiscountPaise = MutableStateFlow(0L)
    val totalRefundPaise = MutableStateFlow(0L)
    val netSalesPaise = MutableStateFlow(0L)
    val returnsCount = MutableStateFlow(0)

    val topSellingItems = MutableStateFlow<List<TopProductSummary>>(emptyList())
    val categorySales = MutableStateFlow<List<CategorySalesSummary>>(emptyList())
    val trendData = MutableStateFlow<List<DailyTrendPoint>>(emptyList())

    private var reportLoadJob: kotlinx.coroutines.Job? = null

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
        reportLoadJob?.cancel()
        reportLoadJob = viewModelScope.launch {
            val (startTime, endTime) = getPeriodRange(period)

            launch {
                invoiceRepository.getInvoicesSince(startTime).collect { list ->
                    val completed = list.filter { it.status == InvoiceStatus.COMPLETED }
                    val grossSales = completed.sumOf { it.grandTotalPaise }
                    totalSalesPaise.value = grossSales
                    totalInvoicesCount.value = list.size
                    totalItemsCount.value = completed.sumOf { it.itemsCount }
                    pendingBillsPaise.value = completed.filter { it.duePaise > 0 }.sumOf { it.duePaise }
                    totalDiscountPaise.value = completed.sumOf { it.discountPaise }
                    totalGstPaise.value = completed.sumOf { it.taxPaise }

                    // Real COGS & Taxable Sales Calculation
                    var periodTaxable = 0L
                    var periodCogs = 0L

                    for (inv in completed) {
                        val items = database?.invoiceDao()?.getInvoiceItemsByInvoiceId(inv.id)
                            ?: invoiceRepository.getInvoiceItems(inv.invoiceNumber)

                        for (item in items) {
                            periodTaxable += item.taxableValuePaise

                            val batches = database?.invoiceItemBatchDao()?.getItemBatchListForInvoiceItem(item.id) ?: emptyList()
                            val itemCost = if (batches.isNotEmpty()) {
                                batches.sumOf { it.costPaise }
                            } else {
                                (item.quantityMilli * item.unitCostPaise + 500L) / 1000L
                            }
                            periodCogs += itemCost
                        }
                    }

                    taxableSalesPaise.value = periodTaxable
                    cogsPaise.value = periodCogs
                    val profit = periodTaxable - periodCogs
                    realGrossProfitPaise.value = profit
                    grossMarginPercent.value = if (periodTaxable > 0L) {
                        (profit.toDouble() / periodTaxable.toDouble()) * 100.0
                    } else {
                        0.0
                    }

                    // Returns & Refunds
                    val returns = database?.returnDao()?.getAllReturnList() ?: emptyList()
                    val periodReturns = returns.filter { it.createdAt >= startTime }
                    returnsCount.value = periodReturns.size
                    val refundTotal = periodReturns.sumOf { it.totalRefundPaise }
                    totalRefundPaise.value = refundTotal
                    netSalesPaise.value = (grossSales - refundTotal).coerceAtLeast(0L)

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
