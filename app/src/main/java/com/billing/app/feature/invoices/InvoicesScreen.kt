package com.billing.app.feature.invoices

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.components.BottomDokandaarNav
import com.billing.app.core.ui.components.DokandaarTab
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.DarkPurple
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.PastelBlueBg
import com.billing.app.core.ui.theme.PastelBlueContent
import com.billing.app.core.ui.theme.PastelGreenBg
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelOrangeBg
import com.billing.app.core.ui.theme.PastelOrangeContent
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PastelPurpleContent
import com.billing.app.core.ui.theme.PastelRedBg
import com.billing.app.core.ui.theme.PastelRedContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.ui.theme.SecondaryPurple
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.model.CartItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicesScreen(
    viewModel: InvoiceViewModel,
    onNavigateToTab: (DokandaarTab) -> Unit,
    onNavigateToHome: () -> Unit = { onNavigateToTab(DokandaarTab.HOME) },
    onNavigateToSettings: () -> Unit = { onNavigateToTab(DokandaarTab.MORE) },
    onNavigateToSalesReport: () -> Unit = onNavigateToSettings,
    onResumeDraft: (List<CartItem>, String) -> Unit = { _, _ -> },
    onNavigateToNewSale: () -> Unit = { onNavigateToTab(DokandaarTab.HOME) }
) {
    val invoices by viewModel.filteredInvoices.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val statusTab by viewModel.statusTab.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("All Invoices", "Sales", "Purchase", "Return")

    var selectedInvoiceForDetails by remember { mutableStateOf<InvoiceEntity?>(null) }
    var showDateFilterMenu by remember { mutableStateOf(false) }

    // Summary KPIs
    val todaySalesAmount = invoices.filter { it.status == InvoiceStatus.COMPLETED }.sumOf { it.grandTotalPaise }
    val invoicesCount = invoices.size
    val pendingBillsAmount = invoices.filter { it.status == InvoiceStatus.COMPLETED && it.duePaise > 0 }.sumOf { it.duePaise }
    val totalItemsCount = invoices.sumOf { it.itemsCount }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

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
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
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
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Invoices",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Kirana Store POS",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "+ New Invoice" Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                modifier = Modifier.clickable { onNavigateToNewSale() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "New Invoice",
                                        color = PrimaryPurple,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            BottomDokandaarNav(
                selectedTab = DokandaarTab.INVOICES,
                onTabSelected = onNavigateToTab
            )
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 4 Filter Tabs: All Invoices | Sales | Purchase | Return
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DokandaarTheme.cardBackground,
                contentColor = PrimaryPurple,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = PrimaryPurple,
                        height = 3.dp
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            when (index) {
                                0 -> viewModel.statusTab.value = InvoiceStatusTab.ALL
                                1 -> viewModel.statusTab.value = InvoiceStatusTab.COMPLETED
                                2 -> viewModel.statusTab.value = InvoiceStatusTab.DRAFT
                                3 -> viewModel.statusTab.value = InvoiceStatusTab.DUES
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTabIndex == index) PrimaryPurple else DokandaarTheme.textMuted
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 KPI Summary Cards (2x2 Grid matching Screenshot 5)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InvoiceKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShoppingCart,
                        iconBg = PastelGreenBg,
                        iconTint = PastelGreenContent,
                        title = "Today Sales",
                        value = Money(todaySalesAmount).format()
                    )
                    InvoiceKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Receipt,
                        iconBg = PastelPurpleBg,
                        iconTint = PastelPurpleContent,
                        title = "Invoices",
                        value = invoicesCount.toString()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InvoiceKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Schedule,
                        iconBg = PastelOrangeBg,
                        iconTint = PastelOrangeContent,
                        title = "Pending Bill",
                        value = Money(pendingBillsAmount).format()
                    )
                    InvoiceKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory2,
                        iconBg = PastelBlueBg,
                        iconTint = PastelBlueContent,
                        title = "Total Items",
                        value = totalItemsCount.toString()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Date Filter Dropdown Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = {
                        Text(
                            text = "Search by Invoice No., Customer...",
                            fontSize = 12.sp,
                            color = DokandaarTheme.textMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = DokandaarTheme.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = DokandaarTheme.cardBackground,
                        unfocusedContainerColor = DokandaarTheme.cardBackground
                    ),
                    singleLine = true
                )

                // Date Dropdown Button
                Box {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DokandaarTheme.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.clickable { showDateFilterMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (dateFilter) {
                                    InvoiceDateFilter.TODAY -> "Today"
                                    InvoiceDateFilter.THIS_WEEK -> "This Week"
                                    InvoiceDateFilter.THIS_MONTH -> "This Month"
                                    else -> "All"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DokandaarTheme.textPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = DokandaarTheme.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showDateFilterMenu,
                        onDismissRequest = { showDateFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Today") },
                            onClick = {
                                viewModel.dateFilter.value = InvoiceDateFilter.TODAY
                                showDateFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("This Week") },
                            onClick = {
                                viewModel.dateFilter.value = InvoiceDateFilter.THIS_WEEK
                                showDateFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("This Month") },
                            onClick = {
                                viewModel.dateFilter.value = InvoiceDateFilter.THIS_MONTH
                                showDateFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("All Time") },
                            onClick = {
                                viewModel.dateFilter.value = InvoiceDateFilter.ALL
                                showDateFilterMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Invoice List Cards
            if (invoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No invoices found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DokandaarTheme.textMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(invoices, key = { it.invoiceNumber }) { invoice ->
                        InvoiceItemCard(
                            invoice = invoice,
                            formattedDate = dateFormat.format(Date(invoice.timestamp)),
                            onViewDetails = { selectedInvoiceForDetails = invoice },
                            onPrint = { viewModel.printInvoice(invoice.invoiceNumber) }
                        )
                    }
                }
            }
        }
    }

    // Invoice Details Dialog
    selectedInvoiceForDetails?.let { inv ->
        AlertDialog(
            onDismissRequest = { selectedInvoiceForDetails = null },
            title = {
                Text(
                    text = "Invoice ${inv.invoiceNumber}",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            },
            text = {
                Column {
                    Text("Customer: ${inv.customerName ?: "Walk-in Customer"}", fontWeight = FontWeight.SemiBold)
                    if (!inv.customerPhone.isNullOrBlank()) {
                        Text("Phone: ${inv.customerPhone}", fontSize = 13.sp, color = DokandaarTheme.textMuted)
                    }
                    Text("Date: ${dateFormat.format(Date(inv.timestamp))}", fontSize = 13.sp, color = DokandaarTheme.textMuted)
                    Text("Items: ${inv.itemsCount}", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Subtotal: ${Money(inv.subtotalPaise).format()}", fontSize = 13.sp)
                    Text("Discount: -${Money(inv.discountPaise).format()}", fontSize = 13.sp, color = PastelGreenContent)
                    Text("Grand Total: ${Money(inv.grandTotalPaise).format()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                    Text("Payment: ${inv.paymentMethod} (${inv.status.name})", fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.printInvoice(inv.invoiceNumber)
                        selectedInvoiceForDetails = null
                    }
                ) {
                    Text("Print Receipt", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedInvoiceForDetails = null }) {
                    Text("Close", color = DokandaarTheme.textMuted)
                }
            }
        )
    }
}

@Composable
private fun InvoiceKpiCard(
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
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
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
private fun InvoiceItemCard(
    invoice: InvoiceEntity,
    formattedDate: String,
    onViewDetails: () -> Unit,
    onPrint: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Purple Document Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PastelPurpleBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Invoice details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invoice.invoiceNumber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DokandaarTheme.textPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = invoice.customerName?.ifBlank { "Walk-in Customer" } ?: "Walk-in Customer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DokandaarTheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!invoice.customerPhone.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = invoice.customerPhone,
                            fontSize = 11.sp,
                            color = DokandaarTheme.textMuted
                        )
                    }
                }
            }

            // Date, Items & Badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = DokandaarTheme.textMuted
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${invoice.itemsCount} Items",
                    fontSize = 11.sp,
                    color = DokandaarTheme.textMuted
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Payment Status Badge
                val (badgeBg, badgeText, badgeColor) = when {
                    invoice.status == InvoiceStatus.DRAFT -> Triple(PastelBlueBg, "Draft", PastelBlueContent)
                    invoice.status == InvoiceStatus.CANCELLED -> Triple(PastelRedBg, "Cancelled", PastelRedContent)
                    invoice.duePaise > 0 -> Triple(PastelOrangeBg, "Pending", PastelOrangeContent)
                    else -> Triple(PastelGreenBg, "Paid", PastelGreenContent)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Amount & 3-dots
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                menuExpanded = false
                                onViewDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Print Receipt") },
                            onClick = {
                                menuExpanded = false
                                onPrint()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = Money(invoice.grandTotalPaise).format(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            }
        }
    }
}
