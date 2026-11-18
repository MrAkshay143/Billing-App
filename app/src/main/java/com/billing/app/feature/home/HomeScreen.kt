package com.billing.app.feature.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.billing.app.core.ui.theme.ErrorColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.billing.app.core.ui.theme.DarkPurple
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.PastelBlueBg
import com.billing.app.core.ui.theme.PastelBlueContent
import com.billing.app.core.ui.theme.PastelGreenBg
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelIndigoBg
import com.billing.app.core.ui.theme.PastelIndigoContent
import com.billing.app.core.ui.theme.PastelOrangeBg
import com.billing.app.core.ui.theme.PastelOrangeContent
import com.billing.app.core.ui.theme.PastelPinkBg
import com.billing.app.core.ui.theme.PastelPinkContent
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PastelPurpleContent
import com.billing.app.core.ui.theme.PastelRedBg
import com.billing.app.core.ui.theme.PastelSlateBg
import com.billing.app.core.ui.theme.PastelSlateContent
import com.billing.app.core.ui.theme.PastelTealBg
import com.billing.app.core.ui.theme.PastelTealContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.ui.theme.SecondaryPurple

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSale: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToMore: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToPrinter: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToImportExport: () -> Unit
) {
    val storeName by viewModel.storeName.collectAsState()
    val strings = appStrings()
    val totalSales by viewModel.totalSales.collectAsState()
    val totalInvoices by viewModel.totalInvoices.collectAsState()
    val totalProducts by viewModel.totalProducts.collectAsState()
    val totalCustomers by viewModel.totalCustomers.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()

    var showPeriodMenu by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Store Notifications", fontWeight = FontWeight.Bold, color = PrimaryPurple)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (lowStockCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (DokandaarTheme.isDark) DokandaarTheme.cardBackground else PastelRedBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (DokandaarTheme.isDark) ErrorColor.copy(alpha = 0.3f) else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showNotificationsDialog = false
                                    onNavigateToProducts()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Low Stock Alert", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ErrorColor)
                                    Text("$lowStockCount items running low on shelf", fontSize = 11.sp, color = DokandaarTheme.textPrimary)
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (DokandaarTheme.isDark) DokandaarTheme.cardBackground else PastelGreenBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (DokandaarTheme.isDark) PastelGreenContent.copy(alpha = 0.3f) else Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PastelGreenContent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Sales Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PastelGreenContent)
                                Text("$totalInvoices bills recorded (${selectedPeriod.label})", fontSize = 11.sp, color = DokandaarTheme.textPrimary)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (DokandaarTheme.isDark) DokandaarTheme.cardBackground else PastelPurpleBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (DokandaarTheme.isDark) PrimaryPurple.copy(alpha = 0.3f) else Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("100% Offline Mode", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryPurple)
                                Text("All store data safely saved on this device", fontSize = 11.sp, color = DokandaarTheme.textPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationsDialog = false
                        if (lowStockCount > 0) onNavigateToProducts()
                    }
                ) {
                    Text(if (lowStockCount > 0) "View Low Stock" else "OK", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Close", color = DokandaarTheme.textMuted)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopDokandaarHeader(
                storeName = storeName,
                subtitle = strings.appSubtitle,
                onSearchClick = onNavigateToProducts,
                onNotificationClick = { showNotificationsDialog = true },
                onMenuClick = onNavigateToMore
            )
        },
        bottomBar = {
            BottomDokandaarNav(
                selectedTab = DokandaarTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        DokandaarTab.HOME -> { /* Already here */ }
                        DokandaarTab.PRODUCTS -> onNavigateToProducts()
                        DokandaarTab.INVOICES -> onNavigateToInvoices()
                        DokandaarTab.REPORTS -> onNavigateToReports()
                        DokandaarTab.MORE -> onNavigateToMore()
                    }
                }
            )
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Promotional Card (Mockup 1)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryPurple, SecondaryPurple, DarkPurple)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.manageYourStore,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.heroSubtitle,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier.clickable { onNavigateToSale() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.newSalePos,
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        // Right POS Terminal Visual Illustration
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "FAST POS",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick Navigation Grid (8 Pastel Tiles)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShoppingCart,
                        title = "New Sale",
                        subtitle = "(POS)",
                        bgColor = PastelPurpleBg,
                        contentColor = PastelPurpleContent,
                        onClick = onNavigateToSale
                    )
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory2,
                        title = strings.quickProducts,
                        subtitle = "Manage Items",
                        bgColor = PastelBlueBg,
                        contentColor = PastelBlueContent,
                        onClick = onNavigateToProducts
                    )
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = strings.quickInvoices,
                        subtitle = "Sales & Returns",
                        bgColor = PastelOrangeBg,
                        contentColor = PastelOrangeContent,
                        onClick = onNavigateToInvoices
                    )
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Print,
                        title = strings.quickPrint,
                        subtitle = "Bills & Reports",
                        bgColor = PastelGreenBg,
                        contentColor = PastelGreenContent,
                        onClick = onNavigateToPrinter
                    )
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.People,
                        title = strings.customers,
                        subtitle = "Manage Customers",
                        bgColor = PastelPinkBg,
                        contentColor = PastelPinkContent,
                        onClick = onNavigateToCustomers
                    )
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.BarChart,
                        title = strings.navReports,
                        subtitle = "Sales, Stock & More",
                        bgColor = PastelIndigoBg,
                        contentColor = PastelIndigoContent,
                        onClick = onNavigateToReports
                    )
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FileDownload,
                        title = strings.importExport,
                        subtitle = "Data (CSV/Excel)",
                        bgColor = PastelTealBg,
                        contentColor = PastelTealContent,
                        onClick = onNavigateToImportExport
                    )
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        title = strings.settings,
                        subtitle = "Store & App",
                        bgColor = PastelSlateBg,
                        contentColor = PastelSlateContent,
                        onClick = onNavigateToMore
                    )
                }
            }

            // 3. Overview Section (Mockup 1)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.overview,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                    }

                    // Period Filter Dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DokandaarTheme.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
                            modifier = Modifier.clickable { showPeriodMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedPeriod.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
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
                            expanded = showPeriodMenu,
                            onDismissRequest = { showPeriodMenu = false }
                        ) {
                            OverviewPeriod.entries.forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period.label) },
                                    onClick = {
                                        viewModel.setPeriod(period)
                                        showPeriodMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 2x2 KPI Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverviewKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShoppingCart,
                        iconBg = PastelGreenBg,
                        iconTint = PastelGreenContent,
                        title = strings.totalSales,
                        value = totalSales.format()
                    )
                    OverviewKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Description,
                        iconBg = PastelBlueBg,
                        iconTint = PastelBlueContent,
                        title = strings.totalInvoices,
                        value = totalInvoices.toString()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverviewKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory2,
                        iconBg = PastelPurpleBg,
                        iconTint = PastelPurpleContent,
                        title = strings.totalProducts,
                        value = totalProducts.toString()
                    )
                    OverviewKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.People,
                        iconBg = PastelPinkBg,
                        iconTint = PastelPinkContent,
                        title = strings.totalCustomers,
                        value = totalCustomers.toString()
                    )
                }
            }

            // 4. Unified Scan QR / Barcode Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onNavigateToScanner() },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryPurple, SecondaryPurple, DarkPurple)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(Color.White.copy(alpha = 0.30f))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.scanQrBarcode,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = strings.scanSubtitle,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun QuickNavTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    bgColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (DokandaarTheme.isDark) DokandaarTheme.cardBackground else bgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (DokandaarTheme.isDark) bgColor else Color.White.copy(alpha = 0.65f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DokandaarTheme.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = DokandaarTheme.textMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OverviewKpiCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = DokandaarTheme.textMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DokandaarTheme.textPrimary
                )
            }
        }
    }
}
