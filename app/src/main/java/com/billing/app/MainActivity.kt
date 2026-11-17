package com.billing.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.billing.app.core.localization.LocalAppStrings
import com.billing.app.core.localization.getAppStrings
import com.billing.app.core.ui.theme.BillingAppTheme
import com.billing.app.feature.checkout.CheckoutViewModel
import com.billing.app.feature.home.HomeViewModel
import com.billing.app.feature.invoices.InvoiceViewModel
import com.billing.app.feature.products.ProductViewModel
import com.billing.app.feature.reports.ReportsViewModel
import com.billing.app.feature.settings.SettingsViewModel
import com.billing.app.navigation.AppNavHost
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge(
                statusBarStyle = androidx.activity.SystemBarStyle.dark(
                    android.graphics.Color.TRANSPARENT
                ),
                navigationBarStyle = androidx.activity.SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "enableEdgeToEdge error", e)
        }

        val app = application as BillingApplication

        val homeViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(app.productRepository, app.invoiceRepository, app.customerRepository, app.shopRepository) as T
                }
            }
        )[HomeViewModel::class.java]

        val checkoutViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CheckoutViewModel(
                        shopRepository = app.shopRepository,
                        printerRepository = app.printerRepository,
                        invoiceRepository = app.invoiceRepository,
                        settingsDataStore = app.settingsDataStore,
                        productRepository = app.productRepository,
                        customerRepository = app.customerRepository,
                        categoryRepository = app.categoryRepository
                    ) as T
                }
            }
        )[CheckoutViewModel::class.java]

        val productViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProductViewModel(
                        productRepository = app.productRepository,
                        categoryRepository = app.categoryRepository
                    ) as T
                }
            }
        )[ProductViewModel::class.java]

        val invoiceViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InvoiceViewModel(app.invoiceRepository, app.printerRepository, app.shopRepository, app.settingsDataStore) as T
                }
            }
        )[InvoiceViewModel::class.java]

        val reportsViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReportsViewModel(app.invoiceRepository, app.productRepository) as T
                }
            }
        )[ReportsViewModel::class.java]

        val settingsViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(app.printerRepository, app.shopRepository, app.database, app.settingsDataStore, app.invoiceRepository) as T
                }
            }
        )[SettingsViewModel::class.java]

        setContent {
            val themePreference by app.settingsDataStore.appThemeFlow.collectAsState(initial = "system")
            val languagePreference by app.settingsDataStore.appLanguageFlow.collectAsState(initial = "en")

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themePreference.lowercase()) {
                "dark" -> true
                "light" -> false
                else -> isSystemDark
            }

            val configuration = LocalConfiguration.current
            val currentLocale = remember(languagePreference) { Locale.forLanguageTag(languagePreference) }
            configuration.setLocale(currentLocale)
            val currentStrings = remember(languagePreference) { getAppStrings(languagePreference) }

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalAppStrings provides currentStrings
            ) {
                BillingAppTheme(darkTheme = isDark) {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        homeViewModel = homeViewModel,
                        checkoutViewModel = checkoutViewModel,
                        productViewModel = productViewModel,
                        invoiceViewModel = invoiceViewModel,
                        reportsViewModel = reportsViewModel,
                        settingsViewModel = settingsViewModel,
                        cartManager = app.cartManager,
                        customerRepository = app.customerRepository,
                        supplierRepository = app.supplierRepository,
                        expenseRepository = app.expenseRepository,
                        categoryRepository = app.categoryRepository,
                        soundHapticManager = app.soundHapticManager
                    )
                }
            }
        }
    }
}
