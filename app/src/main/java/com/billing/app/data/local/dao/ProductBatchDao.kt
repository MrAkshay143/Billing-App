package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.billing.app.data.local.entity.ProductBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductBatchDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBatch(batch: ProductBatchEntity): Long

    @Upsert
    suspend fun upsertBatch(batch: ProductBatchEntity)

    @Upsert
    suspend fun upsertAll(batches: List<ProductBatchEntity>)

    @Query("SELECT * FROM product_batches WHERE id = :id LIMIT 1")
    suspend fun getBatchById(id: Long): ProductBatchEntity?

    @Query("SELECT * FROM product_batches WHERE productId = :productId ORDER BY createdAt DESC")
    fun getBatchesForProduct(productId: String): Flow<List<ProductBatchEntity>>

    @Query("SELECT * FROM product_batches WHERE productId = :productId ORDER BY createdAt DESC")
    suspend fun getBatchListForProduct(productId: String): List<ProductBatchEntity>

    @Query("""
        SELECT * FROM product_batches 
        WHERE productId = :productId 
          AND currentStockMilli > 0 
          AND (expiryDate IS NULL OR expiryDate > :currentTime)
        ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC, createdAt ASC
    """)
    suspend fun getActiveBatchesFEFO(productId: String, currentTime: Long = System.currentTimeMillis()): List<ProductBatchEntity> =
        getActiveBatchesFEFO(productId)

    suspend fun getActiveBatchesFEFO(productId: String): List<ProductBatchEntity> =
        getActiveBatchesFEFO(productId, System.currentTimeMillis())

    @Query("""
        SELECT * FROM product_batches 
        WHERE productId = :productId 
          AND currentStockMilli > 0 
          AND expiryDate IS NOT NULL 
          AND expiryDate <= :currentTime
        ORDER BY expiryDate ASC
    """)
    suspend fun getExpiredBatches(productId: String, currentTime: Long = System.currentTimeMillis()): List<ProductBatchEntity> = emptyList()

    @Query("SELECT * FROM product_batches ORDER BY createdAt DESC")
    fun getAllBatches(): Flow<List<ProductBatchEntity>>

    @Query("SELECT * FROM product_batches")
    suspend fun getAllBatchList(): List<ProductBatchEntity>

    @Query("UPDATE product_batches SET currentStockMilli = :newStockMilli WHERE id = :id")
    suspend fun updateBatchStock(id: Long, newStockMilli: Long): Int

    @Query("UPDATE product_batches SET currentStockMilli = currentStockMilli - :quantityMilli WHERE id = :id AND currentStockMilli >= :quantityMilli")
    suspend fun decrementBatchStock(id: Long, quantityMilli: Long): Int

    @Query("UPDATE product_batches SET currentStockMilli = currentStockMilli + :quantityMilli WHERE id = :id")
    suspend fun incrementBatchStock(id: Long, quantityMilli: Long): Int

    @Query("SELECT * FROM product_batches WHERE expiryDate IS NOT NULL AND expiryDate <= :thresholdTimestamp AND currentStockMilli > 0 ORDER BY expiryDate ASC")
    fun getExpiringBatches(thresholdTimestamp: Long): Flow<List<ProductBatchEntity>>

    @Query("DELETE FROM product_batches WHERE id = :id")
    suspend fun deleteBatch(id: Long)

    @Query("DELETE FROM product_batches WHERE productId = :productId")
    suspend fun deleteBatchesForProduct(productId: String)
}
