package com.billing.app.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.backup.BackupManager
import com.billing.app.core.database.AppDatabase
import com.billing.app.core.datastore.SettingsDataStore
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.model.BluetoothPrinterDevice
import com.billing.app.domain.model.ShopProfile
import com.billing.app.domain.repository.InvoiceRepository
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ShopRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class StaffMember(
    val id: String,
    val name: String,
    val phone: String,
    val role: String
)

data class SalesReportSummary(
    val totalInvoices: Int,
    val totalRevenuePaise: Long,
    val totalDiscountPaise: Long,
    val totalTaxPaise: Long = 0L,
    val totalDuePaise: Long = 0L,
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
    private val settingsDataStore: SettingsDataStore,
    private val invoiceRepository: InvoiceRepository
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

    // PIN Lock State (H3)
    val isPinEnabled: StateFlow<Boolean> = settingsDataStore.pinEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val hasPinSet: StateFlow<Boolean> = settingsDataStore.hasPinSetFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val autoLockMinutes: StateFlow<Int> = settingsDataStore.autoLockMinutesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

    // GST State (H7)
    val isGstEnabled: StateFlow<Boolean> = settingsDataStore.gstEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val gstin: StateFlow<String> = settingsDataStore.gstinFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val taxRate: StateFlow<String> = settingsDataStore.taxRateFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "0")

    private val _staffList = MutableStateFlow<List<StaffMember>>(emptyList())
    val staffList: StateFlow<List<StaffMember>> = _staffList.asStateFlow()

    init {
        // Printer state observers
        viewModelScope.launch {
            printerRepository.getSavedPrinterMac().collect { mac ->
                _uiState.value = _uiState.value.copy(
                    connectedMac = mac,
                    isConnected = printerRepository.isConnected
                )
            }
        }

        viewModelScope.launch {
            printerRepository.getSavedPrinterName().collect { name ->
                _uiState.value = _uiState.value.copy(connectedName = name)
            }
        }

        viewModelScope.launch {
            settingsDataStore.paperWidthFlow.collect { width ->
                _uiState.value = _uiState.value.copy(paperWidth = width)
            }
        }

        loadBondedPrinters()
        loadStaffMembers()
    }

    // 1. Bluetooth Printer Management
    fun loadBondedPrinters() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            val bonded = printerRepository.getBondedPrinters()
            _uiState.value = _uiState.value.copy(
                bondedDevices = bonded,
                isScanning = false,
                isConnected = printerRepository.isConnected
            )
        }
    }

    fun refreshBondedDevices() = loadBondedPrinters()

    fun connectPrinter(device: BluetoothPrinterDevice) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, errorMessage = null)
            val result = printerRepository.connect(device.macAddress)
            if (result.isSuccess) {
                printerRepository.savePrinterData(device.macAddress, device.name)
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    isConnected = true,
                    connectedMac = device.macAddress,
                    connectedName = device.name,
                    successMessage = "Connected to ${device.name}"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    isConnected = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Connection failed"
                )
            }
        }
    }

    fun connectToPrinter(device: BluetoothPrinterDevice) = connectPrinter(device)
    fun connectToPrinter(macAddress: String, name: String) =
        connectPrinter(BluetoothPrinterDevice(name = name, macAddress = macAddress))

    fun disconnectPrinter() {
        printerRepository.disconnect()
        _uiState.value = _uiState.value.copy(
            isConnected = false,
            successMessage = "Printer disconnected"
        )
    }

    fun forgetPrinter() {
        viewModelScope.launch {
            printerRepository.disconnect()
            printerRepository.clearPrinterData()
            _uiState.value = _uiState.value.copy(
                isConnected = false,
                connectedMac = null,
                connectedName = null,
                successMessage = "Printer unpaired"
            )
        }
    }

    fun setPaperWidth(width: String) {
        viewModelScope.launch {
            settingsDataStore.savePaperWidth(width)
            _uiState.value = _uiState.value.copy(paperWidth = width)
        }
    }

    fun printTestReceipt() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrintingTest = true, errorMessage = null)
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

    // 2. User & Access (Staff Management - H3)
    private fun loadStaffMembers() {
        viewModelScope.launch {
            val json = settingsDataStore.staffListJsonFlow.firstOrNull()
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
            // Starts empty - no fake seeded phone numbers (H3)
            _staffList.value = emptyList()
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

    // PIN Security Management (H3)
    suspend fun setSecurityPin(pin: String) {
        settingsDataStore.setPin(pin)
    }

    suspend fun verifySecurityPin(pin: String): Boolean {
        return settingsDataStore.verifyPin(pin)
    }

    suspend fun removeSecurityPin() {
        settingsDataStore.removePin()
    }

    suspend fun togglePinSecurity(enabled: Boolean) {
        settingsDataStore.setPinEnabled(enabled)
    }

    suspend fun updateAutoLockMinutes(minutes: Int) {
        settingsDataStore.saveAutoLockMinutes(minutes)
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
            shopRepository.updateShopProfile(
                current.copy(
                    name = name.trim(),
                    addressLine1 = address1.trim(),
                    addressLine2 = address2.trim(),
                    phoneNumber = phone.trim(),
                    upiId = upiId.trim(),
                    footerText = footer.trim()
                )
            )
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

    // GST Configuration (H7)
    fun saveGstConfiguration(enabled: Boolean, gstin: String, taxRate: String) {
        viewModelScope.launch {
            settingsDataStore.saveGstSettings(enabled, gstin, taxRate)
            _uiState.value = _uiState.value.copy(successMessage = "GST settings saved!")
        }
    }

    // 4. Sales Report Generation (M1: Parameterized & Accurate)
    suspend fun generateSalesReport(
        startTime: Long = 0L,
        endTime: Long = Long.MAX_VALUE
    ): SalesReportSummary = withContext(Dispatchers.IO) {
        val invoices = if (startTime == 0L && endTime == Long.MAX_VALUE) {
            invoiceRepository.getInvoicesByStatus(InvoiceStatus.COMPLETED).first()
        } else {
            invoiceRepository.getInvoicesBetween(startTime, endTime).first()
                .filter { it.status == InvoiceStatus.COMPLETED }
        }

        val totalCount = invoices.size
        val totalRevenue = invoices.sumOf { it.grandTotalPaise }
        val totalDiscount = invoices.sumOf { it.discountPaise }
        val totalTax = invoices.sumOf { it.taxPaise }
        val totalDue = invoices.sumOf { it.duePaise }
        val cashSales = invoices.filter { it.paymentMethod == "CASH" }.sumOf { it.grandTotalPaise }
        val upiSales = invoices.filter { it.paymentMethod == "UPI" }.sumOf { it.grandTotalPaise }
        val otherSales = invoices.filter { it.paymentMethod != "CASH" && it.paymentMethod != "UPI" }.sumOf { it.grandTotalPaise }
        val avg = if (totalCount > 0) totalRevenue / totalCount else 0L

        SalesReportSummary(
            totalInvoices = totalCount,
            totalRevenuePaise = totalRevenue,
            totalDiscountPaise = totalDiscount,
            totalTaxPaise = totalTax,
            totalDuePaise = totalDue,
            cashSalesPaise = cashSales,
            upiSalesPaise = upiSales,
            otherSalesPaise = otherSales,
            avgBillPaise = avg
        )
    }

    // 5. Rebuilt Backup & Restore with Checksum, Upsert, and SAF (C3)
    suspend fun createDatabaseBackup(context: Context): Result<String> {
        val fileResult = BackupManager.saveBackupToInternalStorage(context, database)
        return if (fileResult.isSuccess) {
            val file = fileResult.getOrThrow()
            Result.success("Backup saved to ${file.name}")
        } else {
            Result.failure(fileResult.exceptionOrNull() ?: Exception("Backup failed"))
        }
    }

    suspend fun exportBackupToUri(context: Context, uri: Uri): Result<String> {
        return BackupManager.exportBackupToUri(context, database, uri)
    }

    suspend fun restoreDatabaseBackup(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = file.readText()
            BackupManager.restoreBackupFromJson(database, content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackupFromUri(context: Context, uri: Uri): Result<String> {
        return BackupManager.importBackupFromUri(context, database, uri)
    }

    fun shareBackup(context: Context, file: File) {
        BackupManager.shareBackupFile(context, file)
    }

    fun listBackups(context: Context): List<File> {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteBackup(file: File): Boolean {
        return file.delete()
    }

    // 6. Preferences
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveNotificationsEnabled(enabled)
        }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch {
            settingsDataStore.saveAppLanguage(language)
        }
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            settingsDataStore.saveAppTheme(theme)
        }
    }

    fun triggerSalesSummaryNotification(context: Context) {
        viewModelScope.launch {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis

            val report = generateSalesReport(startTime = todayStart)
            val formatted = com.billing.app.core.util.Money.fromPaise(report.totalRevenuePaise).format()
            com.billing.app.core.notification.DokandaarNotificationManager.showDailySalesNotification(
                context = context,
                totalInvoices = report.totalInvoices,
                totalRevenueFormatted = formatted
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
