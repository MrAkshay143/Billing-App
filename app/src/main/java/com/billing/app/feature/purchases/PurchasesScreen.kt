package com.billing.app.feature.purchases

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import com.billing.app.core.util.WholesaleConverter
import com.billing.app.core.util.WholesaleConversion
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.components.DokandaarActionPill
import com.billing.app.core.ui.components.InputField
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
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
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.core.util.Money
import com.billing.app.core.util.Quantity
import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.SupplierEntity
import com.billing.app.domain.model.Product
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PurchasesScreen(
    viewModel: PurchasesViewModel,
    onNavigateBack: () -> Unit
) {
    val strings = appStrings()
    val context = LocalContext.current
    val purchases by viewModel.purchases.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val products by viewModel.products.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var showNewGrnDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            FeedbackManager.showToast(context, it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            FeedbackManager.showToast(context, it)
            viewModel.clearSuccess()
        }
    }

    val totalSpendPaise = remember(purchases) {
        purchases.fold(0L) { acc, p -> acc + p.totalPaise }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = "Purchases & GRN",
                subtitle = "${purchases.size} Purchases • ${Money.fromPaise(totalSpendPaise).format()} Total Spend",
                icon = Icons.Default.LocalShipping,
                onNavigateBack = onNavigateBack,
                actions = {
                    DokandaarActionPill(
                        text = "New GRN",
                        onClick = {
                            viewModel.clearDraft()
                            showNewGrnDialog = true
                        }
                    )
                }
            )
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (purchases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = PastelBlueBg
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = PastelBlueContent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.noPurchasesRecorded,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.tapNewGrnPrompt,
                            fontSize = 13.sp,
                            color = DokandaarTheme.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(purchases, key = { it.id }) { purchase ->
                        PurchaseItemCard(purchase = purchase, dateFormat = dateFormat)
                    }
                }
            }
        }
    }

    if (showNewGrnDialog) {
        NewGrnDialog(
            viewModel = viewModel,
            suppliers = suppliers,
            products = products,
            onDismiss = {
                showNewGrnDialog = false
                viewModel.clearDraft()
            },
            onSaveSuccess = {
                showNewGrnDialog = false
            }
        )
    }
}

@Composable
private fun PurchaseItemCard(
    purchase: PurchaseEntity,
    dateFormat: SimpleDateFormat
) {
    val isDue = purchase.duePaise > 0L || purchase.paymentStatus.equals("DUE", ignoreCase = true)
    val isPartial = purchase.paidPaise > 0L && purchase.duePaise > 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    .clip(CircleShape)
                    .background(if (isDue) PastelOrangeBg else PastelGreenBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = if (isDue) PastelOrangeContent else PastelGreenContent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = purchase.purchaseNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DokandaarTheme.textPrimary
                    )
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isDue && !isPartial -> PastelRedBg
                            isPartial -> PastelOrangeBg
                            else -> PastelGreenBg
                        }
                    ) {
                        Text(
                            text = when {
                                isDue && !isPartial -> "DUE"
                                isPartial -> "PARTIAL"
                                else -> "PAID"
                            },
                            color = when {
                                isDue && !isPartial -> PastelRedContent
                                isPartial -> PastelOrangeContent
                                else -> PastelGreenContent
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = purchase.supplierName ?: "Unknown Supplier",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DokandaarTheme.textPrimary
                )
                Text(
                    text = dateFormat.format(Date(purchase.timestamp)),
                    fontSize = 11.sp,
                    color = DokandaarTheme.textMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.fromPaise(purchase.totalPaise).format(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PrimaryPurple
                )
                if (purchase.duePaise > 0L) {
                    Text(
                        text = "Due: ${Money.fromPaise(purchase.duePaise).format()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ErrorColor
                    )
                }
            }
        }
    }
}

