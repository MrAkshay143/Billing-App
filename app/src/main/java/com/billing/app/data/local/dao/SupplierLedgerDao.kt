package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.billing.app.data.local.entity.SupplierLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierLedgerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: SupplierLedgerEntity): Long

    @Upsert
    suspend fun upsertEntry(entry: SupplierLedgerEntity)

    @Upsert
    suspend fun upsertAll(entries: List<SupplierLedgerEntity>)

    @Query("SELECT * FROM supplier_ledger WHERE supplierId = :supplierId ORDER BY createdAt ASC")
    fun getEntriesForSupplier(supplierId: String): Flow<List<SupplierLedgerEntity>>

    @Query("SELECT * FROM supplier_ledger WHERE supplierId = :supplierId ORDER BY createdAt ASC")
    suspend fun getEntryListForSupplier(supplierId: String): List<SupplierLedgerEntity>

    @Query("SELECT * FROM supplier_ledger ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<SupplierLedgerEntity>>

    @Query("SELECT * FROM supplier_ledger WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): SupplierLedgerEntity?

    @Query("SELECT * FROM supplier_ledger WHERE supplierId = :supplierId ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun getLatestEntryForSupplier(supplierId: String): SupplierLedgerEntity?

    @Query("SELECT * FROM supplier_ledger WHERE purchaseId = :purchaseId ORDER BY createdAt ASC")
    fun getEntriesForPurchase(purchaseId: Long): Flow<List<SupplierLedgerEntity>>

    @Query("SELECT * FROM supplier_ledger WHERE supplierId = :supplierId AND createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt ASC")
    fun getEntriesBetween(supplierId: String, startTime: Long, endTime: Long): Flow<List<SupplierLedgerEntity>>

    @Query("SELECT COALESCE(SUM(debitPaise), 0) FROM supplier_ledger WHERE supplierId = :supplierId")
    fun getTotalDebitForSupplier(supplierId: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(creditPaise), 0) FROM supplier_ledger WHERE supplierId = :supplierId")
    fun getTotalCreditForSupplier(supplierId: String): Flow<Long>

    @Query("DELETE FROM supplier_ledger WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("DELETE FROM supplier_ledger WHERE supplierId = :supplierId")
    suspend fun deleteEntriesForSupplier(supplierId: String)
}
