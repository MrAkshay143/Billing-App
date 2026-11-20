package com.billing.app.feature.checkout

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.cart.CartManager
import com.billing.app.core.qr.UpiQrGenerator
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.core.ui.components.CategoryCard
import com.billing.app.core.ui.components.DokandaarBottomBarCta
import com.billing.app.core.ui.components.DokandaarBottomIconButton
import com.billing.app.core.ui.components.DokandaarSearchBar
import com.billing.app.core.ui.components.ManagerPinDialog
import com.billing.app.core.ui.components.ProductThumbnail
import com.billing.app.core.ui.components.ShiftOpenDialog
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.DarkPurple
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelGreenBg
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PastelPurpleContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.ui.theme.SecondaryPurple
import com.billing.app.core.util.Money
import com.billing.app.core.util.Quantity
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.Product
import com.billing.app.feature.checkout.components.AddCustomItemDialog
import com.billing.app.feature.checkout.components.CartItemEditDialog
import com.billing.app.feature.checkout.components.CustomerSelectionBottomSheet
import com.billing.app.feature.checkout.components.ReactivateProductDialog
import java.util.Locale

private fun formatCartQuantity(quantityMilli: Long, unit: String): String {
    return Quantity.formatDisplay(quantityMilli, unit)
}

private fun formatCartTabTitle(tabName: String, cartWord: String): String {
    val numStr = tabName.removePrefix("Cart ").removePrefix("কার্ট ").trim()
    val localizedNum = when (cartWord) {
        "কার্ট" -> when (numStr) {
            "1" -> "১"
            "2" -> "২"
            "3" -> "৩"
            "4" -> "৪"
            "5" -> "৫"
            else -> numStr
        }
        "कार्ट" -> when (numStr) {
            "1" -> "१"
            "2" -> "२"
            "3" -> "३"
            "4" -> "४"
            "5" -> "५"
            else -> numStr
        }
        else -> numStr
    }
    return "$cartWord $localizedNum"
}

