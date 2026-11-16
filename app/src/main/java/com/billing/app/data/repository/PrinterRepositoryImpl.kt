package com.billing.app.data.repository

import com.billing.app.core.datastore.SettingsDataStore
import com.billing.app.core.printer.BluetoothPrinterManager
import com.billing.app.core.printer.EscPosFormatter
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.domain.model.BluetoothPrinterDevice
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.InvoiceReceipt
import com.billing.app.domain.model.ReceiptItem
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

    private suspend fun ensureConnected(): Result<Unit> {
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
        return Result.success(Unit)
    }

    override suspend fun printReceipt(shop: ShopProfile, items: List<CartItem>): Result<Unit> {
        val ensureResult = ensureConnected()
        if (ensureResult.isFailure) return ensureResult

        val total = items.fold(Money.ZERO) { acc, item -> acc + item.total }
        val paperWidth = settingsDataStore.getPaperWidth()
        val receiptItems = items.map {
            ReceiptItem(
                name = it.product.name,
                quantity = it.quantity,
                unitPricePaise = it.product.price.paise,
                lineTotalPaise = it.total.paise
            )
        }
        val receipt = InvoiceReceipt(
            shopName = shop.name,
            shopAddressLine1 = shop.addressLine1,
            shopAddressLine2 = shop.addressLine2,
            shopPhone = shop.phoneNumber,
            shopGstin = "",
            footerText = shop.footerText,
            invoiceNumber = "INV-SAMPLE",
            timestamp = System.currentTimeMillis(),
            items = receiptItems,
            subtotalPaise = total.paise,
            discountPaise = 0L,
            taxPaise = 0L,
            grandTotalPaise = total.paise,
            paymentMethod = "CASH"
        )
        val bytes = EscPosFormatter.formatReceiptAuto(receipt, paperWidth)
        return printerManager.printBytes(bytes)
    }

    override suspend fun printInvoiceReceipt(receipt: InvoiceReceipt): Result<Unit> {
        val ensureResult = ensureConnected()
        if (ensureResult.isFailure) return ensureResult

        val paperWidth = settingsDataStore.getPaperWidth()
        val bytes = EscPosFormatter.formatReceiptAuto(receipt, paperWidth)
        return printerManager.printBytes(bytes)
    }

    override suspend fun printInvoice(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        shop: ShopProfile
    ): Result<Unit> {
        val receiptItems = items.map {
            ReceiptItem(
                name = it.productName,
                quantity = it.quantity,
                unitPricePaise = it.unitPricePaise,
                lineTotalPaise = it.lineTotalPaise
            )
        }
        val receipt = InvoiceReceipt(
            shopName = shop.name,
            shopAddressLine1 = shop.addressLine1,
            shopAddressLine2 = shop.addressLine2,
            shopPhone = shop.phoneNumber,
            shopGstin = "",
            footerText = shop.footerText,
            invoiceNumber = invoice.invoiceNumber,
            timestamp = invoice.timestamp,
            items = receiptItems,
            subtotalPaise = invoice.subtotalPaise,
            discountPaise = invoice.discountPaise,
            taxPaise = invoice.taxPaise,
            grandTotalPaise = invoice.grandTotalPaise,
            paymentMethod = invoice.paymentMethod,
            cashReceivedPaise = invoice.cashReceivedPaise,
            changeReturnedPaise = invoice.changeReturnedPaise,
            duePaise = invoice.duePaise,
            customerName = invoice.customerName,
            customerPhone = invoice.customerPhone
        )
        return printInvoiceReceipt(receipt)
    }

    override suspend fun printTestReceipt(shop: ShopProfile): Result<Unit> {
        val testItems = listOf(
            CartItem(
                product = com.billing.app.domain.model.Product(
                    id = "test-1",
                    name = "Test Item 1",
                    barcode = "890100000001",
                    price = Money.fromRupees(10.0)
                ),
                quantity = 1
            ),
            CartItem(
                product = com.billing.app.domain.model.Product(
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
