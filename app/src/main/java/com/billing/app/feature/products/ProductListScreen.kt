package com.billing.app.feature.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.components.BottomDokandaarNav
import com.billing.app.core.ui.components.CategoryCard
import com.billing.app.core.ui.components.DokandaarActionPill
import com.billing.app.core.ui.components.DokandaarSearchBar
import com.billing.app.core.ui.components.DokandaarTab
import com.billing.app.core.ui.components.ManagerPinDialog
import com.billing.app.core.ui.components.ProductThumbnail
import com.billing.app.core.ui.theme.DarkPurple
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
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
import com.billing.app.core.util.Quantity
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.StockAdjustmentReason

@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (String) -> Unit,
    onNavigateToScannerForSearch: () -> Unit,
    onNavigateToTab: (DokandaarTab) -> Unit
) {
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val allCount by viewModel.allCount.collectAsState()
    val strings = appStrings()

    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var productPendingStockAdjust by remember { mutableStateOf<Product?>(null) }
    var productToAdjustStock by remember { mutableStateOf<Product?>(null) }
    var adjustStockStr by remember { mutableStateOf("") }
    var adjustReason by remember { mutableStateOf(StockAdjustmentReason.CORRECTION) }
    var adjustNotes by remember { mutableStateOf("") }
    var showFilterMenu by remember { mutableStateOf(false) }

    val dbCategories by viewModel.categories.collectAsState()

    // Manager PIN Gate for Stock Adjustment
    if (productPendingStockAdjust != null) {
        val prod = productPendingStockAdjust!!
        ManagerPinDialog(
            title = "Manager Authorization",
            subtitle = "Enter Manager PIN to adjust stock for ${prod.name}",
            onDismissRequest = { productPendingStockAdjust = null },
            onAuthorized = {
                productPendingStockAdjust = null
                productToAdjustStock = prod
                adjustStockStr = prod.stock.toString()
            }
        )
    }

    // Stock Adjustment Dialog
    if (productToAdjustStock != null) {
        val prod = productToAdjustStock!!
        AlertDialog(
            onDismissRequest = { productToAdjustStock = null },
            title = {
                Column {
                    Text(strings.quickStockAdjust, fontWeight = FontWeight.Bold)
                    Text(prod.name, fontSize = 12.sp, color = DokandaarTheme.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            text = {
                Column {
                    Text("${strings.currentStock}: ${Quantity.formatDisplay(prod.stockMilli, prod.unit)}", fontSize = 13.sp, color = DokandaarTheme.textMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = adjustStockStr,
                        onValueChange = { adjustStockStr = it },
                        label = { Text(strings.enterQuantity) },
                        placeholder = { Text(strings.enterQuantity) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(strings.returnReason, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DokandaarTheme.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(StockAdjustmentReason.values()) { reason ->
                            val isSelected = adjustReason == reason
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PrimaryPurple else DokandaarTheme.cardBackground,
                                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border) else null,
                                modifier = Modifier.clickable { adjustReason = reason }
                            ) {
                                Text(
                                    text = reason.displayNameBn,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else DokandaarTheme.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adjustNotes,
                        onValueChange = { adjustNotes = it },
                        label = { Text("${strings.notes} (${strings.optional})") },
                        placeholder = { Text(strings.paymentNotePlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newQty = adjustStockStr.toIntOrNull()
                        if (newQty != null) {
                            viewModel.adjustStock(
                                productId = prod.id,
                                newStock = newQty,
                                reason = adjustReason,
                                notes = adjustNotes
                            )
                        }
                        productToAdjustStock = null
                        adjustNotes = ""
                    }
                ) {
                    Text(strings.save, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToAdjustStock = null }) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted)
                }
            },
            containerColor = DokandaarTheme.cardBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        val product = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text(strings.deleteProductTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.deleteProductPrompt(product.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProduct(product.id)
                        productToDelete = null
                    }
                ) {
                    Text(strings.delete, color = ErrorColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted, maxLines = 1, softWrap = false)
                }
            },
            containerColor = DokandaarTheme.cardBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            // Purple Top App Bar matching Screenshot 4
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
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.productsTitle,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (products.isNotEmpty()) strings.productsInCatalog(products.size) else strings.productsSubtitle,
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
                                onClick = { /* Search is available in bar below */ },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }

                            // Filter Button
                            Box {
                                IconButton(
                                    onClick = { showFilterMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filter",
                                        tint = Color.White
                                    )
                                }
                                DropdownMenu(
                                    expanded = showFilterMenu,
                                    onDismissRequest = { showFilterMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(strings.filterAll) },
                                        onClick = {
                                            viewModel.setFilter(StockFilter.ALL)
                                            showFilterMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("${strings.filterLowStock} (1-5)") },
                                        onClick = {
                                            viewModel.setFilter(StockFilter.LOW_STOCK)
                                            showFilterMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("${strings.filterOutOfStock} (0)") },
                                        onClick = {
                                            viewModel.setFilter(StockFilter.OUT_OF_STOCK)
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }

                            // "+ Add Product" Button
                            DokandaarActionPill(
                                text = strings.quickAddProduct,
                                onClick = onNavigateToAddProduct
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            BottomDokandaarNav(
                selectedTab = DokandaarTab.PRODUCTS,
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
            // Search Input with Barcode Scanner Icon matching Screenshot 4
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                DokandaarSearchBar(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = strings.searchProducts,
                    modifier = Modifier.fillMaxWidth(),
                    trailingContent = {
                        IconButton(
                            onClick = onNavigateToScannerForSearch,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = strings.scanQrBarcode,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }

            // Category Filter Cards matching Mockup Screenshot 4 / 7
            val categoryCounts = remember(products) {
                products.groupBy { it.category }.mapValues { it.value.size }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // "All (count)" card
                item(key = "all") {
                    CategoryCard(
                        title = "${strings.filterAll} ($allCount)",
                        isSelected = selectedCategory.isBlank() || selectedCategory.equals("All", ignoreCase = true),
                        isAllItem = true,
                        onClick = { viewModel.setCategory("All") }
                    )
                }

                // Dynamic Category cards from database
                items(dbCategories, key = { it.id }) { cat ->
                    CategoryCard(
                        title = cat.name,
                        isSelected = selectedCategory.equals(cat.name, ignoreCase = true),
                        iconName = cat.iconName,
                        imageUri = cat.imageUri,
                        onClick = { viewModel.setCategory(cat.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pagination setup
            val pageSize = 10
            val visibleCount by viewModel.visibleItemCount.collectAsState()
            val isLoadingMore by viewModel.isLoadingMore.collectAsState()
            val displayedProducts = remember(filteredProducts, visibleCount) {
                filteredProducts.take(visibleCount)
            }
            val hasMore = visibleCount < filteredProducts.size

            val listState = rememberLazyListState()
            listState.OnBottomReached(buffer = 5, enabled = hasMore && !isLoadingMore) {
                viewModel.loadMoreProducts()
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.noProductsFound,
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
                    items(displayedProducts, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onEdit = { onNavigateToEditProduct(product.id) },
                            onAdjustStock = {
                                productPendingStockAdjust = product
                            },
                            onDelete = { productToDelete = product }
                        )
                    }

                    item(key = "pagination_footer") {
                        LazyLoadingFooter(
                            isLoadingMore = isLoadingMore,
                            hasMore = hasMore,
                            totalCount = filteredProducts.size,
                            itemLabel = "products"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductItemCard(
    product: Product,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = appStrings()
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // Product Thumbnail
            ProductThumbnail(
                product = product,
                size = 54.dp,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DokandaarTheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "SKU: ${product.sku.ifBlank { "N/A" }} | Barcode: ${product.barcode}",
                    fontSize = 11.sp,
                    color = DokandaarTheme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stock Badge - Tri-state (Out, Low, In Stock)
                val isLow = product.stock in 1..5
                val isOut = product.stock <= 0
                val badgeBg = when {
                    isOut -> PastelRedBg
                    isLow -> PastelOrangeBg
                    else -> PastelGreenBg
                }
                val badgeColor = when {
                    isOut -> PastelRedContent
                    isLow -> PastelOrangeContent
                    else -> PastelGreenContent
                }
                val stockDisplay = Quantity.formatDisplay(product.stockMilli, product.unit)
                val badgeText = when {
                    isOut -> strings.filterOutOfStock
                    isLow -> strings.lowStockColon(stockDisplay)
                    else -> strings.inStockColon(stockDisplay)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Price & 3-dots Menu
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
                            text = { Text(strings.editProductTitle) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.adjustStock) },
                            onClick = {
                                menuExpanded = false
                                onAdjustStock()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.delete, color = ErrorColor) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.price.format(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            }
        }
    }
}
