package com.billing.app.feature.checkout

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.qr.UpiQrGenerator
import com.billing.app.core.ui.components.BottomDokandaarNav
import com.billing.app.core.ui.components.DokandaarTab
import com.billing.app.core.ui.components.ProductThumbnail
import com.billing.app.core.ui.components.TopDokandaarHeader
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.InStockText
import com.billing.app.core.ui.theme.TextMutedColor
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.core.ui.util.debouncedClickable
import com.billing.app.core.util.Money
import com.billing.app.domain.model.CartItem

@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    cartItems: List<CartItem>,
    subtotalAmount: Money,
    onNavigateBack: () -> Unit,
    onSaleCompleted: () -> Unit,
    onNavigateToTab: (DokandaarTab) -> Unit
) {
    val context = LocalContext.current
    val shopProfile by viewModel.shopProfile.collectAsState()
    val selectedMethod by viewModel.selectedPaymentMethod.collectAsState()
    val discountPaise by viewModel.discountPaise.collectAsState()
    val cashReceivedPaise by viewModel.cashReceivedPaise.collectAsState()
    val custName by viewModel.customerName.collectAsState()
    val custPhone by viewModel.customerPhone.collectAsState()
    val isCompletingSale by viewModel.isCompletingSale.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val printSuccess by viewModel.printSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
        if (granted) {
            viewModel.printReceipt(cartItems, subtotalAmount)
        } else {
            FeedbackManager.showToast(context, "Bluetooth permission required to print")
        }
    }

    val onPrintClicked: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                bluetoothPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            } else {
                viewModel.printReceipt(cartItems, subtotalAmount)
            }
        } else {
            viewModel.printReceipt(cartItems, subtotalAmount)
        }
    }

    var showDiscountDialog by rememberSaveable { mutableStateOf(false) }
    var discountInput by rememberSaveable { mutableStateOf("") }
    var cashInput by rememberSaveable { mutableStateOf("") }

    val grandTotalPaise = viewModel.calculateGrandTotal(subtotalAmount.paise)
    val taxPaise = viewModel.calculateTax(subtotalAmount.paise, discountPaise)
    val grandTotal = Money.fromPaise(grandTotalPaise)
    val (duePaise, changeReturnedPaise) = viewModel.calculateDueAndChange(grandTotalPaise)

    // Back handler preserves cart!
    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(printSuccess) {
        if (printSuccess) {
            snackbarHostState.showSnackbar("Receipt printed successfully")
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Discount Dialog
    if (showDiscountDialog) {
        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Add Discount (₹)", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it },
                    placeholder = { Text("Enter discount amount in ₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val d = Money.fromRupees(discountInput)
                        viewModel.setDiscount(d.paise, subtotalAmount.paise)
                        showDiscountDialog = false
                    }
                ) {
                    Text("Apply", color = HeaderGradientStart, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopDokandaarHeader(
                storeName = "Checkout",
                subtitle = "Review, Pay & Print",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                onNotificationClick = { onNavigateToTab(DokandaarTab.INVOICES) },
                onMenuClick = { onNavigateToTab(DokandaarTab.MORE) }
            )
        },
        bottomBar = {
            BottomDokandaarNav(
                selectedTab = DokandaarTab.HOME,
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. Items Card (Mockup 2.3)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header row: Items (count) and [Edit Cart] button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = DokandaarTheme.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Items (${cartItems.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onNavigateBack() },
                            color = AddProductBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = HeaderGradientStart,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit Cart",
                                    color = HeaderGradientStart,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (taxPaise > 0L) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GST / Taxes",
                                fontSize = 14.sp,
                                color = DokandaarTheme.textMuted
                            )
                            Text(
                                text = "+ ${Money.fromPaise(taxPaise).format()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DokandaarTheme.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DokandaarTheme.border.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Line Items
                    cartItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductThumbnail(
                                imageUri = item.product.imageUri,
                                size = 42.dp,
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DokandaarTheme.textPrimary
                                )
                                Text(
                                    text = item.product.barcode,
                                    fontSize = 11.sp,
                                    color = DokandaarTheme.textMuted
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${item.quantity} × ${item.product.price.format()}",
                                    fontSize = 12.sp,
                                    color = DokandaarTheme.textMuted
                                )
                                Text(
                                    text = item.total.format(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DokandaarTheme.textPrimary
                                )
                            }
                        }

                        if (index < cartItems.size - 1) {
                            HorizontalDivider(
                                color = DokandaarTheme.border.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Bill Summary Card (Subtotal, Discount, Grand Total)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subtotal",
                            fontSize = 14.sp,
                            color = DokandaarTheme.textMuted
                        )
                        Text(
                            text = subtotalAmount.format(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Discount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Discount",
                                fontSize = 14.sp,
                                color = DokandaarTheme.textMuted
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showDiscountDialog = true },
                                color = AddProductBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = HeaderGradientStart,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (discountPaise > 0L) "Change" else "Add Discount",
                                        color = HeaderGradientStart,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "- ${Money.fromPaise(discountPaise).format()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (discountPaise > 0L) Color(0xFFEF4444) else DokandaarTheme.textMuted
                        )
                    }

                    if (taxPaise > 0L) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GST / Taxes",
                                fontSize = 14.sp,
                                color = DokandaarTheme.textMuted
                            )
                            Text(
                                text = "+ ${Money.fromPaise(taxPaise).format()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DokandaarTheme.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DokandaarTheme.border.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Grand Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grand Total",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                        Text(
                            text = grandTotal.format(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HeaderGradientStart
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Customer Details Card (U3)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (duePaise > 0L) Color(0xFFDC2626) else HeaderGradientStart,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Customer Information",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (duePaise > 0L) Color(0xFFDC2626) else DokandaarTheme.textPrimary
                        )
                        if (duePaise > 0L) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(Required for Due)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = custName,
                        onValueChange = { viewModel.customerName.value = it },
                        label = { Text("Customer Name" + if (duePaise > 0L) " *" else "") },
                        placeholder = { Text("Enter customer name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = custPhone,
                        onValueChange = { viewModel.customerPhone.value = it },
                        label = { Text("Customer Phone") },
                        placeholder = { Text("Enter contact number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Payment Method Card (Mockup 2.3)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = DokandaarTheme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Payment Method",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Payment Mode Selector Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentTabPill(
                            title = "Cash",
                            icon = Icons.Default.Payments,
                            isSelected = selectedMethod == PaymentMethodMode.CASH,
                            onClick = { viewModel.setPaymentMethod(PaymentMethodMode.CASH) },
                            modifier = Modifier.weight(1f)
                        )

                        PaymentTabPill(
                            title = "UPI",
                            icon = Icons.Default.QrCode,
                            isSelected = selectedMethod == PaymentMethodMode.UPI,
                            onClick = { viewModel.setPaymentMethod(PaymentMethodMode.UPI) },
                            modifier = Modifier.weight(1f)
                        )

                        PaymentTabPill(
                            title = "Other",
                            icon = Icons.Default.CreditCard,
                            isSelected = selectedMethod == PaymentMethodMode.OTHER,
                            onClick = { viewModel.setPaymentMethod(PaymentMethodMode.OTHER) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Details
                    when (selectedMethod) {
                        PaymentMethodMode.UPI -> {
                            val activeUpiId = shopProfile.upiId.ifBlank { "dokandaar@upi" }
                            val qrBitmap = remember(activeUpiId, shopProfile.name, grandTotal) {
                                UpiQrGenerator.generateUpiQrBitmap(
                                    upiId = activeUpiId,
                                    shopName = shopProfile.name.ifBlank { "Dokandaar" },
                                    amount = grandTotal,
                                    sizePx = 360
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DokandaarTheme.surfaceVariant)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (qrBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .border(1.dp, DokandaarTheme.border, RoundedCornerShape(8.dp))
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = qrBitmap.asImageBitmap(),
                                            contentDescription = "UPI QR Code",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Scan & Pay (UPI)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DokandaarTheme.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("UPI ID", activeUpiId)
                                            clipboard.setPrimaryClip(clip)
                                            FeedbackManager.showToast(context, "UPI ID copied: $activeUpiId")
                                        }
                                    ) {
                                        Text(
                                            text = "UPI ID: $activeUpiId",
                                            fontSize = 11.sp,
                                            color = Color(0xFF475569)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy UPI ID",
                                            tint = HeaderGradientStart,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Amount: ${grandTotal.format()}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HeaderGradientStart
                                    )
                                }
                            }
                        }

                        PaymentMethodMode.CASH -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DokandaarTheme.surfaceVariant)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Cash Received (₹)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = cashInput,
                                    onValueChange = {
                                        cashInput = it
                                        val m = Money.fromRupees(it)
                                        viewModel.setCashReceived(m.paise)
                                    },
                                    placeholder = { Text("0.00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Cash Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    QuickCashChip(
                                        label = "Exact",
                                        onClick = {
                                            cashInput = grandTotal.format(includeSymbol = false)
                                            viewModel.setCashReceived(grandTotalPaise)
                                        }
                                    )
                                    QuickCashChip(
                                        label = "+₹50",
                                        onClick = {
                                            val current = cashReceivedPaise + 5000L
                                            cashInput = Money.fromPaise(current).format(includeSymbol = false)
                                            viewModel.setCashReceived(current)
                                        }
                                    )
                                    QuickCashChip(
                                        label = "+₹100",
                                        onClick = {
                                            val current = cashReceivedPaise + 10000L
                                            cashInput = Money.fromPaise(current).format(includeSymbol = false)
                                            viewModel.setCashReceived(current)
                                        }
                                    )
                                    QuickCashChip(
                                        label = "+₹500",
                                        onClick = {
                                            val current = cashReceivedPaise + 50000L
                                            cashInput = Money.fromPaise(current).format(includeSymbol = false)
                                            viewModel.setCashReceived(current)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (duePaise > 0L) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Balance Due (Credit):",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                        Text(
                                            text = Money.fromPaise(duePaise).format(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Change to Return:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DokandaarTheme.textPrimary
                                        )
                                        Text(
                                            text = Money.fromPaise(changeReturnedPaise).format(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = InStockText
                                        )
                                    }
                                }
                            }
                        }

                        PaymentMethodMode.OTHER -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DokandaarTheme.surfaceVariant)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "Payment via Card, NetBanking, or Store Credit.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hold / Park Bill Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(14.dp))
                    .debouncedClickable(enabled = !isCompletingSale && !isPrinting) {
                        viewModel.holdSaleAsDraft(cartItems, subtotalAmount) {
                            FeedbackManager.showToast(context, "Bill held as draft!")
                            onSaleCompleted()
                        }
                    },
                color = Color(0xFFFFFBEB)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hold Bill as Draft (Park Cart)",
                        color = Color(0xFFD97706),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Dual Action Buttons: [Print Receipt] and [Complete Sale]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [Print Receipt] Button (Lavender background with border)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, HeaderGradientStart, RoundedCornerShape(14.dp))
                        .debouncedClickable(enabled = !isPrinting && !isCompletingSale) {
                            onPrintClicked()
                        },
                    color = AddProductBg
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = HeaderGradientStart,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = HeaderGradientStart,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Print Receipt",
                                color = HeaderGradientStart,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // [Complete Sale] Button (Solid Purple with multi-click lock)
                Surface(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .debouncedClickable(enabled = !isCompletingSale && !isPrinting) {
                            viewModel.completeSale(cartItems, subtotalAmount) {
                                onSaleCompleted()
                            }
                        },
                    color = if (isCompletingSale) HeaderGradientStart.copy(alpha = 0.7f) else HeaderGradientStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCompletingSale) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Saving...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Complete Sale",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaymentTabPill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) AddProductBg else DokandaarTheme.cardBackground)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) HeaderGradientStart else DokandaarTheme.border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) HeaderGradientStart else DokandaarTheme.textMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) HeaderGradientStart else DokandaarTheme.textMuted,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickCashChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = DokandaarTheme.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = DokandaarTheme.textPrimary,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
