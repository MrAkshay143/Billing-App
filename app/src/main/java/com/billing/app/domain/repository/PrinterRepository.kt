package com.billing.app.domain.repository

import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.domain.model.BluetoothPrinterDevice
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.InvoiceReceipt
import com.billing.app.domain.model.ShopProfile
import kotlinx.coroutines.flow.Flow

interface PrinterRepository {
    fun getSavedPrinterMac(): Flow<String?>
    fun getSavedPrinterName(): Flow<String?>
    suspend fun getBondedPrinters(): List<BluetoothPrinterDevice>
    suspend fun connect(macAddress: String): Result<Unit>
    fun disconnect()
    val isConnected: Boolean
    suspend fun savePrinterData(mac: String, name: String)
    suspend fun clearPrinterData()

    suspend fun printReceipt(shop: ShopProfile, items: List<CartItem>): Result<Unit>
    suspend fun printInvoiceReceipt(receipt: InvoiceReceipt): Result<Unit>
    suspend fun printInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>, shop: ShopProfile): Result<Unit>
    suspend fun printTestReceipt(shop: ShopProfile): Result<Unit>
}
