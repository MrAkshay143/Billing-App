package com.billing.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Checkout : Screen("checkout")
    object Products : Screen("products")
    object AddProduct : Screen("products/add")
    object EditProduct : Screen("products/edit/{productId}") {
        fun createRoute(productId: String) = "products/edit/$productId"
    }
    object Invoices : Screen("invoices")
    object More : Screen("more")
    object FullScanner : Screen("scanner")

    // More Subscreens
    object StoreProfile : Screen("more/store_profile")
    object UserAccess : Screen("more/user_access")
    object PrinterSetup : Screen("more/printer_setup")
    object PaymentSettings : Screen("more/payment_settings")
    object SalesReport : Screen("more/sales_report")
    object BackupRestore : Screen("more/backup_restore")
    object Appearance : Screen("more/appearance")
    object Language : Screen("more/language")
    object About : Screen("more/about")
}
