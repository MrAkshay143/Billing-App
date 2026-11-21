package com.billing.app.core.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.core.util.UnitRegistry
import com.billing.app.data.local.entity.AuditLogEntity
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.CategoryEntity
import com.billing.app.data.local.entity.CustomUnitEntity
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import com.billing.app.data.local.entity.ExpenseEntity
import com.billing.app.data.local.entity.InvoiceCounterEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemBatchEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.data.local.entity.PaymentAllocationEntity
import com.billing.app.data.local.entity.ProductBatchEntity
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.PurchaseItemEntity
import com.billing.app.data.local.entity.ReturnEntity
import com.billing.app.data.local.entity.ReturnItemBatchEntity
import com.billing.app.data.local.entity.ReturnItemEntity
import com.billing.app.data.local.entity.ShiftEntity
import com.billing.app.data.local.entity.ShopEntity
import com.billing.app.data.local.entity.StockAdjustmentEntity
import com.billing.app.data.local.entity.SupplierEntity
import com.billing.app.data.local.entity.SupplierLedgerEntity
import com.billing.app.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private const val BACKUP_VERSION = 6

    private fun calculateSha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun createBackupJson(database: AppDatabase): String = withContext(Dispatchers.IO) {
        // Query all 25 entities in dependency order
        val users = database.userDao().getAllUsers().first()
        val shop = database.shopDao().getShopProfileSync()
        val categories = database.categoryDao().getAllCategories().first()
        val customers = database.customerDao().getAllCustomers().first()
        val suppliers = database.supplierDao().getAllSuppliers().first()
        val shifts = database.shiftDao().getAllShifts().first()
        val cashMovements = database.cashMovementDao().getAllCashMovements().first()
        val products = database.productDao().getAllProducts().first()
        val productBatches = database.productBatchDao().getAllBatchList()
        val stockAdjustments = database.stockAdjustmentDao().getAllStockAdjustmentList()
        val invoices = database.invoiceDao().getAllInvoices().first()
        val invoiceItems = database.invoiceDao().getAllInvoiceItems()
        val invoiceItemBatches = database.invoiceItemBatchDao().getAllItemBatchList()
        val customerLedger = database.customerLedgerDao().getAllEntries().first()
        val supplierLedger = database.supplierLedgerDao().getAllEntries().first()
        val paymentAllocations = database.paymentAllocationDao().getAllocations().first()
        val purchases = database.purchaseDao().getAllPurchases().first()
        val purchaseItems = database.purchaseDao().getAllPurchaseItems()
        val returnsList = database.returnDao().getAllReturnList()
        val returnItems = database.returnDao().getAllReturnItems()
        val returnItemBatches = database.returnItemBatchDao().getAllReturnItemBatchList()
        val expenses = database.expenseDao().getAllExpenses().first()
        val invoiceCounter = database.invoiceDao().getLastInvoiceNumber() ?: 0L
        val auditLogs = database.auditLogDao().getAllAuditLogList()
        val customUnits = database.customUnitDao().getAllCustomUnitsSync()

        val root = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("app", "Dokandaar")
            put("timestamp", System.currentTimeMillis())
            put("invoiceCounter", invoiceCounter)

            // 1. Users (CRITICAL PIN SECURITY: exclude pinHash and pinSalt)
            val userArr = JSONArray()
            for (u in users) {
                userArr.put(JSONObject().apply {
                    put("id", u.id)
                    put("username", u.username)
                    put("displayName", u.displayName)
                    // Note: pinHash and pinSalt are deliberately EXCLUDED for security
                    put("role", u.role)
                    put("isActive", u.isActive)
                    put("failedAttempts", u.failedAttempts)
                    if (u.lockedUntil != null) put("lockedUntil", u.lockedUntil)
                    put("mustSetPin", u.mustSetPin)
                    put("createdAt", u.createdAt)
                })
            }
            put("users", userArr)

            // 2. Shop Profile
            if (shop != null) {
                put("shopProfile", JSONObject().apply {
                    put("id", shop.id)
                    put("name", shop.name)
                    put("addressLine1", shop.addressLine1)
                    put("addressLine2", shop.addressLine2)
                    put("phoneNumber", shop.phoneNumber)
                    put("upiId", shop.upiId)
                    put("footerText", shop.footerText)
                })
            }

            // 3. Categories
            val catArr = JSONArray()
            for (cat in categories) {
                catArr.put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("iconName", cat.iconName ?: "")
                    put("imageUri", cat.imageUri ?: "")
                    put("displayOrder", cat.displayOrder)
                })
            }
            put("categories", catArr)

            // 4. Customers
            val custArr = JSONArray()
            for (c in customers) {
                custArr.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("phone", c.phone)
                    put("email", c.email ?: "")
                    put("address", c.address ?: "")
                    put("gstin", c.gstin ?: "")
                    put("openingBalancePaise", c.openingBalancePaise)
                    put("createdAt", c.createdAt)
                    put("isActive", c.isActive)
                    put("creditLimitPaise", c.creditLimitPaise)
                })
            }
            put("customers", custArr)

            // 5. Suppliers
            val supArr = JSONArray()
            for (s in suppliers) {
                supArr.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("contactPerson", s.contactPerson ?: "")
                    put("phone", s.phone)
                    put("email", s.email ?: "")
                    put("address", s.address ?: "")
                    put("gstin", s.gstin ?: "")
                    put("openingBalancePaise", s.openingBalancePaise)
                    put("createdAt", s.createdAt)
                    put("isActive", s.isActive)
                })
            }
            put("suppliers", supArr)

            // 6. Shifts
            val shiftArr = JSONArray()
            for (s in shifts) {
                shiftArr.put(JSONObject().apply {
                    put("id", s.id)
                    put("userId", s.userId)
                    put("openedAt", s.openedAt)
                    if (s.closedAt != null) put("closedAt", s.closedAt)
                    put("openingFloatPaise", s.openingFloatPaise)
                    if (s.actualCashPaise != null) put("actualCashPaise", s.actualCashPaise)
                    if (s.expectedCashPaise != null) put("expectedCashPaise", s.expectedCashPaise)
                    if (s.variancePaise != null) put("variancePaise", s.variancePaise)
                    if (s.note != null) put("note", s.note)
                    put("status", s.status)
                })
            }
            put("shifts", shiftArr)

            // 7. Cash Movements
            val cmArr = JSONArray()
            for (cm in cashMovements) {
                cmArr.put(JSONObject().apply {
                    put("id", cm.id)
                    put("shiftId", cm.shiftId)
                    put("movementType", cm.movementType)
                    put("amountPaise", cm.amountPaise)
                    if (cm.referenceId != null) put("referenceId", cm.referenceId)
                    if (cm.reason != null) put("reason", cm.reason)
                    put("createdAt", cm.createdAt)
                })
            }
            put("cashMovements", cmArr)

            // 8. Products
            val prodArr = JSONArray()
            for (p in products) {
                prodArr.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("barcode", p.barcode)
                    put("pricePaise", p.pricePaise)
                    put("stockMilli", p.stockMilli)
                    put("stock", p.stock)
                    put("imageUri", p.imageUri ?: "")
                    put("createdAt", p.createdAt)
                    put("sku", p.sku)
                    put("category", p.category)
                    put("unit", p.unit)
                    put("purchasePricePaise", p.purchasePricePaise)
                    put("mrpPaise", p.mrpPaise)
                    put("minStockAlertMilli", p.minStockAlertMilli)
                    put("gstRateBp", p.gstRateBp)
                    put("description", p.description)
                    put("trackBatch", p.trackBatch)
                    put("hsnCode", p.hsnCode ?: "")
                    put("isActive", p.isActive)
                })
            }
            put("products", prodArr)

            // 9. Product Batches
            val pbArr = JSONArray()
            for (b in productBatches) {
                pbArr.put(JSONObject().apply {
                    put("id", b.id)
                    put("productId", b.productId)
                    put("batchNumber", b.batchNumber)
                    if (b.expiryDate != null) put("expiryDate", b.expiryDate)
                    put("purchasePricePaise", b.purchasePricePaise)
                    put("mrpPaise", b.mrpPaise)
                    put("initialStockMilli", b.initialStockMilli)
                    put("currentStockMilli", b.currentStockMilli)
                    put("createdAt", b.createdAt)
                })
            }
            put("productBatches", pbArr)

            // 10. Stock Adjustments
            val saArr = JSONArray()
            for (sa in stockAdjustments) {
                saArr.put(JSONObject().apply {
                    put("id", sa.id)
                    put("productId", sa.productId)
                    if (sa.batchId != null) put("batchId", sa.batchId)
                    put("adjustedQuantityMilli", sa.adjustedQuantityMilli)
                    put("reason", sa.reason)
                    put("adjustedByUserId", sa.adjustedByUserId)
                    put("adjustedAt", sa.adjustedAt)
                })
            }
            put("stockAdjustments", saArr)

            // 11. Invoices
            val invArr = JSONArray()
            for (inv in invoices) {
                invArr.put(JSONObject().apply {
                    put("id", inv.id)
                    put("invoiceNumber", inv.invoiceNumber)
                    put("timestamp", inv.timestamp)
                    put("subtotalPaise", inv.subtotalPaise)
                    put("discountPaise", inv.discountPaise)
                    put("taxPaise", inv.taxPaise)
                    put("grandTotalPaise", inv.grandTotalPaise)
                    put("duePaise", inv.duePaise)
                    put("status", inv.status.name)
                    put("paymentMethod", inv.paymentMethod)
                    put("customerName", inv.customerName ?: "")
                    put("customerPhone", inv.customerPhone ?: "")
                    put("cashReceivedPaise", inv.cashReceivedPaise)
                    put("changeReturnedPaise", inv.changeReturnedPaise)
                    put("invoiceType", inv.invoiceType)
                    put("itemsCount", inv.itemsCount)
                    if (inv.shiftId != null) put("shiftId", inv.shiftId)
                    if (inv.createdByUserId != null) put("createdByUserId", inv.createdByUserId)
                    if (inv.customerId != null) put("customerId", inv.customerId)
                    put("upiPaidPaise", inv.upiPaidPaise)
                    put("originalDuePaise", inv.originalDuePaise)
                    put("roundOffPaise", inv.roundOffPaise)
                    if (inv.voidedAt != null) put("voidedAt", inv.voidedAt)
                    if (inv.voidedByUserId != null) put("voidedByUserId", inv.voidedByUserId)
                    if (inv.voidReason != null) put("voidReason", inv.voidReason)
                })
            }
            put("invoices", invArr)

            // 12. Invoice Items
            val itemArr = JSONArray()
            for (item in invoiceItems) {
                itemArr.put(JSONObject().apply {
                    put("id", item.id)
                    put("invoiceId", item.invoiceId)
                    put("productId", item.productId ?: "")
                    put("productName", item.productName)
                    put("barcode", item.barcode)
                    put("unitPricePaise", item.unitPricePaise)
                    put("quantityMilli", item.quantityMilli)
                    put("quantity", item.quantity)
                    put("lineTotalPaise", item.lineTotalPaise)
                    put("discountPaise", item.discountPaise)
                    put("billDiscountSharePaise", item.billDiscountSharePaise)
                    put("gstRateBp", item.gstRateBp)
                    put("hsnCode", item.hsnCode ?: "")
                    put("taxableValuePaise", item.taxableValuePaise)
                    put("taxPaise", item.taxPaise)
                    put("unitCostPaise", item.unitCostPaise)
                    put("unit", item.unit)
                    put("isCustomItem", item.isCustomItem)
                    put("isCostUnknown", item.isCostUnknown)
                })
            }
            put("invoiceItems", itemArr)

            // 13. Invoice Item Batches
            val iibArr = JSONArray()
            for (iib in invoiceItemBatches) {
                iibArr.put(JSONObject().apply {
                    put("id", iib.id)
                    put("invoiceItemId", iib.invoiceItemId)
                    put("batchId", iib.batchId)
                    put("quantityMilli", iib.quantityMilli)
                    put("costPaise", iib.costPaise)
                })
            }
            put("invoiceItemBatches", iibArr)

            // 14. Customer Ledger
            val clArr = JSONArray()
            for (cl in customerLedger) {
                clArr.put(JSONObject().apply {
                    put("id", cl.id)
                    put("customerId", cl.customerId)
                    put("transactionType", cl.transactionType)
                    if (cl.invoiceId != null) put("invoiceId", cl.invoiceId)
                    put("debitPaise", cl.debitPaise)
                    put("creditPaise", cl.creditPaise)
                    put("balanceAfterPaise", cl.balanceAfterPaise)
                    if (cl.paymentMode != null) put("paymentMode", cl.paymentMode)
                    if (cl.note != null) put("note", cl.note)
                    put("createdAt", cl.createdAt)
                })
            }
            put("customerLedger", clArr)

            // 15. Supplier Ledger
            val slArr = JSONArray()
            for (sl in supplierLedger) {
                slArr.put(JSONObject().apply {
                    put("id", sl.id)
                    put("supplierId", sl.supplierId)
                    put("transactionType", sl.transactionType)
                    if (sl.purchaseId != null) put("purchaseId", sl.purchaseId)
                    put("debitPaise", sl.debitPaise)
                    put("creditPaise", sl.creditPaise)
                    put("balanceAfterPaise", sl.balanceAfterPaise)
                    if (sl.paymentMode != null) put("paymentMode", sl.paymentMode)
                    if (sl.note != null) put("note", sl.note)
                    put("createdAt", sl.createdAt)
                })
            }
            put("supplierLedger", slArr)

            // 16. Payment Allocations
            val paArr = JSONArray()
            for (pa in paymentAllocations) {
                paArr.put(JSONObject().apply {
                    put("id", pa.id)
                    put("ledgerEntryId", pa.ledgerEntryId)
                    if (pa.invoiceId != null) put("invoiceId", pa.invoiceId)
                    put("allocatedAmountPaise", pa.allocatedAmountPaise)
                    if (pa.reversesAllocationId != null) put("reversesAllocationId", pa.reversesAllocationId)
                    put("createdAt", pa.createdAt)
                })
            }
            put("paymentAllocations", paArr)

            // 17. Purchases
            val purArr = JSONArray()
            for (p in purchases) {
                purArr.put(JSONObject().apply {
                    put("id", p.id)
                    put("purchaseNumber", p.purchaseNumber)
                    put("supplierId", p.supplierId ?: "")
                    put("supplierName", p.supplierName ?: "")
                    put("timestamp", p.timestamp)
                    put("totalPaise", p.totalPaise)
                    put("paymentStatus", p.paymentStatus)
                    if (p.shiftId != null) put("shiftId", p.shiftId)
                    if (p.createdByUserId != null) put("createdByUserId", p.createdByUserId)
                    put("paidPaise", p.paidPaise)
                    put("duePaise", p.duePaise)
                    put("status", p.status)
                })
            }
            put("purchases", purArr)

            // 18. Purchase Items
            val piArr = JSONArray()
            for (it in purchaseItems) {
                piArr.put(JSONObject().apply {
                    put("id", it.id)
                    put("purchaseId", it.purchaseId)
                    put("productId", it.productId)
                    put("productName", it.productName)
                    put("quantityMilli", it.quantityMilli)
                    put("quantity", it.quantity)
                    put("unitPricePaise", it.unitPricePaise)
                    put("lineTotalPaise", it.lineTotalPaise)
                    put("gstRateBp", it.gstRateBp)
                    if (it.batchNumber != null) put("batchNumber", it.batchNumber)
                    if (it.expiryDate != null) put("expiryDate", it.expiryDate)
                    put("mrpPaise", it.mrpPaise)
                    if (it.batchId != null) put("batchId", it.batchId)
                    put("unit", it.unit)
                })
            }
            put("purchaseItems", piArr)

            // 19. Returns
            val retArr = JSONArray()
            for (r in returnsList) {
                retArr.put(JSONObject().apply {
                    put("id", r.id)
                    put("returnNumber", r.returnNumber)
                    put("invoiceId", r.invoiceId)
                    put("shiftId", r.shiftId)
                    put("totalRefundPaise", r.totalRefundPaise)
                    put("cashRefundPaise", r.cashRefundPaise)
                    put("upiRefundPaise", r.upiRefundPaise)
                    put("khataCreditPaise", r.khataCreditPaise)
                    put("reason", r.reason)
                    put("processedByUserId", r.processedByUserId)
                    put("createdAt", r.createdAt)
                })
            }
            put("returns", retArr)

            // 20. Return Items
            val riArr = JSONArray()
            for (ri in returnItems) {
                riArr.put(JSONObject().apply {
                    put("id", ri.id)
                    put("returnId", ri.returnId)
                    put("invoiceItemId", ri.invoiceItemId)
                    put("quantityMilli", ri.quantityMilli)
                    put("refundAmountPaise", ri.refundAmountPaise)
                    put("taxableRefundPaise", ri.taxableRefundPaise)
                    put("taxRefundPaise", ri.taxRefundPaise)
                    put("restock", ri.restock)
                    put("unit", ri.unit)
                })
            }
            put("returnItems", riArr)

            // 21. Return Item Batches
            val ribArr = JSONArray()
            for (rib in returnItemBatches) {
                ribArr.put(JSONObject().apply {
                    put("id", rib.id)
                    put("returnItemId", rib.returnItemId)
                    put("batchId", rib.batchId)
                    put("quantityMilli", rib.quantityMilli)
                })
            }
            put("returnItemBatches", ribArr)

            // 22. Expenses
            val expArr = JSONArray()
            for (e in expenses) {
                expArr.put(JSONObject().apply {
                    put("id", e.id)
                    put("title", e.title)
                    put("category", e.category)
                    put("amountPaise", e.amountPaise)
                    put("date", e.date)
                    put("paymentMethod", e.paymentMethod)
                    put("description", e.description ?: "")
                    put("createdAt", e.createdAt)
                    if (e.shiftId != null) put("shiftId", e.shiftId)
                    if (e.createdByUserId != null) put("createdByUserId", e.createdByUserId)
                })
            }
            put("expenses", expArr)

            // 24. Audit Logs
            val alArr = JSONArray()
            for (al in auditLogs) {
                alArr.put(JSONObject().apply {
                    put("id", al.id)
                    put("userId", al.userId)
                    put("action", al.action)
                    put("entityType", al.entityType)
                    put("entityId", al.entityId)
                    put("details", al.details)
                    put("timestamp", al.timestamp)
                })
            }
            put("auditLogs", alArr)

            // 25. Custom Units
            val cuArr = JSONArray()
            for (cu in customUnits) {
                cuArr.put(JSONObject().apply {
                    put("id", cu.id)
                    put("code", cu.code)
                    put("name", cu.name)
                    put("baseUnitCode", cu.baseUnitCode)
                    put("conversionFactor", cu.conversionFactor)
                    put("allowsFractional", cu.allowsFractional)
                    put("createdAt", cu.createdAt)
                })
            }
            put("customUnits", cuArr)
        }

        val jsonString = root.toString(2)
        val checksum = calculateSha256(jsonString)

        val envelope = JSONObject().apply {
            put("checksum", checksum)
            put("data", jsonString)
        }
        envelope.toString()
    }

    suspend fun saveBackupToInternalStorage(context: Context, database: AppDatabase): Result<File> = saveBackupToFile(context, database)

    suspend fun exportBackupToUri(context: Context, database: AppDatabase, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = createBackupJson(database)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
            } ?: return@withContext Result.failure(Exception("Cannot open destination stream"))
            Result.success("Exported backup successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBackupToFile(context: Context, database: AppDatabase): Result<File> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(backupDir, "dokandaar_backup_$timestamp.json")
            val content = createBackupJson(database)
            file.writeText(content, Charsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportBackupToStream(database: AppDatabase, outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val content = createBackupJson(database)
            outputStream.bufferedWriter(Charsets.UTF_8).use {
                it.write(content)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupFromJson(database: AppDatabase, envelopeJson: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val envelope = JSONObject(envelopeJson)
            val dataJson: String
            if (envelope.has("checksum") && envelope.has("data")) {
                val expectedChecksum = envelope.getString("checksum")
                dataJson = envelope.getString("data")
                val actualChecksum = calculateSha256(dataJson)
                if (expectedChecksum != actualChecksum) {
                    return@withContext Result.failure(SecurityException("Backup file corrupted or checksum mismatch"))
                }
            } else {
                dataJson = envelopeJson
            }

            val root = JSONObject(dataJson)
            val version = root.optInt("version", 1)

            var productCount = 0
            var invoiceCount = 0
            var itemCount = 0
            var maxInvoiceSuffix = 0L
            val restoredInvoiceNumbers = mutableSetOf<String>()
            val invoiceNumberToId = mutableMapOf<String, Long>()

            database.withTransaction {
                // 1. Restore Users (CRITICAL PIN SECURITY: set pinHash = "" and mustSetPin = true)
                if (root.has("users")) {
                    val uArray = root.getJSONArray("users")
                    for (i in 0 until uArray.length()) {
                        val obj = uArray.getJSONObject(i)
                        val user = UserEntity(
                            id = obj.getString("id"),
                            username = obj.getString("username"),
                            displayName = obj.getString("displayName"),
                            pinHash = "",
                            pinSalt = "",
                            role = obj.optString("role", "CASHIER"),
                            isActive = obj.optBoolean("isActive", true),
                            failedAttempts = obj.optInt("failedAttempts", 0),
                            lockedUntil = if (obj.isNull("lockedUntil") || !obj.has("lockedUntil")) null else obj.getLong("lockedUntil"),
                            mustSetPin = true,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.userDao().upsertUser(user)
                    }
                }

                // 2. Restore Shop Profile
                if (root.has("shopProfile")) {
                    val sObj = root.getJSONObject("shopProfile")
                    val shop = ShopEntity(
                        id = sObj.optInt("id", 1),
                        name = sObj.optString("name", "Store"),
                        addressLine1 = sObj.optString("addressLine1", ""),
                        addressLine2 = sObj.optString("addressLine2", ""),
                        phoneNumber = sObj.optString("phoneNumber", ""),
                        upiId = sObj.optString("upiId", ""),
                        footerText = sObj.optString("footerText", "")
                    )
                    database.shopDao().insertOrUpdate(shop)
                }

                // 3. Restore Categories
                if (root.has("categories")) {
                    val catArray = root.getJSONArray("categories")
                    val catList = mutableListOf<CategoryEntity>()
                    for (i in 0 until catArray.length()) {
                        val obj = catArray.getJSONObject(i)
                        catList.add(
                            CategoryEntity(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                iconName = obj.optString("iconName").ifBlank { null },
                                imageUri = obj.optString("imageUri").ifBlank { null },
                                displayOrder = obj.optInt("displayOrder", 0)
                            )
                        )
                    }
                    database.categoryDao().insertAll(catList)
                }

                // 4. Restore Customers
                if (root.has("customers")) {
                    val cArray = root.getJSONArray("customers")
                    val custList = mutableListOf<CustomerEntity>()
                    for (i in 0 until cArray.length()) {
                        val obj = cArray.getJSONObject(i)
                        custList.add(
                            CustomerEntity(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                phone = obj.getString("phone"),
                                email = obj.optString("email").ifBlank { null },
                                address = obj.optString("address").ifBlank { null },
                                gstin = obj.optString("gstin").ifBlank { null },
                                openingBalancePaise = obj.optLong("openingBalancePaise", 0L),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                isActive = obj.optBoolean("isActive", true),
                                creditLimitPaise = obj.optLong("creditLimitPaise", 0L)
                            )
                        )
                    }
                    database.customerDao().insertAll(custList)
                }

                // 5. Restore Suppliers
                if (root.has("suppliers")) {
                    val sArray = root.getJSONArray("suppliers")
                    for (i in 0 until sArray.length()) {
                        val obj = sArray.getJSONObject(i)
                        val sup = SupplierEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            contactPerson = obj.optString("contactPerson").ifBlank { null },
                            phone = obj.getString("phone"),
                            email = obj.optString("email").ifBlank { null },
                            address = obj.optString("address").ifBlank { null },
                            gstin = obj.optString("gstin").ifBlank { null },
                            openingBalancePaise = obj.optLong("openingBalancePaise", 0L),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            isActive = obj.optBoolean("isActive", true)
                        )
                        database.supplierDao().insertSupplier(sup)
                    }
                }

                // 6. Restore Shifts
                if (root.has("shifts")) {
                    val sArray = root.getJSONArray("shifts")
                    for (i in 0 until sArray.length()) {
                        val obj = sArray.getJSONObject(i)
                        val shift = ShiftEntity(
                            id = obj.optLong("id", 0L),
                            userId = obj.getString("userId"),
                            openedAt = obj.optLong("openedAt", System.currentTimeMillis()),
                            closedAt = if (obj.isNull("closedAt") || !obj.has("closedAt")) null else obj.getLong("closedAt"),
                            openingFloatPaise = obj.optLong("openingFloatPaise", 0L),
                            actualCashPaise = if (obj.isNull("actualCashPaise") || !obj.has("actualCashPaise")) null else obj.getLong("actualCashPaise"),
                            expectedCashPaise = if (obj.isNull("expectedCashPaise") || !obj.has("expectedCashPaise")) null else obj.getLong("expectedCashPaise"),
                            variancePaise = if (obj.isNull("variancePaise") || !obj.has("variancePaise")) null else obj.getLong("variancePaise"),
                            note = obj.optString("note").ifBlank { null },
                            status = obj.optString("status", "OPEN")
                        )
                        database.shiftDao().upsertShift(shift)
                    }
                }

                // 7. Restore Cash Movements
                if (root.has("cashMovements")) {
                    val cmArray = root.getJSONArray("cashMovements")
                    for (i in 0 until cmArray.length()) {
                        val obj = cmArray.getJSONObject(i)
                        val cm = CashMovementEntity(
                            id = obj.optLong("id", 0L),
                            shiftId = obj.getLong("shiftId"),
                            movementType = obj.getString("movementType"),
                            amountPaise = obj.getLong("amountPaise"),
                            referenceId = obj.optString("referenceId").ifBlank { null },
                            reason = obj.optString("reason").ifBlank { null },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.cashMovementDao().upsertCashMovement(cm)
                    }
                }

                // 8. Restore Products
                if (root.has("products")) {
                    val pArray = root.getJSONArray("products")
                    productCount = pArray.length()
                    for (i in 0 until pArray.length()) {
                        val obj = pArray.getJSONObject(i)
                        val stockMilli = if (obj.has("stockMilli")) {
                            obj.getLong("stockMilli")
                        } else {
                            obj.optInt("stock", 0).toLong() * 1000L
                        }
                        val minStockAlertMilli = if (obj.has("minStockAlertMilli")) {
                            obj.getLong("minStockAlertMilli")
                        } else {
                            obj.optInt("minStockAlert", 5).toLong() * 1000L
                        }
                        val gstRateBp = if (obj.has("gstRateBp")) {
                            obj.getInt("gstRateBp")
                        } else {
                            obj.optInt("gstRate", 0) * 100
                        }
                        val rawUnit = obj.optString("unit", "PCS")
                        val canonicalUnit = UnitRegistry.resolve(rawUnit).code
                        val existing = database.productDao().getProductById(obj.getString("id"))
                        val finalUnit = if (existing != null) {
                            val existingUnit = UnitRegistry.resolve(existing.unit).code
                            if (existingUnit != canonicalUnit && (existing.stockMilli != 0L || database.productDao().getProductTransactionCount(existing.id) > 0)) {
                                existingUnit // Preserve established unit
                            } else {
                                canonicalUnit
                            }
                        } else {
                            canonicalUnit
                        }
                        val entity = ProductEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            barcode = obj.getString("barcode"),
                            pricePaise = obj.getLong("pricePaise"),
                            stockMilli = stockMilli,
                            imageUri = obj.optString("imageUri").ifBlank { null },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            sku = obj.optString("sku", ""),
                            category = obj.optString("category", "Grocery"),
                            unit = finalUnit,
                            purchasePricePaise = obj.optLong("purchasePricePaise", 0L),
                            mrpPaise = obj.optLong("mrpPaise", 0L),
                            minStockAlertMilli = minStockAlertMilli,
                            gstRateBp = gstRateBp,
                            description = obj.optString("description", ""),
                            trackBatch = obj.optBoolean("trackBatch", false),
                            hsnCode = obj.optString("hsnCode").ifBlank { null },
                            isActive = obj.optBoolean("isActive", true)
                        )
                        database.productDao().upsertProduct(entity)
                    }
                }

                // 9. Restore Product Batches
                if (root.has("productBatches")) {
                    val bArray = root.getJSONArray("productBatches")
                    for (i in 0 until bArray.length()) {
                        val obj = bArray.getJSONObject(i)
                        val batch = ProductBatchEntity(
                            id = obj.optLong("id", 0L),
                            productId = obj.getString("productId"),
                            batchNumber = obj.getString("batchNumber"),
                            expiryDate = if (obj.isNull("expiryDate") || !obj.has("expiryDate")) null else obj.getLong("expiryDate"),
                            purchasePricePaise = obj.optLong("purchasePricePaise", 0L),
                            mrpPaise = obj.optLong("mrpPaise", 0L),
                            initialStockMilli = obj.optLong("initialStockMilli", 0L),
                            currentStockMilli = obj.optLong("currentStockMilli", 0L),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.productBatchDao().upsertBatch(batch)
                    }
                }

                // 10. Restore Stock Adjustments
                if (root.has("stockAdjustments")) {
                    val saArray = root.getJSONArray("stockAdjustments")
                    for (i in 0 until saArray.length()) {
                        val obj = saArray.getJSONObject(i)
                        val sa = StockAdjustmentEntity(
                            id = obj.optLong("id", 0L),
                            productId = obj.getString("productId"),
                            batchId = if (obj.isNull("batchId") || !obj.has("batchId")) null else obj.getLong("batchId"),
                            adjustedQuantityMilli = obj.getLong("adjustedQuantityMilli"),
                            reason = obj.getString("reason"),
                            adjustedByUserId = obj.getString("adjustedByUserId"),
                            adjustedAt = obj.optLong("adjustedAt", System.currentTimeMillis())
                        )
                        database.stockAdjustmentDao().upsertStockAdjustment(sa)
                    }
                }

                // 11. Restore Invoices
                if (root.has("invoices")) {
                    val iArray = root.getJSONArray("invoices")
                    invoiceCount = iArray.length()
                    for (i in 0 until iArray.length()) {
                        val obj = iArray.getJSONObject(i)
                        val invNumber = obj.getString("invoiceNumber")
                        restoredInvoiceNumbers.add(invNumber)
                        val suffix = invNumber.substringAfterLast("-").toLongOrNull() ?: 0L
                        if (suffix > maxInvoiceSuffix) maxInvoiceSuffix = suffix

                        val statusStr = obj.optString("status", "COMPLETED")
                        val status = try {
                            InvoiceStatus.valueOf(statusStr)
                        } catch (_: Exception) {
                            InvoiceStatus.COMPLETED
                        }
                        val inv = InvoiceEntity(
                            id = obj.optLong("id", 0L),
                            invoiceNumber = invNumber,
                            timestamp = obj.getLong("timestamp"),
                            subtotalPaise = obj.getLong("subtotalPaise"),
                            taxPaise = obj.optLong("taxPaise", 0L),
                            discountPaise = obj.optLong("discountPaise", 0L),
                            grandTotalPaise = obj.getLong("grandTotalPaise"),
                            duePaise = obj.optLong("duePaise", 0L),
                            status = status,
                            paymentMethod = obj.optString("paymentMethod", "UPI"),
                            customerName = obj.optString("customerName").ifBlank { null },
                            customerPhone = obj.optString("customerPhone").ifBlank { null },
                            cashReceivedPaise = obj.optLong("cashReceivedPaise", 0L),
                            changeReturnedPaise = obj.optLong("changeReturnedPaise", 0L),
                            invoiceType = obj.optString("invoiceType", "SALE"),
                            itemsCount = obj.optInt("itemsCount", 1),
                            shiftId = if (obj.isNull("shiftId") || !obj.has("shiftId")) null else obj.getLong("shiftId"),
                            createdByUserId = obj.optString("createdByUserId").ifBlank { null },
                            customerId = obj.optString("customerId").ifBlank { null },
                            upiPaidPaise = obj.optLong("upiPaidPaise", 0L),
                            originalDuePaise = obj.optLong("originalDuePaise", 0L),
                            roundOffPaise = obj.optLong("roundOffPaise", 0L),
                            voidedAt = if (obj.isNull("voidedAt") || !obj.has("voidedAt")) null else obj.getLong("voidedAt"),
                            voidedByUserId = obj.optString("voidedByUserId").ifBlank { null },
                            voidReason = obj.optString("voidReason").ifBlank { null }
                        )
                        database.invoiceDao().upsertInvoice(inv)
                        if (inv.id != 0L) {
                            invoiceNumberToId[inv.invoiceNumber] = inv.id
                        }
                    }
                }

                // 12. Restore Invoice Items
                if (root.has("invoiceItems")) {
                    val itemArray = root.getJSONArray("invoiceItems")
                    itemCount = itemArray.length()
                    val itemsList = mutableListOf<InvoiceItemEntity>()
                    for (i in 0 until itemArray.length()) {
                        val obj = itemArray.getJSONObject(i)
                        val rawInvoiceId = obj.optLong("invoiceId", 0L)
                        val targetInvoiceId = if (rawInvoiceId != 0L) {
                            rawInvoiceId
                        } else {
                            val invNum = obj.optString("invoiceNumber", "")
                            invoiceNumberToId[invNum] ?: 0L
                        }
                        val qtyMilli = if (obj.has("quantityMilli")) {
                            obj.getLong("quantityMilli")
                        } else {
                            obj.optInt("quantity", 1).toLong() * 1000L
                        }
                        itemsList.add(
                            InvoiceItemEntity(
                                id = obj.optLong("id", 0L),
                                invoiceId = targetInvoiceId,
                                productId = obj.optString("productId").ifBlank { null },
                                productName = obj.getString("productName"),
                                barcode = obj.optString("barcode", ""),
                                unitPricePaise = obj.getLong("unitPricePaise"),
                                quantityMilli = qtyMilli,
                                lineTotalPaise = obj.getLong("lineTotalPaise"),
                                discountPaise = obj.optLong("discountPaise", 0L),
                                billDiscountSharePaise = obj.optLong("billDiscountSharePaise", 0L),
                                gstRateBp = obj.optInt("gstRateBp", 0),
                                hsnCode = obj.optString("hsnCode").ifBlank { null },
                                taxableValuePaise = obj.optLong("taxableValuePaise", 0L),
                                taxPaise = obj.optLong("taxPaise", 0L),
                                unitCostPaise = obj.optLong("unitCostPaise", 0L),
                                unit = UnitRegistry.resolve(obj.optString("unit", "PCS")).code,
                                isCustomItem = obj.optBoolean("isCustomItem", false),
                                isCostUnknown = obj.optBoolean("isCostUnknown", false)
                            )
                        )
                    }
                    if (itemsList.isNotEmpty()) {
                        database.invoiceDao().upsertInvoiceItems(itemsList)
                    }
                }

                // 13. Restore Invoice Item Batches
                if (root.has("invoiceItemBatches")) {
                    val iibArray = root.getJSONArray("invoiceItemBatches")
                    val iibList = mutableListOf<InvoiceItemBatchEntity>()
                    for (i in 0 until iibArray.length()) {
                        val obj = iibArray.getJSONObject(i)
                        iibList.add(
                            InvoiceItemBatchEntity(
                                id = obj.optLong("id", 0L),
                                invoiceItemId = obj.getLong("invoiceItemId"),
                                batchId = obj.getLong("batchId"),
                                quantityMilli = obj.getLong("quantityMilli"),
                                costPaise = obj.getLong("costPaise")
                            )
                        )
                    }
                    if (iibList.isNotEmpty()) {
                        database.invoiceItemBatchDao().upsertAll(iibList)
                    }
                }

                // 14. Restore Customer Ledger
                if (root.has("customerLedger")) {
                    val clArray = root.getJSONArray("customerLedger")
                    for (i in 0 until clArray.length()) {
                        val obj = clArray.getJSONObject(i)
                        val entry = CustomerLedgerEntity(
                            id = obj.optLong("id", 0L),
                            customerId = obj.getString("customerId"),
                            transactionType = obj.getString("transactionType"),
                            invoiceId = if (obj.isNull("invoiceId") || !obj.has("invoiceId")) null else obj.getLong("invoiceId"),
                            debitPaise = obj.optLong("debitPaise", 0L),
                            creditPaise = obj.optLong("creditPaise", 0L),
                            balanceAfterPaise = obj.getLong("balanceAfterPaise"),
                            paymentMode = obj.optString("paymentMode").ifBlank { null },
                            note = obj.optString("note").ifBlank { null },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.customerLedgerDao().upsertEntry(entry)
                    }
                }

                // 15. Restore Supplier Ledger
                if (root.has("supplierLedger")) {
                    val slArray = root.getJSONArray("supplierLedger")
                    for (i in 0 until slArray.length()) {
                        val obj = slArray.getJSONObject(i)
                        val entry = SupplierLedgerEntity(
                            id = obj.optLong("id", 0L),
                            supplierId = obj.getString("supplierId"),
                            transactionType = obj.getString("transactionType"),
                            purchaseId = if (obj.isNull("purchaseId") || !obj.has("purchaseId")) null else obj.getLong("purchaseId"),
                            debitPaise = obj.optLong("debitPaise", 0L),
                            creditPaise = obj.optLong("creditPaise", 0L),
                            balanceAfterPaise = obj.getLong("balanceAfterPaise"),
                            paymentMode = obj.optString("paymentMode").ifBlank { null },
                            note = obj.optString("note").ifBlank { null },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.supplierLedgerDao().upsertEntry(entry)
                    }
                }

                // 16. Restore Payment Allocations
                if (root.has("paymentAllocations")) {
                    val paArray = root.getJSONArray("paymentAllocations")
                    for (i in 0 until paArray.length()) {
                        val obj = paArray.getJSONObject(i)
                        val pa = PaymentAllocationEntity(
                            id = obj.optLong("id", 0L),
                            ledgerEntryId = obj.getLong("ledgerEntryId"),
                            invoiceId = if (obj.isNull("invoiceId") || !obj.has("invoiceId")) null else obj.getLong("invoiceId"),
                            allocatedAmountPaise = obj.getLong("allocatedAmountPaise"),
                            reversesAllocationId = if (obj.isNull("reversesAllocationId") || !obj.has("reversesAllocationId")) null else obj.getLong("reversesAllocationId"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.paymentAllocationDao().upsertAllocation(pa)
                    }
                }

                // 17. Restore Purchases & Purchase Items
                if (root.has("purchases")) {
                    val purArray = root.getJSONArray("purchases")
                    for (i in 0 until purArray.length()) {
                        val pObj = purArray.getJSONObject(i)
                        val purchase = PurchaseEntity(
                            id = pObj.optLong("id", 0L),
                            purchaseNumber = pObj.getString("purchaseNumber"),
                            supplierId = pObj.optString("supplierId").ifBlank { null },
                            supplierName = pObj.optString("supplierName").ifBlank { null },
                            timestamp = pObj.optLong("timestamp", System.currentTimeMillis()),
                            totalPaise = pObj.getLong("totalPaise"),
                            paymentStatus = pObj.optString("paymentStatus", "PAID"),
                            shiftId = if (pObj.isNull("shiftId") || !pObj.has("shiftId")) null else pObj.getLong("shiftId"),
                            createdByUserId = pObj.optString("createdByUserId").ifBlank { null },
                            paidPaise = pObj.optLong("paidPaise", 0L),
                            duePaise = pObj.optLong("duePaise", 0L),
                            status = pObj.optString("status", "RECEIVED")
                        )
                        val insertedId = database.purchaseDao().insertPurchase(purchase)
                        val targetPurchaseId = if (purchase.id != 0L) purchase.id else insertedId

                        // Handle legacy nested items in purchases
                        if (pObj.has("items")) {
                            val itemsArr = pObj.getJSONArray("items")
                            val itemsList = mutableListOf<PurchaseItemEntity>()
                            for (j in 0 until itemsArr.length()) {
                                val itObj = itemsArr.getJSONObject(j)
                                val qtyMilli = if (itObj.has("quantityMilli")) {
                                    itObj.getLong("quantityMilli")
                                } else {
                                    itObj.getInt("quantity").toLong() * 1000L
                                }
                                itemsList.add(
                                    PurchaseItemEntity(
                                        id = itObj.optLong("id", 0L),
                                        purchaseId = targetPurchaseId,
                                        productId = itObj.getString("productId"),
                                        productName = itObj.getString("productName"),
                                        quantityMilli = qtyMilli,
                                        unitPricePaise = itObj.getLong("unitPricePaise"),
                                        lineTotalPaise = itObj.getLong("lineTotalPaise"),
                                        gstRateBp = itObj.optInt("gstRateBp", 0),
                                        batchNumber = itObj.optString("batchNumber").ifBlank { null },
                                        expiryDate = if (itObj.isNull("expiryDate") || !itObj.has("expiryDate")) null else itObj.getLong("expiryDate"),
                                        mrpPaise = itObj.optLong("mrpPaise", 0L),
                                        batchId = if (itObj.isNull("batchId") || !itObj.has("batchId")) null else itObj.getLong("batchId"),
                                        unit = UnitRegistry.resolve(itObj.optString("unit", "PCS")).code
                                    )
                                )
                            }
                            if (itemsList.isNotEmpty()) {
                                database.purchaseDao().insertPurchaseItems(itemsList)
                            }
                        }
                    }
                }

                // 18. Restore top-level Purchase Items if present
                if (root.has("purchaseItems")) {
                    val piArray = root.getJSONArray("purchaseItems")
                    val itemsList = mutableListOf<PurchaseItemEntity>()
                    for (i in 0 until piArray.length()) {
                        val itObj = piArray.getJSONObject(i)
                        val qtyMilli = if (itObj.has("quantityMilli")) {
                            itObj.getLong("quantityMilli")
                        } else {
                            itObj.optInt("quantity", 1).toLong() * 1000L
                        }
                        itemsList.add(
                            PurchaseItemEntity(
                                id = itObj.optLong("id", 0L),
                                purchaseId = itObj.getLong("purchaseId"),
                                productId = itObj.getString("productId"),
                                productName = itObj.getString("productName"),
                                quantityMilli = qtyMilli,
                                unitPricePaise = itObj.getLong("unitPricePaise"),
                                lineTotalPaise = itObj.getLong("lineTotalPaise"),
                                gstRateBp = itObj.optInt("gstRateBp", 0),
                                batchNumber = itObj.optString("batchNumber").ifBlank { null },
                                expiryDate = if (itObj.isNull("expiryDate") || !itObj.has("expiryDate")) null else itObj.getLong("expiryDate"),
                                mrpPaise = itObj.optLong("mrpPaise", 0L),
                                batchId = if (itObj.isNull("batchId") || !itObj.has("batchId")) null else itObj.getLong("batchId"),
                                unit = UnitRegistry.resolve(itObj.optString("unit", "PCS")).code
                            )
                        )
                    }
                    if (itemsList.isNotEmpty()) {
                        database.purchaseDao().insertPurchaseItems(itemsList)
                    }
                }

                // 19. Restore Returns
                if (root.has("returns")) {
                    val rArray = root.getJSONArray("returns")
                    for (i in 0 until rArray.length()) {
                        val obj = rArray.getJSONObject(i)
                        val ret = ReturnEntity(
                            id = obj.optLong("id", 0L),
                            returnNumber = obj.getString("returnNumber"),
                            invoiceId = obj.getLong("invoiceId"),
                            shiftId = obj.getLong("shiftId"),
                            totalRefundPaise = obj.getLong("totalRefundPaise"),
                            cashRefundPaise = obj.optLong("cashRefundPaise", 0L),
                            upiRefundPaise = obj.optLong("upiRefundPaise", 0L),
                            khataCreditPaise = obj.optLong("khataCreditPaise", 0L),
                            reason = obj.getString("reason"),
                            processedByUserId = obj.getString("processedByUserId"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.returnDao().upsertReturn(ret)
                    }
                }

                // 20. Restore Return Items
                if (root.has("returnItems")) {
                    val riArray = root.getJSONArray("returnItems")
                    val riList = mutableListOf<ReturnItemEntity>()
                    for (i in 0 until riArray.length()) {
                        val obj = riArray.getJSONObject(i)
                        riList.add(
                            ReturnItemEntity(
                                id = obj.optLong("id", 0L),
                                returnId = obj.getLong("returnId"),
                                invoiceItemId = obj.getLong("invoiceItemId"),
                                quantityMilli = obj.getLong("quantityMilli"),
                                unit = UnitRegistry.resolve(obj.optString("unit", "PCS")).code,
                                refundAmountPaise = obj.getLong("refundAmountPaise"),
                                taxableRefundPaise = obj.optLong("taxableRefundPaise", 0L),
                                taxRefundPaise = obj.optLong("taxRefundPaise", 0L),
                                restock = obj.optBoolean("restock", true)
                            )
                        )
                    }
                    if (riList.isNotEmpty()) {
                        database.returnDao().upsertReturnItems(riList)
                    }
                }

                // 21. Restore Return Item Batches
                if (root.has("returnItemBatches")) {
                    val ribArray = root.getJSONArray("returnItemBatches")
                    val ribList = mutableListOf<ReturnItemBatchEntity>()
                    for (i in 0 until ribArray.length()) {
                        val obj = ribArray.getJSONObject(i)
                        ribList.add(
                            ReturnItemBatchEntity(
                                id = obj.optLong("id", 0L),
                                returnItemId = obj.getLong("returnItemId"),
                                batchId = obj.getLong("batchId"),
                                quantityMilli = obj.getLong("quantityMilli")
                            )
                        )
                    }
                    if (ribList.isNotEmpty()) {
                        database.returnItemBatchDao().upsertAll(ribList)
                    }
                }

                // 22. Restore Expenses
                if (root.has("expenses")) {
                    val eArray = root.getJSONArray("expenses")
                    for (i in 0 until eArray.length()) {
                        val obj = eArray.getJSONObject(i)
                        val exp = ExpenseEntity(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            category = obj.getString("category"),
                            amountPaise = obj.getLong("amountPaise"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            paymentMethod = obj.optString("paymentMethod", "CASH"),
                            description = obj.optString("description").ifBlank { null },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            shiftId = if (obj.isNull("shiftId") || !obj.has("shiftId")) null else obj.getLong("shiftId"),
                            createdByUserId = obj.optString("createdByUserId").ifBlank { null }
                        )
                        database.expenseDao().insertExpense(exp)
                    }
                }

                // 23. Restore & Sync Invoice Counter
                val backupCounter = root.optLong("invoiceCounter", 0L)
                val currentDbCounter = database.invoiceDao().getLastInvoiceNumber() ?: 0L
                val maxCounter = maxOf(backupCounter, maxInvoiceSuffix, currentDbCounter)
                if (maxCounter > 0L) {
                    database.invoiceDao().setInvoiceCounter(
                        InvoiceCounterEntity(id = 1, lastNumber = maxCounter)
                    )
                }

                // 24. Restore Audit Logs
                if (root.has("auditLogs")) {
                    val alArray = root.getJSONArray("auditLogs")
                    for (i in 0 until alArray.length()) {
                        val obj = alArray.getJSONObject(i)
                        val al = AuditLogEntity(
                            id = obj.optLong("id", 0L),
                            userId = obj.getString("userId"),
                            action = obj.getString("action"),
                            entityType = obj.getString("entityType"),
                            entityId = obj.getString("entityId"),
                            details = obj.getString("details"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        database.auditLogDao().upsertAuditLog(al)
                    }
                }

                // 25. Restore Custom Units
                if (root.has("customUnits")) {
                    val cuArray = root.getJSONArray("customUnits")
                    for (i in 0 until cuArray.length()) {
                        val obj = cuArray.getJSONObject(i)
                        val cu = CustomUnitEntity(
                            id = obj.optLong("id", 0L),
                            code = obj.getString("code"),
                            name = obj.getString("name"),
                            baseUnitCode = obj.getString("baseUnitCode"),
                            conversionFactor = obj.getDouble("conversionFactor"),
                            allowsFractional = obj.optBoolean("allowsFractional", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.customUnitDao().insert(cu)
                    }
                }
            }

            Result.success("Restored: $productCount products, $invoiceCount bills")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackupFromUri(context: Context, database: AppDatabase, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext Result.failure(Exception("Cannot read source file"))

            restoreBackupFromJson(database, content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Dokandaar Backup ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Store Backup")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
