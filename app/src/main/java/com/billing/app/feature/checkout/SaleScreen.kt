package com.billing.app.feature.checkout

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
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
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.cart.CartManager
import com.billing.app.core.qr.UpiQrGenerator
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.components.CategoryCard
import com.billing.app.core.ui.components.DokandaarBottomBarCta
import com.billing.app.core.ui.components.DokandaarBottomIconButton
import com.billing.app.core.ui.components.DokandaarSearchBar
import com.billing.app.core.ui.components.ProductThumbnail
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
import com.billing.app.domain.model.Product

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

    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val customers by viewModel.customers.collectAsState()

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()
    val saleSuccess by viewModel.saleSuccess.collectAsState()
    val printSuccess by viewModel.printSuccess.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()

    var showCustomerMenu by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }

    val isGstEnabled = viewModel.isGstEnabled
    val taxRatePercent = viewModel.taxRatePercent
    val taxPaise = viewModel.calculateTax(cartManager.subtotal.paise, discountPaise)
    val grandTotalPaise = (cartManager.subtotal.paise - discountPaise + taxPaise).coerceAtLeast(0L)
    val grandTotalMoney = Money.fromPaise(grandTotalPaise)

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
                title = "Sale",
                subtitle = "Kirana Store POS",
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
                                text = { Text("Clear Cart") },
                                onClick = {
                                    cartManager.clearCart()
                                    showSaleMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hold Bill / Save Draft") },
                                onClick = {
                                    showSaleMenu = false
                                    viewModel.holdDraft(cartItems, cartManager.subtotal) {
                                        cartManager.clearCart()
                                        onNavigateBack()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Held Drafts") },
                                onClick = {
                                    showSaleMenu = false
                                    onNavigateToHeldDrafts()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Printer Settings") },
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
                                showPaymentDialog = true
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
                Box {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DokandaarTheme.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
                        modifier = Modifier.clickable { showCustomerMenu = true }
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

                    DropdownMenu(
                        expanded = showCustomerMenu,
                        onDismissRequest = { showCustomerMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.walkInCustomer, fontWeight = FontWeight.Bold) },
                            onClick = {
                                cartManager.setCustomer("Walk-in Customer", "")
                                showCustomerMenu = false
                            }
                        )
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text("${cust.name} (${cust.phone})") },
                                onClick = {
                                    cartManager.setCustomer(cust.name, cust.phone)
                                    showCustomerMenu = false
                                }
                            )
                        }
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

            // 2. Product Search Bar with Scanner Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                DokandaarSearchBar(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = strings.searchProducts,
                    modifier = Modifier.fillMaxWidth(),
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

            // 4. Products Display Row (Horizontal Cards with + button)
            if (products.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        Card(
                            modifier = Modifier
                                .width(125.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { cartManager.addProduct(product, 1) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ProductThumbnail(
                                    imageUri = product.imageUri,
                                    size = 56.dp,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = product.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DokandaarTheme.textPrimary,
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
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryPurple),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cartItems, key = { it.product.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
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
                                            text = item.product.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DokandaarTheme.textPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${item.product.price.format()} / pc",
                                            fontSize = 10.sp,
                                            color = DokandaarTheme.textMuted
                                        )
                                    }

                                    // Stepper - [qty] +
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PastelPurpleBg)
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clickable { cartManager.decrement(item.product.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Decrease",
                                                tint = PrimaryPurple,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        Text(
                                            text = "${item.quantity}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryPurple,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clickable { cartManager.increment(item.product.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Increase",
                                                tint = PrimaryPurple,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = item.total.format(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DokandaarTheme.textPrimary
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { cartManager.removeItem(item.product.id) },
                                        modifier = Modifier.size(24.dp)
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
                            Text("Discount (-)", fontSize = 11.sp, color = PastelGreenContent)
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
            title = { Text("Add Customer", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCustName,
                        onValueChange = { newCustName = it },
                        label = { Text("Customer Name *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCustPhone,
                        onValueChange = { newCustPhone = it },
                        label = { Text("Mobile Number") },
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
                    Text("Save Customer", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text("Cancel")
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
            title = { Text("Apply Bill Discount", fontWeight = FontWeight.Bold) },
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
                                text = "Flat (₹)",
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
                                text = "Percentage (%)",
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
                        label = { Text(if (isPercent) "Discount Percentage (%)" else "Discount Amount (₹)") },
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
                    Text("Apply", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Payment Dialog
    if (showPaymentDialog) {
        var selectedMode by remember { mutableStateOf(PaymentMethodMode.UPI) }
        var cashGiven by remember { mutableStateOf((grandTotalPaise / 100.0).toString()) }

        val cashPaise = Money.fromRupees(cashGiven).paise
        val changePaise = maxOf(0L, cashPaise - grandTotalPaise)
        val duePaise = maxOf(0L, grandTotalPaise - cashPaise)

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Complete Payment", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(Money(grandTotalPaise).format(), fontWeight = FontWeight.ExtraBold, color = PrimaryPurple, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Payment Mode Selector Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair("UPI", PaymentMethodMode.UPI),
                            Pair("Cash", PaymentMethodMode.CASH),
                            Pair("Credit / Due", PaymentMethodMode.OTHER)
                        ).forEach { (label, mode) ->
                            val isSel = selectedMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedMode = mode
                                        viewModel.setPaymentMethod(mode)
                                        if (mode == PaymentMethodMode.OTHER && cashGiven == (grandTotalPaise / 100.0).toString()) {
                                            cashGiven = "0"
                                        } else if (mode == PaymentMethodMode.CASH && cashGiven == "0") {
                                            cashGiven = (grandTotalPaise / 100.0).toString()
                                        }
                                    },
                                color = if (isSel) PrimaryPurple else PastelPurpleBg
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.White else PrimaryPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    if (selectedMode == PaymentMethodMode.CASH) {
                        OutlinedTextField(
                            value = cashGiven,
                            onValueChange = { cashGiven = it },
                            label = { Text("Cash Received (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Change to Return:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PastelGreenContent)
                            Text(Money(changePaise).format(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PastelGreenContent)
                        }
                        if (duePaise > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Balance Due:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                                Text(Money(duePaise).format(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ErrorColor)
                            }
                        }
                    } else if (selectedMode == PaymentMethodMode.OTHER) {
                        OutlinedTextField(
                            value = cashGiven,
                            onValueChange = { cashGiven = it },
                            label = { Text("Amount Paid Now (₹) - Rest is Due") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Balance Due (Khata):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                            Text(Money(duePaise).format(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ErrorColor)
                        }
                        if (customerName.isBlank() || customerName == "Walk-in Customer") {
                            Text(
                                text = "⚠️ Notice: Due/Khata bill requires an actual Customer name.",
                                color = ErrorColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (selectedMode == PaymentMethodMode.UPI) {
                        Text(
                            text = "Customer can scan UPI QR code via GPay, PhonePe, Paytm",
                            fontSize = 12.sp,
                            color = DokandaarTheme.textMuted
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val enteredCashPaise = if (selectedMode == PaymentMethodMode.UPI) {
                            grandTotalPaise
                        } else {
                            Money.fromRupees(cashGiven).paise
                        }
                        viewModel.setCashReceived(enteredCashPaise)
                        viewModel.setCustomer(customerName, customerPhone)
                        viewModel.completeSale(cartItems, cartManager.subtotal) {
                            // Handled by saleSuccess state
                        }
                    }
                ) {
                    Text("Confirm & Print Bill", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Receipt & Print Dialog
    if (showReceiptDialog) {
        AlertDialog(
            onDismissRequest = {
                showReceiptDialog = false
                onSaleCompleted()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = PastelGreenContent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sale Completed!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("The invoice has been saved and stock updated.")
                    Spacer(modifier = Modifier.height(6.dp))
                    if (isPrinting) {
                        Text("Printing thermal receipt...", color = PrimaryPurple)
                    } else if (printSuccess) {
                        Text("Receipt printed successfully!", color = PastelGreenContent)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.printLastReceipt {
                            showReceiptDialog = false
                            onSaleCompleted()
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print Receipt", fontWeight = FontWeight.Bold, color = PrimaryPurple)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReceiptDialog = false
                    onSaleCompleted()
                }) {
                    Text("Done")
                }
            }
        )
    }
}
