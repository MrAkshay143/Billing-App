package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.billing.app.data.local.entity.ReturnItemBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnItemBatchDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturnItemBatch(batch: ReturnItemBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(batches: List<ReturnItemBatchEntity>)

    @Upsert
    suspend fun upsertReturnItemBatch(batch: ReturnItemBatchEntity)

    @Upsert
    suspend fun upsertAll(batches: List<ReturnItemBatchEntity>)

    @Query("SELECT * FROM return_item_batches WHERE returnItemId = :returnItemId")
    fun getBatchesForReturnItem(returnItemId: Long): Flow<List<ReturnItemBatchEntity>>

    @Query("SELECT * FROM return_item_batches WHERE returnItemId = :returnItemId")
    suspend fun getBatchListForReturnItem(returnItemId: Long): List<ReturnItemBatchEntity>

    @Query("SELECT * FROM return_item_batches")
    fun getAllReturnItemBatches(): Flow<List<ReturnItemBatchEntity>>

    @Query("SELECT * FROM return_item_batches")
    suspend fun getAllReturnItemBatchList(): List<ReturnItemBatchEntity>

    @Query("DELETE FROM return_item_batches WHERE returnItemId = :returnItemId")
    suspend fun deleteForReturnItem(returnItemId: Long)

    @Query("DELETE FROM return_item_batches WHERE id = :id")
    suspend fun deleteById(id: Long)
}
