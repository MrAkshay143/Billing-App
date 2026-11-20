package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.billing.app.data.local.entity.ReturnEntity
import com.billing.app.data.local.entity.ReturnItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturn(returnEntity: ReturnEntity): Long

    @Upsert
    suspend fun upsertReturn(returnEntity: ReturnEntity)

    @Upsert
    suspend fun upsertAllReturns(returns: List<ReturnEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturnItem(item: ReturnItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturnItems(items: List<ReturnItemEntity>)

    @Upsert
    suspend fun upsertReturnItems(items: List<ReturnItemEntity>)

    @Query("SELECT * FROM `returns` WHERE id = :id LIMIT 1")
    suspend fun getReturnById(id: Long): ReturnEntity?

    @Query("SELECT * FROM `returns` WHERE returnNumber = :returnNumber LIMIT 1")
    suspend fun getReturnByNumber(returnNumber: String): ReturnEntity?

    @Query("SELECT * FROM `returns` WHERE invoiceId = :invoiceId ORDER BY createdAt DESC")
    fun getReturnsForInvoice(invoiceId: Long): Flow<List<ReturnEntity>>

    @Query("SELECT * FROM `returns` WHERE invoiceId = :invoiceId ORDER BY createdAt DESC")
    suspend fun getReturnsForInvoiceList(invoiceId: Long): List<ReturnEntity>

    @Query("SELECT * FROM `returns` WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    fun getReturnsForShift(shiftId: Long): Flow<List<ReturnEntity>>

    @Query("SELECT * FROM `returns` WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    suspend fun getReturnListForShift(shiftId: Long): List<ReturnEntity>

    @Query("SELECT * FROM `returns` ORDER BY createdAt DESC")
    fun getAllReturns(): Flow<List<ReturnEntity>>

    @Query("SELECT * FROM `returns` ORDER BY createdAt DESC")
    suspend fun getAllReturnList(): List<ReturnEntity>

    @Query("SELECT * FROM return_items WHERE returnId = :returnId")
    fun getReturnItems(returnId: Long): Flow<List<ReturnItemEntity>>

    @Query("SELECT * FROM return_items WHERE returnId = :returnId")
    suspend fun getReturnItemList(returnId: Long): List<ReturnItemEntity>

    @Query("SELECT * FROM return_items WHERE invoiceItemId = :invoiceItemId")
    suspend fun getReturnItemsForInvoiceItem(invoiceItemId: Long): List<ReturnItemEntity>

    @Query("SELECT * FROM return_items")
    suspend fun getAllReturnItems(): List<ReturnItemEntity>

    @Query("SELECT COALESCE(SUM(totalRefundPaise), 0) FROM `returns` WHERE shiftId = :shiftId")
    fun getTotalRefundsForShift(shiftId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(cashRefundPaise), 0) FROM `returns` WHERE shiftId = :shiftId")
    fun getTotalCashRefundsForShift(shiftId: Long): Flow<Long>

    @Query("DELETE FROM `returns` WHERE id = :id")
    suspend fun deleteReturn(id: Long)

    @Query("DELETE FROM return_items WHERE returnId = :returnId")
    suspend fun deleteReturnItems(returnId: Long)
}
