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
import com.billing.app.data.repository.PrinterRepositoryImpl
import com.billing.app.data.repository.ProductRepositoryImpl
import com.billing.app.data.repository.ShopRepositoryImpl
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.ShopRepository
import java.io.PrintWriter
import java.io.StringWriter

class BillingApplication : Application() {
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

    override fun onCreate() {
        super.onCreate()

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

                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra("error_message", throwable.javaClass.simpleName + ": " + (throwable.message ?: "Unknown Error"))
                    putExtra("error_stacktrace", stackTrace)
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
            printerRepository = PrinterRepositoryImpl(printerManager, settingsDataStore)

            DokandaarNotificationManager.initChannels(this)
        } catch (e: Exception) {
            Log.e("BillingApplication", "Error initializing repositories in onCreate", e)
        }
    }
}
