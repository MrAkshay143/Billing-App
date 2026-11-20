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
import com.billing.app.core.ui.components.ManagerPinDialog
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
import com.billing.app.core.util.Quantity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.repository.ReturnItemInput
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
    val tabTitles = listOf(
        strings.invoicesTabAll,
        strings.invoicesTabSales,
        strings.invoicesTabDrafts,
        strings.customerKhata,
        strings.invoicesTabReturns
    )

    var selectedInvoiceForDetails by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToVoid by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToAuthorizeVoid by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToDiscard by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToSettle by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToAuthorizeSettle by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToReturn by remember { mutableStateOf<InvoiceEntity?>(null) }
    var returnItemList by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }
    var returnQuantities by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var returnReason by remember { mutableStateOf("Customer return") }
    var showDateFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(invoiceToReturn) {
        invoiceToReturn?.let { inv ->
            val items = viewModel.getInvoiceItemList(inv.invoiceNumber)
            returnItemList = items
            returnQuantities = items.associate { item ->
                val qtyStr = if (item.quantityMilli % 1000L == 0L) {
                    (item.quantityMilli / 1000L).toString()
                } else {
                    (item.quantityMilli / 1000.0).toString()
                }
                item.id to qtyStr
            }
        }
    }

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
                    title = strings.kpiTodaySales,
                    value = Money(todaySalesAmount).format(),
                    icon = Icons.Default.ShoppingCart,
                    iconTint = PastelGreenContent,
                    containerBg = PastelGreenBg
                )
                DokandaarStatCard(
                    modifier = Modifier.weight(1f),
                    title = strings.kpiInvoicesCount,
                    value = invoicesCount.toString(),
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    iconTint = PastelPurpleContent,
                    containerBg = PastelPurpleBg
                )
                DokandaarStatCard(
                    modifier = Modifier.weight(1f),
                    title = strings.kpiPendingBills,
                    value = Money(pendingBillsAmount).format(),
                    icon = Icons.Default.Schedule,
                    iconTint = PastelOrangeContent,
                    containerBg = PastelOrangeBg,
                    valueColor = Color(0xFFD97706)
                )
                DokandaarStatCard(
                    modifier = Modifier.weight(1f),
                    title = strings.kpiTotalItems,
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
                    placeholder = strings.searchInvoices,
                    modifier = Modifier.weight(1f)
                )

                // Date Dropdown Button
                Box {
                    DokandaarDateFilterPill(
                        label = when (dateFilter) {
                            InvoiceDateFilter.TODAY -> strings.periodToday
                            InvoiceDateFilter.THIS_WEEK -> strings.periodThisWeek
                            InvoiceDateFilter.THIS_MONTH -> strings.periodThisMonth
                            else -> strings.periodAllTime
                        },
                        onClick = { showDateFilterMenu = true }
                    )

                    DropdownMenu(
                        expanded = showDateFilterMenu,
                        onDismissRequest = { showDateFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.periodToday) },
                            onClick = {
                                viewModel.dateFilter.value = InvoiceDateFilter.TODAY
                                showDateFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.periodThisWeek) },
                            onClick = {
                                viewModel.dateFilter.value = InvoiceDateFilter.THIS_WEEK
                                showDateFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.periodThisMonth) },
                            onClick = {
                                viewModel.dateFilter.value = InvoiceDateFilter.THIS_MONTH
                                showDateFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.periodAllTime) },
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
                            text = strings.noInvoicesFound,
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
                Row {
                    if (inv.status == InvoiceStatus.COMPLETED) {
                        TextButton(
                            onClick = {
                                val target = inv
                                selectedInvoiceForDetails = null
                                invoiceToReturn = target
                            }
                        ) {
                            Text(strings.salesReturn, color = PastelOrangeContent, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                    TextButton(
                        onClick = {
                            viewModel.printInvoice(inv.invoiceNumber)
                            selectedInvoiceForDetails = null
                        }
                    ) {
                        Text(strings.reprint, color = PrimaryPurple, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
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
                        val target = inv
                        invoiceToVoid = null
                        invoiceToAuthorizeVoid = target
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

    // Manager PIN Authorization for Void Invoice
    invoiceToAuthorizeVoid?.let { inv ->
        ManagerPinDialog(
            title = "Authorize Bill Cancellation",
            subtitle = "Enter Manager PIN to void invoice #${inv.invoiceNumber}",
            onDismissRequest = { invoiceToAuthorizeVoid = null },
            onAuthorized = {
                viewModel.voidInvoice(inv.invoiceNumber)
                invoiceToAuthorizeVoid = null
            }
        )
    }

    // Sales Return Dialog
    invoiceToReturn?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToReturn = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = DokandaarTheme.cardBackground,
            title = {
                Text(
                    text = "Sales Return - #${inv.invoiceNumber}",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter return quantity for items to return to stock:",
                        fontSize = 13.sp,
                        color = DokandaarTheme.textMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(returnItemList, key = { it.id }) { item ->
                            val soldQtyStr = Quantity.formatDisplay(item.quantityMilli, item.unit)
                            val currentInput = returnQuantities[item.id] ?: "0"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.background),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.productName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = DokandaarTheme.textPrimary
                                        )
                                        Text(
                                            text = "Sold: $soldQtyStr | Price: ${Money(item.unitPricePaise).format()}",
                                            fontSize = 11.sp,
                                            color = DokandaarTheme.textMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = currentInput,
                                        onValueChange = { newVal ->
                                            val filtered = newVal.filter { it.isDigit() }
                                            returnQuantities = returnQuantities + (item.id to filtered)
                                        },
                                        label = { Text(strings.returnQuantity, fontSize = 10.sp) },
                                        singleLine = true,
                                        modifier = Modifier.width(90.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = returnReason,
                        onValueChange = { returnReason = it },
                        label = { Text(strings.returnReason, fontSize = 11.sp) },
                        placeholder = { Text(strings.paymentNotePlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val itemsToReturn = returnItemList.mapNotNull { item ->
                            val returnQty = returnQuantities[item.id]?.toLongOrNull() ?: 0L
                            if (returnQty > 0L) {
                                ReturnItemInput(
                                    invoiceItemId = item.id,
                                    quantityMilli = returnQty * 1000L,
                                    reason = returnReason
                                )
                            } else null
                        }
                        if (itemsToReturn.isNotEmpty()) {
                            viewModel.processSalesReturn(
                                invoiceNumber = inv.invoiceNumber,
                                itemsToReturn = itemsToReturn,
                                reason = returnReason,
                                userId = "Cashier",
                                onSuccess = {
                                    invoiceToReturn = null
                                }
                            )
                        } else {
                            FeedbackManager.showToast(context, "Please select at least 1 item to return")
                        }
                    }
                ) {
                    Text(strings.confirmReturn, color = PastelOrangeContent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToReturn = null }) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted)
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
                        val target = inv
                        invoiceToSettle = null
                        invoiceToAuthorizeSettle = target
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

    // Manager PIN Authorization for Settle Due
    invoiceToAuthorizeSettle?.let { inv ->
        ManagerPinDialog(
            title = "Authorize Settle Due",
            subtitle = "Enter Manager PIN to settle due of ${Money(inv.duePaise).format()} for #${inv.invoiceNumber}",
            onDismissRequest = { invoiceToAuthorizeSettle = null },
            onAuthorized = {
                viewModel.settleDue(inv.invoiceNumber)
                invoiceToAuthorizeSettle = null
            }
        )
    }
}




