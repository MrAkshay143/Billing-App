package com.billing.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.billing.app.core.audio.SoundHapticManager
import com.billing.app.core.ui.components.DokandaarTab
import com.billing.app.feature.checkout.CheckoutScreen
import com.billing.app.feature.checkout.CheckoutViewModel
import com.billing.app.feature.home.HomeScreen
import com.billing.app.feature.home.HomeViewModel
import com.billing.app.feature.invoices.InvoiceViewModel
import com.billing.app.feature.invoices.InvoicesScreen
import com.billing.app.feature.more.MoreScreen
import com.billing.app.feature.more.subscreens.AboutDokandaarScreen
import com.billing.app.feature.more.subscreens.AppearanceScreen
import com.billing.app.feature.more.subscreens.BackupRestoreScreen
import com.billing.app.feature.more.subscreens.LanguageScreen
import com.billing.app.feature.more.subscreens.PaymentSettingsScreen
import com.billing.app.feature.more.subscreens.PrinterSetupScreen
import com.billing.app.feature.more.subscreens.SalesReportScreen
import com.billing.app.feature.more.subscreens.StoreProfileScreen
import com.billing.app.feature.more.subscreens.UserAccessScreen
import com.billing.app.feature.products.AddEditProductScreen
import com.billing.app.feature.products.ProductListScreen
import com.billing.app.feature.products.ProductViewModel
import com.billing.app.feature.scanner.FullScannerScreen
import com.billing.app.feature.settings.SettingsViewModel
import com.billing.app.feature.splash.SplashScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    checkoutViewModel: CheckoutViewModel,
    productViewModel: ProductViewModel,
    invoiceViewModel: InvoiceViewModel,
    settingsViewModel: SettingsViewModel,
    soundHapticManager: SoundHapticManager
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val totalAmount by homeViewModel.totalAmount.collectAsState()

    var scannedBarcodeForAddProduct by rememberSaveable { mutableStateOf<String?>(null) }
    var scanTargetMode by rememberSaveable { mutableStateOf("add") } // "add" or "search"

    fun navigateToTab(tab: DokandaarTab) {
        when (tab) {
            DokandaarTab.HOME -> {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
            DokandaarTab.PRODUCTS -> {
                navController.navigate(Screen.Products.route) {
                    launchSingleTop = true
                }
            }
            DokandaarTab.INVOICES -> {
                navController.navigate(Screen.Invoices.route) {
                    launchSingleTop = true
                }
            }
            DokandaarTab.MORE -> {
                navController.navigate(Screen.More.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToCheckout = { navController.navigate(Screen.Checkout.route) },
                onNavigateToProducts = { navigateToTab(DokandaarTab.PRODUCTS) },
                onNavigateToInvoices = { navigateToTab(DokandaarTab.INVOICES) },
                onNavigateToSettings = { navigateToTab(DokandaarTab.MORE) },
                onNavigateToAddProduct = {
                    scannedBarcodeForAddProduct = null
                    navController.navigate(Screen.AddProduct.route)
                },
                onNavigateToPrinter = { navController.navigate(Screen.PrinterSetup.route) }
            )
        }

        composable(Screen.Checkout.route) {
            CheckoutScreen(
                viewModel = checkoutViewModel,
                cartItems = homeUiState.cartItems,
                subtotalAmount = totalAmount,
                onNavigateBack = {
                    // DO NOT clear cart on back navigation
                    navController.popBackStack()
                },
                onSaleCompleted = {
                    // Reset cart and checkout state on successful sale
                    checkoutViewModel.resetSaleState()
                    homeViewModel.clearCart()
                    navController.navigate(Screen.Invoices.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateToTab = { tab -> navigateToTab(tab) }
            )
        }

        composable(Screen.Products.route) {
            ProductListScreen(
                viewModel = productViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddProduct = {
                    scannedBarcodeForAddProduct = null
                    navController.navigate(Screen.AddProduct.route)
                },
                onNavigateToEditProduct = { productId ->
                    navController.navigate(Screen.EditProduct.createRoute(productId))
                },
                onNavigateToScannerForSearch = {
                    scanTargetMode = "search"
                    navController.navigate(Screen.FullScanner.route)
                },
                onNavigateToTab = { tab -> navigateToTab(tab) }
            )
        }

        composable(Screen.Invoices.route) {
            InvoicesScreen(
                viewModel = invoiceViewModel,
                onNavigateToTab = { tab -> navigateToTab(tab) },
                onNavigateToHome = { navigateToTab(DokandaarTab.HOME) },
                onNavigateToSettings = { navigateToTab(DokandaarTab.MORE) },
                onNavigateToSalesReport = { navController.navigate(Screen.SalesReport.route) },
                onResumeDraft = { items, draftInvoiceNumber ->
                    homeViewModel.loadCartItems(items)
                    checkoutViewModel.setResumedDraft(draftInvoiceNumber)
                    navController.navigate(Screen.Checkout.route)
                }
            )
        }

        composable(Screen.More.route) {
            MoreScreen(
                viewModel = settingsViewModel,
                onNavigateToStoreProfile = { navController.navigate(Screen.StoreProfile.route) },
                onNavigateToUserAccess = { navController.navigate(Screen.UserAccess.route) },
                onNavigateToPrinterSetup = { navController.navigate(Screen.PrinterSetup.route) },
                onNavigateToPaymentSettings = { navController.navigate(Screen.PaymentSettings.route) },
                onNavigateToSalesReport = { navController.navigate(Screen.SalesReport.route) },
                onNavigateToBackupRestore = { navController.navigate(Screen.BackupRestore.route) },
                onNavigateToAppearance = { navController.navigate(Screen.Appearance.route) },
                onNavigateToLanguage = { navController.navigate(Screen.Language.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToInvoices = { navigateToTab(DokandaarTab.INVOICES) },
                onNavigateToTab = { tab -> navigateToTab(tab) }
            )
        }

        // Subscreens for More
        composable(Screen.StoreProfile.route) {
            StoreProfileScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.UserAccess.route) {
            UserAccessScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PrinterSetup.route) {
            PrinterSetupScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PaymentSettings.route) {
            PaymentSettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SalesReport.route) {
            SalesReportScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInvoices = { navigateToTab(DokandaarTab.INVOICES) }
            )
        }

        composable(Screen.BackupRestore.route) {
            BackupRestoreScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Appearance.route) {
            AppearanceScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Language.route) {
            LanguageScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutDokandaarScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddProduct.route) {
            AddEditProductScreen(
                viewModel = productViewModel,
                existingProduct = null,
                scannedBarcode = scannedBarcodeForAddProduct,
                onBarcodeConsumed = { scannedBarcodeForAddProduct = null },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScanner = {
                    scanTargetMode = "add"
                    navController.navigate(Screen.FullScanner.route)
                }
            )
        }

        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val allProducts by productViewModel.products.collectAsState()
            val product = allProducts.firstOrNull { it.id == productId }

            AddEditProductScreen(
                viewModel = productViewModel,
                existingProduct = product,
                scannedBarcode = scannedBarcodeForAddProduct,
                onBarcodeConsumed = { scannedBarcodeForAddProduct = null },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScanner = {
                    scanTargetMode = "add"
                    navController.navigate(Screen.FullScanner.route)
                }
            )
        }

        composable(Screen.FullScanner.route) {
            FullScannerScreen(
                soundHapticManager = soundHapticManager,
                onNavigateBack = { navController.popBackStack() },
                onBarcodeCaptured = { barcode ->
                    if (scanTargetMode == "add") {
                        scannedBarcodeForAddProduct = barcode
                    } else {
                        productViewModel.searchQuery.value = barcode
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}
