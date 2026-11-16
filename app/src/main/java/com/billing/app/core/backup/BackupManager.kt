package com.billing.app.core.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.data.local.entity.ShopEntity
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

    private const val BACKUP_VERSION = 2

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

        val root = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("app", "Dokandaar")
            put("timestamp", System.currentTimeMillis())

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

            // Products
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
                })
            }
            put("products", prodArr)

            // Invoices
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
        }

        // Add payload SHA-256 checksum for integrity check
        val payloadToHash = root.getJSONArray("products").toString() +
                root.getJSONArray("invoices").toString() +
                root.getJSONArray("invoiceItems").toString()
        root.put("checksum", calculateSha256(payloadToHash))

        root.toString(2)
    }

    suspend fun saveBackupToInternalStorage(context: Context, database: AppDatabase): Result<File> = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson(database)
            val dir = File(context.filesDir, "backups")
            if (!dir.exists()) dir.mkdirs()
            val fileName = "dokandaar_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
            val file = File(dir, fileName)
            file.writeText(json)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportBackupToUri(context: Context, database: AppDatabase, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson(database)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: return@withContext Result.failure(Exception("Cannot open destination file"))
            Result.success("Backup exported successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupFromJson(database: AppDatabase, jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Validate integrity checksum if present (Version 2+)
            if (root.has("checksum") && root.has("products") && root.has("invoices") && root.has("invoiceItems")) {
                val expectedChecksum = root.getString("checksum")
                val payloadToHash = root.getJSONArray("products").toString() +
                        root.getJSONArray("invoices").toString() +
                        root.getJSONArray("invoiceItems").toString()
                val actualChecksum = calculateSha256(payloadToHash)
                if (!expectedChecksum.equals(actualChecksum, ignoreCase = true)) {
                    return@withContext Result.failure(SecurityException("Backup data integrity check failed: corrupt or modified backup file"))
                }
            }

            var productCount = 0
            var invoiceCount = 0
            var itemCount = 0

            database.withTransaction {
                // 1. Restore Shop Profile (Fixes C3)
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

                // 2. Restore Products with Upsert/Merge (Fixes C3)
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
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        database.productDao().upsertProduct(entity)
                    }
                }

                // 3. Restore Invoices with Upsert/Merge (Fixes C3)
                if (root.has("invoices")) {
                    val iArray = root.getJSONArray("invoices")
                    invoiceCount = iArray.length()
                    for (i in 0 until iArray.length()) {
                        val obj = iArray.getJSONObject(i)
                        val statusStr = obj.optString("status", "COMPLETED")
                        val status = try {
                            InvoiceStatus.valueOf(statusStr)
                        } catch (_: Exception) {
                            InvoiceStatus.COMPLETED
                        }
                        val inv = InvoiceEntity(
                            id = obj.optLong("id", 0L),
                            invoiceNumber = obj.getString("invoiceNumber"),
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
                            changeReturnedPaise = obj.optLong("changeReturnedPaise", 0L)
                        )
                        database.invoiceDao().upsertInvoice(inv)
                    }
                }

                // 4. Restore Invoice Items with Upsert/Merge
                if (root.has("invoiceItems")) {
                    val itemArray = root.getJSONArray("invoiceItems")
                    itemCount = itemArray.length()
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
                    database.invoiceDao().upsertInvoiceItems(itemsList)
                }
            }

            Result.success("Restored successfully ($productCount products, $invoiceCount invoices, $itemCount line items)")
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
