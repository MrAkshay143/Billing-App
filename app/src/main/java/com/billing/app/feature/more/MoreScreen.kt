package com.billing.app.feature.more

import android.widget.Toast
import com.billing.app.core.ui.util.FeedbackManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.components.BottomDokandaarNav
import com.billing.app.core.ui.components.DokandaarTab
import com.billing.app.core.ui.components.TopDokandaarHeader
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.TextMutedColor
import com.billing.app.feature.settings.SettingsViewModel

@Composable
fun MoreScreen(
    viewModel: SettingsViewModel,
    onNavigateToStoreProfile: () -> Unit,
    onNavigateToUserAccess: () -> Unit,
    onNavigateToPrinterSetup: () -> Unit,
    onNavigateToPaymentSettings: () -> Unit,
    onNavigateToSalesReport: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToTab: (DokandaarTab) -> Unit
) {
    val context = LocalContext.current
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopDokandaarHeader(
                storeName = "Dokandaar",
                subtitle = "Billing & POS",
                onNotificationClick = onNavigateToInvoices,
                onMenuClick = { /* Current Tab */ }
            )
        },
        bottomBar = {
            BottomDokandaarNav(
                selectedTab = DokandaarTab.MORE,
                onTabSelected = onNavigateToTab
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = HeaderGradientStart,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "More",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Settings, backup, reports and more",
                            fontSize = 12.sp,
                            color = TextMutedColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Group: Business
            Text(
                text = "Business",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    MoreOptionItem(
                        icon = Icons.Default.Storefront,
                        title = "Store Profile",
                        subtitle = "Store info, logo, address",
                        onClick = onNavigateToStoreProfile
                    )
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    MoreOptionItem(
                        icon = Icons.Default.Group,
                        title = "User & Access",
                        subtitle = "Add staff, set permissions",
                        onClick = onNavigateToUserAccess
                    )
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    MoreOptionItem(
                        icon = Icons.Default.Print,
                        title = "Printer Setup",
                        subtitle = "Thermal/Bluetooth printer",
                        onClick = onNavigateToPrinterSetup
                    )
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    MoreOptionItem(
                        icon = Icons.Default.CreditCard,
                        title = "Payment Settings",
                        subtitle = "UPI, cash and other options",
                        onClick = onNavigateToPaymentSettings
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Group: Data & Reports
            Text(
                text = "Data & Reports",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    MoreOptionItem(
                        icon = Icons.Default.BarChart,
                        title = "Sales Report",
                        subtitle = "Daily, weekly and monthly reports",
                        onClick = onNavigateToSalesReport
                    )
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    MoreOptionItem(
                        icon = Icons.Default.CloudDownload,
                        title = "Backup & Restore",
                        subtitle = "Backup your data and restore anytime",
                        onClick = onNavigateToBackupRestore
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Group: App
            Text(
                text = "App",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    MoreOptionItem(
                        icon = Icons.Default.Palette,
                        title = "Appearance",
                        subtitle = "Theme, color and display",
                        onClick = onNavigateToAppearance
                    )
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    MoreOptionItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = "English, বাংলা, हिंदी",
                        onClick = onNavigateToLanguage
                    )
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                    // Notifications Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AddProductBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = HeaderGradientStart,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifications",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Sale, stock and app alerts",
                                fontSize = 12.sp,
                                color = TextMutedColor
                            )
                        }

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                viewModel.setNotificationsEnabled(it)
                                if (it) {
                                    viewModel.triggerSalesSummaryNotification(context)
                                }
                                FeedbackManager.showToast(
                                    context,
                                    if (it) "Notifications Enabled" else "Notifications Muted"
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HeaderGradientStart
                            )
                        )
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    MoreOptionItem(
                        icon = Icons.Default.Info,
                        title = "About Dokandaar",
                        subtitle = "App version, update and support",
                        onClick = onNavigateToAbout
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MoreOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AddProductBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HeaderGradientStart,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMutedColor
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
        )
    }
}
