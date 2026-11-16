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
import com.billing.app.data.repository.InvoiceRepositoryImpl
import com.billing.app.data.repository.PrinterRepositoryImpl
import com.billing.app.data.repository.ProductRepositoryImpl
import com.billing.app.data.repository.ShopRepositoryImpl
import com.billing.app.domain.repository.InvoiceRepository
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.ShopRepository
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

            DokandaarNotificationManager.initChannels(this)
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
