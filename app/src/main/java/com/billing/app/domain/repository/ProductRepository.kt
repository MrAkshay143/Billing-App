package com.billing.app.domain.repository

import com.billing.app.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<Product>>
    fun getProductsByCategory(category: String): Flow<List<Product>>
    fun searchProducts(query: String, category: String? = null): Flow<List<Product>>
    fun getTotalCount(): Flow<Int>
    fun getLowStockCount(): Flow<Int>
    fun getOutOfStockCount(): Flow<Int>
    fun getAllCategories(): Flow<List<String>>
    suspend fun getProductByBarcode(barcode: String): Product?
    suspend fun getProductById(id: String): Product?
    suspend fun getProductBySku(sku: String): Product?
    suspend fun getLowAndOutOfStockProducts(): List<Product>
    suspend fun addProduct(product: Product): Result<Unit>
    suspend fun addProducts(products: List<Product>): Result<Unit>
    suspend fun updateProduct(product: Product): Result<Unit>
    suspend fun adjustStock(id: String, newStock: Int): Result<Unit>
    suspend fun deleteProduct(id: String): Result<Unit>
    suspend fun getProductTransactionCount(productId: String): Int = 0
}
