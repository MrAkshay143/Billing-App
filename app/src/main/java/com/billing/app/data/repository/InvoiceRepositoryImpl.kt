package com.billing.app.data.repository

import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.data.local.dao.CategorySalesSummary
import com.billing.app.data.local.dao.TopProductSummary
import com.billing.app.data.local.entity.InvoiceCounterEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.repository.InvoiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class InvoiceRepositoryImpl(
    private val database: AppDatabase
) : InvoiceRepository {

    override fun getAllInvoices(): Flow<List<InvoiceEntity>> =
        database.invoiceDao().getAllInvoices()

    override fun getInvoicesByType(type: String): Flow<List<InvoiceEntity>> =
        database.invoiceDao().getInvoicesByType(type)

    override fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>> =
        database.invoiceDao().getInvoicesByStatus(status)

    override fun getInvoicesSince(startTime: Long): Flow<List<InvoiceEntity>> =
        database.invoiceDao().getInvoicesSince(startTime)

    override fun getInvoicesBetween(startTime: Long, endTime: Long): Flow<List<InvoiceEntity>> =
        database.invoiceDao().getInvoicesBetween(startTime, endTime)

    override fun searchInvoices(query: String, status: InvoiceStatus?, type: String?): Flow<List<InvoiceEntity>> {
        val escaped = query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

        return when {
            status != null -> database.invoiceDao().searchInvoicesByStatus(escaped, status)
            !type.isNullOrBlank() && !type.equals("ALL", ignoreCase = true) -> database.invoiceDao().searchInvoicesByType(escaped, type)
            else -> database.invoiceDao().searchInvoices(escaped)
        }
    }

    override fun getTotalInvoiceCount(): Flow<Int> =
        database.invoiceDao().getTotalInvoiceCount()

    override fun getInvoiceCountBetween(startTime: Long, endTime: Long): Flow<Int> =
        database.invoiceDao().getInvoiceCountBetween(startTime, endTime)

    override fun getTotalSalesBetween(startTime: Long, endTime: Long): Flow<Long> =
        database.invoiceDao().getTotalSalesBetween(startTime, endTime)

    override fun getTotalItemsSoldBetween(startTime: Long, endTime: Long): Flow<Int> =
        database.invoiceDao().getTotalItemsSoldBetween(startTime, endTime)

    override fun getTotalItemsSoldAllTime(): Flow<Int> =
        database.invoiceDao().getTotalItemsSoldAllTime()

    override fun getAverageBillBetween(startTime: Long, endTime: Long): Flow<Double> =
        database.invoiceDao().getAverageBillBetween(startTime, endTime)

    override fun getCreditDuesInvoices(): Flow<List<InvoiceEntity>> =
        database.invoiceDao().getCreditDuesInvoices()

    override fun getTotalDuePaise(): Flow<Long> =
        database.invoiceDao().getTotalDuePaise()

    override fun getPendingBillBetween(startTime: Long, endTime: Long): Flow<Long> =
        database.invoiceDao().getPendingBillBetween(startTime, endTime)

    override fun getInvoicesByCustomerPhone(phone: String): Flow<List<InvoiceEntity>> =
        database.invoiceDao().getInvoicesByCustomerPhone(phone)

    override fun getTopSellingProducts(startTime: Long, endTime: Long, limit: Int): Flow<List<TopProductSummary>> =
        database.invoiceDao().getTopSellingProducts(startTime, endTime, limit)

    override fun getSalesByCategory(startTime: Long, endTime: Long): Flow<List<CategorySalesSummary>> =
        database.invoiceDao().getSalesByCategory(startTime, endTime)

    override suspend fun getInvoiceByNumber(invoiceNumber: String): InvoiceEntity? =
        database.invoiceDao().getInvoiceByNumber(invoiceNumber)

    override suspend fun getInvoiceItems(invoiceNumber: String): List<InvoiceItemEntity> =
        database.invoiceDao().getInvoiceItems(invoiceNumber)

    override suspend fun getAllInvoiceItems(): List<InvoiceItemEntity> =
        database.invoiceDao().getAllInvoiceItems()

    override suspend fun generateNextInvoiceNumber(): String = withContext(Dispatchers.IO) {
        database.withTransaction {
            val lastNum = database.invoiceDao().getLastInvoiceNumber() ?: 0L
            val nextNum = lastNum + 1
            database.invoiceDao().setInvoiceCounter(InvoiceCounterEntity(id = 1, lastNumber = nextNum))
            String.format(Locale.US, "INV-%06d", nextNum)
        }
    }

    override suspend fun completeSale(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        draftInvoiceNumberToDelete: String?
    ): Result<InvoiceEntity> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                // 1. Transactional stock decrement with strict verification
                for (item in items) {
                    val rowsUpdated = database.productDao().decrementProductStock(item.productId, item.quantity)
                    if (rowsUpdated == 0) {
                        throw IllegalStateException("Insufficient stock for product '${item.productName}'")
                    }
                }

                // 2. Insert invoice and items with OnConflictStrategy.ABORT
                database.invoiceDao().insertInvoice(invoice)
                database.invoiceDao().insertInvoiceItems(items)

                // 3. Delete draft if sale was from resumed draft
                if (!draftInvoiceNumberToDelete.isNullOrBlank()) {
                    database.invoiceDao().deleteInvoice(draftInvoiceNumberToDelete)
                }
            }
            Result.success(invoice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveDraft(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Result<InvoiceEntity> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                database.invoiceDao().upsertInvoice(invoice)
                database.invoiceDao().deleteInvoiceItems(invoice.invoiceNumber)
                database.invoiceDao().insertInvoiceItems(items)
            }
            Result.success(invoice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun voidInvoice(invoiceNumber: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val inv = database.invoiceDao().getInvoiceByNumber(invoiceNumber)
                    ?: throw IllegalArgumentException("Invoice $invoiceNumber not found")
                if (inv.status != InvoiceStatus.COMPLETED) {
                    throw IllegalStateException("Only COMPLETED invoices can be voided (status: ${inv.status})")
                }

                // Mark as cancelled
                database.invoiceDao().updateInvoiceStatus(invoiceNumber, InvoiceStatus.CANCELLED)

                // Return stock to inventory
                val items = database.invoiceDao().getInvoiceItems(invoiceNumber)
                for (item in items) {
                    database.productDao().restoreProductStock(item.productId, item.quantity)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun settleInvoiceDue(invoiceNumber: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.invoiceDao().updateInvoiceDue(invoiceNumber, 0L)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteInvoice(invoiceNumber: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.invoiceDao().deleteInvoice(invoiceNumber)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun upsertInvoicesAndItems(
        invoices: List<InvoiceEntity>,
        items: List<InvoiceItemEntity>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                for (inv in invoices) {
                    database.invoiceDao().upsertInvoice(inv)
                }
                database.invoiceDao().upsertInvoiceItems(items)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
