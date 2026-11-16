package com.billing.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.core.datastore.SettingsDataStore
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.domain.model.BluetoothPrinterDevice
import com.billing.app.domain.model.ShopProfile
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ShopRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class StaffMember(
    val id: String,
    val name: String,
    val phone: String,
    val role: String // "Administrator", "Manager", "Cashier"
)

data class SalesReportSummary(
    val totalInvoices: Int,
    val totalRevenuePaise: Long,
    val totalDiscountPaise: Long,
    val cashSalesPaise: Long,
    val upiSalesPaise: Long,
    val otherSalesPaise: Long,
    val avgBillPaise: Long
)

data class SettingsUiState(
    val connectedMac: String? = null,
    val connectedName: String? = null,
    val paperWidth: String = "58mm",
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isPrintingTest: Boolean = false,
    val bondedDevices: List<BluetoothPrinterDevice> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SettingsViewModel(
    private val printerRepository: PrinterRepository,
    private val shopRepository: ShopRepository,
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val shopProfile: StateFlow<ShopProfile> = shopRepository.getShopProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShopProfile())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val notificationsEnabled: StateFlow<Boolean> = settingsDataStore.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val appLanguage: StateFlow<String> = settingsDataStore.appLanguageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    val appTheme: StateFlow<String> = settingsDataStore.appThemeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    val paperWidth: StateFlow<String> = settingsDataStore.paperWidthFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "58mm")

    val defaultPaymentMethod: StateFlow<String> = settingsDataStore.defaultPaymentMethodFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "UPI")

    private val _staffList = MutableStateFlow<List<StaffMember>>(emptyList())
    val staffList: StateFlow<List<StaffMember>> = _staffList.asStateFlow()

    init {
        // Printer state observers
        viewModelScope.launch {
            printerRepository.getSavedPrinterMac().collect { mac ->
                _uiState.value = _uiState.value.copy(
                    connectedMac = mac,
                    isConnected = printerRepository.isConnected && mac != null
                )
            }
        }
        viewModelScope.launch {
            printerRepository.getSavedPrinterName().collect { name ->
                _uiState.value = _uiState.value.copy(connectedName = name)
            }
        }
        viewModelScope.launch {
            paperWidth.collect { width ->
                _uiState.value = _uiState.value.copy(paperWidth = width)
            }
        }
        // Load Staff Members
        loadStaffMembers()
    }

    // 1. Printer Setup Features
    fun refreshBondedDevices() {
        if (_uiState.value.isScanning) return
        _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)

        viewModelScope.launch {
            val devices = printerRepository.getBondedPrinters()
            _uiState.value = _uiState.value.copy(
                bondedDevices = devices,
                isScanning = false,
                errorMessage = if (devices.isEmpty()) "No paired Bluetooth printers found in Phone Bluetooth settings." else null
            )
        }
    }

    fun connectToPrinter(mac: String, name: String) {
        _uiState.value = _uiState.value.copy(isConnecting = true, errorMessage = null)
        viewModelScope.launch {
            val result = printerRepository.connect(mac)
            if (result.isSuccess) {
                printerRepository.savePrinterData(mac, name)
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    isConnected = true,
                    connectedMac = mac,
                    connectedName = name,
                    successMessage = "Connected to $name"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    isConnected = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to connect to printer"
                )
            }
        }
    }

    fun disconnectPrinter() {
        printerRepository.disconnect()
        viewModelScope.launch {
            printerRepository.clearPrinterData()
            _uiState.value = _uiState.value.copy(
                isConnected = false,
                connectedMac = null,
                connectedName = null,
                successMessage = "Printer disconnected"
            )
        }
    }

    fun setPaperWidth(width: String) {
        viewModelScope.launch {
            settingsDataStore.savePaperWidth(width)
        }
    }

    fun printTestReceipt() {
        if (_uiState.value.isPrintingTest) return
        _uiState.value = _uiState.value.copy(isPrintingTest = true, errorMessage = null)

        viewModelScope.launch {
            val shop = shopRepository.getShopProfileSync()
            val result = printerRepository.printTestReceipt(shop)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isPrintingTest = false,
                    successMessage = "Test receipt printed successfully!"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isPrintingTest = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Print failed: printer offline"
                )
            }
        }
    }

    // 2. User & Access (Staff Management)
    private fun loadStaffMembers() {
        viewModelScope.launch {
            val json = settingsDataStore.staffListJsonFlow.first()
            if (!json.isNullOrBlank()) {
                try {
                    val array = JSONArray(json)
                    val list = mutableListOf<StaffMember>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            StaffMember(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                phone = obj.getString("phone"),
                                role = obj.getString("role")
                            )
                        )
                    }
                    _staffList.value = list
                    return@launch
                } catch (_: Exception) {}
            }

            // Default Staff if none exists
            val defaultStaff = listOf(
                StaffMember(
                    id = "owner-1",
                    name = "Store Owner",
                    phone = "+91 9876543210",
                    role = "Administrator (Full Access)"
                ),
                StaffMember(
                    id = "cashier-1",
                    name = "Head Cashier",
                    phone = "+91 9876543211",
                    role = "Cashier (Billing Only)"
                )
            )
            _staffList.value = defaultStaff
            saveStaffMembersInternal(defaultStaff)
        }
    }

    fun addStaffMember(name: String, phone: String, role: String) {
        val newMember = StaffMember(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            phone = phone.trim(),
            role = role
        )
        val updated = _staffList.value + newMember
        _staffList.value = updated
        saveStaffMembersInternal(updated)
    }

    fun deleteStaffMember(id: String) {
        val updated = _staffList.value.filter { it.id != id }
        _staffList.value = updated
        saveStaffMembersInternal(updated)
    }

    private fun saveStaffMembersInternal(list: List<StaffMember>) {
        viewModelScope.launch {
            val array = JSONArray()
            list.forEach { staff ->
                val obj = JSONObject().apply {
                    put("id", staff.id)
                    put("name", staff.name)
                    put("phone", staff.phone)
                    put("role", staff.role)
                }
                array.put(obj)
            }
            settingsDataStore.saveStaffListJson(array.toString())
        }
    }

    // 3. Store Profile & Payment Settings
    fun updateShopProfile(
        name: String,
        address1: String,
        address2: String,
        phone: String,
        upiId: String,
        footer: String
    ) {
        viewModelScope.launch {
            val current = shopRepository.getShopProfileSync()
            val updated = current.copy(
                name = name.trim(),
                addressLine1 = address1.trim(),
                addressLine2 = address2.trim(),
                phoneNumber = phone.trim(),
                upiId = upiId.trim(),
                footerText = footer.trim()
            )
            shopRepository.updateShopProfile(updated)
            _uiState.value = _uiState.value.copy(successMessage = "Store profile updated successfully!")
        }
    }

    fun updatePaymentSettings(upiId: String, defaultMethod: String) {
        viewModelScope.launch {
            val current = shopRepository.getShopProfileSync()
            shopRepository.updateShopProfile(current.copy(upiId = upiId.trim()))
            settingsDataStore.saveDefaultPaymentMethod(defaultMethod)
            _uiState.value = _uiState.value.copy(successMessage = "Payment settings saved!")
        }
    }

    // 4. Sales Report Generation
    suspend fun generateSalesReport(): SalesReportSummary = withContext(Dispatchers.IO) {
        val allInvoices = database.invoiceDao().getAllInvoices().first()
        val invoices = allInvoices.filter { it.status == InvoiceStatus.COMPLETED }
        val totalCount = invoices.size
        val totalRevenue = invoices.sumOf { it.grandTotalPaise }
        val totalDiscount = invoices.sumOf { it.discountPaise }
        val cashSales = invoices.filter { it.paymentMethod == "CASH" }.sumOf { it.grandTotalPaise }
        val upiSales = invoices.filter { it.paymentMethod == "UPI" }.sumOf { it.grandTotalPaise }
        val otherSales = invoices.filter { it.paymentMethod != "CASH" && it.paymentMethod != "UPI" }.sumOf { it.grandTotalPaise }
        val avg = if (totalCount > 0) totalRevenue / totalCount else 0L

        SalesReportSummary(
            totalInvoices = totalCount,
            totalRevenuePaise = totalRevenue,
            totalDiscountPaise = totalDiscount,
            cashSalesPaise = cashSales,
            upiSalesPaise = upiSales,
            otherSalesPaise = otherSales,
            avgBillPaise = avg
        )
    }

    // 5. Backup & Restore
    suspend fun createDatabaseBackup(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val products = database.productDao().getAllProducts().first()
            val invoices = database.invoiceDao().getAllInvoices().first()
            val invoiceItems = database.invoiceDao().getAllInvoiceItems()
            val shop = shopRepository.getShopProfileSync()

            val rootJson = JSONObject().apply {
                put("version", 2)
                put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                put("timestamp", System.currentTimeMillis())

                // Shop info
                put("shop", JSONObject().apply {
                    put("name", shop.name)
                    put("address1", shop.addressLine1)
                    put("address2", shop.addressLine2)
                    put("phone", shop.phoneNumber)
                    put("upiId", shop.upiId)
                    put("footer", shop.footerText)
                })

                // Products
                val productsArr = JSONArray()
                products.forEach { p ->
                    productsArr.put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("barcode", p.barcode)
                        put("pricePaise", p.pricePaise)
                        put("stock", p.stock)
                        put("imageUri", p.imageUri ?: "")
                    })
                }
                put("products", productsArr)

                // Invoices
                val invoicesArr = JSONArray()
                invoices.forEach { inv ->
                    invoicesArr.put(JSONObject().apply {
                        put("invoiceNumber", inv.invoiceNumber)
                        put("timestamp", inv.timestamp)
                        put("subtotalPaise", inv.subtotalPaise)
                        put("taxPaise", inv.taxPaise)
                        put("discountPaise", inv.discountPaise)
                        put("grandTotalPaise", inv.grandTotalPaise)
                        put("status", inv.status.name)
                        put("paymentMethod", inv.paymentMethod)
                        put("customerName", inv.customerName ?: "")
                        put("customerPhone", inv.customerPhone ?: "")
                        put("cashReceivedPaise", inv.cashReceivedPaise)
                        put("changeReturnedPaise", inv.changeReturnedPaise)
                    })
                }
                put("invoices", invoicesArr)

                // Invoice Items
                val itemsArr = JSONArray()
                invoiceItems.forEach { item ->
                    itemsArr.put(JSONObject().apply {
                        put("id", item.id)
                        put("invoiceNumber", item.invoiceNumber)
                        put("productId", item.productId)
                        put("productName", item.productName)
                        put("barcode", item.barcode)
                        put("unitPricePaise", item.unitPricePaise)
                        put("quantity", item.quantity)
                        put("lineTotalPaise", item.lineTotalPaise)
                    })
                }
                put("invoiceItems", itemsArr)
            }

            val dir = File(context.filesDir, "backups")
            if (!dir.exists()) dir.mkdirs()
            val fileName = "dokandaar_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
            val file = File(dir, fileName)
            file.writeText(rootJson.toString(2))

            Result.success("Backup saved to ${file.name} (${products.size} products, ${invoices.size} invoices, ${invoiceItems.size} items)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreDatabaseBackup(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = file.readText()
            val root = JSONObject(content)

            database.withTransaction {
                // Restore Products
                if (root.has("products")) {
                    val pArray = root.getJSONArray("products")
                    for (i in 0 until pArray.length()) {
                        val obj = pArray.getJSONObject(i)
                        val entity = ProductEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            barcode = obj.getString("barcode"),
                            pricePaise = obj.getLong("pricePaise"),
                            stock = obj.getInt("stock"),
                            imageUri = obj.optString("imageUri").ifBlank { null }
                        )
                        database.productDao().insertProduct(entity)
                    }
                }

                // Restore Invoices
                if (root.has("invoices")) {
                    val iArray = root.getJSONArray("invoices")
                    for (i in 0 until iArray.length()) {
                        val obj = iArray.getJSONObject(i)
                        val statusStr = obj.optString("status", "COMPLETED")
                        val status = try {
                            InvoiceStatus.valueOf(statusStr)
                        } catch (_: Exception) {
                            InvoiceStatus.COMPLETED
                        }
                        val inv = InvoiceEntity(
                            invoiceNumber = obj.getString("invoiceNumber"),
                            timestamp = obj.getLong("timestamp"),
                            subtotalPaise = obj.getLong("subtotalPaise"),
                            taxPaise = obj.optLong("taxPaise", 0L),
                            discountPaise = obj.optLong("discountPaise", 0L),
                            grandTotalPaise = obj.getLong("grandTotalPaise"),
                            status = status,
                            paymentMethod = obj.getString("paymentMethod"),
                            customerName = obj.optString("customerName").ifBlank { null },
                            customerPhone = obj.optString("customerPhone").ifBlank { null },
                            cashReceivedPaise = obj.optLong("cashReceivedPaise", 0L),
                            changeReturnedPaise = obj.optLong("changeReturnedPaise", 0L)
                        )
                        database.invoiceDao().insertInvoice(inv)
                    }
                }

                // Restore Invoice Items
                if (root.has("invoiceItems")) {
                    val itemArray = root.getJSONArray("invoiceItems")
                    val itemsList = mutableListOf<InvoiceItemEntity>()
                    for (i in 0 until itemArray.length()) {
                        val obj = itemArray.getJSONObject(i)
                        itemsList.add(
                            InvoiceItemEntity(
                                id = obj.optLong("id", 0L),
                                invoiceNumber = obj.getString("invoiceNumber"),
                                productId = obj.getString("productId"),
                                productName = obj.getString("productName"),
                                barcode = obj.optString("barcode", ""),
                                unitPricePaise = obj.getLong("unitPricePaise"),
                                quantity = obj.getInt("quantity"),
                                lineTotalPaise = obj.getLong("lineTotalPaise")
                            )
                        )
                    }
                    database.invoiceDao().insertInvoiceItems(itemsList)
                }
            }

            Result.success("Restored database from ${file.name} successfully!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listBackups(context: Context): List<File> {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    // 6. Appearance & Language & Notifications
    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            settingsDataStore.saveAppTheme(theme)
        }
    }

    fun setAppLanguage(lang: String) {
        viewModelScope.launch {
            settingsDataStore.saveAppLanguage(lang)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveNotificationsEnabled(enabled)
        }
    }

    fun triggerSalesSummaryNotification(context: Context) {
        viewModelScope.launch {
            if (settingsDataStore.areNotificationsEnabled()) {
                val report = generateSalesReport()
                com.billing.app.core.notification.DokandaarNotificationManager.showDailySalesNotification(
                    context = context,
                    totalInvoices = report.totalInvoices,
                    totalRevenueFormatted = Money.fromPaise(report.totalRevenuePaise).format()
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun clearError() {
        clearMessages()
    }

    fun refreshAndAutoConnect() {
        refreshBondedDevices()
    }
}
