package com.billing.app.core.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.data.local.entity.CategoryEntity
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.ExpenseEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.PurchaseItemEntity
import com.billing.app.data.local.entity.ShopEntity
import com.billing.app.data.local.entity.SupplierEntity
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

    private const val BACKUP_VERSION = 3

    private fun calculateSha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun createBackupJson(database: AppDatabase): String = withContext(Dispatchers.IO) {
        val shop = database.shopDao().getShopProfileSync()
        val products = database.productDao().getAllProducts().first()
        val invoices = database.invoiceDao().getAllInvoices().first()
        val invoiceItems = database.invoiceDao().getAllInvoiceItems()
        val invoiceCounter = database.invoiceDao().getLastInvoiceNumber() ?: 0L

        // Room v4 entities
        val customers = database.customerDao().getAllCustomers().first()
        val categories = database.categoryDao().getAllCategories().first()
        val suppliers = database.supplierDao().getAllSuppliers().first()
        val expenses = database.expenseDao().getAllExpenses().first()
        val purchases = database.purchaseDao().getAllPurchases().first()

        val root = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("app", "Dokandaar")
            put("timestamp", System.currentTimeMillis())
            put("invoiceCounter", invoiceCounter)

            // Shop Profile
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

            // Products (including all Room v4 columns)
            val prodArr = JSONArray()
            for (p in products) {
                prodArr.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("barcode", p.barcode)
                    put("pricePaise", p.pricePaise)
                    put("stock", p.stock)
                    put("imageUri", p.imageUri ?: "")
                    put("createdAt", p.createdAt)
                    put("sku", p.sku)
                    put("category", p.category)
                    put("unit", p.unit)
                    put("purchasePricePaise", p.purchasePricePaise)
                    put("mrpPaise", p.mrpPaise)
                    put("minStockAlert", p.minStockAlert)
                    put("gstRate", p.gstRate)
                    put("description", p.description)
                })
            }
            put("products", prodArr)

            // Invoices (including invoiceType & itemsCount)
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
                })
            }
            put("invoices", invArr)

            // Invoice Items
            val itemArr = JSONArray()
            for (item in invoiceItems) {
                itemArr.put(JSONObject().apply {
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
            put("invoiceItems", itemArr)

            // Customers
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
                })
            }
            put("customers", custArr)

            // Categories
            val catArr = JSONArray()
            for (cat in categories) {
                catArr.put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("iconName", cat.iconName ?: "")
                    put("displayOrder", cat.displayOrder)
                })
            }
            put("categories", catArr)

            // Suppliers
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
                })
            }
            put("suppliers", supArr)

            // Expenses
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
                })
            }
            put("expenses", expArr)

            // Purchases
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
                })
            }
            put("purchases", purArr)
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

            database.withTransaction {
                // 1. Restore Shop Profile
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

                // 2. Restore Products with all fields
                if (root.has("products")) {
                    val pArray = root.getJSONArray("products")
                    productCount = pArray.length()
                    for (i in 0 until pArray.length()) {
                        val obj = pArray.getJSONObject(i)
                        val entity = ProductEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            barcode = obj.getString("barcode"),
                            pricePaise = obj.getLong("pricePaise"),
                            stock = obj.getInt("stock"),
                            imageUri = obj.optString("imageUri").ifBlank { null },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            sku = obj.optString("sku", ""),
                            category = obj.optString("category", "Grocery"),
                            unit = obj.optString("unit", "Piece"),
                            purchasePricePaise = obj.optLong("purchasePricePaise", 0L),
                            mrpPaise = obj.optLong("mrpPaise", 0L),
                            minStockAlert = obj.optInt("minStockAlert", 5),
                            gstRate = obj.optInt("gstRate", 0),
                            description = obj.optString("description", "")
                        )
                        database.productDao().upsertProduct(entity)
                    }
                }

                // 3. Restore Invoices
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
                            itemsCount = obj.optInt("itemsCount", 1)
                        )
                        database.invoiceDao().upsertInvoice(inv)
                    }
                }

                // 4. Restore Invoice Items
                if (root.has("invoiceItems")) {
                    val itemArray = root.getJSONArray("invoiceItems")
                    itemCount = itemArray.length()
                    val itemsList = mutableListOf<InvoiceItemEntity>()
                    for (i in 0 until itemArray.length()) {
                        val obj = itemArray.getJSONObject(i)
                        itemsList.add(
                            InvoiceItemEntity(
                                id = 0L,
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
                    for (invNum in restoredInvoiceNumbers) {
                        database.invoiceDao().deleteInvoiceItems(invNum)
                    }
                    database.invoiceDao().insertInvoiceItems(itemsList)
                }

                // 5. Restore Customers
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
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                    database.customerDao().insertAll(custList)
                }

                // 6. Restore Categories
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
                                displayOrder = obj.optInt("displayOrder", 0)
                            )
                        )
                    }
                    database.categoryDao().insertAll(catList)
                }

                // 7. Restore Suppliers
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
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.supplierDao().insertSupplier(sup)
                    }
                }

                // 8. Restore Expenses
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
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.expenseDao().insertExpense(exp)
                    }
                }

                // 9. Restore & Sync Invoice Counter
                val backupCounter = root.optLong("invoiceCounter", 0L)
                val currentDbCounter = database.invoiceDao().getLastInvoiceNumber() ?: 0L
                val maxCounter = maxOf(backupCounter, maxInvoiceSuffix, currentDbCounter)
                if (maxCounter > 0L) {
                    database.invoiceDao().setInvoiceCounter(
                        com.billing.app.data.local.entity.InvoiceCounterEntity(id = 1, lastNumber = maxCounter)
                    )
                }
            }

            Result.success("Restored successfully ($productCount products, $invoiceCount invoices, $itemCount items)")
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
