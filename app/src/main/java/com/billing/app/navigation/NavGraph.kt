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
import com.billing.app.core.cart.CartManager
import com.billing.app.core.ui.components.DokandaarTab
import com.billing.app.domain.repository.CategoryRepository
import com.billing.app.domain.repository.CustomerRepository
import com.billing.app.domain.repository.ExpenseRepository
import com.billing.app.domain.repository.SupplierRepository
import com.billing.app.feature.categories.CategoriesScreen
import com.billing.app.feature.checkout.CheckoutScreen
import com.billing.app.feature.checkout.CheckoutViewModel
import com.billing.app.feature.checkout.SaleScreen
import com.billing.app.feature.customers.CustomersScreen
import com.billing.app.feature.expenses.ExpensesScreen
import com.billing.app.feature.home.HomeScreen
import com.billing.app.feature.home.HomeViewModel
import com.billing.app.feature.importexport.ImportExportScreen
import com.billing.app.feature.invoices.InvoiceViewModel
import com.billing.app.feature.invoices.InvoicesScreen
import com.billing.app.feature.more.MoreScreen
import com.billing.app.feature.more.subscreens.AboutDokandaarScreen
import com.billing.app.feature.more.subscreens.PrivacyPolicyScreen
import com.billing.app.feature.more.subscreens.TermsConditionsScreen
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
import com.billing.app.feature.reports.ReportsScreen
import com.billing.app.feature.reports.ReportsViewModel
import com.billing.app.feature.scanner.FullScannerScreen
import com.billing.app.feature.settings.SettingsViewModel
import com.billing.app.feature.splash.SplashScreen
import com.billing.app.feature.suppliers.SuppliersScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    checkoutViewModel: CheckoutViewModel,
    productViewModel: ProductViewModel,
    invoiceViewModel: InvoiceViewModel,
    reportsViewModel: ReportsViewModel,
    settingsViewModel: SettingsViewModel,
    cartManager: CartManager,
    customerRepository: CustomerRepository,
    supplierRepository: SupplierRepository,
    expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
    soundHapticManager: SoundHapticManager
) {
    var scanTargetMode by rememberSaveable { mutableStateOf("cart") } // "cart", "add", "search"

    fun navigateToTab(tab: DokandaarTab) {
        val route = when (tab) {
            DokandaarTab.HOME -> Screen.Home.route
            DokandaarTab.PRODUCTS -> Screen.Products.route
            DokandaarTab.INVOICES -> Screen.Invoices.route
            DokandaarTab.REPORTS -> Screen.Reports.route
            DokandaarTab.MORE -> Screen.More.route
        }
        navController.navigate(route) {
            popUpTo(Screen.Home.route) {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState = true
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
                onNavigateToSale = { navController.navigate(Screen.Sale.route) },
                onNavigateToProducts = { navigateToTab(DokandaarTab.PRODUCTS) },
                onNavigateToInvoices = { navigateToTab(DokandaarTab.INVOICES) },
                onNavigateToReports = { navigateToTab(DokandaarTab.REPORTS) },
                onNavigateToMore = { navigateToTab(DokandaarTab.MORE) },
                onNavigateToScanner = {
                    scanTargetMode = "cart"
                    navController.navigate(Screen.FullScanner.route)
                },
                onNavigateToPrinter = { navController.navigate(Screen.PrinterSetup.route) },
                onNavigateToCustomers = { navController.navigate(Screen.Customers.route) },
                onNavigateToImportExport = { navController.navigate(Screen.ImportExport.route) }
            )
        }

        composable(Screen.Sale.route) {
            SaleScreen(
                viewModel = checkoutViewModel,
                cartManager = cartManager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScanner = {
                    scanTargetMode = "cart"
                    navController.navigate(Screen.FullScanner.route)
                },
                onSaleCompleted = {
                    cartManager.clearCart()
                    navController.navigate(Screen.Invoices.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateToPrinter = {
                    navController.navigate(Screen.PrinterSetup.route)
                },
                onNavigateToHeldDrafts = {
                    invoiceViewModel.statusTab.value = com.billing.app.feature.invoices.InvoiceStatusTab.DRAFT
                    navController.navigate(Screen.Invoices.route)
                }
            )
        }

        composable(Screen.Checkout.route) {
            SaleScreen(
                viewModel = checkoutViewModel,
                cartManager = cartManager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScanner = {
                    scanTargetMode = "cart"
                    navController.navigate(Screen.FullScanner.route)
                },
                onSaleCompleted = {
                    cartManager.clearCart()
                    navController.navigate(Screen.Invoices.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateToPrinter = {
                    navController.navigate(Screen.PrinterSetup.route)
                },
                onNavigateToHeldDrafts = {
                    invoiceViewModel.statusTab.value = com.billing.app.feature.invoices.InvoiceStatusTab.DRAFT
                    navController.navigate(Screen.Invoices.route)
                }
            )
        }

        composable(Screen.Products.route) {
            ProductListScreen(
                viewModel = productViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddProduct = {
                    productViewModel.clearDraft()
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
                onNavigateToNewSale = { navController.navigate(Screen.Sale.route) },
                onResumeDraft = { items, draftInvoice ->
                    cartManager.setResumedDraft(
                        draftNumber = draftInvoice.invoiceNumber,
                        items = items,
                        customerName = draftInvoice.customerName,
                        customerPhone = draftInvoice.customerPhone,
                        discountPaise = draftInvoice.discountPaise
                    )
                    checkoutViewModel.setResumedDraft(draftInvoice.invoiceNumber)
                    checkoutViewModel.setCustomer(
                        draftInvoice.customerName ?: "Walk-in Customer",
                        draftInvoice.customerPhone ?: ""
                    )
                    val subtotal = items.fold(0L) { acc, it -> acc + it.total.paise }
                    checkoutViewModel.setDiscount(draftInvoice.discountPaise, subtotal)
                    navController.navigate(Screen.Sale.route)
                }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                viewModel = reportsViewModel,
                onNavigateToTab = { tab -> navigateToTab(tab) },
                onNavigateToSalesReport = { navController.navigate(Screen.SalesReport.route) },
                onNavigateToStockReport = { navigateToTab(DokandaarTab.PRODUCTS) }
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
                onNavigateToTab = { tab -> navigateToTab(tab) },
                onNavigateToScanner = {
                    scanTargetMode = "cart"
                    navController.navigate(Screen.FullScanner.route)
                },
                onNavigateToCustomers = { navController.navigate(Screen.Customers.route) },
                onNavigateToSuppliers = { navController.navigate(Screen.Suppliers.route) },
                onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToImportExport = { navController.navigate(Screen.ImportExport.route) },
                onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateToTerms = { navController.navigate(Screen.TermsConditions.route) }
            )
        }

        // Subscreens
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

        composable(Screen.Customers.route) {
            CustomersScreen(
                customerRepository = customerRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Suppliers.route) {
            SuppliersScreen(
                supplierRepository = supplierRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Expenses.route) {
            ExpensesScreen(
                expenseRepository = expenseRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                categoryRepository = categoryRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ImportExport.route) {
            ImportExportScreen(
                productRepository = productViewModel.let {
                    // productRepository from BillingApplication
                    com.billing.app.BillingApplication.instance.productRepository
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TermsConditions.route) {
            TermsConditionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddProduct.route) {
            AddEditProductScreen(
                viewModel = productViewModel,
                existingProduct = null,
                scannedBarcode = null,
                onBarcodeConsumed = {},
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
                scannedBarcode = null,
                onBarcodeConsumed = {},
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScanner = {
                    scanTargetMode = "add"
                    navController.navigate(Screen.FullScanner.route)
                }
            )
        }

        composable(Screen.FullScanner.route) {
            FullScannerScreen(
                cartManager = cartManager,
                onNavigateBack = {
                    scanTargetMode = "cart"
                    navController.popBackStack()
                },
                onDoneScanning = {
                    scanTargetMode = "cart"
                    if (navController.previousBackStackEntry?.destination?.route == Screen.Sale.route) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Sale.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onBarcodeCaptured = if (scanTargetMode == "add" || scanTargetMode == "search") {
                    { barcode ->
                        if (scanTargetMode == "add") {
                            productViewModel.setScannedBarcode(barcode)
                        } else {
                            productViewModel.searchQuery.value = barcode
                        }
                        scanTargetMode = "cart"
                        navController.popBackStack()
                    }
                } else null
            )
        }
    }
}