@Composable
private fun NewGrnDialog(
    viewModel: PurchasesViewModel,
    suppliers: List<SupplierEntity>,
    products: List<Product>,
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val strings = appStrings()
    val context = LocalContext.current
    val selectedSupplier by viewModel.selectedSupplier.collectAsState()
    val invoiceNumber by viewModel.invoiceNumber.collectAsState()
    val isTaxInclusive by viewModel.isTaxInclusive.collectAsState()
    val paymentMode by viewModel.paymentMode.collectAsState()
    val paidAmountPaise by viewModel.paidAmountPaise.collectAsState()
    val items by viewModel.items.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    // Form inputs for current item being added
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var itemQuantityText by remember { mutableStateOf("1") }
    var itemUnitPriceText by remember { mutableStateOf("") }
    var itemGstRateBp by remember { mutableIntStateOf(0) }
    var itemBatchNumber by remember { mutableStateOf("") }
    var itemExpiryDate by remember { mutableStateOf<Long?>(null) }
    var itemMrpText by remember { mutableStateOf("") }

    // Wholesale / Carton conversion wizard state
    var showWholesaleWizard by remember { mutableStateOf(false) }
    var wholesalePackagingUnit by remember { mutableStateOf("CARTON") }
    var wholesaleUnitsPerPkgText by remember { mutableStateOf("12") }
    var wholesalePackageCountText by remember { mutableStateOf("1") }
    var wholesalePricePerPkgText by remember { mutableStateOf("") }

    // Dropdown states
    var supplierDropdownExpanded by remember { mutableStateOf(false) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }

    val filteredProducts = remember(products, productSearchQuery) {
        if (productSearchQuery.isBlank()) products.take(40)
        else products.filter {
            it.name.contains(productSearchQuery, ignoreCase = true) ||
            it.barcode.contains(productSearchQuery) ||
            it.sku.contains(productSearchQuery, ignoreCase = true)
        }.take(40)
    }

    val (subtotalPaise, taxPaise, grandTotalPaise) = viewModel.calculateTotals()
    val duePaise = (grandTotalPaise - paidAmountPaise).coerceAtLeast(0L)

    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                itemExpiryDate = cal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(20.dp),
            color = DokandaarTheme.cardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Modal Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.newGrnTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                        Text(
                            text = strings.recordIncomingStockSubtitle,
                            fontSize = 12.sp,
                            color = DokandaarTheme.textMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DokandaarTheme.textMuted
                        )
                    }
                }

                HorizontalDivider(color = DokandaarTheme.border)

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: Supplier & Invoice Details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = strings.supplierAndInvoiceInfo,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )

                            // Supplier Selector
                            Column {
                                Text(
                                    text = strings.selectSupplier,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DokandaarTheme.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedSupplier?.let { "${it.name} (${it.phone})" } ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        placeholder = { Text(strings.chooseSupplier, color = DokandaarTheme.textMuted) },
                                        trailingIcon = {
                                            IconButton(onClick = { supplierDropdownExpanded = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Select Supplier"
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { supplierDropdownExpanded = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryPurple,
                                            unfocusedBorderColor = DokandaarTheme.border
                                        )
                                    )

                                    DropdownMenu(
                                        expanded = supplierDropdownExpanded,
                                        onDismissRequest = { supplierDropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        if (suppliers.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text(strings.noSuppliersFoundPrompt) },
                                                onClick = { supplierDropdownExpanded = false }
                                            )
                                        } else {
                                            suppliers.forEach { s ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(s.name, fontWeight = FontWeight.SemiBold)
                                                            Text(s.phone, fontSize = 11.sp, color = DokandaarTheme.textMuted)
                                                        }
                                                    },
                                                    onClick = {
                                                        viewModel.setSelectedSupplier(s)
                                                        supplierDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Invoice Number
                            InputField(
                                label = "Supplier Invoice Number *",
                                value = invoiceNumber,
                                onValueChange = { viewModel.setInvoiceNumber(it) },
                                placeholder = "e.g. INV-98234 or GRN-01"
                            )

                            // Tax Inclusive vs Exclusive toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DokandaarTheme.cardBackground)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = strings.taxCalculationMode,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DokandaarTheme.textPrimary
                                    )
                                    Text(
                                        text = if (isTaxInclusive) "Buy prices INCLUDE GST" else "GST is ADDED to buy prices",
                                        fontSize = 11.sp,
                                        color = if (isTaxInclusive) PastelGreenContent else PrimaryPurple
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isTaxInclusive) "Inclusive" else "Exclusive",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTaxInclusive) PastelGreenContent else PrimaryPurple,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Switch(
                                        checked = isTaxInclusive,
                                        onCheckedChange = { viewModel.setTaxInclusive(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = PrimaryPurple,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = DokandaarTheme.border
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 2: Add Item Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = strings.addItemToGrn,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple
                            )

                            // Product Selector
                            Column {
                                Text(
                                    text = strings.selectProduct,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DokandaarTheme.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedProduct?.let {
                                            "${it.name} [Stock: ${it.stock} ${it.unit}]${if (it.trackBatch) " (Batch Tracked)" else ""}"
                                        } ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        placeholder = { Text(strings.chooseProduct, color = DokandaarTheme.textMuted) },
                                        trailingIcon = {
                                            IconButton(onClick = { productDropdownExpanded = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Select Product"
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { productDropdownExpanded = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryPurple,
                                            unfocusedBorderColor = DokandaarTheme.border
                                        )
                                    )

                                    DropdownMenu(
                                        expanded = productDropdownExpanded,
                                        onDismissRequest = { productDropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        // Search Bar inside product dropdown
                                        Box(modifier = Modifier.padding(8.dp)) {
                                            OutlinedTextField(
                                                value = productSearchQuery,
                                                onValueChange = { productSearchQuery = it },
                                                placeholder = { Text(strings.searchProductPlaceholder, fontSize = 12.sp) },
                                                singleLine = true,
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        if (filteredProducts.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text(strings.noMatchingProductsFound) },
                                                onClick = {}
                                            )
                                        } else {
                                            filteredProducts.forEach { p ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(p.name, fontWeight = FontWeight.SemiBold)
                                                                if (p.trackBatch) {
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Surface(
                                                                        shape = RoundedCornerShape(4.dp),
                                                                        color = PastelOrangeBg
                                                                    ) {
                                                                        Text(
                                                                            text = "BATCH",
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = PastelOrangeContent,
                                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Text(
                                                                text = "Stock: ${p.stock} ${p.unit} • Buy: ${p.purchasePrice.format()}",
                                                                fontSize = 11.sp,
                                                                color = DokandaarTheme.textMuted
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedProduct = p
                                                        itemGstRateBp = p.gstRateBp
                                                        itemUnitPriceText = if (p.purchasePrice.paise > 0L) {
                                                            (p.purchasePrice.paise / 100.0).toString()
                                                        } else ""
                                                        itemMrpText = if (p.mrp.paise > 0L) {
                                                            (p.mrp.paise / 100.0).toString()
                                                        } else ""
                                                        itemQuantityText = "1"
                                                        itemBatchNumber = ""
                                                        itemExpiryDate = null
                                                        productDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Wholesale Conversion (Carton / Dozen) Toggle & Wizard
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showWholesaleWizard = !showWholesaleWizard },
                                color = if (showWholesaleWizard) PrimaryPurple.copy(alpha = 0.12f) else DokandaarTheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (showWholesaleWizard) PrimaryPurple else DokandaarTheme.border
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = if (showWholesaleWizard) PrimaryPurple else DokandaarTheme.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = strings.wholesaleConversionTitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (showWholesaleWizard) PrimaryPurple else DokandaarTheme.textPrimary
                                        )
                                    }
                                    Text(
                                        text = if (showWholesaleWizard) "▲" else "▼",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (showWholesaleWizard) PrimaryPurple else DokandaarTheme.textMuted
                                    )
                                }
                            }

                            if (showWholesaleWizard) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = DokandaarTheme.surfaceVariant
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = strings.wholesaleConversionHelp,
                                            fontSize = 11.sp,
                                            color = DokandaarTheme.textMuted
                                        )

                                        // Packaging Unit Chips
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            WholesaleConverter.PRESET_PACKAGING_UNITS.forEach { unit ->
                                                val isSelected = wholesalePackagingUnit == unit
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            wholesalePackagingUnit = unit
                                                            if (unit == "DOZEN") {
                                                                wholesaleUnitsPerPkgText = "12"
                                                            }
                                                        },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) PrimaryPurple else DokandaarTheme.cardBackground,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isSelected) PrimaryPurple else DokandaarTheme.border
                                                    )
                                                ) {
                                                    Text(
                                                        text = unit,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else DokandaarTheme.textPrimary,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                InputField(
                                                    label = "${strings.packageCount} *",
                                                    value = wholesalePackageCountText,
                                                    onValueChange = { wholesalePackageCountText = it },
                                                    placeholder = "e.g. 5",
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                                )
                                            }
                                            Box(modifier = Modifier.weight(1f)) {
                                                InputField(
                                                    label = "${strings.unitsPerPackage} *",
                                                    value = wholesaleUnitsPerPkgText,
                                                    onValueChange = { wholesaleUnitsPerPkgText = it },
                                                    placeholder = "e.g. 24",
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                                )
                                            }
                                        }

                                        InputField(
                                            label = "${strings.pricePerPackage} *",
                                            value = wholesalePricePerPkgText,
                                            onValueChange = { wholesalePricePerPkgText = it },
                                            placeholder = "e.g. 1440.00",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                        )

                                        // Compute conversion
                                        val pkgCount = wholesalePackageCountText.toDoubleOrNull() ?: 0.0
                                        val unitsPerPkg = wholesaleUnitsPerPkgText.toIntOrNull() ?: 0
                                        val pricePerPkg = (wholesalePricePerPkgText.toDoubleOrNull()?.let { (it * 100.0).toLong() }) ?: 0L
                                        val baseCode = selectedProduct?.unit ?: "PCS"

                                        val convResult = if (pkgCount > 0.0 && unitsPerPkg > 0 && pricePerPkg >= 0L) {
                                            WholesaleConverter.convert(
                                                wholesaleUnit = wholesalePackagingUnit,
                                                unitsPerPackage = unitsPerPkg,
                                                packageCount = pkgCount,
                                                pricePerPackagePaise = pricePerPkg,
                                                baseUnitCode = baseCode
                                            )
                                        } else null

                                        if (convResult?.isSuccess == true) {
                                            val conv = convResult.getOrThrow()
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = PastelGreenBg,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, PastelGreenContent.copy(alpha = 0.3f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    val displaySummary = "✓ ${conv.displayText}"
                                                    val costSummary = "${strings.totalLineCost}: Rs. ${String.format(Locale.ROOT, "%.2f", conv.totalCostPaise / 100.0)}"
                                                    Text(
                                                        text = displaySummary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PastelGreenContent
                                                    )
                                                    Text(
                                                        text = costSummary,
                                                        fontSize = 11.sp,
                                                        color = PastelGreenContent
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    val baseQty = conv.baseQuantityMilli / 1000.0
                                                    itemQuantityText = if (baseQty % 1.0 == 0.0) baseQty.toInt().toString() else String.format(Locale.ROOT, "%.3f", baseQty)
                                                    itemUnitPriceText = String.format(Locale.ROOT, "%.2f", conv.unitPricePaise / 100.0)
                                                    showWholesaleWizard = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = PrimaryPurple,
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text(strings.applyWholesaleToItem, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        } else if (convResult?.isFailure == true) {
                                            Text(
                                                text = convResult.exceptionOrNull()?.message ?: "Invalid conversion parameters",
                                                fontSize = 11.sp,
                                                color = ErrorColor
                                            )
                                        }
                                    }
                                }
                            }

                            // Quantity & Unit Buy Price
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    InputField(
                                        label = "Quantity (${selectedProduct?.unit ?: "PCS"}) *",
                                        value = itemQuantityText,
                                        onValueChange = { itemQuantityText = it },
                                        placeholder = "e.g. 10",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    InputField(
                                        label = "Unit Buy Price (₹) *",
                                        value = itemUnitPriceText,
                                        onValueChange = { itemUnitPriceText = it },
                                        placeholder = "e.g. 85.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                }
                            }

                            // GST Slab Selector (0%, 5%, 18%, 40%)
                            Column {
                                Text(
                                    text = "GST Slab",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DokandaarTheme.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        0 to "0%",
                                        500 to "5%",
                                        1200 to "12%",
                                        1800 to "18%",
                                        4000 to "40%"
                                    ).forEach { (bp, label) ->
                                        val isSelected = itemGstRateBp == bp
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { itemGstRateBp = bp },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) PrimaryPurple else DokandaarTheme.surfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) PrimaryPurple else DokandaarTheme.border
                                            )
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else DokandaarTheme.textPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // DYNAMIC BATCH SECTION (Point 19): Mandatory if product.trackBatch == true
                            if (selectedProduct?.trackBatch == true) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (DokandaarTheme.isDark) Color(0xFF2A1F17) else PastelOrangeBg.copy(alpha = 0.5f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PastelOrangeContent.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = PastelOrangeContent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = strings.batchDetailsTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PastelOrangeContent
                                            )
                                        }

                                        // Batch Number
                                        InputField(
                                            label = "Batch Number *",
                                            value = itemBatchNumber,
                                            onValueChange = { itemBatchNumber = it },
                                            placeholder = "e.g. BATCH-2026-X"
                                        )

                                        // Expiry Date & MRP
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Expiry Date Field with Date Picker
                                            Box(modifier = Modifier.weight(1f)) {
                                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                                    Text(
                                                        text = strings.expiryDateLabel,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = DokandaarTheme.textPrimary,
                                                        modifier = Modifier.padding(bottom = 6.dp)
                                                    )
                                                    OutlinedTextField(
                                                        value = itemExpiryDate?.let {
                                                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                                                        } ?: "",
                                                        onValueChange = {},
                                                        readOnly = true,
                                                        placeholder = { Text("DD/MM/YYYY", color = DokandaarTheme.textMuted, fontSize = 12.sp) },
                                                        trailingIcon = {
                                                            IconButton(onClick = { datePickerDialog.show() }) {
                                                                Icon(
                                                                    imageVector = Icons.Default.CalendarToday,
                                                                    contentDescription = "Pick Date",
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { datePickerDialog.show() },
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = PrimaryPurple,
                                                            unfocusedBorderColor = DokandaarTheme.border
                                                        )
                                                    )
                                                }
                                            }

                                            // Batch MRP
                                            Box(modifier = Modifier.weight(1f)) {
                                                InputField(
                                                    label = "MRP (₹) *",
                                                    value = itemMrpText,
                                                    onValueChange = { itemMrpText = it },
                                                    placeholder = "e.g. 120.00",
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // "+ Add Item" Button
                            Button(
                                onClick = {
                                    val prod = selectedProduct
                                    if (prod == null) {
                                        FeedbackManager.showToast(context, "Please select a product")
                                        return@Button
                                    }
                                    val qty = itemQuantityText.toDoubleOrNull() ?: 0.0
                                    if (qty <= 0.0) {
                                        FeedbackManager.showToast(context, "Quantity must be greater than zero")
                                        return@Button
                                    }
                                    val unitPrice = itemUnitPriceText.toDoubleOrNull() ?: -1.0
                                    if (unitPrice < 0.0) {
                                        FeedbackManager.showToast(context, "Please enter a valid buy price")
                                        return@Button
                                    }
                                    val unitPricePaise = Money.fromRupees(unitPrice).paise
                                    val mrpPaise = if (itemMrpText.isNotBlank()) Money.fromRupees(itemMrpText).paise else 0L

                                    val added = viewModel.addItem(
                                        PurchaseItemDraft(
                                            product = prod,
                                            quantityMilli = (qty * 1000).toLong(),
                                            unitPricePaise = unitPricePaise,
                                            gstRateBp = itemGstRateBp,
                                            mrpPaise = mrpPaise,
                                            batchNumber = itemBatchNumber.trim(),
                                            expiryDate = itemExpiryDate
                                        )
                                    )
                                    if (added) {
                                        // Reset Item form
                                        selectedProduct = null
                                        itemQuantityText = "1"
                                        itemUnitPriceText = ""
                                        itemGstRateBp = 0
                                        itemBatchNumber = ""
                                        itemExpiryDate = null
                                        itemMrpText = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryPurple,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.addItemToGrn, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // SECTION 3: Added Items Table / List
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${strings.itemsInGrn} (${items.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )

                            if (items.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.noItemsAddedYet,
                                        fontSize = 12.sp,
                                        color = DokandaarTheme.textMuted
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items.forEachIndexed { index, draft ->
                                        val lineTotalPaise = if (isTaxInclusive) {
                                            (draft.unitPricePaise * draft.quantityMilli + 500L) / 1000L
                                        } else {
                                            val base = (draft.unitPricePaise * draft.quantityMilli + 500L) / 1000L
                                            val gst = if (draft.gstRateBp > 0) (base * draft.gstRateBp + 5000L) / 10000L else 0L
                                            base + gst
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(DokandaarTheme.surfaceVariant)
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = draft.product.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = DokandaarTheme.textPrimary
                                                )
                                                Text(
                                                    text = "${Quantity.formatDisplay(draft.quantityMilli, draft.product.unit)} @ ${Money.fromPaise(draft.unitPricePaise).format()} • GST ${draft.gstRateBp / 100}%",
                                                    fontSize = 11.sp,
                                                    color = DokandaarTheme.textMuted
                                                )
                                                if (draft.product.trackBatch) {
                                                    val expStr = draft.expiryDate?.let {
                                                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                                                    } ?: "No Exp"
                                                    Text(
                                                        text = "Batch: ${draft.batchNumber} • Exp: $expStr • MRP: ${Money.fromPaise(draft.mrpPaise).format()}",
                                                        fontSize = 10.sp,
                                                        color = PastelOrangeContent,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = Money.fromPaise(lineTotalPaise).format(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = PrimaryPurple
                                                )
                                                IconButton(
                                                    onClick = { viewModel.removeItem(index) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Remove",
                                                        tint = ErrorColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 4: Payment & Settlement Section (Point 18)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Payment & Settlement",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )

                            // Totals summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${strings.subtotal}:", fontSize = 12.sp, color = DokandaarTheme.textMuted)
                                Text(Money.fromPaise(subtotalPaise).format(), fontSize = 12.sp, color = DokandaarTheme.textPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total GST:", fontSize = 12.sp, color = DokandaarTheme.textMuted)
                                Text(Money.fromPaise(taxPaise).format(), fontSize = 12.sp, color = DokandaarTheme.textPrimary)
                            }
                            HorizontalDivider(color = DokandaarTheme.border)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.grandTotalLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DokandaarTheme.textPrimary)
                                Text(Money.fromPaise(grandTotalPaise).format(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Payment Mode selector ([Cash] [UPI] [Credit])
                            Text(
                                text = "Payment Mode *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DokandaarTheme.textPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("CASH" to strings.cash, "UPI" to strings.upiOnline, "CREDIT" to strings.customerKhata).forEach { (mode, label) ->
                                    val isSelected = paymentMode.equals(mode, ignoreCase = true)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.setPaymentMode(mode) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) PrimaryPurple else DokandaarTheme.cardBackground,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) PrimaryPurple else DokandaarTheme.border
                                        )
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else DokandaarTheme.textPrimary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            // Paid Amount Input (if not Credit)
                            if (!paymentMode.equals("CREDIT", ignoreCase = true)) {
                                InputField(
                                    label = "Paid Amount (₹) *",
                                    value = if (paidAmountPaise > 0L) (paidAmountPaise / 100.0).toString() else "",
                                    onValueChange = {
                                        val amt = it.toDoubleOrNull() ?: 0.0
                                        viewModel.setPaidAmountPaise(Money.fromRupees(amt).paise)
                                    },
                                    placeholder = "e.g. ${(grandTotalPaise / 100.0)}",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }

                            // Balance Due Display
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (duePaise > 0L) PastelRedBg else PastelGreenBg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (duePaise > 0L) "Balance Due to Supplier:" else "Settlement Status:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (duePaise > 0L) PastelRedContent else PastelGreenContent
                                )
                                Text(
                                    text = if (duePaise > 0L) Money.fromPaise(duePaise).format() else "Fully Paid",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (duePaise > 0L) PastelRedContent else PastelGreenContent
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = DokandaarTheme.border)

                // Dialog Footer with Cancel and Save Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Text(strings.cancel, color = DokandaarTheme.textMuted)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            viewModel.savePurchase(onSuccess = onSaveSuccess)
                        },
                        enabled = !isSaving && items.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple,
                            contentColor = Color.White
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.saving)
                        } else {
                            Text(strings.saveGrn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
