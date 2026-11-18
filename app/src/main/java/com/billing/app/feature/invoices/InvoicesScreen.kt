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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.billing.app.core.pagination.OnBottomReached
import com.billing.app.core.pagination.LazyLoadingFooter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.core.ui.components.BottomDokandaarNav
import com.billing.app.core.ui.components.DokandaarActionPill
import com.billing.app.core.ui.components.DokandaarDateFilterPill
import com.billing.app.core.ui.components.DokandaarInvoiceCard
import com.billing.app.core.ui.components.DokandaarKpiFourRow
import com.billing.app.core.ui.components.DokandaarSearchBar
import com.billing.app.core.ui.components.DokandaarStatCard
import com.billing.app.core.ui.components.DokandaarTab
import com.billing.app.core.ui.theme.DarkPurple
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import kotlinx.coroutines.launch
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
    onResumeDraft: (List<CartItem>, InvoiceEntity) -> Unit = { _, _ -> },
    onNavigateToNewSale: () -> Unit = { onNavigateToTab(DokandaarTab.HOME) }
) {
    val invoices by viewModel.filteredInvoices.collectAsState()
    val visibleCount by viewModel.visibleItemCount.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val displayedInvoices = remember(invoices, visibleCount) {
        invoices.take(visibleCount)
    }
    val hasMore = visibleCount < invoices.size

    val listState = rememberLazyListState()
    listState.OnBottomReached(buffer = 5, enabled = hasMore && !isLoadingMore) {
        viewModel.loadMoreInvoices()
    }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val statusTab by viewModel.statusTab.collectAsState()
    val strings = appStrings()

    val context = LocalContext.current
    val printMessage by viewModel.printMessage.collectAsState()

    LaunchedEffect(printMessage) {
        printMessage?.let { msg ->
            FeedbackManager.showToast(context, msg)
            viewModel.clearPrintMessage()
        }
    }

    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("All Invoices", "Sales", "Drafts (Hold)", "Credit Dues", "Return")

    var selectedInvoiceForDetails by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToVoid by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToDiscard by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToSettle by remember { mutableStateOf<InvoiceEntity?>(null) }
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.invoicesTitle,
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (invoices.isNotEmpty()) "${invoices.size} Bills Recorded" else strings.invoicesSubtitle,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Search Icon
                            IconButton(
                                onClick = { /* Search field is directly below */ },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }

                            // Filter Icon
                            IconButton(
                                onClick = { showDateFilterMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = Color.White
                                )
                            }

                            // "+ New Invoice" Button
                            DokandaarActionPill(
                                text = strings.newInvoice,
                                onClick = onNavigateToNewSale
                            )
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
            // 4 Filter Tabs: All Invoices | Sales | Purchase | Return matching Screenshot 5
            val filterTabs = listOf(
                Triple(strings.allInvoices, Icons.AutoMirrored.Filled.ReceiptLong, InvoiceStatusTab.ALL),
                Triple(strings.sales, Icons.Default.ShoppingCart, InvoiceStatusTab.COMPLETED),
                Triple(strings.purchase, Icons.Default.Inventory2, InvoiceStatusTab.DRAFT),
                Triple(strings.returns, Icons.Default.Undo, InvoiceStatusTab.RETURN)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterTabs) { (title, icon, tab) ->
                    val isSelected = statusTab == tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PastelPurpleBg else DokandaarTheme.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) PrimaryPurple else DokandaarTheme.border
                        ),
                        modifier = Modifier.clickable {
                            viewModel.statusTab.value = tab
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) PrimaryPurple else DokandaarTheme.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title,
                                color = if (isSelected) PrimaryPurple else DokandaarTheme.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 KPI Summary Cards in a Single Horizontal Row matching Screenshot 5
            DokandaarKpiFourRow {
                DokandaarStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Today Sales",
                    value = Money(todaySalesAmount).format(),
                    icon = Icons.Default.ShoppingCart,
                    iconTint = PastelGreenContent,
                    containerBg = PastelGreenBg
                )
                DokandaarStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Invoices",
                    value = invoicesCount.toString(),
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    iconTint = PastelPurpleContent,
                    containerBg = PastelPurpleBg
                )
                DokandaarStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Pending Bill",
                    value = Money(pendingBillsAmount).format(),
                    icon = Icons.Default.Schedule,
                    iconTint = PastelOrangeContent,
                    containerBg = PastelOrangeBg,
                    valueColor = Color(0xFFD97706)
                )
                DokandaarStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Items",
                    value = totalItemsCount.toString(),
                    icon = Icons.Default.Inventory2,
                    iconTint = PastelBlueContent,
                    containerBg = PastelBlueBg
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Date Filter Dropdown Row (Matched Height 44.dp & Non-breaking placeholder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DokandaarSearchBar(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = "Search by Invoice No., Customer, or Mobile...",
                    modifier = Modifier.weight(1f)
                )

                // Date Dropdown Button
                Box {
                    DokandaarDateFilterPill(
                        label = when (dateFilter) {
                            InvoiceDateFilter.TODAY -> "Today"
                            InvoiceDateFilter.THIS_WEEK -> "This Week"
                            InvoiceDateFilter.THIS_MONTH -> "This Month"
                            else -> "All"
                        },
                        onClick = { showDateFilterMenu = true }
                    )

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
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedInvoices, key = { it.invoiceNumber }) { invoice ->
                        DokandaarInvoiceCard(
                            invoice = invoice,
                            formattedDate = dateFormat.format(Date(invoice.timestamp)),
                            onViewDetails = { selectedInvoiceForDetails = invoice },
                            onPrint = { viewModel.printInvoice(invoice.invoiceNumber) },
                            onResumeDraft = {
                                scope.launch {
                                    val items = viewModel.getCartItemsForInvoice(invoice.invoiceNumber)
                                    onResumeDraft(items, invoice)
                                }
                            },
                            onDiscardDraft = { invoiceToDiscard = invoice },
                            onSettleDue = { invoiceToSettle = invoice },
                            onVoidInvoice = { invoiceToVoid = invoice }
                        )
                    }

                    item(key = "pagination_footer") {
                        LazyLoadingFooter(
                            isLoadingMore = isLoadingMore,
                            hasMore = hasMore,
                            totalCount = invoices.size,
                            itemLabel = "invoices"
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
            shape = RoundedCornerShape(16.dp),
            containerColor = DokandaarTheme.cardBackground,
            title = {
                Text(
                    text = "${strings.invoiceHash}${inv.invoiceNumber}",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            },
            text = {
                Column {
                    Text(
                        "${strings.billedTo}: ${inv.customerName ?: strings.walkInCustomer}",
                        fontWeight = FontWeight.SemiBold,
                        color = DokandaarTheme.textPrimary
                    )
                    if (!inv.customerPhone.isNullOrBlank()) {
                        Text("Phone: ${inv.customerPhone}", fontSize = 13.sp, color = DokandaarTheme.textMuted)
                    }
                    Text("Date: ${dateFormat.format(Date(inv.timestamp))}", fontSize = 13.sp, color = DokandaarTheme.textMuted)
                    Text("${strings.items}: ${inv.itemsCount}", fontSize = 13.sp, color = DokandaarTheme.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${strings.subtotal}: ${Money(inv.subtotalPaise).format()}", fontSize = 13.sp, color = DokandaarTheme.textPrimary)
                    Text("${strings.discounts}: -${Money(inv.discountPaise).format()}", fontSize = 13.sp, color = PastelGreenContent)
                    Text("${strings.grandTotal}: ${Money(inv.grandTotalPaise).format()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                    Text("${strings.paymentMode}: ${inv.paymentMethod} (${inv.status.name})", fontSize = 13.sp, color = DokandaarTheme.textPrimary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.printInvoice(inv.invoiceNumber)
                        selectedInvoiceForDetails = null
                    }
                ) {
                    Text(strings.reprint, color = PrimaryPurple, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedInvoiceForDetails = null }) {
                    Text(strings.close, color = DokandaarTheme.textMuted, maxLines = 1, softWrap = false)
                }
            }
        )
    }

    // Void / Return Confirmation Dialog
    invoiceToVoid?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToVoid = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = DokandaarTheme.cardBackground,
            title = { Text(strings.voidInvoiceTitle, fontWeight = FontWeight.Bold, color = ErrorColor, maxLines = 1, softWrap = false) },
            text = {
                Text(
                    strings.voidInvoicePrompt,
                    color = DokandaarTheme.textPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.voidInvoice(inv.invoiceNumber)
                        invoiceToVoid = null
                    }
                ) {
                    Text(strings.delete, color = ErrorColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToVoid = null }) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted, maxLines = 1, softWrap = false)
                }
            }
        )
    }

    // Discard Draft Dialog
    invoiceToDiscard?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToDiscard = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = DokandaarTheme.cardBackground,
            title = { Text(strings.discardBill, fontWeight = FontWeight.Bold, color = ErrorColor, maxLines = 1, softWrap = false) },
            text = {
                Text(
                    "Draft #${inv.invoiceNumber}",
                    color = DokandaarTheme.textPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.discardDraft(inv.invoiceNumber)
                        invoiceToDiscard = null
                    }
                ) {
                    Text(strings.delete, color = ErrorColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDiscard = null }) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted, maxLines = 1, softWrap = false)
                }
            }
        )
    }

    // Settle Due Dialog
    invoiceToSettle?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToSettle = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = DokandaarTheme.cardBackground,
            title = { Text(strings.settleDueTitle, fontWeight = FontWeight.Bold, color = PrimaryPurple) },
            text = {
                Text(
                    "${strings.settleDuePrompt} (${Money(inv.duePaise).format()})",
                    color = DokandaarTheme.textPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.settleDue(inv.invoiceNumber)
                        invoiceToSettle = null
                    }
                ) {
                    Text(strings.confirm, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToSettle = null }) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted)
                }
            }
        )
    }
}




