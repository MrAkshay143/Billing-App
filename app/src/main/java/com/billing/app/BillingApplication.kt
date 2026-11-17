package com.billing.app

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import com.billing.app.core.audio.SoundHapticManager
import com.billing.app.core.database.AppDatabase
import com.billing.app.core.datastore.SettingsDataStore
import com.billing.app.core.notification.DokandaarNotificationManager
import com.billing.app.core.printer.BluetoothPrinterManager
import com.billing.app.core.util.Money
import com.billing.app.data.repository.CategoryRepositoryImpl
import com.billing.app.data.repository.CustomerRepositoryImpl
import com.billing.app.data.repository.ExpenseRepositoryImpl
import com.billing.app.data.repository.InvoiceRepositoryImpl
import com.billing.app.data.repository.PrinterRepositoryImpl
import com.billing.app.data.repository.ProductRepositoryImpl
import com.billing.app.data.repository.PurchaseRepositoryImpl
import com.billing.app.data.repository.ShopRepositoryImpl
import com.billing.app.data.repository.SupplierRepositoryImpl
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.CategoryRepository
import com.billing.app.domain.repository.CustomerRepository
import com.billing.app.domain.repository.ExpenseRepository
import com.billing.app.domain.repository.InvoiceRepository
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.PurchaseRepository
import com.billing.app.domain.repository.ShopRepository
import com.billing.app.domain.repository.SupplierRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillingApplication : Application() {
    companion object {
        lateinit var instance: BillingApplication
            private set
    }

    lateinit var database: AppDatabase
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var soundHapticManager: SoundHapticManager
        private set
    lateinit var printerManager: BluetoothPrinterManager
        private set

    lateinit var productRepository: ProductRepository
        private set
    lateinit var shopRepository: ShopRepository
        private set
    lateinit var printerRepository: PrinterRepository
        private set
    lateinit var invoiceRepository: InvoiceRepository
        private set
    lateinit var customerRepository: CustomerRepository
        private set
    lateinit var categoryRepository: CategoryRepository
        private set
    lateinit var supplierRepository: SupplierRepository
        private set
    lateinit var expenseRepository: ExpenseRepository
        private set
    lateinit var purchaseRepository: PurchaseRepository
        private set
    lateinit var cartManager: com.billing.app.core.cart.CartManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val currentProcessName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            ""
        }
        if (currentProcessName.endsWith(":crash")) {
            return
        }

        // Global crash handler to catch and display any startup crash
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("FATAL_CRASH", "Uncaught exception in ${thread.name}", throwable)
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()

                val logFilePath = saveCrashLog(stackTrace)

                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra("error_message", throwable.javaClass.simpleName + ": " + (throwable.message ?: "Unexpected error"))
                    putExtra("error_stacktrace", stackTrace)
                    putExtra("crash_log_path", logFilePath)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } catch (e: Exception) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        try {
            database = AppDatabase.getInstance(this)
            settingsDataStore = SettingsDataStore(this)
            soundHapticManager = SoundHapticManager(this)
            printerManager = BluetoothPrinterManager(this)

            productRepository = ProductRepositoryImpl(database.productDao())
            shopRepository = ShopRepositoryImpl(database.shopDao())
            invoiceRepository = InvoiceRepositoryImpl(database)
            printerRepository = PrinterRepositoryImpl(printerManager, settingsDataStore)
            customerRepository = CustomerRepositoryImpl(database.customerDao())
            categoryRepository = CategoryRepositoryImpl(database.categoryDao())
            supplierRepository = SupplierRepositoryImpl(database.supplierDao())
            expenseRepository = ExpenseRepositoryImpl(database.expenseDao())
            purchaseRepository = PurchaseRepositoryImpl(database)
            cartManager = com.billing.app.core.cart.CartManager(productRepository, soundHapticManager)

            DokandaarNotificationManager.initChannels(this)

            // Seed default categories & sample products if empty
            CoroutineScope(Dispatchers.IO).launch {
                categoryRepository.seedDefaultCategoriesIfEmpty()
                seedSampleProductsIfEmpty()
            }
        } catch (e: Exception) {
            Log.e("BillingApplication", "Error initializing repositories in onCreate", e)
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()
            val logFilePath = saveCrashLog(stackTrace)

            val intent = Intent(this, CrashActivity::class.java).apply {
                putExtra("error_message", "Initialization failure: ${e.message}")
                putExtra("error_stacktrace", stackTrace)
                putExtra("crash_log_path", logFilePath)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }

    private suspend fun seedSampleProductsIfEmpty() {
        val count = productRepository.getTotalCount().first()
        if (count == 0) {
            val samples = listOf(
                Product(
                    id = "p-atta-5kg",
                    name = "Aashirvaad Atta (5 kg)",
                    sku = "ATT-5KG",
                    barcode = "8901512567890",
                    price = Money.fromRupees(285.0),
                    stock = 28,
                    category = "Grocery",
                    unit = "Packet",
                    mrp = Money.fromRupees(310.0),
                    minStockAlert = 5
                ),
                Product(
                    id = "p-oil-1l",
                    name = "Fortune Refined Sunflower Oil (1 L)",
                    sku = "OIL-SF-1L",
                    barcode = "8901156101234",
                    price = Money.fromRupees(155.0),
                    stock = 42,
                    category = "Oil & Ghee",
                    unit = "Packet",
                    mrp = Money.fromRupees(175.0),
                    minStockAlert = 8
                ),
                Product(
                    id = "p-salt-1kg",
                    name = "Tata Salt (1 kg)",
                    sku = "SALT-1KG",
                    barcode = "8901052000123",
                    price = Money.fromRupees(28.0),
                    stock = 60,
                    category = "Grocery",
                    unit = "Packet",
                    mrp = Money.fromRupees(30.0),
                    minStockAlert = 10
                ),
                Product(
                    id = "p-sugar-1kg",
                    name = "Sugar (1 kg)",
                    sku = "SUGAR-1KG",
                    barcode = "8906001056789",
                    price = Money.fromRupees(44.0),
                    stock = 75,
                    category = "Grocery",
                    unit = "kg",
                    mrp = Money.fromRupees(48.0),
                    minStockAlert = 15
                ),
                Product(
                    id = "p-milk-1l",
                    name = "Amul Gold Milk (1 L)",
                    sku = "MILK-1L",
                    barcode = "8901262078901",
                    price = Money.fromRupees(68.0),
                    stock = 8,
                    category = "Dairy Products",
                    unit = "Packet",
                    mrp = Money.fromRupees(70.0),
                    minStockAlert = 10
                )
            )
            productRepository.addProducts(samples)
        }
    }

    private fun saveCrashLog(stackTrace: String): String? {
        return try {
            val crashDir = File(filesDir, "crash_logs")
            if (!crashDir.exists()) crashDir.mkdirs()
            val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(crashDir, "crash_$time.log")
            file.writeText(stackTrace)
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
