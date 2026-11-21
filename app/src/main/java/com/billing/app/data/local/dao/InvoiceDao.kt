package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.billing.app.data.local.entity.InvoiceCounterEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import kotlinx.coroutines.flow.Flow

data class TopProductSummary(
    val productId: String,
    val productName: String,
    val totalQuantity: Int,
    val totalRevenuePaise: Long
)

data class CategorySalesSummary(
    val category: String,
    val totalRevenuePaise: Long
)

@Dao
interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoiceItem(item: InvoiceItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Upsert
    suspend fun upsertInvoice(invoice: InvoiceEntity)

    @Upsert
    suspend fun upsertInvoiceItems(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE invoiceType = :type ORDER BY timestamp DESC")
    fun getInvoicesByType(type: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY timestamp DESC")
    fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getInvoicesSince(startTime: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getInvoicesBetween(startTime: Long, endTime: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE invoiceNumber LIKE '%' || :query || '%' ESCAPE '\\' OR customerName LIKE '%' || :query || '%' ESCAPE '\\' OR customerPhone LIKE '%' || :query || '%' ESCAPE '\\' ORDER BY timestamp DESC")
    fun searchInvoices(query: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status = :status AND (invoiceNumber LIKE '%' || :query || '%' ESCAPE '\\' OR customerName LIKE '%' || :query || '%' ESCAPE '\\') ORDER BY timestamp DESC")
    fun searchInvoicesByStatus(query: String, status: InvoiceStatus): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE invoiceType = :type AND (invoiceNumber LIKE '%' || :query || '%' ESCAPE '\\' OR customerName LIKE '%' || :query || '%' ESCAPE '\\') ORDER BY timestamp DESC")
    fun searchInvoicesByType(query: String, type: String): Flow<List<InvoiceEntity>>

    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'COMPLETED'")
    fun getTotalInvoiceCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'COMPLETED' AND timestamp >= :startTime")
    fun getInvoiceCountSince(startTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'COMPLETED' AND timestamp BETWEEN :startTime AND :endTime")
    fun getInvoiceCountBetween(startTime: Long, endTime: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(grandTotalPaise), 0) FROM invoices WHERE status = 'COMPLETED' AND timestamp >= :startTime")
    fun getTotalSalesSince(startTime: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(grandTotalPaise), 0) FROM invoices WHERE status = 'COMPLETED' AND timestamp BETWEEN :startTime AND :endTime")
    fun getTotalSalesBetween(startTime: Long, endTime: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(quantityMilli), 0) FROM invoice_items WHERE invoiceId IN (SELECT id FROM invoices WHERE status = 'COMPLETED' AND timestamp BETWEEN :startTime AND :endTime)")
    fun getTotalItemsSoldBetween(startTime: Long, endTime: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantityMilli), 0) FROM invoice_items WHERE invoiceId IN (SELECT id FROM invoices WHERE status = 'COMPLETED')")
    fun getTotalItemsSoldAllTime(): Flow<Int>

    @Query("SELECT COALESCE(AVG(grandTotalPaise), 0.0) FROM invoices WHERE status = 'COMPLETED' AND timestamp BETWEEN :startTime AND :endTime")
    fun getAverageBillBetween(startTime: Long, endTime: Long): Flow<Double>

    @Query("SELECT * FROM invoices WHERE duePaise > 0 AND status = 'COMPLETED' ORDER BY timestamp DESC")
    fun getCreditDuesInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT COALESCE(SUM(duePaise), 0) FROM invoices WHERE status = 'COMPLETED'")
    fun getTotalDuePaise(): Flow<Long>

    @Query("SELECT COALESCE(SUM(duePaise), 0) FROM invoices WHERE status = 'COMPLETED' AND timestamp BETWEEN :startTime AND :endTime")
    fun getPendingBillBetween(startTime: Long, endTime: Long): Flow<Long>

    @Query("SELECT * FROM invoices WHERE customerPhone = :phone ORDER BY timestamp DESC")
    fun getInvoicesByCustomerPhone(phone: String): Flow<List<InvoiceEntity>>

    @Query("""
        SELECT productId, productName, SUM(quantityMilli) as totalQuantity, SUM(lineTotalPaise) as totalRevenuePaise
        FROM invoice_items
        WHERE invoiceId IN (SELECT id FROM invoices WHERE status = 'COMPLETED' AND timestamp BETWEEN :startTime AND :endTime)
        GROUP BY productId, productName
        ORDER BY totalQuantity DESC
        LIMIT :limit
    """)
    fun getTopSellingProducts(startTime: Long, endTime: Long, limit: Int = 10): Flow<List<TopProductSummary>>

    @Query("""
        SELECT COALESCE(p.category, 'Others') as category, SUM(i.lineTotalPaise) as totalRevenuePaise
        FROM invoice_items i
        LEFT JOIN products p ON i.productId = p.id
        WHERE i.invoiceId IN (SELECT id FROM invoices WHERE status = 'COMPLETED' AND timestamp BETWEEN :startTime AND :endTime)
        GROUP BY p.category
        ORDER BY totalRevenuePaise DESC
    """)
    fun getSalesByCategory(startTime: Long, endTime: Long): Flow<List<CategorySalesSummary>>

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumber(invoiceNumber: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Query("SELECT invoice_items.* FROM invoice_items INNER JOIN invoices ON invoice_items.invoiceId = invoices.id WHERE invoices.invoiceNumber = :invoiceNumber")
    suspend fun getInvoiceItems(invoiceNumber: String): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItemsByInvoiceId(invoiceId: Long): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items")
    suspend fun getAllInvoiceItems(): List<InvoiceItemEntity>

    @Query("UPDATE invoices SET status = :status WHERE invoiceNumber = :invoiceNumber")
    suspend fun updateInvoiceStatus(invoiceNumber: String, status: InvoiceStatus)

    @Query("UPDATE invoices SET status = 'CANCELLED', voidedAt = :voidedAt, voidedByUserId = :voidedByUserId, voidReason = :voidReason WHERE invoiceNumber = :invoiceNumber")
    suspend fun updateInvoiceVoidStatus(
        invoiceNumber: String,
        voidedAt: Long,
        voidedByUserId: String?,
        voidReason: String?
    )

    @Query("DELETE FROM invoices WHERE invoiceNumber = :invoiceNumber")
    suspend fun deleteInvoice(invoiceNumber: String)

    @Query("DELETE FROM invoice_items WHERE invoiceId IN (SELECT id FROM invoices WHERE invoiceNumber = :invoiceNumber)")
    suspend fun deleteInvoiceItems(invoiceNumber: String)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceItemsByInvoiceId(invoiceId: Long)

    @Query("UPDATE invoices SET duePaise = :duePaise WHERE invoiceNumber = :invoiceNumber")
    suspend fun updateInvoiceDue(invoiceNumber: String, duePaise: Long)

    // Sequential invoice numbering counter
    @Query("SELECT lastNumber FROM invoice_counter WHERE id = 1")
    suspend fun getLastInvoiceNumber(): Long?

    @Upsert
    suspend fun setInvoiceCounter(counter: InvoiceCounterEntity)

    @Query("SELECT * FROM invoices WHERE shiftId = :shiftId AND status = 'COMPLETED'")
    suspend fun getInvoicesByShiftId(shiftId: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoice_items WHERE invoiceId IN (SELECT id FROM invoices WHERE shiftId = :shiftId AND status = 'COMPLETED')")
    suspend fun getInvoiceItemsByShiftId(shiftId: Long): List<InvoiceItemEntity>
}
