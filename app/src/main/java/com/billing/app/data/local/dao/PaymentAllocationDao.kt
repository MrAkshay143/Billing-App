package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.billing.app.data.local.entity.PaymentAllocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentAllocationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllocation(allocation: PaymentAllocationEntity): Long

    @Upsert
    suspend fun upsertAllocation(allocation: PaymentAllocationEntity)

    @Upsert
    suspend fun upsertAll(allocations: List<PaymentAllocationEntity>)

    @Query("SELECT * FROM payment_allocations WHERE ledgerEntryId = :ledgerEntryId ORDER BY createdAt ASC")
    fun getAllocationsForLedgerEntry(ledgerEntryId: Long): Flow<List<PaymentAllocationEntity>>

    @Query("SELECT * FROM payment_allocations WHERE invoiceId = :invoiceId ORDER BY createdAt ASC")
    fun getAllocationsForInvoice(invoiceId: Long): Flow<List<PaymentAllocationEntity>>

    @Query("SELECT * FROM payment_allocations WHERE invoiceId = :invoiceId ORDER BY createdAt ASC")
    suspend fun getAllocationListForInvoice(invoiceId: Long): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocations ORDER BY createdAt DESC")
    fun getAllocations(): Flow<List<PaymentAllocationEntity>>

    @Query("SELECT * FROM payment_allocations WHERE id = :id LIMIT 1")
    suspend fun getAllocationById(id: Long): PaymentAllocationEntity?

    @Query("SELECT COALESCE(SUM(allocatedAmountPaise), 0) FROM payment_allocations WHERE invoiceId = :invoiceId")
    fun getTotalAllocatedForInvoice(invoiceId: Long): Flow<Long>

    @Query("DELETE FROM payment_allocations WHERE id = :id")
    suspend fun deleteAllocation(id: Long)
}
