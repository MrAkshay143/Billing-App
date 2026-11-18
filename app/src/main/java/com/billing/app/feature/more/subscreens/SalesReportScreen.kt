package com.billing.app.feature.more.subscreens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.billing.app.core.ui.util.FeedbackManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.billing.app.core.ui.components.SubPageHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.TextMutedColor
import com.billing.app.core.util.Money
import com.billing.app.feature.settings.SalesReportSummary
import com.billing.app.feature.settings.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToInvoices: () -> Unit
) {
    val context = LocalContext.current
    val shopProfile by viewModel.shopProfile.collectAsState()
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.TODAY) }
    var summary by remember { mutableStateOf<SalesReportSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun getPeriodTimestamps(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        return when (period) {
            ReportPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            ReportPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            ReportPeriod.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
        }
    }

    LaunchedEffect(selectedPeriod) {
        isLoading = true
        val (start, end) = getPeriodTimestamps(selectedPeriod)
        summary = viewModel.generateSalesReport(start, end)
        isLoading = false
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = "Sales Report",
                subtitle = "Daily & monthly sales figures",
                icon = Icons.Default.BarChart,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Period Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriod.values().forEach { period ->
                    val isSelected = selectedPeriod == period
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedPeriod = period },
                        color = if (isSelected) HeaderGradientStart else DokandaarTheme.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) HeaderGradientStart else DokandaarTheme.border),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = period.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else DokandaarTheme.textMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HeaderGradientStart)
                }
            } else if (summary != null) {
                val report = summary!!
                // Header Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AddProductBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = HeaderGradientStart, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Performance Summary (${selectedPeriod.label})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )
                            Text(
                                text = "Calculated from ${report.totalInvoices} sales in selected period",
                                fontSize = 12.sp,
                                color = TextMutedColor
                            )
                        }
                    }
                }

                // 4 KPI Cards (2x2 Grid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Revenue
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = Money.fromPaise(report.totalRevenuePaise).format(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                            Text(text = "Total Revenue", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF5B21B6))
                        }
                    }

                    // Total Invoices
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "${report.totalInvoices}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                            Text(text = "Total Sales", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF065F46))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Average Bill
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = Money.fromPaise(report.avgBillPaise).format(), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                            Text(text = "Avg. Bill Value", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF075985))
                        }
                    }

                    // Discounts Given
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = Money.fromPaise(report.totalDiscountPaise).format(), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                            Text(text = "Discounts Given", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF92400E))
                        }
                    }
                }

                // Payment Mode Breakdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Payment Method Breakdown",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Cash
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cash Collected", fontSize = 13.sp, color = DokandaarTheme.textPrimary)
                            }
                            Text(text = Money.fromPaise(report.cashSalesPaise).format(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        HorizontalDivider(color = DokandaarTheme.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))

                        // UPI
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = HeaderGradientStart, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("UPI Payments", fontSize = 13.sp, color = DokandaarTheme.textPrimary)
                            }
                            Text(text = Money.fromPaise(report.upiSalesPaise).format(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        if (report.otherSalesPaise > 0L) {
                            HorizontalDivider(color = DokandaarTheme.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))
                            // Other
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Other / Card", fontSize = 13.sp, color = DokandaarTheme.textPrimary)
                                }
                                Text(text = Money.fromPaise(report.otherSalesPaise).format(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Actions: Copy Report & View Invoices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val reportText = """
                                    --- DOKANDAAR SALES REPORT ---
                                    Store: ${shopProfile.name}
                                    Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())}
                                    Total Sales: ${report.totalInvoices}
                                    Total Revenue: ${Money.fromPaise(report.totalRevenuePaise).format()}
                                    Discounts: ${Money.fromPaise(report.totalDiscountPaise).format()}
                                    Cash Collected: ${Money.fromPaise(report.cashSalesPaise).format()}
                                    UPI Collected: ${Money.fromPaise(report.upiSalesPaise).format()}
                                    Average Bill: ${Money.fromPaise(report.avgBillPaise).format()}
                                """.trimIndent()

                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Sales Report", reportText))
                                FeedbackManager.showToast(context, "Report copied to clipboard!")
                            },
                        color = AddProductBg
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = HeaderGradientStart, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Report", color = HeaderGradientStart, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToInvoices() },
                        color = HeaderGradientStart
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("View Invoices", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
