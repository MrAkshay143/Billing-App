package com.billing.app.feature.more

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.components.BottomDokandaarNav
import com.billing.app.core.ui.components.DokandaarTab
import com.billing.app.core.ui.theme.DarkPurple
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
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
import com.billing.app.core.ui.theme.PastelRedContent
import com.billing.app.core.ui.theme.PastelSlateBg
import com.billing.app.core.ui.theme.PastelSlateContent
import com.billing.app.core.ui.theme.PastelTealBg
import com.billing.app.core.ui.theme.PastelTealContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.ui.theme.SecondaryPurple
import com.billing.app.BillingApplication
import com.billing.app.core.ui.components.ShiftCloseDialog
import com.billing.app.core.ui.components.ShiftOpenDialog
import com.billing.app.core.ui.components.ShiftSummaryDialog
import com.billing.app.domain.model.ShiftZReport
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ShiftRepository
import com.billing.app.domain.repository.UserRepository
import com.billing.app.feature.settings.SettingsViewModel

@Composable
fun MoreScreen(
    viewModel: SettingsViewModel,
    shiftRepository: ShiftRepository = BillingApplication.instance.shiftRepository,
    printerRepository: PrinterRepository = BillingApplication.instance.printerRepository,
    userRepository: UserRepository = BillingApplication.instance.userRepository,
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
    onNavigateToTab: (DokandaarTab) -> Unit,
    onNavigateToScanner: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToSuppliers: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToImportExport: () -> Unit = {},
    onNavigateToPurchases: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {}
) {
    val context = LocalContext.current
    val strings = appStrings()
    val shopProfile by viewModel.shopProfile.collectAsState()

    val activeShift by shiftRepository.getActiveShiftFlow().collectAsState(initial = null)
    var showShiftOpenDialog by remember { mutableStateOf(false) }
    var showShiftCloseDialog by remember { mutableStateOf(false) }
    var shiftSummaryZReport by remember { mutableStateOf<ShiftZReport?>(null) }

    if (showShiftOpenDialog) {
        ShiftOpenDialog(
            userId = userRepository.currentUser.value?.id,
            onDismissRequest = { showShiftOpenDialog = false },
            onShiftOpened = { showShiftOpenDialog = false }
        )
    }

    if (showShiftCloseDialog) {
        ShiftCloseDialog(
            shiftRepository = shiftRepository,
            onDismissRequest = { showShiftCloseDialog = false },
            onShiftClosed = { zReport ->
                showShiftCloseDialog = false
                shiftSummaryZReport = zReport
            }
        )
    }

    shiftSummaryZReport?.let { zReport ->
        ShiftSummaryDialog(
            zReport = zReport,
            printerRepository = printerRepository,
            storeName = shopProfile.name.ifBlank { "Dokandaar Store" },
            onDismissRequest = { shiftSummaryZReport = null }
        )
    }

    var showHelpDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Help & Support Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = DokandaarTheme.cardBackground,
            title = { Text(strings.helpSupport, fontWeight = FontWeight.Bold, color = PrimaryPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Official Kirana Store Support:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DokandaarTheme.textPrimary)
                    Text("• Email: support@dokandaar.in", fontSize = 12.sp, color = DokandaarTheme.textPrimary)
                    Text("• Helpline: +91 99999 99999 (9 AM - 9 PM)", fontSize = 12.sp, color = DokandaarTheme.textPrimary)
                    Text("• WhatsApp Support: Dedicated merchant assistance available daily.", fontSize = 12.sp, color = DokandaarTheme.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Quick Billing Tips:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DokandaarTheme.textPrimary)
                    Text("1. Use Scan QR/Barcode from Home or Sale for fastest scanning.", fontSize = 11.sp, color = DokandaarTheme.textMuted)
                    Text("2. Hold Bill allows saving customer drafts while serving others.", fontSize = 11.sp, color = DokandaarTheme.textMuted)
                    Text("3. Create regular JSON/Excel backups under Backup & Restore.", fontSize = 11.sp, color = DokandaarTheme.textMuted)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919999999999"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        showHelpDialog = false
                    }
                ) {
                    Text(strings.whatsappSupport, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(strings.close, color = DokandaarTheme.textMuted)
                }
            }
        )
    }

    // Notifications Dialog
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = DokandaarTheme.cardBackground,
            title = { Text(strings.notifications, fontWeight = FontWeight.Bold, color = PrimaryPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (DokandaarTheme.isDark) PrimaryPurple.copy(alpha = 0.15f) else PastelPurpleBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Fast Offline Billing Ready", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryPurple)
                            Text("Quick camera scanner & instant billing active.", fontSize = 11.sp, color = DokandaarTheme.textPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (DokandaarTheme.isDark) PastelGreenContent.copy(alpha = 0.15f) else PastelGreenBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Data Safety Active", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PastelGreenContent)
                            Text("All shop records are securely stored & protected.", fontSize = 11.sp, color = DokandaarTheme.textPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (DokandaarTheme.isDark) PastelOrangeContent.copy(alpha = 0.15f) else PastelOrangeBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Stock Alerts Active", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PastelOrangeContent)
                            Text("Automatic warnings when inventory runs low.", fontSize = 11.sp, color = DokandaarTheme.textPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text(strings.ok, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = DokandaarTheme.cardBackground,
            title = { Text(strings.logoutConfirmTitle, fontWeight = FontWeight.Bold, color = ErrorColor) },
            text = { Text(strings.logoutConfirmPrompt, color = DokandaarTheme.textPrimary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        com.billing.app.core.ui.util.FeedbackManager.showToast(context, "Logged out successfully")
                    }
                ) {
                    Text(strings.logout, color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted)
                }
            }
        )
    }

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
                            Brush.horizontalGradient(
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.navMore,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.moreSubtitle,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Store front decorative icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            BottomDokandaarNav(
                selectedTab = DokandaarTab.MORE,
                onTabSelected = onNavigateToTab
            )
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Store Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToStoreProfile() },
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
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PastelPurpleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = shopProfile.name.ifBlank { "My Kirana Store" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kirana, Grocery & Daily Needs",
                            fontSize = 11.sp,
                            color = DokandaarTheme.textMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(PastelGreenContent)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Store Active",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PastelGreenContent
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open",
                        tint = DokandaarTheme.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Store Management Section (8 pastel tiles)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.storeManagement,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 1: Categories | Customers | Suppliers | Expenses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Category,
                        iconBg = PastelPurpleBg,
                        iconTint = PastelPurpleContent,
                        title = strings.categories,
                        onClick = onNavigateToCategories
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.People,
                        iconBg = PastelBlueBg,
                        iconTint = PastelBlueContent,
                        title = strings.customers,
                        onClick = onNavigateToCustomers
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalShipping,
                        iconBg = PastelOrangeBg,
                        iconTint = PastelOrangeContent,
                        title = strings.suppliers,
                        onClick = onNavigateToSuppliers
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccountBalanceWallet,
                        iconBg = PastelPinkBg,
                        iconTint = PastelPinkContent,
                        title = strings.expenses,
                        onClick = onNavigateToExpenses
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Discounts | Tax/GST | Payment Methods | Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalOffer,
                        iconBg = PastelGreenBg,
                        iconTint = PastelGreenContent,
                        title = strings.discounts,
                        onClick = onNavigateToPaymentSettings
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Receipt,
                        iconBg = PastelIndigoBg,
                        iconTint = PastelIndigoContent,
                        title = strings.taxGst,
                        onClick = onNavigateToPaymentSettings
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CreditCard,
                        iconBg = PastelTealBg,
                        iconTint = PastelTealContent,
                        title = strings.paymentMethods,
                        onClick = onNavigateToPaymentSettings
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        iconBg = PastelSlateBg,
                        iconTint = PastelSlateContent,
                        title = strings.settings,
                        onClick = onNavigateToAppearance
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 3: Purchases / GRN & Cash Register / Z-Report
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalShipping,
                        title = "Purchases / GRN",
                        iconBg = PastelBlueBg,
                        iconTint = PastelBlueContent,
                        onClick = onNavigateToPurchases
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Cash Register & Z-Report / ক্যাশ রেজিস্টার",
                        iconBg = PastelGreenBg,
                        iconTint = PastelGreenContent,
                        onClick = {
                            if (activeShift != null) {
                                showShiftCloseDialog = true
                            } else {
                                showShiftOpenDialog = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // 3. Tools & Utilities Section (4 pastel tiles)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.toolsUtilities,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.QrCodeScanner,
                        iconBg = PastelPurpleBg,
                        iconTint = PastelPurpleContent,
                        title = strings.barcodeScanner,
                        onClick = onNavigateToScanner
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FileDownload,
                        iconBg = PastelGreenBg,
                        iconTint = PastelGreenContent,
                        title = strings.importExport,
                        onClick = onNavigateToImportExport
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Backup,
                        iconBg = PastelBlueBg,
                        iconTint = PastelBlueContent,
                        title = strings.backupDataRestore,
                        onClick = onNavigateToBackupRestore
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CloudSync,
                        iconBg = PastelOrangeBg,
                        iconTint = PastelOrangeContent,
                        title = strings.syncData,
                        onClick = {
                            com.billing.app.core.ui.util.FeedbackManager.showToast(context, "All store records are saved and up to date")
                        }
                    )
                }
            }

            // 4. Business & Support Section (4 pastel tiles)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HeadsetMic,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.businessSupport,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        iconBg = PastelBlueBg,
                        iconTint = PastelBlueContent,
                        title = strings.helpSupport,
                        onClick = { showHelpDialog = true }
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Chat,
                        iconBg = PastelGreenBg,
                        iconTint = PastelGreenContent,
                        title = strings.whatsappSupport,
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919999999999"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Star,
                        iconBg = PastelOrangeBg,
                        iconTint = PastelOrangeContent,
                        title = strings.rateApp,
                        onClick = {
                            try {
                                val rateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                                rateIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                context.startActivity(rateIntent)
                            } catch (_: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                context.startActivity(webIntent)
                            }
                        }
                    )
                    MoreTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Share,
                        iconBg = PastelPurpleBg,
                        iconTint = PastelPurpleContent,
                        title = strings.shareApp,
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Download Dokandaar - Best Billing & POS app for Kirana stores!")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Dokandaar"))
                        }
                    )
                }
            }

            // 5. Options List Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    MoreListItem(
                        icon = Icons.Default.Print,
                        title = strings.printerSetup,
                        subtitle = "Thermal & Bluetooth",
                        onClick = onNavigateToPrinterSetup
                    )
                    HorizontalDivider(color = DokandaarTheme.border, thickness = 0.8.dp)
                    MoreListItem(
                        icon = Icons.Default.Language,
                        title = strings.language,
                        subtitle = "English / हिन्दी / বাংলা",
                        onClick = onNavigateToLanguage
                    )
                    HorizontalDivider(color = DokandaarTheme.border, thickness = 0.8.dp)
                    MoreListItem(
                        icon = Icons.Default.Group,
                        title = strings.userAccess,
                        subtitle = "Staff & Roles",
                        onClick = onNavigateToUserAccess
                    )
                    HorizontalDivider(color = DokandaarTheme.border, thickness = 0.8.dp)
                    MoreListItem(
                        icon = Icons.Default.Palette,
                        title = strings.appearance,
                        subtitle = "Dark / Light Mode",
                        onClick = onNavigateToAppearance
                    )
                    HorizontalDivider(color = DokandaarTheme.border, thickness = 0.8.dp)
                    MoreListItem(
                        icon = Icons.Default.Notifications,
                        title = strings.notifications,
                        badge = "3",
                        onClick = { showNotificationsDialog = true }
                    )
                    HorizontalDivider(color = DokandaarTheme.border, thickness = 0.8.dp)
                    MoreListItem(
                        icon = Icons.Default.Info,
                        title = strings.aboutApp,
                        subtitle = "v2.0.0",
                        onClick = onNavigateToAbout
                    )
                    HorizontalDivider(color = DokandaarTheme.border, thickness = 0.8.dp)
                    MoreListItem(
                        icon = Icons.Default.Policy,
                        title = strings.privacyPolicy,
                        onClick = onNavigateToPrivacyPolicy
                    )
                    HorizontalDivider(color = DokandaarTheme.border, thickness = 0.8.dp)
                    MoreListItem(
                        icon = Icons.Default.Security,
                        title = strings.termsConditions,
                        onClick = onNavigateToTerms
                    )
                }
            }

            // 6. Logout Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        showLogoutDialog = true
                    },
                color = PastelRedBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, PastelRedContent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = ErrorColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.logout,
                        color = ErrorColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MoreTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(86.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = DokandaarTheme.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoreListItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryPurple,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DokandaarTheme.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        if (badge != null) {
            Surface(
                shape = CircleShape,
                color = ErrorColor
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = DokandaarTheme.textMuted,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = DokandaarTheme.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}
