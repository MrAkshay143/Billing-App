package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.billing.app.data.local.entity.InvoiceItemBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceItemBatchDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoiceItemBatch(itemBatch: InvoiceItemBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemBatch(itemBatch: InvoiceItemBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(itemBatches: List<InvoiceItemBatchEntity>)

    @Upsert
    suspend fun upsertInvoiceItemBatch(itemBatch: InvoiceItemBatchEntity)

    @Upsert
    suspend fun upsertAll(itemBatches: List<InvoiceItemBatchEntity>)

    @Query("SELECT * FROM invoice_item_batches WHERE invoiceItemId = :invoiceItemId")
    fun getItemBatchesForInvoiceItem(invoiceItemId: Long): Flow<List<InvoiceItemBatchEntity>>

    @Query("SELECT * FROM invoice_item_batches WHERE invoiceItemId = :invoiceItemId")
    suspend fun getItemBatchListForInvoiceItem(invoiceItemId: Long): List<InvoiceItemBatchEntity>

    @Query("SELECT * FROM invoice_item_batches WHERE batchId = :batchId")
    fun getItemBatchesForBatch(batchId: Long): Flow<List<InvoiceItemBatchEntity>>

    @Query("SELECT * FROM invoice_item_batches")
    fun getAllItemBatches(): Flow<List<InvoiceItemBatchEntity>>

    @Query("SELECT * FROM invoice_item_batches")
    suspend fun getAllItemBatchList(): List<InvoiceItemBatchEntity>

    @Query("DELETE FROM invoice_item_batches WHERE invoiceItemId = :invoiceItemId")
    suspend fun deleteForItem(invoiceItemId: Long)

    @Query("DELETE FROM invoice_item_batches WHERE id = :id")
    suspend fun deleteById(id: Long)
}
