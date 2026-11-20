package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.billing.app.data.local.entity.CustomerLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerLedgerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: CustomerLedgerEntity): Long

    @Upsert
    suspend fun upsertEntry(entry: CustomerLedgerEntity)

    @Upsert
    suspend fun upsertAll(entries: List<CustomerLedgerEntity>)

    @Query("SELECT * FROM customer_ledger WHERE customerId = :customerId ORDER BY createdAt ASC")
    fun getEntriesForCustomer(customerId: String): Flow<List<CustomerLedgerEntity>>

    @Query("SELECT * FROM customer_ledger WHERE customerId = :customerId ORDER BY createdAt ASC")
    suspend fun getEntryListForCustomer(customerId: String): List<CustomerLedgerEntity>

    @Query("SELECT * FROM customer_ledger ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<CustomerLedgerEntity>>

    @Query("SELECT * FROM customer_ledger WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): CustomerLedgerEntity?

    @Query("SELECT * FROM customer_ledger WHERE customerId = :customerId ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun getLatestEntryForCustomer(customerId: String): CustomerLedgerEntity?

    @Query("SELECT * FROM customer_ledger WHERE invoiceId = :invoiceId ORDER BY createdAt ASC")
    fun getEntriesForInvoice(invoiceId: Long): Flow<List<CustomerLedgerEntity>>

    @Query("SELECT * FROM customer_ledger WHERE customerId = :customerId AND createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt ASC")
    fun getEntriesBetween(customerId: String, startTime: Long, endTime: Long): Flow<List<CustomerLedgerEntity>>

    @Query("SELECT COALESCE(SUM(debitPaise), 0) FROM customer_ledger WHERE customerId = :customerId")
    fun getTotalDebitForCustomer(customerId: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(creditPaise), 0) FROM customer_ledger WHERE customerId = :customerId")
    fun getTotalCreditForCustomer(customerId: String): Flow<Long>

    @Query("DELETE FROM customer_ledger WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("DELETE FROM customer_ledger WHERE customerId = :customerId")
    suspend fun deleteEntriesForCustomer(customerId: String)
}
