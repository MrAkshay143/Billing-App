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
import androidx.compose.material.icons.filled.Add
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
import com.billing.app.core.ui.components.ProductThumbnail
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.BorderColor
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
    onSaleCompleted: () -> Unit
) {
    val context = LocalContext.current
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
                    IconButton(
                        onClick = { /* Menu */ },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
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
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.holdDraft(cartItems, cartManager.subtotal) {
                                    cartManager.clearCart()
                                    onNavigateBack()
                                }
                            },
                        color = PastelPurpleBg
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Hold", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                        }
                    }

                    // Discard Button
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { cartManager.clearCart() },
                        color = Color(0xFFFEE2E2)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = ErrorColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Discard", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                        }
                    }

                    // Proceed to Pay Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (cartItems.isNotEmpty()) {
                                    viewModel.setCustomer(customerName, customerPhone)
                                    viewModel.setDiscount(discountPaise, cartManager.subtotal.paise)
                                    showPaymentDialog = true
                                }
                            },
                        color = if (cartItems.isNotEmpty()) PrimaryPurple else PrimaryPurple.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Proceed to Pay",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cartManager.grandTotal.format(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
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
                                color = DokandaarTheme.textPrimary
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
                            text = { Text("Walk-in Customer", fontWeight = FontWeight.Bold) },
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
                            text = "Add Customer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                    }
                }
            }

            // 2. Product Search Bar with Scanner Icon
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = DokandaarTheme.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Search product by name, barcode or SKU...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = onNavigateToScanner,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 3. Category Chips (Horizontal Scroll)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isSelected = selectedCategory.equals("All", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryPurple else DokandaarTheme.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) PrimaryPurple else DokandaarTheme.border
                        ),
                        modifier = Modifier.clickable { viewModel.selectedCategory.value = "All" }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else PrimaryPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else DokandaarTheme.textPrimary
                            )
                        }
                    }
                }

                items(categories) { cat ->
                    val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryPurple else DokandaarTheme.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) PrimaryPurple else DokandaarTheme.border
                        ),
                        modifier = Modifier.clickable { viewModel.selectedCategory.value = cat.name }
                    ) {
                        Text(
                            text = cat.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else DokandaarTheme.textPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
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
                                text = "Cart (${cartManager.totalItemsCount} Items)",
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
                                    Text("Clear Cart", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
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
                                    Text("Add Discount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
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
                                text = "Cart is empty. Tap products above or scan barcodes.",
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
                                            modifier = Modifier.size(14.dp)
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
                        Text("Items: ${cartManager.totalItemsCount}   Qty: ${cartManager.totalQuantityCount}", fontSize = 11.sp, color = DokandaarTheme.textMuted)
                        Text("Subtotal: ${cartManager.subtotal.format()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DokandaarTheme.textPrimary)
                    }

                    if (discountPaise > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discount (-)", fontSize = 11.sp, color = PastelGreenContent)
                            Text("-${Money(discountPaise).format()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PastelGreenContent)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GST (0%)", fontSize = 11.sp, color = DokandaarTheme.textMuted)
                        Text("₹0.00", fontSize = 11.sp, color = DokandaarTheme.textMuted)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grand Total",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                        Text(
                            text = cartManager.grandTotal.format(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryPurple
                        )
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
        var discountInput by remember { mutableStateOf((discountPaise / 100.0).toString()) }

        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Apply Bill Discount", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it },
                    label = { Text("Discount Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = discountInput.toDoubleOrNull() ?: 0.0
                    cartManager.setDiscount((amount * 100).toLong())
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
        var cashGiven by remember { mutableStateOf((cartManager.grandTotal.paise / 100.0).toString()) }

        val grandTotalPaise = cartManager.grandTotal.paise
        val cashVal = (cashGiven.toDoubleOrNull() ?: 0.0) * 100
        val changePaise = maxOf(0L, (cashVal - grandTotalPaise).toLong())
        val duePaise = maxOf(0L, (grandTotalPaise - cashVal).toLong())

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Complete Payment", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(cartManager.grandTotal.format(), fontWeight = FontWeight.ExtraBold, color = PrimaryPurple, fontSize = 18.sp)
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
                            Pair("Card / Due", PaymentMethodMode.OTHER)
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
                        val cashPaise = ((cashGiven.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        viewModel.setCashReceived(cashPaise)
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
