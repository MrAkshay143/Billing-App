package com.billing.app.data.repository

import com.billing.app.core.datastore.SettingsDataStore
import com.billing.app.core.printer.BluetoothPrinterManager
import com.billing.app.core.printer.EscPosFormatter
import com.billing.app.core.util.Money
import com.billing.app.domain.model.BluetoothPrinterDevice
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.Product
import com.billing.app.domain.model.ShopProfile
import com.billing.app.domain.repository.PrinterRepository
import kotlinx.coroutines.flow.Flow

class PrinterRepositoryImpl(
    private val printerManager: BluetoothPrinterManager,
    private val settingsDataStore: SettingsDataStore
) : PrinterRepository {

    override fun getSavedPrinterMac(): Flow<String?> = settingsDataStore.printerMacFlow

    override fun getSavedPrinterName(): Flow<String?> = settingsDataStore.printerNameFlow

    override suspend fun getBondedPrinters(): List<BluetoothPrinterDevice> {
        return printerManager.getBondedPrinters()
    }

    override suspend fun connect(macAddress: String): Result<Unit> {
        return printerManager.connect(macAddress)
    }

    override fun disconnect() {
        printerManager.disconnect()
    }

    override val isConnected: Boolean get() = printerManager.isConnected

    override suspend fun savePrinterData(mac: String, name: String) {
        settingsDataStore.savePrinterData(mac, name)
    }

    override suspend fun clearPrinterData() {
        settingsDataStore.clearPrinterData()
    }

    override suspend fun printReceipt(shop: ShopProfile, items: List<CartItem>): Result<Unit> {
        if (!printerManager.isConnected) {
            val savedMac = settingsDataStore.getSavedPrinterMac()
            if (!savedMac.isNullOrBlank()) {
                val connectResult = printerManager.connect(savedMac)
                if (connectResult.isFailure) {
                    return Result.failure(Exception("Failed to auto-connect to printer ($savedMac)"))
                }
            } else {
                return Result.failure(Exception("Printer not connected and no saved printer found"))
            }
        }

        val total = items.fold(Money.ZERO) { acc, item -> acc + item.total }
        val bytes = EscPosFormatter.formatReceipt(shop, items, total)
        return printerManager.printBytes(bytes)
    }

    override suspend fun printTestReceipt(shop: ShopProfile): Result<Unit> {
        val testItems = listOf(
            CartItem(
                product = Product(
                    id = "test-1",
                    name = "Test Item 1",
                    barcode = "890100000001",
                    price = Money.fromRupees(10.0)
                ),
                quantity = 1
            ),
            CartItem(
                product = Product(
                    id = "test-2",
                    name = "Test Item 2",
                    barcode = "890100000002",
                    price = Money.fromRupees(25.0)
                ),
                quantity = 2
            )
        )
        return printReceipt(shop, testItems)
    }
}
