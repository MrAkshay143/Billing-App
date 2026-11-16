package com.billing.app.domain.repository

import com.billing.app.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<Product>>
    fun getTotalCount(): Flow<Int>
    fun getLowStockCount(): Flow<Int>
    fun getOutOfStockCount(): Flow<Int>
    suspend fun getProductByBarcode(barcode: String): Product?
    suspend fun getProductById(id: String): Product?
    suspend fun getLowAndOutOfStockProducts(): List<Product>
    suspend fun addProduct(product: Product): Result<Unit>
    suspend fun updateProduct(product: Product): Result<Unit>
    suspend fun deleteProduct(id: String): Result<Unit>
}
