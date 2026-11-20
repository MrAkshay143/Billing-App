package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.billing.app.data.local.entity.StockAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockAdjustmentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockAdjustment(adjustment: StockAdjustmentEntity): Long

    @Upsert
    suspend fun upsertStockAdjustment(adjustment: StockAdjustmentEntity)

    @Upsert
    suspend fun upsertAll(adjustments: List<StockAdjustmentEntity>)

    @Query("SELECT * FROM stock_adjustments WHERE productId = :productId ORDER BY adjustedAt DESC")
    fun getAdjustmentsForProduct(productId: String): Flow<List<StockAdjustmentEntity>>

    @Query("SELECT * FROM stock_adjustments ORDER BY adjustedAt DESC")
    fun getAllStockAdjustments(): Flow<List<StockAdjustmentEntity>>

    @Query("SELECT * FROM stock_adjustments ORDER BY adjustedAt DESC")
    suspend fun getAllStockAdjustmentList(): List<StockAdjustmentEntity>

    @Query("SELECT * FROM stock_adjustments WHERE id = :id LIMIT 1")
    suspend fun getAdjustmentById(id: Long): StockAdjustmentEntity?

    @Query("SELECT * FROM stock_adjustments WHERE adjustedAt BETWEEN :startTime AND :endTime ORDER BY adjustedAt DESC")
    fun getAdjustmentsBetween(startTime: Long, endTime: Long): Flow<List<StockAdjustmentEntity>>

    @Query("DELETE FROM stock_adjustments WHERE id = :id")
    suspend fun deleteAdjustment(id: Long)
}
