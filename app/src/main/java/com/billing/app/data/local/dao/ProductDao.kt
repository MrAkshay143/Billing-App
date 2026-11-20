package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.billing.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): ProductEntity?

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' ESCAPE '\\' OR barcode LIKE '%' || :query || '%' ESCAPE '\\' OR sku LIKE '%' || :query || '%' ESCAPE '\\' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category AND (name LIKE '%' || :query || '%' ESCAPE '\\' OR barcode LIKE '%' || :query || '%' ESCAPE '\\' OR sku LIKE '%' || :query || '%' ESCAPE '\\') ORDER BY name ASC")
    fun searchProductsByCategory(query: String, category: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity)

    @Upsert
    suspend fun upsertProduct(product: ProductEntity)

    @Upsert
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    @Query("UPDATE products SET stockMilli = stockMilli - :quantityMilli WHERE id = :productId AND stockMilli >= :quantityMilli")
    suspend fun decrementProductStockMilli(productId: String, quantityMilli: Long): Int

    suspend fun decrementProductStock(productId: String, quantity: Int): Int =
        decrementProductStockMilli(productId, quantity * 1000L)

    @Query("UPDATE products SET stockMilli = stockMilli - :quantityMilli WHERE id = :productId")
    suspend fun forceDecrementProductStockMilli(productId: String, quantityMilli: Long): Int

    suspend fun forceDecrementProductStock(productId: String, quantity: Int): Int =
        forceDecrementProductStockMilli(productId, quantity * 1000L)

    @Query("UPDATE products SET stockMilli = stockMilli + :quantityMilli WHERE id = :productId")
    suspend fun restoreProductStockMilli(productId: String, quantityMilli: Long): Int

    suspend fun restoreProductStock(productId: String, quantity: Int): Int =
        restoreProductStockMilli(productId, quantity * 1000L)

    @Query("UPDATE products SET stockMilli = :newStockMilli WHERE id = :productId")
    suspend fun updateStockMilli(productId: String, newStockMilli: Long): Int

    suspend fun updateStock(productId: String, newStock: Int): Int =
        updateStockMilli(productId, newStock * 1000L)

    @Query("SELECT COUNT(*) FROM products")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE stockMilli > 0 AND stockMilli <= minStockAlertMilli")
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE stockMilli <= 0")
    fun getOutOfStockCount(): Flow<Int>

    @Query("SELECT * FROM products WHERE stockMilli <= minStockAlertMilli ORDER BY stockMilli ASC")
    suspend fun getLowAndOutOfStockProducts(): List<ProductEntity>

    @Query("SELECT DISTINCT category FROM products WHERE category IS NOT NULL AND category != ''")
    fun getAllCategories(): Flow<List<String>>

    @Query("""
        SELECT (
            (SELECT COUNT(*) FROM invoice_items WHERE productId = :productId) +
            (SELECT COUNT(*) FROM purchase_items WHERE productId = :productId) +
            (SELECT COUNT(*) FROM product_batches WHERE productId = :productId)
        )
    """)
    suspend fun getProductTransactionCount(productId: String): Int
}
