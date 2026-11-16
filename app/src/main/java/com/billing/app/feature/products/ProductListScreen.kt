package com.billing.app.feature.products

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.components.BottomDokandaarNav
import com.billing.app.core.ui.components.DokandaarTab
import com.billing.app.core.ui.components.ProductThumbnail
import com.billing.app.core.ui.components.StockStatusBadge
import com.billing.app.core.ui.components.TopDokandaarHeader
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.LowStockBg
import com.billing.app.core.ui.theme.LowStockText
import com.billing.app.core.ui.theme.OutOfStockBg
import com.billing.app.core.ui.theme.OutOfStockText
import com.billing.app.core.ui.theme.TextMutedColor
import com.billing.app.domain.model.Product

@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (String) -> Unit,
    onNavigateToScannerForSearch: () -> Unit,
    onNavigateToTab: (DokandaarTab) -> Unit,
    onNavigateToInvoices: () -> Unit = { onNavigateToTab(DokandaarTab.INVOICES) },
    onNavigateToSettings: () -> Unit = { onNavigateToTab(DokandaarTab.MORE) }
) {
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val allCount by viewModel.allCount.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val outOfStockCount by viewModel.outOfStockCount.collectAsState()

    var productToDelete by remember { mutableStateOf<Product?>(null) }

    if (productToDelete != null) {
        val product = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${product.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProduct(product.id)
                        productToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
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
                onNotificationClick = onNavigateToInvoices,
                onMenuClick = onNavigateToSettings
            )
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Products Header Card with [+ Add Product]
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
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = HeaderGradientStart,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Products",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Manage your store products",
                            fontSize = 12.sp,
                            color = TextMutedColor
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onNavigateToAddProduct() },
                        color = HeaderGradientStart,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Product",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar with Barcode Scanner Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = {
                        Text(
                            text = "Search by name, barcode or QR...",
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.searchQuery.value = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AddProductBg)
                                    .clickable { onNavigateToScannerForSearch() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Barcode",
                                    tint = HeaderGradientStart,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stock Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Chip
                FilterChipPill(
                    label = "All ($allCount)",
                    isSelected = activeFilter == StockFilter.ALL,
                    onClick = { viewModel.setFilter(StockFilter.ALL) }
                )

                // Low Stock Chip
                FilterChipPill(
                    label = "Low Stock ($lowStockCount)",
                    icon = Icons.Default.Warning,
                    isSelected = activeFilter == StockFilter.LOW_STOCK,
                    onClick = { viewModel.setFilter(StockFilter.LOW_STOCK) },
                    selectedBg = HeaderGradientStart,
                    unselectedBg = LowStockBg,
                    unselectedText = LowStockText
                )

                // Out of Stock Chip
                FilterChipPill(
                    label = "Out of Stock ($outOfStockCount)",
                    icon = Icons.Default.DoNotDisturbOn,
                    isSelected = activeFilter == StockFilter.OUT_OF_STOCK,
                    onClick = { viewModel.setFilter(StockFilter.OUT_OF_STOCK) },
                    selectedBg = HeaderGradientStart,
                    unselectedBg = OutOfStockBg,
                    unselectedText = OutOfStockText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No products in inventory.\nTap '+ Add Product' to start." else "No matching products found.",
                        color = TextMutedColor,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onEdit = { onNavigateToEditProduct(product.id) },
                            onDelete = { productToDelete = product }
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
private fun FilterChipPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedBg: Color = HeaderGradientStart,
    unselectedBg: Color = Color.White,
    unselectedText: Color = Color(0xFF475569)
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) selectedBg else unselectedBg)
            .border(
                width = 1.dp,
                color = if (isSelected) selectedBg else BorderColor,
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
                    tint = if (isSelected) Color.White else unselectedText,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else unselectedText
            )
        }
    }
}

@Composable
private fun ProductItemCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
            // Product image or placeholder
            ProductThumbnail(
                imageUri = product.imageUri,
                size = 52.dp,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DokandaarTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "||||||",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = DokandaarTheme.textMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = product.barcode,
                        fontSize = 12.sp,
                        color = DokandaarTheme.textMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                StockStatusBadge(stock = product.stock)
            }

            // Price & Actions
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = product.price.format(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AddProductBg)
                            .clickable { onEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Product",
                            tint = HeaderGradientStart,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2))
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Product",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
