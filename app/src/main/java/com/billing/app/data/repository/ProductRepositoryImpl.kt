package com.billing.app.data.repository

import com.billing.app.core.util.Money
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val productDao: ProductDao
) : ProductRepository {

    override fun getProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getProductsByCategory(category: String): Flow<List<Product>> {
        return productDao.getProductsByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchProducts(query: String, category: String?): Flow<List<Product>> {
        val flow = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) {
            productDao.searchProducts(query)
        } else {
            productDao.searchProductsByCategory(query, category)
        }
        return flow.map { entities -> entities.map { it.toDomain() } }
    }

    override fun getTotalCount(): Flow<Int> = productDao.getTotalCount()

    override fun getLowStockCount(): Flow<Int> = productDao.getLowStockCount()

    override fun getOutOfStockCount(): Flow<Int> = productDao.getOutOfStockCount()

    override fun getAllCategories(): Flow<List<String>> = productDao.getAllCategories()

    override suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)?.toDomain()
    }

    override suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)?.toDomain()
    }

    override suspend fun getProductBySku(sku: String): Product? {
        return productDao.getProductBySku(sku)?.toDomain()
    }

    override suspend fun getLowAndOutOfStockProducts(): List<Product> = withContext(Dispatchers.IO) {
        productDao.getLowAndOutOfStockProducts().map { it.toDomain() }
    }

    override suspend fun addProduct(product: Product): Result<Unit> {
        return try {
            productDao.insertProduct(product.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addProducts(products: List<Product>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            productDao.insertAll(products.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = productDao.getProductById(product.id)
            val entity = product.toEntity().copy(
                createdAt = existing?.createdAt ?: product.createdAt
            )
            productDao.updateProduct(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun adjustStock(id: String, newStock: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            productDao.updateStock(id, newStock)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(id: String): Result<Unit> {
        return try {
            val product = productDao.getProductById(id)
            if (product?.imageUri != null) {
                com.billing.app.core.util.ImageUtil.deleteImageFile(product.imageUri)
            }
            productDao.deleteProduct(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ProductEntity.toDomain(): Product {
        return Product(
            id = id,
            name = name,
            barcode = barcode,
            price = Money(pricePaise),
            stock = stock,
            imageUri = imageUri,
            sku = sku,
            category = category,
            unit = unit,
            purchasePrice = Money(purchasePricePaise),
            mrp = Money(mrpPaise),
            minStockAlert = minStockAlert,
            gstRate = gstRate,
            description = description,
            createdAt = createdAt
        )
    }

    private fun Product.toEntity(): ProductEntity {
        return ProductEntity(
            id = id,
            name = name,
            barcode = barcode,
            pricePaise = price.paise,
            stock = stock,
            imageUri = imageUri,
            sku = sku,
            category = category,
            unit = unit,
            purchasePricePaise = purchasePrice.paise,
            mrpPaise = mrp.paise,
            minStockAlert = minStockAlert,
            gstRate = gstRate,
            description = description,
            createdAt = createdAt
        )
    }
}
