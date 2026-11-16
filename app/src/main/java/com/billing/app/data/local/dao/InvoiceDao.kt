package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY timestamp DESC")
    fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getInvoicesSince(startTime: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE invoiceNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchInvoices(query: String): Flow<List<InvoiceEntity>>

    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'COMPLETED'")
    fun getTotalInvoiceCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'COMPLETED' AND timestamp >= :startTime")
    fun getInvoiceCountSince(startTime: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(grandTotalPaise), 0) FROM invoices WHERE status = 'COMPLETED' AND timestamp >= :startTime")
    fun getTotalSalesSince(startTime: Long): Flow<Long>

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumber(invoiceNumber: String): InvoiceEntity?

    @Query("SELECT * FROM invoice_items WHERE invoiceNumber = :invoiceNumber")
    suspend fun getInvoiceItems(invoiceNumber: String): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items")
    suspend fun getAllInvoiceItems(): List<InvoiceItemEntity>

    @Query("UPDATE invoices SET status = :status WHERE invoiceNumber = :invoiceNumber")
    suspend fun updateInvoiceStatus(invoiceNumber: String, status: InvoiceStatus)

    @Query("DELETE FROM invoices WHERE invoiceNumber = :invoiceNumber")
    suspend fun deleteInvoice(invoiceNumber: String)
}