@Composable
fun SaleScreen(
    viewModel: CheckoutViewModel,
    cartManager: CartManager,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onSaleCompleted: () -> Unit,
    onNavigateToPrinter: () -> Unit = {},
    onNavigateToHeldDrafts: () -> Unit = {}
) {
    val context = LocalContext.current
    val strings = appStrings()
    val snackbarHostState = remember { SnackbarHostState() }

    val cartItems by cartManager.cartItems.collectAsState()
    val discountPaise by cartManager.discountPaise.collectAsState()
    val customerName by cartManager.customerName.collectAsState()
    val customerPhone by cartManager.customerPhone.collectAsState()
    val tabs by cartManager.tabs.collectAsState()
    val activeTabId by cartManager.activeTabId.collectAsState()

    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val customers by viewModel.customers.collectAsState()

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()
    val saleSuccess by viewModel.saleSuccess.collectAsState()
    val printSuccess by viewModel.printSuccess.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val isCompositionScheme by viewModel.isCompositionScheme.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()

    var showCustomerBottomSheet by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showAddCustomItemDialog by remember { mutableStateOf(false) }
    var showShiftOpenDialog by remember { mutableStateOf(false) }
    var showManagerPinDialog by remember { mutableStateOf(false) }
    var managerPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var itemToEdit by remember { mutableStateOf<CartItem?>(null) }
    var productToReactivate by remember { mutableStateOf<Product?>(null) }

    val isGstEnabled = viewModel.isGstEnabled
    val taxRatePercent = viewModel.taxRatePercent
    val billResult = remember(cartItems, discountPaise, isGstEnabled, taxRatePercent, isCompositionScheme) {
        viewModel.calculateBillResult(cartItems, discountPaise)
    }
    val subtotalPaise = billResult.subtotalPaise
    val subtotalMoney = remember(subtotalPaise) { Money.fromPaise(subtotalPaise) }
    val taxPaise = billResult.totalTaxPaise
    val roundOffPaise = billResult.roundOffPaise
    val grandTotalPaise = billResult.grandTotalPaise
    val grandTotalMoney = remember(grandTotalPaise) { Money.fromPaise(grandTotalPaise) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(saleSuccess) {
        if (saleSuccess) {
            cartManager.clearCart()
            showPaymentDialog = false
            showReceiptDialog = true
        }
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = strings.newSalePos,
                subtitle = "${cartItems.size} items • ${grandTotalMoney.format()}",
                icon = Icons.Default.ShoppingCart,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = onNavigateToScanner,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    var showSaleMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showSaleMenu = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSaleMenu,
                            onDismissRequest = { showSaleMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.clearCartConfirmTitle) },
                                onClick = {
                                    cartManager.clearCart()
                                    showSaleMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.holdBillOrSaveDraft) },
                                onClick = {
                                    showSaleMenu = false
                                    viewModel.holdDraft(cartItems, cartManager.subtotal) {
                                        cartManager.clearCart()
                                        onNavigateBack()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.viewHeldDrafts) },
                                onClick = {
                                    showSaleMenu = false
                                    onNavigateToHeldDrafts()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.printerSettings) },
                                onClick = {
                                    showSaleMenu = false
                                    onNavigateToPrinter()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DokandaarTheme.cardBackground,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hold Button
                    DokandaarBottomIconButton(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        label = strings.holdBill,
                        onClick = {
                            if (cartItems.isNotEmpty()) {
                                viewModel.setCustomer(customerName, customerPhone)
                                viewModel.setDiscount(discountPaise, cartManager.subtotal.paise)
                                viewModel.holdDraft(cartItems, cartManager.subtotal) {
                                    cartManager.clearCart()
                                    onNavigateBack()
                                }
                            }
                        }
                    )

                    // Discard Button
                    DokandaarBottomIconButton(
                        icon = Icons.Default.Delete,
                        label = strings.discardBill,
                        onClick = { cartManager.clearCart() }
                    )

                    // Proceed to Pay Button
                    DokandaarBottomBarCta(
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Default.CreditCard,
                        title = strings.proceedToPay,
                        trailingText = grandTotalMoney.format(),
                        enabled = cartItems.isNotEmpty(),
                        onClick = {
                            if (cartItems.isNotEmpty()) {
                                viewModel.setCustomer(customerName, customerPhone)
                                viewModel.setDiscount(discountPaise, cartManager.subtotal.paise)
                                if (activeShift == null) {
                                    showShiftOpenDialog = true
                                } else {
                                    showPaymentDialog = true
                                }
                            }
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Customer Section (Mockup 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Customer Selector Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DokandaarTheme.cardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
                    modifier = Modifier.clickable { showCustomerBottomSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = customerName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DokandaarTheme.textPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Add Customer Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PastelPurpleBg,
                    modifier = Modifier.clickable { showAddCustomerDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.addCustomer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                    }
                }
            }

            // 2. Product Search Bar with Scanner Icon and + Custom Item button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DokandaarSearchBar(
                    value = searchQuery,
                    onValueChange = { query ->
                        viewModel.searchQuery.value = query
                        val trimmed = query.trim()
                        if (trimmed.isNotBlank()) {
                            val inactiveMatch = products.firstOrNull {
                                !it.isActive && (it.barcode.equals(trimmed, ignoreCase = true) || it.sku.equals(trimmed, ignoreCase = true))
                            }
                            if (inactiveMatch != null) {
                                productToReactivate = inactiveMatch
                            }
                        }
                    },
                    placeholder = strings.searchProducts,
                    modifier = Modifier.weight(1f),
                    trailingContent = {
                        IconButton(
                            onClick = onNavigateToScanner,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PastelPurpleBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { showAddCustomItemDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
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
                            text = if (strings.cartTitle == "কার্ট") "কাস্টম" else "Custom",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                    }
                }
            }

            // 3. Category Cards (Horizontal Scroll matching Screenshot 2)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "all") {
                    CategoryCard(
                        title = strings.filterAll,
                        isSelected = selectedCategory.isBlank() || selectedCategory.equals("All", ignoreCase = true),
                        isAllItem = true,
                        onClick = { viewModel.selectedCategory.value = "All" }
                    )
                }

                items(categories, key = { it.id }) { cat ->
                    CategoryCard(
                        title = cat.name,
                        isSelected = selectedCategory.equals(cat.name, ignoreCase = true),
                        iconName = cat.iconName,
                        imageUri = cat.imageUri,
                        onClick = { viewModel.selectedCategory.value = cat.name }
                    )
                }
            }

            // 4. Products Display Grid (Responsive Grid Cells Adaptive 110.dp)
            if (products.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        val isOutOfStock = product.stockMilli <= 0L
                        val isLowStock = !isOutOfStock && product.stockMilli <= product.minStockAlertMilli
                        val isInactive = !product.isActive

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isInactive) {
                                        productToReactivate = product
                                    } else if (product.trackBatch && isOutOfStock) {
                                        FeedbackManager.showToast(context, "${product.name} is out of stock (batch tracked)")
                                    } else {
                                        val (success, warning) = cartManager.addProduct(product, 1000L)
                                        if (!product.trackBatch && isOutOfStock) {
                                            FeedbackManager.showToast(context, "Warning: ${product.name} is out of stock (selling on negative stock)")
                                        } else if (warning != null) {
                                            FeedbackManager.showToast(context, warning)
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOutOfStock) DokandaarTheme.cardBackground.copy(alpha = 0.65f) else DokandaarTheme.cardBackground
                            ),
                            border = when {
                                isOutOfStock -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f))
                                isLowStock -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.45f))
                                else -> androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
                            },
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isOutOfStock) 0.dp else 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    ProductThumbnail(
                                        imageUri = product.imageUri,
                                        size = 48.dp,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    if (isOutOfStock) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "OUT",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    } else if (isLowStock) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "LOW",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = product.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutOfStock) DokandaarTheme.textMuted else DokandaarTheme.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = product.price.format(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOutOfStock) DokandaarTheme.textMuted else PrimaryPurple
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isOutOfStock && product.trackBatch) Color.LightGray.copy(alpha = 0.4f) else PrimaryPurple),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = if (isOutOfStock && product.trackBatch) Color.Gray else Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. Shopping Cart Section (Mockup 2)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Multi-Cart Tabs Row (Point 10)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(tabs, key = { it.id }) { tab ->
                            val isActive = tab.id == activeTabId
                            val tabTitle = formatCartTabTitle(tab.name, strings.cartTitle)
                            val itemsCount = tab.items.size
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isActive) PrimaryPurple else PastelPurpleBg,
                                modifier = Modifier.clickable { cartManager.switchTab(tab.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 10.dp,
                                        end = if (tabs.size > 1) 4.dp else 10.dp,
                                        top = 4.dp,
                                        bottom = 4.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$tabTitle ($itemsCount)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isActive) Color.White else PrimaryPurple
                                    )
                                    if (tabs.size > 1) {
                                        Spacer(modifier = Modifier.width(2.dp))
                                        IconButton(
                                            onClick = { cartManager.closeTab(tab.id) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close tab",
                                                tint = if (isActive) Color.White.copy(alpha = 0.85f) else DokandaarTheme.textMuted,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (tabs.size < 5) {
                            item(key = "add_new_cart_tab") {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = DokandaarTheme.cardBackground,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.35f)),
                                    modifier = Modifier.clickable { cartManager.addNewTab() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Cart",
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Cart Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.cartTitle} (${cartManager.totalItemsCount} ${strings.items})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Clear Cart
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier.clickable { cartManager.clearCart() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = ErrorColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.clearCart, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                                }
                            }

                            // Add Discount
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PastelPurpleBg,
                                modifier = Modifier.clickable { showDiscountDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Discount,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.addDiscount, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = DokandaarTheme.border,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Cart Items List
                    if (cartItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.cartEmptySub,
                                fontSize = 12.sp,
                                color = DokandaarTheme.textMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(cartItems, key = { it.product.id }) { item ->
                                val formattedQty = formatCartQuantity(item.quantityMilli, item.product.unit)
                                val isPriceOverridden = item.customPricePaise != null && item.customPricePaise != item.product.price.paise

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DokandaarTheme.background.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { itemToEdit = item }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ProductThumbnail(
                                            imageUri = item.product.imageUri,
                                            size = 38.dp,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.displayName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DokandaarTheme.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            // Price display (strikethrough original if overridden)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isPriceOverridden) {
                                                    Text(
                                                        text = item.product.price.format(),
                                                        fontSize = 10.sp,
                                                        color = DokandaarTheme.textMuted,
                                                        textDecoration = TextDecoration.LineThrough
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = "${item.effectiveUnitPrice.format()} / ${item.product.unit.lowercase()}",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isPriceOverridden) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isPriceOverridden) PrimaryPurple else DokandaarTheme.textMuted
                                                )
                                            }

                                            // Line discount indicator
                                            if (item.discountPaise > 0L) {
                                                Text(
                                                    text = "Line Disc: -${Money(item.discountPaise).format()}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = PastelGreenContent
                                                )
                                            }
                                        }

                                        // Stepper - [qty] +
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PastelPurpleBg)
                                                .padding(horizontal = 2.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clickable { cartManager.decrement(item.product.id) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Remove,
                                                    contentDescription = "Decrease",
                                                    tint = PrimaryPurple,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            Text(
                                                text = formattedQty,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clickable { cartManager.increment(item.product.id) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Increase",
                                                    tint = PrimaryPurple,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = item.total.format(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DokandaarTheme.textPrimary
                                        )

                                        Spacer(modifier = Modifier.width(2.dp))

                                        IconButton(
                                            onClick = { cartManager.removeItem(item.product.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = DokandaarTheme.textMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Calculation Summary
                    HorizontalDivider(thickness = 1.dp, color = DokandaarTheme.border, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${strings.items}: ${cartManager.totalItemsCount}   Qty: ${cartManager.totalQuantityCount}", fontSize = 11.sp, color = DokandaarTheme.textMuted)
                        Text("${strings.subtotal}: ${cartManager.subtotal.format()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DokandaarTheme.textPrimary)
                    }

                    if (discountPaise > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.discountMinus, fontSize = 11.sp, color = PastelGreenContent)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("-${Money(discountPaise).format()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PastelGreenContent)
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Discount",
                                    tint = PastelGreenContent,
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable { showDiscountDialog = true }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val gstLabel = if (isGstEnabled && taxRatePercent > 0.0) "GST (${taxRatePercent.toInt()}%)" else "GST (0%)"
                        Text(gstLabel, fontSize = 11.sp, color = DokandaarTheme.textMuted)
                        Text(Money.fromPaise(taxPaise).format(), fontSize = 11.sp, color = DokandaarTheme.textMuted)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PastelPurpleBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.grandTotal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple
                            )
                            Text(
                                text = grandTotalMoney.format(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryPurple
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        var newCustName by remember { mutableStateOf("") }
        var newCustPhone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text(strings.newCustomer, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCustName,
                        onValueChange = { newCustName = it },
                        label = { Text(strings.customerName) },
                        placeholder = { Text("Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCustPhone,
                        onValueChange = { newCustPhone = it },
                        label = { Text(strings.customerPhone) },
                        placeholder = { Text("Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCustName.isNotBlank()) {
                        viewModel.addNewCustomer(newCustName, newCustPhone)
                        cartManager.setCustomer(newCustName, newCustPhone)
                        showAddCustomerDialog = false
                    }
                }) {
                    Text(strings.save, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Add Discount Dialog
    if (showDiscountDialog) {
        var isPercent by remember { mutableStateOf(false) }
        var discountInput by remember { mutableStateOf(if (discountPaise > 0) (discountPaise / 100.0).toString() else "") }

        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text(strings.addDiscount, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isPercent = false },
                            color = if (!isPercent) PrimaryPurple else PastelPurpleBg
                        ) {
                            Text(
                                text = strings.flatRupees,
                                color = if (!isPercent) Color.White else PrimaryPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isPercent = true },
                            color = if (isPercent) PrimaryPurple else PastelPurpleBg
                        ) {
                            Text(
                                text = strings.percentOff,
                                color = if (isPercent) Color.White else PrimaryPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        label = { Text(if (isPercent) "% Off" else strings.discountMinus) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val discPaise = if (isPercent) {
                        val pct = discountInput.toDoubleOrNull() ?: 0.0
                        ((cartManager.subtotal.paise * pct) / 100.0).toLong().coerceIn(0L, cartManager.subtotal.paise)
                    } else {
                        Money.fromRupees(discountInput).paise.coerceIn(0L, cartManager.subtotal.paise)
                    }
                    cartManager.setDiscount(discPaise)
                    viewModel.setDiscount(discPaise, cartManager.subtotal.paise)
                    showDiscountDialog = false
                }) {
                    Text(strings.apply, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Payment Sheet Dialog
    if (showPaymentDialog) {
        var selectedMode by remember { mutableStateOf(viewModel.selectedPaymentMethod.value) }
        var cashGiven by remember { mutableStateOf((grandTotalPaise / 100.0).toString()) }
        var splitCashStr by remember { mutableStateOf(((grandTotalPaise / 2) / 100.0).toString()) }
        var splitUpiStr by remember { mutableStateOf(((grandTotalPaise - grandTotalPaise / 2) / 100.0).toString()) }
        var creditCashPaidStr by remember { mutableStateOf("0") }

        // Live calculation depending on selected mode
        val cashPaise = Money.fromRupees(cashGiven).paise
        val splitCashPaise = Money.fromRupees(splitCashStr).paise
        val splitUpiPaise = Money.fromRupees(splitUpiStr).paise
        val creditCashPaise = Money.fromRupees(creditCashPaidStr).paise

        val (duePaise, changePaise) = when (selectedMode) {
            PaymentMethodMode.UPI -> Pair(0L, 0L)
            PaymentMethodMode.CASH -> {
                if (cashPaise >= grandTotalPaise) Pair(0L, cashPaise - grandTotalPaise)
                else Pair(grandTotalPaise - cashPaise, 0L)
            }
            PaymentMethodMode.SPLIT -> {
                val totalPaid = splitCashPaise + splitUpiPaise
                if (totalPaid >= grandTotalPaise) Pair(0L, totalPaid - grandTotalPaise)
                else Pair(grandTotalPaise - totalPaid, 0L)
            }
            PaymentMethodMode.OTHER -> {
                if (creditCashPaise >= grandTotalPaise) Pair(0L, creditCashPaise - grandTotalPaise)
                else Pair(grandTotalPaise - creditCashPaise, 0L)
            }
        }

        // Customer details for Credit Check
        val selectedCustomer = customers.firstOrNull {
            (customerPhone.isNotBlank() && it.phone == customerPhone.trim()) ||
            (customerName.isNotBlank() && customerName != "Walk-in Customer" && it.name.equals(customerName.trim(), ignoreCase = true))
        }
        val creditLimitPaise = selectedCustomer?.creditLimitPaise ?: 0L
        val currentBalancePaise = selectedCustomer?.openingBalancePaise ?: 0L
        val isCreditLimitExceeded = duePaise > 0L && creditLimitPaise > 0L && (currentBalancePaise + duePaise) > creditLimitPaise

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.paymentMode, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(Money(grandTotalPaise).format(), fontWeight = FontWeight.ExtraBold, color = PrimaryPurple, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Payment Mode Selector Tabs: [UPI] [Cash] [Split] [Udhaar / Credit]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair(strings.upiOnline, PaymentMethodMode.UPI),
                            Pair(strings.cash, PaymentMethodMode.CASH),
                            Pair("Split", PaymentMethodMode.SPLIT),
                            Pair(strings.customerKhata, PaymentMethodMode.OTHER)
                        ).forEach { (label, mode) ->
                            val isSel = selectedMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedMode = mode
                                        viewModel.setPaymentMethod(mode)
                                    },
                                color = if (isSel) PrimaryPurple else PastelPurpleBg
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.White else PrimaryPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    when (selectedMode) {
                        PaymentMethodMode.UPI -> {
                            val upiQrBitmap = remember(shopProfile.upiId, shopProfile.name, grandTotalPaise) {
                                if (shopProfile.upiId.isNotBlank()) {
                                    UpiQrGenerator.generateUpiQrBitmap(
                                        upiId = shopProfile.upiId,
                                        shopName = shopProfile.name,
                                        amount = Money(grandTotalPaise),
                                        transactionRef = "INV-${(System.currentTimeMillis() / 1000) % 1000000}"
                                    )
                                } else null
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (upiQrBitmap != null) {
                                    Image(
                                        bitmap = upiQrBitmap.asImageBitmap(),
                                        contentDescription = "Dynamic UPI QR Code",
                                        modifier = Modifier
                                            .size(170.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, PastelPurpleBg, RoundedCornerShape(12.dp))
                                    )
                                    Text(
                                        text = strings.scanWithApps,
                                        fontSize = 11.sp,
                                        color = DokandaarTheme.textMuted
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(PastelPurpleBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = strings.setShopUpiPrompt,
                                            fontSize = 11.sp,
                                            color = PrimaryPurple,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                if (shopProfile.upiId.isNotBlank()) {
                                    Text(
                                        text = "UPI ID: ${shopProfile.upiId}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryPurple
                                    )
                                }
                                Text(
                                    text = "${strings.payableAmountLabel} ${Money(grandTotalPaise).format()}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DokandaarTheme.textPrimary
                                )
                            }
                        }

                        PaymentMethodMode.CASH -> {
                            OutlinedTextField(
                                value = cashGiven,
                                onValueChange = { cashGiven = it },
                                label = { Text(strings.cashReceivedInput) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.changeToReturn, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PastelGreenContent)
                                Text(Money(changePaise).format(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PastelGreenContent)
                            }
                            if (duePaise > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(strings.balanceDueKhata, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                                    Text(Money(duePaise).format(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ErrorColor)
                                }
                            }
                        }

                        PaymentMethodMode.SPLIT -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = splitCashStr,
                                    onValueChange = { splitCashStr = it },
                                    label = { Text(strings.cashAmountInput) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = splitUpiStr,
                                    onValueChange = { splitUpiStr = it },
                                    label = { Text(strings.upiAmountInput) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val splitTotalPaid = splitCashPaise + splitUpiPaise
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(strings.totalPaidLabel, fontSize = 12.sp, color = DokandaarTheme.textMuted)
                                    Text(Money(splitTotalPaid).format(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                if (changePaise > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(strings.changeToReturnCash, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PastelGreenContent)
                                        Text(Money(changePaise).format(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PastelGreenContent)
                                    }
                                }

                                if (duePaise > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(strings.balanceDueKhata, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                                        Text(Money(duePaise).format(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ErrorColor)
                                    }
                                }

                                if (splitUpiPaise > 0L && shopProfile.upiId.isNotBlank()) {
                                    val splitQrBitmap = remember(shopProfile.upiId, shopProfile.name, splitUpiPaise) {
                                        UpiQrGenerator.generateUpiQrBitmap(
                                            upiId = shopProfile.upiId,
                                            shopName = shopProfile.name,
                                            amount = Money(splitUpiPaise),
                                            transactionRef = "SPLIT-${(System.currentTimeMillis() / 1000) % 1000000}"
                                        )
                                    }
                                    if (splitQrBitmap != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PastelPurpleBg)
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Image(
                                                bitmap = splitQrBitmap.asImageBitmap(),
                                                contentDescription = "Split UPI QR",
                                                modifier = Modifier.size(60.dp)
                                            )
                                            Column {
                                                Text("Scan for UPI part: ${Money(splitUpiPaise).format()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                                                Text(shopProfile.upiId, fontSize = 10.sp, color = DokandaarTheme.textMuted)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        PaymentMethodMode.OTHER -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = creditCashPaidStr,
                                    onValueChange = { creditCashPaidStr = it },
                                    label = { Text(strings.amountPaidNowInput) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(strings.balanceDueKhata, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                                    Text(Money(duePaise).format(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ErrorColor)
                                }

                                if (customerName.isBlank() || customerName == "Walk-in Customer") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = ErrorColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = strings.udhaarCustomerRequired,
                                            color = ErrorColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else if (selectedCustomer != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DokandaarTheme.background.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "Customer: ${selectedCustomer.name}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("${strings.currentKhataLabel} ${Money(currentBalancePaise).format()}", fontSize = 11.sp, color = DokandaarTheme.textMuted)
                                                if (creditLimitPaise > 0L) {
                                                    Text("${strings.creditLimitLabel} ${Money(creditLimitPaise).format()}", fontSize = 11.sp, color = DokandaarTheme.textMuted)
                                                }
                                            }
                                        }
                                    }

                                    if (isCreditLimitExceeded) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFEF2F2),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = strings.creditLimitExceededPrompt,
                                                    color = Color(0xFFDC2626),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val proceedWithSale: (Boolean) -> Unit = { managerApproved ->
                            when (selectedMode) {
                                PaymentMethodMode.UPI -> {
                                    viewModel.setCashReceived(0L)
                                    viewModel.setSplitPayment(0L, grandTotalPaise)
                                }
                                PaymentMethodMode.CASH -> {
                                    viewModel.setCashReceived(cashPaise)
                                }
                                PaymentMethodMode.SPLIT -> {
                                    viewModel.setSplitPayment(splitCashPaise, splitUpiPaise)
                                    viewModel.setCashReceived(splitCashPaise)
                                }
                                PaymentMethodMode.OTHER -> {
                                    viewModel.setCashReceived(creditCashPaise)
                                }
                            }
                            viewModel.setPaymentMethod(selectedMode)
                            viewModel.setCustomer(customerName, customerPhone, selectedCustomer?.id)
                            viewModel.completeSale(
                                items = cartItems,
                                subtotal = cartManager.subtotal,
                                managerApprovedCreditLimit = managerApproved
                            ) {
                                // Handled by saleSuccess state
                            }
                        }

                        if (isCreditLimitExceeded) {
                            managerPinAction = { proceedWithSale(true) }
                            showManagerPinDialog = true
                        } else {
                            proceedWithSale(false)
                        }
                    }
                ) {
                    Text(strings.finishAndBill, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Receipt & Print Dialog
    if (showReceiptDialog) {
        val lastReceipt = remember { viewModel.getLastInvoiceReceipt() }
        val receiptType = lastReceipt?.receiptType ?: if (isCompositionScheme) "Bill of Supply" else "Tax Invoice"

        AlertDialog(
            onDismissRequest = {
                showReceiptDialog = false
                viewModel.clearMessages()
                onSaleCompleted()
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PastelGreenContent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.billCompleted, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PastelPurpleBg
                    ) {
                        Text(
                            text = receiptType,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (lastReceipt != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DokandaarTheme.background.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(strings.invoiceHash, fontSize = 12.sp, color = DokandaarTheme.textMuted)
                                    Text(lastReceipt.invoiceNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                if (lastReceipt.cashierName.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Cashier:", fontSize = 12.sp, color = DokandaarTheme.textMuted)
                                        Text(lastReceipt.cashierName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (!lastReceipt.customerName.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Customer:", fontSize = 12.sp, color = DokandaarTheme.textMuted)
                                        Text(lastReceipt.customerName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(strings.grandTotalLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(Money(lastReceipt.grandTotalPaise).format(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryPurple)
                                }
                                if (lastReceipt.cashReceivedPaise > 0L) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(strings.cashPaidLabel, fontSize = 12.sp, color = DokandaarTheme.textMuted)
                                        Text(Money(lastReceipt.cashReceivedPaise).format(), fontSize = 12.sp)
                                    }
                                }
                                if (lastReceipt.upiPaidPaise > 0L) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(strings.upiPaidLabel, fontSize = 12.sp, color = DokandaarTheme.textMuted)
                                        Text(Money(lastReceipt.upiPaidPaise).format(), fontSize = 12.sp)
                                    }
                                }
                                if (lastReceipt.duePaise > 0L) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(strings.balanceDueKhata, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                                        Text(Money(lastReceipt.duePaise).format(), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ErrorColor)
                                    }
                                }
                            }
                        }
                    }

                    if (isPrinting) {
                        Text(strings.printingThermalBill, color = PrimaryPurple, fontSize = 12.sp)
                    } else if (printSuccess) {
                        Text(strings.billPrintedSuccess, color = PastelGreenContent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val waUrl = viewModel.getLastInvoiceWhatsAppUrl()
                            if (waUrl != null) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                context.startActivity(intent)
                            } else {
                                FeedbackManager.showToast(context, "Customer phone number not available for WhatsApp bill")
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = PastelGreenContent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.whatsApp, fontWeight = FontWeight.Bold, color = PastelGreenContent)
                        }
                    }

                    TextButton(
                        onClick = {
                            viewModel.printLastReceipt {
                                showReceiptDialog = false
                                viewModel.clearMessages()
                                onSaleCompleted()
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.quickPrint, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReceiptDialog = false
                    viewModel.clearMessages()
                    onSaleCompleted()
                }) {
                    Text(strings.newSalePos)
                }
            }
        )
    }

    // Shift Open Dialog if Cashier needs to open shift
    if (showShiftOpenDialog) {
        ShiftOpenDialog(
            onDismissRequest = { showShiftOpenDialog = false },
            onShiftOpened = { _ ->
                showShiftOpenDialog = false
                showPaymentDialog = true
            }
        )
    }

    // Manager PIN Dialog for Credit Limit override
    if (showManagerPinDialog) {
        ManagerPinDialog(
            title = "Credit Limit Exceeded",
            subtitle = "Manager or Owner PIN required to authorize credit override",
            onDismissRequest = {
                showManagerPinDialog = false
                managerPinAction = null
            },
            onAuthorized = {
                showManagerPinDialog = false
                managerPinAction?.invoke()
                managerPinAction = null
            }
        )
    }

    // Customer Selection BottomSheet
    if (showCustomerBottomSheet) {
        CustomerSelectionBottomSheet(
            customers = customers,
            onDismissRequest = { showCustomerBottomSheet = false },
            onSelectCustomer = { cust ->
                cartManager.setCustomer(cust.name, cust.phone, cust.id)
                viewModel.setCustomer(cust.name, cust.phone)
            },
            onSelectWalkIn = {
                cartManager.setCustomer("Walk-in Customer", "", null)
                viewModel.setCustomer("Walk-in Customer", "")
            },
            onAddNewCustomer = {
                showAddCustomerDialog = true
            },
            onReactivateCustomer = { cust ->
                viewModel.reactivateCustomer(cust)
            }
        )
    }

    // Add Custom Item Dialog
    if (showAddCustomItemDialog) {
        AddCustomItemDialog(
            onDismissRequest = { showAddCustomItemDialog = false },
            onConfirm = { name, pricePaise, quantityMilli, unit ->
                cartManager.addCustomItem(name, pricePaise, quantityMilli, unit)
                showAddCustomItemDialog = false
            }
        )
    }

    // Cart Item Edit Dialog (Price Override, Line Discount, Quantity Editing)
    itemToEdit?.let { item ->
        CartItemEditDialog(
            item = item,
            onDismissRequest = { itemToEdit = null },
            onSave = { newQtyMilli, newPricePaise, lineDiscPaise ->
                val warning = cartManager.setQuantityMilli(item.product.id, newQtyMilli)
                if (warning != null) {
                    FeedbackManager.showToast(context, warning)
                }

                if (newPricePaise != item.product.price.paise) {
                    cartManager.overrideItemPrice(item.product.id, newPricePaise)
                    viewModel.logPriceOverride(
                        productId = item.product.id,
                        productName = item.displayName,
                        oldPricePaise = item.product.price.paise,
                        newPricePaise = newPricePaise
                    )
                } else if (item.customPricePaise != null && newPricePaise == item.product.price.paise) {
                    cartManager.overrideItemPrice(item.product.id, newPricePaise)
                }

                cartManager.setItemDiscount(item.product.id, lineDiscPaise)
                itemToEdit = null
            },
            onRemove = {
                cartManager.removeItem(item.product.id)
                itemToEdit = null
            }
        )
    }

    // Reactivate Product Dialog
    productToReactivate?.let { prod ->
        ReactivateProductDialog(
            product = prod,
            onDismissRequest = { productToReactivate = null },
            onConfirmReactivate = {
                viewModel.reactivateProduct(prod)
                val (success, warning) = cartManager.addProduct(prod.copy(isActive = true), 1000L)
                if (warning != null) {
                    FeedbackManager.showToast(context, warning)
                }
                productToReactivate = null
            }
        )
    }
}
