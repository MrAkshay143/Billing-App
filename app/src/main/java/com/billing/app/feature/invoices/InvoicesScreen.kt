package com.billing.app.feature.invoices

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.model.CartItem
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.components.BottomDokandaarNav
import com.billing.app.core.ui.components.DokandaarTab
import com.billing.app.core.ui.components.TopDokandaarHeader
import com.billing.app.core.ui.components.PinVerificationDialog
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.InStockText
import com.billing.app.core.ui.theme.TextMutedColor
import com.billing.app.core.ui.util.FeedbackManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.InvoiceEntity
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
    onResumeDraft: (List<CartItem>, String) -> Unit = { _, _ -> }
) {
    val invoices by viewModel.displayedInvoices.collectAsState()
    val activeStatusTab by viewModel.statusTab.collectAsState()
    val draftCount by viewModel.draftCount.collectAsState()
    val dueCount by viewModel.dueInvoicesCount.collectAsState()
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.dateFilter.collectAsState()
    val totalCount by viewModel.totalSalesCount.collectAsState()
    val totalSalesPaise by viewModel.totalSalesAmountPaise.collectAsState()
    val avgBillPaise by viewModel.avgBillAmountPaise.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val printMessage by viewModel.printMessage.collectAsState()
    val selectedInvoicePair by viewModel.selectedInvoice.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showVoidPinDialog by remember { mutableStateOf(false) }
    var invoiceToVoid by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(printMessage) {
        printMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearPrintMessage()
        }
    }

    // Invoice Details Dialog
    if (selectedInvoicePair != null) {
        val (invoice, items) = selectedInvoicePair!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissInvoiceDetails() },
            title = {
                Text(
                    text = "Invoice: ${invoice.invoiceNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(invoice.timestamp))}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Payment Mode: ${invoice.paymentMethod}",
                        fontSize = 12.sp,
                        color = HeaderGradientStart,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.productName}",
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Money.fromPaise(item.lineTotalPaise).format(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Grand Total:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = Money.fromPaise(invoice.grandTotalPaise).format(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = HeaderGradientStart
                        )
                    }

                    if (invoice.duePaise > 0L) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Balance Due:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFDC2626))
                            Text(
                                text = Money.fromPaise(invoice.duePaise).format(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (invoice.status == InvoiceStatus.DRAFT) {
                    TextButton(
                        onClick = {
                            val invNum = invoice.invoiceNumber
                            viewModel.dismissInvoiceDetails()
                            scope.launch {
                                val cartItems = viewModel.getCartItemsForInvoice(invNum)
                                onResumeDraft(cartItems, invNum)
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = HeaderGradientStart,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume Sale", color = HeaderGradientStart, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    TextButton(
                        onClick = {
                            viewModel.reprintInvoice(invoice.invoiceNumber)
                            viewModel.dismissInvoiceDetails()
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = HeaderGradientStart,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reprint", color = HeaderGradientStart, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                Row {
                    if (invoice.status == InvoiceStatus.DRAFT) {
                        TextButton(
                            onClick = {
                                viewModel.discardDraft(invoice.invoiceNumber)
                            }
                        ) {
                            Text("Discard Draft", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    } else if (invoice.status == InvoiceStatus.COMPLETED) {
                        if (invoice.duePaise > 0L) {
                            TextButton(
                                onClick = {
                                    viewModel.settleDue(invoice.invoiceNumber) {
                                        FeedbackManager.showToast(context, "Balance due settled!")
                                    }
                                }
                            ) {
                                Text("Settle Due", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(
                            onClick = {
                                if (isPinEnabled) {
                                    invoiceToVoid = invoice.invoiceNumber
                                    showVoidPinDialog = true
                                } else {
                                    viewModel.voidInvoice(invoice.invoiceNumber) {
                                        viewModel.dismissInvoiceDetails()
                                    }
                                }
                            }
                        ) {
                            Text("Void Sale", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { viewModel.dismissInvoiceDetails() }) {
                        Text("Close", color = Color.Gray)
                    }
                }
            }
        )
    }

    if (showVoidPinDialog && invoiceToVoid != null) {
        PinVerificationDialog(
            title = "Verify Owner PIN",
            subtitle = "Enter PIN to void invoice $invoiceToVoid and restore stock to inventory",
            onDismissRequest = {
                showVoidPinDialog = false
                invoiceToVoid = null
            },
            onVerify = { pin, onError ->
                scope.launch {
                    if (viewModel.verifySecurityPin(pin)) {
                        showVoidPinDialog = false
                        invoiceToVoid?.let { num ->
                            viewModel.voidInvoice(num) {
                                viewModel.dismissInvoiceDetails()
                            }
                        }
                        invoiceToVoid = null
                    } else {
                        onError("Incorrect PIN. Please try again.")
                    }
                }
            }
        )
    }

    val strings = appStrings()

    Scaffold(
        topBar = {
            TopDokandaarHeader(
                storeName = "Dokandaar",
                subtitle = strings.appSubtitle,
                onNotificationClick = { /* Already on Invoices */ },
                onMenuClick = onNavigateToSettings
            )
        },
        bottomBar = {
            BottomDokandaarNav(
                selectedTab = DokandaarTab.INVOICES,
                onTabSelected = onNavigateToTab
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Invoices Header Card (Mockup 2.4)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AddProductBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = HeaderGradientStart,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.invoicesTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                        Text(
                            text = strings.invoicesSubtitle,
                            fontSize = 12.sp,
                            color = TextMutedColor
                        )
                    }

                    // Sales Report Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onNavigateToSalesReport() },
                        color = HeaderGradientStart,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sales Report",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1.5 Status Tabs Row: Completed, Drafts, Dues, All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusTabPill(
                    label = strings.tabCompleted,
                    isSelected = activeStatusTab == InvoiceStatusTab.COMPLETED,
                    onClick = { viewModel.setStatusTab(InvoiceStatusTab.COMPLETED) }
                )
                StatusTabPill(
                    label = strings.tabDrafts,
                    badgeCount = draftCount,
                    isSelected = activeStatusTab == InvoiceStatusTab.DRAFT,
                    onClick = { viewModel.setStatusTab(InvoiceStatusTab.DRAFT) }
                )
                StatusTabPill(
                    label = "Dues",
                    badgeCount = dueCount,
                    isSelected = activeStatusTab == InvoiceStatusTab.DUES,
                    onClick = { viewModel.setStatusTab(InvoiceStatusTab.DUES) }
                )
                StatusTabPill(
                    label = strings.tabAll,
                    isSelected = activeStatusTab == InvoiceStatusTab.ALL,
                    onClick = { viewModel.setStatusTab(InvoiceStatusTab.ALL) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Search Bar with Calendar Icon
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = {
                    Text(
                        text = strings.searchInvoices,
                        fontSize = 13.sp,
                        color = TextMutedColor
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Calendar Filter",
                        tint = HeaderGradientStart,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(22.dp)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = HeaderGradientStart,
                    unfocusedBorderColor = BorderColor
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Date Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateFilterPill(
                    label = "All",
                    isSelected = activeFilter == InvoiceDateFilter.ALL,
                    onClick = { viewModel.setDateFilter(InvoiceDateFilter.ALL) }
                )
                DateFilterPill(
                    label = "Today",
                    icon = Icons.Default.CalendarMonth,
                    isSelected = activeFilter == InvoiceDateFilter.TODAY,
                    onClick = { viewModel.setDateFilter(InvoiceDateFilter.TODAY) }
                )
                DateFilterPill(
                    label = "This Week",
                    icon = Icons.Default.CalendarMonth,
                    isSelected = activeFilter == InvoiceDateFilter.THIS_WEEK,
                    onClick = { viewModel.setDateFilter(InvoiceDateFilter.THIS_WEEK) }
                )
                DateFilterPill(
                    label = "This Month",
                    icon = Icons.Default.CalendarMonth,
                    isSelected = activeFilter == InvoiceDateFilter.THIS_MONTH,
                    onClick = { viewModel.setDateFilter(InvoiceDateFilter.THIS_MONTH) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. 3 KPI Metric Cards Row (Total Sales, Total Amount, Avg. Bill)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Total Sales
                KpiMetricCard(
                    icon = Icons.Default.ShoppingCart,
                    iconTint = Color(0xFF10B981),
                    value = "$totalCount",
                    label = "Total Sales",
                    bgColor = Color(0xFFECFDF5),
                    borderColor = Color(0xFFA7F3D0),
                    modifier = Modifier.weight(1f)
                )

                // Card 2: Total Amount
                KpiMetricCard(
                    icon = Icons.Default.Payments,
                    iconTint = Color(0xFF7C3AED),
                    value = Money.fromPaise(totalSalesPaise).format(),
                    label = "Total Amount",
                    bgColor = Color(0xFFF3E8FF),
                    borderColor = Color(0xFFDDD6FE),
                    modifier = Modifier.weight(1.3f)
                )

                // Card 3: Avg. Bill
                KpiMetricCard(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    iconTint = Color(0xFF0284C7),
                    value = Money.fromPaise(avgBillPaise).format(),
                    label = "Avg. Bill",
                    bgColor = Color(0xFFE0F2FE),
                    borderColor = Color(0xFFBAE6FD),
                    modifier = Modifier.weight(1.1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Invoices List Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Invoices ($totalCount)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Sort: Newest ⌵",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HeaderGradientStart
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Invoices List
            if (invoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No invoices found for selected period." else "No invoices match '$searchQuery'",
                        color = TextMutedColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(invoices, key = { it.invoiceNumber }) { invoice ->
                        InvoiceItemCard(
                            invoice = invoice,
                            onViewDetails = { viewModel.viewInvoiceDetails(invoice) },
                            onReprint = { viewModel.reprintInvoice(invoice.invoiceNumber) },
                            onResumeDraft = {
                                scope.launch {
                                    val cartItems = viewModel.getCartItemsForInvoice(invoice.invoiceNumber)
                                    onResumeDraft(cartItems, invoice.invoiceNumber)
                                }
                            },
                            onDiscardDraft = {
                                viewModel.discardDraft(invoice.invoiceNumber)
                            },
                            isPrinting = isPrinting
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DateFilterPill(
    label: String,
    icon: ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) HeaderGradientStart else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) HeaderGradientStart else BorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF475569)
            )
        }
    }
}

@Composable
private fun KpiMetricCard(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    bgColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
        color = bgColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569)
            )
        }
    }
}

@Composable
private fun StatusTabPill(
    label: String,
    badgeCount: Int = 0,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) HeaderGradientStart else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) HeaderGradientStart else BorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF475569)
            )
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color(0xFFF59E0B))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$badgeCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) HeaderGradientStart else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoiceItemCard(
    invoice: InvoiceEntity,
    onViewDetails: () -> Unit,
    onReprint: () -> Unit,
    onResumeDraft: () -> Unit = {},
    onDiscardDraft: () -> Unit = {},
    isPrinting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Receipt Icon Box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (invoice.status == InvoiceStatus.DRAFT) Color(0xFFFEF3C7) else AddProductBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = if (invoice.status == InvoiceStatus.DRAFT) Color(0xFFD97706) else HeaderGradientStart,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Invoice number & date
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = invoice.invoiceNumber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary
                    )
                    if (invoice.status == InvoiceStatus.DRAFT) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DRAFT",
                                color = Color(0xFFD97706),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (invoice.status == InvoiceStatus.CANCELLED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CANCELLED",
                                color = Color(0xFFDC2626),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (invoice.status == InvoiceStatus.COMPLETED && invoice.duePaise > 0L) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DUE: ${Money.fromPaise(invoice.duePaise).format()}",
                                color = Color(0xFFDC2626),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(invoice.timestamp)),
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Amount
            Text(
                text = Money.fromPaise(invoice.grandTotalPaise).format(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DokandaarTheme.textPrimary
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Action Icons
            if (invoice.status == InvoiceStatus.DRAFT) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Resume Sale button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7))
                            .clickable { onResumeDraft() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume Draft",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Discard button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2))
                            .clickable { onDiscardDraft() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Discard Draft",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // View details
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AddProductBg)
                            .clickable { onViewDetails() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "View Details",
                            tint = HeaderGradientStart,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AddProductBg)
                            .clickable { onViewDetails() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "View Invoice",
                            tint = HeaderGradientStart,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AddProductBg)
                            .clickable { onReprint() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Reprint",
                            tint = HeaderGradientStart,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
