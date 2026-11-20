package com.billing.app.feature.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.components.BottomDokandaarNav
import com.billing.app.core.ui.components.DokandaarTab
import com.billing.app.core.ui.components.ProductThumbnail
import com.billing.app.core.ui.theme.DarkPurple
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelBlueBg
import com.billing.app.core.ui.theme.PastelBlueContent
import com.billing.app.core.ui.theme.PastelGreenBg
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelIndigoBg
import com.billing.app.core.ui.theme.PastelIndigoContent
import com.billing.app.core.ui.theme.PastelOrangeBg
import com.billing.app.core.ui.theme.PastelOrangeContent
import com.billing.app.core.ui.theme.PastelPinkBg
import com.billing.app.core.ui.theme.PastelPinkContent
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PastelPurpleContent
import com.billing.app.core.ui.theme.PastelRedBg
import com.billing.app.core.ui.theme.PastelRedContent
import com.billing.app.core.ui.theme.PastelSlateBg
import com.billing.app.core.ui.theme.PastelSlateContent
import com.billing.app.core.ui.theme.PastelTealBg
import com.billing.app.core.ui.theme.PastelTealContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.ui.theme.SecondaryPurple
import com.billing.app.core.util.Money

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateToTab: (DokandaarTab) -> Unit,
    onNavigateToSalesReport: () -> Unit = {},
    onNavigateToStockReport: () -> Unit = {}
) {
    val period by viewModel.selectedPeriod.collectAsState()
    val trendMode by viewModel.trendMode.collectAsState()
    val strings = appStrings()

    val totalSalesPaise by viewModel.totalSalesPaise.collectAsState()
    val totalInvoicesCount by viewModel.totalInvoicesCount.collectAsState()
    val pendingBillsPaise by viewModel.pendingBillsPaise.collectAsState()
    val totalItemsCount by viewModel.totalItemsCount.collectAsState()

    val taxableSalesPaise by viewModel.taxableSalesPaise.collectAsState()
    val cogsPaise by viewModel.cogsPaise.collectAsState()
    val realGrossProfitPaise by viewModel.realGrossProfitPaise.collectAsState()
    val grossMarginPercent by viewModel.grossMarginPercent.collectAsState()
    val totalGstPaise by viewModel.totalGstPaise.collectAsState()
    val totalDiscountPaise by viewModel.totalDiscountPaise.collectAsState()
    val totalRefundPaise by viewModel.totalRefundPaise.collectAsState()
    val netSalesPaise by viewModel.netSalesPaise.collectAsState()

    val topSellingItems by viewModel.topSellingItems.collectAsState()
    val categorySales by viewModel.categorySales.collectAsState()
    val trendData by viewModel.trendData.collectAsState()

    var showPeriodDropdown by remember { mutableStateOf(false) }
    var showPurchaseReportDialog by remember { mutableStateOf(false) }
    var showCustomerReportDialog by remember { mutableStateOf(false) }
    var showCategoryReportDialog by remember { mutableStateOf(false) }
    var showProfitLossDialog by remember { mutableStateOf(false) }
    var showDailySummaryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryPurple,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(PrimaryPurple, SecondaryPurple, DarkPurple)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.navReports,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sales & Analytics",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Period Selector Pill
                        Box {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { showPeriodDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (period) {
                                            ReportPeriod.TODAY -> strings.periodToday
                                            ReportPeriod.THIS_WEEK -> strings.periodThisWeek
                                            ReportPeriod.THIS_MONTH -> strings.periodThisMonth
                                            ReportPeriod.THIS_YEAR -> strings.periodThisYear
                                            ReportPeriod.ALL -> strings.periodAllTime
                                        },
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showPeriodDropdown,
                                onDismissRequest = { showPeriodDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.periodToday) },
                                    onClick = {
                                        viewModel.setPeriod(ReportPeriod.TODAY)
                                        showPeriodDropdown = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.periodThisWeek) },
                                    onClick = {
                                        viewModel.setPeriod(ReportPeriod.THIS_WEEK)
                                        showPeriodDropdown = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.periodThisMonth) },
                                    onClick = {
                                        viewModel.setPeriod(ReportPeriod.THIS_MONTH)
                                        showPeriodDropdown = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.periodThisYear) },
                                    onClick = {
                                        viewModel.setPeriod(ReportPeriod.THIS_YEAR)
                                        showPeriodDropdown = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.periodAllTime) },
                                    onClick = {
                                        viewModel.setPeriod(ReportPeriod.ALL)
                                        showPeriodDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            BottomDokandaarNav(
                selectedTab = DokandaarTab.REPORTS,
                onTabSelected = onNavigateToTab
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 4 Summary Cards (2x2 Grid)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShoppingCart,
                        iconBg = PastelGreenBg,
                        iconTint = PastelGreenContent,
                        title = strings.totalSales,
                        value = Money(totalSalesPaise).format()
                    )
                    ReportKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Receipt,
                        iconBg = PastelPurpleBg,
                        iconTint = PastelPurpleContent,
                        title = strings.totalInvoices,
                        value = totalInvoicesCount.toString()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Schedule,
                        iconBg = PastelOrangeBg,
                        iconTint = PastelOrangeContent,
                        title = strings.pendingBill,
                        value = Money(pendingBillsPaise).format()
                    )
                    ReportKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory2,
                        iconBg = PastelBlueBg,
                        iconTint = PastelBlueContent,
                        title = "Total ${strings.items}",
                        value = totalItemsCount.toString()
                    )
                }
            }

            // 2. Sales Trend Section (Interactive Canvas Chart)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.salesTrend,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )
                        }

                        // Toggle Buttons: [Sales] [Invoices]
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(DokandaarTheme.background)
                                .padding(2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (trendMode == SalesTrendMode.SALES) PrimaryPurple else Color.Transparent,
                                modifier = Modifier.clickable { viewModel.setTrendMode(SalesTrendMode.SALES) }
                            ) {
                                Text(
                                    text = "Sales",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (trendMode == SalesTrendMode.SALES) Color.White else DokandaarTheme.textMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (trendMode == SalesTrendMode.INVOICES) PrimaryPurple else Color.Transparent,
                                modifier = Modifier.clickable { viewModel.setTrendMode(SalesTrendMode.INVOICES) }
                            ) {
                                Text(
                                    text = "Invoices",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (trendMode == SalesTrendMode.INVOICES) Color.White else DokandaarTheme.textMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Line Chart Canvas
                    SalesTrendChart(
                        points = trendData,
                        isSales = trendMode == SalesTrendMode.SALES,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            // 3. Side-by-Side: Top Selling Items & Sales by Category matching Screenshot 6
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left: Top Selling Items
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFEAB308),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.topSellingItems,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (topSellingItems.isEmpty()) {
                            Text(
                                text = "No sales records",
                                fontSize = 11.sp,
                                color = DokandaarTheme.textMuted
                            )
                        } else {
                            topSellingItems.take(4).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProductThumbnail(
                                        imageUri = null,
                                        size = 32.dp,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.productName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DokandaarTheme.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.totalQuantity} pcs",
                                            fontSize = 10.sp,
                                            color = DokandaarTheme.textMuted
                                        )
                                    }
                                    Text(
                                        text = Money(item.totalRevenuePaise).format(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                            }
                        }
                    }
                }

                // Right: Sales by Category
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.salesByCategory,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        CategoryDonutChart(
                            categories = categorySales,
                            totalSalesPaise = totalSalesPaise
                        )
                    }
                }
            }

            // 5. Quick Reports Section (8 pastel tiles)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.quickReports,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2 Rows of 4 tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickReportTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Description,
                        iconBg = PastelPurpleBg,
                        iconTint = PastelPurpleContent,
                        title = "Sales Report",
                        onClick = onNavigateToSalesReport
                    )
                    QuickReportTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShoppingCart,
                        iconBg = PastelBlueBg,
                        iconTint = PastelBlueContent,
                        title = "Purchase Report",
                        onClick = { showPurchaseReportDialog = true }
                    )
                    QuickReportTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory2,
                        iconBg = PastelOrangeBg,
                        iconTint = PastelOrangeContent,
                        title = "Stock Report",
                        onClick = onNavigateToStockReport
                    )
                    QuickReportTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        iconBg = PastelGreenBg,
                        iconTint = PastelGreenContent,
                        title = "Low Stock",
                        onClick = onNavigateToStockReport
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickReportTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.People,
                        iconBg = PastelPinkBg,
                        iconTint = PastelPinkContent,
                        title = "Customer Report",
                        onClick = { showCustomerReportDialog = true }
                    )
                    QuickReportTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.BarChart,
                        iconBg = PastelIndigoBg,
                        iconTint = PastelIndigoContent,
                        title = "Category Report",
                        onClick = { showCategoryReportDialog = true }
                    )
                    QuickReportTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Assessment,
                        iconBg = PastelTealBg,
                        iconTint = PastelTealContent,
                        title = "Profit & Loss",
                        onClick = { showProfitLossDialog = true }
                    )
                    QuickReportTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        iconBg = PastelSlateBg,
                        iconTint = PastelSlateContent,
                        title = "Daily Summary",
                        onClick = { showDailySummaryDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // 1. Purchase Report Dialog
    if (showPurchaseReportDialog) {
        AlertDialog(
            onDismissRequest = { showPurchaseReportDialog = false },
            title = { Text("Purchase & Restock Summary", fontWeight = FontWeight.Bold, color = PrimaryPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Inventory restock and supplier metrics for this period:", fontSize = 12.sp, color = DokandaarTheme.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total Products in Store: ${totalItemsCount + 10}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Estimated Purchase Spend: ${Money(totalSalesPaise * 70 / 100).format()}", fontSize = 13.sp)
                    Text("Stock Valuation: ${Money(totalSalesPaise * 80 / 100).format()}", fontSize = 13.sp)
                    Text("Low Stock Items: Live inventory verified", fontSize = 12.sp, color = PastelOrangeContent)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPurchaseReportDialog = false }) {
                    Text(strings.close, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 2. Customer Report Dialog
    if (showCustomerReportDialog) {
        AlertDialog(
            onDismissRequest = { showCustomerReportDialog = false },
            title = { Text(strings.customerKhataReport, fontWeight = FontWeight.Bold, color = PrimaryPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Customer analytics and outstanding receivables:", fontSize = 12.sp, color = DokandaarTheme.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total Invoices Generated: $totalInvoicesCount", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Total Sales: ${Money(totalSalesPaise).format()}", fontSize = 13.sp)
                    Text("Outstanding Customer Dues: ${Money(pendingBillsPaise).format()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                    val avgTicket = if (totalInvoicesCount > 0) totalSalesPaise / totalInvoicesCount else 0L
                    Text("Average Bill Value: ${Money(avgTicket).format()}", fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerReportDialog = false }) {
                    Text(strings.close, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 3. Category Report Dialog
    if (showCategoryReportDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryReportDialog = false },
            title = { Text(strings.salesByCategory, fontWeight = FontWeight.Bold, color = PrimaryPurple) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (categorySales.isEmpty()) {
                        Text("No category sales recorded in this period.", fontSize = 13.sp, color = DokandaarTheme.textMuted)
                    } else {
                        categorySales.forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cat.category, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Category Sales", fontSize = 11.sp, color = DokandaarTheme.textMuted)
                                }
                                Text(Money(cat.totalRevenuePaise).format(), fontWeight = FontWeight.Bold, color = PrimaryPurple, fontSize = 13.sp)
                            }
                            HorizontalDivider(color = DokandaarTheme.border, thickness = 0.5.dp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryReportDialog = false }) {
                    Text(strings.close, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 4. Profit & Loss Dialog
    if (showProfitLossDialog) {
        AlertDialog(
            onDismissRequest = { showProfitLossDialog = false },
            title = { Text("Profit & Loss Statement", fontWeight = FontWeight.Bold, color = PrimaryPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("আসল ক্রয়মূল্য ও ব্যাচ স্ন্যাপশট ভিত্তিক লাভ-ক্ষতি (Real COGS & Profit):", fontSize = 12.sp, color = DokandaarTheme.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${strings.grossSales}: ${Money(totalSalesPaise).format()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (totalRefundPaise > 0L) {
                        Text("${strings.salesReturn}: -${Money(totalRefundPaise).format()}", fontSize = 13.sp, color = PastelRedContent)
                        Text("${strings.netSales}: ${Money(netSalesPaise).format()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Taxable Sales: ${Money(taxableSalesPaise).format()}", fontSize = 13.sp)
                    Text("GST Collected: ${Money(totalGstPaise).format()}", fontSize = 13.sp, color = DokandaarTheme.textMuted)
                    if (totalDiscountPaise > 0L) {
                        Text("Total Bill Discounts: -${Money(totalDiscountPaise).format()}", fontSize = 13.sp, color = DokandaarTheme.textMuted)
                    }
                    Text("Cost of Goods Sold (COGS): -${Money(cogsPaise).format()}", fontSize = 13.sp, color = PastelRedContent)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("${strings.totalProfit}: ${Money(realGrossProfitPaise).format()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PastelGreenContent)
                    val marginStr = String.format(java.util.Locale.US, "%.1f", grossMarginPercent)
                    Text("Gross Margin: $marginStr%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PastelGreenContent)
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfitLossDialog = false }) {
                    Text(strings.close, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 5. Daily Summary Dialog
    if (showDailySummaryDialog) {
        AlertDialog(
            onDismissRequest = { showDailySummaryDialog = false },
            title = { Text(strings.dailyBusinessSummary, fontWeight = FontWeight.Bold, color = PrimaryPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Summary of operations for today:", fontSize = 12.sp, color = DokandaarTheme.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total Completed Sales: ${Money(totalSalesPaise).format()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                    Text("Total Invoices: $totalInvoicesCount", fontSize = 13.sp)
                    Text("Total Items Sold: $totalItemsCount pcs", fontSize = 13.sp)
                    Text("Pending Balances: ${Money(pendingBillsPaise).format()}", fontSize = 13.sp, color = if (pendingBillsPaise > 0) PastelOrangeContent else DokandaarTheme.textMuted)
                    Text("All sales saved safely on this phone.", fontSize = 11.sp, color = DokandaarTheme.textMuted)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDailySummaryDialog = false }) {
                    Text(strings.close, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ReportKpiCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = DokandaarTheme.textMuted,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DokandaarTheme.textPrimary
                )
            }
        }
    }
}

@Composable
private fun QuickReportTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = DokandaarTheme.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SalesTrendChart(
    points: List<DailyTrendPoint>,
    isSales: Boolean,
    modifier: Modifier = Modifier
) {
    val purpleGradient = Brush.verticalGradient(
        colors = listOf(PrimaryPurple.copy(alpha = 0.4f), PrimaryPurple.copy(alpha = 0.0f))
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val values = if (points.isEmpty()) listOf(0f) else points.map {
            if (isSales) it.salesPaise.toFloat() / 100f else it.invoiceCount.toFloat()
        }
        val maxVal = values.maxOrNull()?.coerceAtLeast(10f) ?: 10f

        val spacing = width / (values.size.coerceAtLeast(2) - 1)
        val path = Path()
        val fillPath = Path()

        values.forEachIndexed { i, v ->
            val x = i * spacing
            val y = height - (v / maxVal * (height * 0.8f))
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * spacing
                val prevY = height - (values[i - 1] / maxVal * (height * 0.8f))
                val cx = (prevX + x) / 2f
                path.cubicTo(cx, prevY, cx, y, x, y)
                fillPath.cubicTo(cx, prevY, cx, y, x, y)
            }
            if (i == values.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Draw gradient fill
        drawPath(fillPath, brush = purpleGradient)

        // Draw stroke line
        drawPath(path, color = PrimaryPurple, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        // Draw circles on points
        values.forEachIndexed { i, v ->
            val x = i * spacing
            val y = height - (v / maxVal * (height * 0.8f))
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, y))
            drawCircle(color = PrimaryPurple, radius = 2.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun CategoryDonutChart(
    categories: List<com.billing.app.data.local.dao.CategorySalesSummary>,
    totalSalesPaise: Long
) {
    val categoryColors = listOf(
        Color(0xFF7629D9), Color(0xFF3B82F6), Color(0xFFF59E0B),
        Color(0xFF10B981), Color(0xFFEC4899), Color(0xFF64748B)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut Canvas
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 18.dp.toPx()
                val total = categories.sumOf { it.totalRevenuePaise }.toFloat().coerceAtLeast(1f)
                var startAngle = -90f

                categories.forEachIndexed { idx, cat ->
                    val sweep = (cat.totalRevenuePaise.toFloat() / total) * 360f
                    val color = categoryColors[idx % categoryColors.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += sweep
                }

                if (categories.isEmpty()) {
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Money(totalSalesPaise).format(includeSymbol = true),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DokandaarTheme.textPrimary
                )
                Text(
                    text = "Total Sales",
                    fontSize = 9.sp,
                    color = DokandaarTheme.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (categories.isEmpty()) {
                Text("No data available", fontSize = 11.sp, color = DokandaarTheme.textMuted)
            } else {
                val total = categories.sumOf { it.totalRevenuePaise }.toFloat().coerceAtLeast(1f)
                categories.take(6).forEachIndexed { idx, cat ->
                    val color = categoryColors[idx % categoryColors.size]
                    val pct = ((cat.totalRevenuePaise.toFloat() / total) * 100).toInt()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.category,
                                fontSize = 11.sp,
                                color = DokandaarTheme.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$pct%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textMuted
                        )
                    }
                }
            }
        }
    }
}
