package com.billing.app.data.repository

import com.billing.app.core.util.Money
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl(
    private val productDao: ProductDao
) : ProductRepository {

    override fun getProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTotalCount(): Flow<Int> = productDao.getTotalCount()

    override fun getLowStockCount(): Flow<Int> = productDao.getLowStockCount()

    override fun getOutOfStockCount(): Flow<Int> = productDao.getOutOfStockCount()

    override suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)?.toDomain()
    }

    override suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)?.toDomain()
    }

    override suspend fun addProduct(product: Product): Result<Unit> {
        return try {
            productDao.insertProduct(product.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            productDao.updateProduct(product.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(id: String): Result<Unit> {
        return try {
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
            imageUri = imageUri
        )
    }

    private fun Product.toEntity(): ProductEntity {
        return ProductEntity(
            id = id,
            name = name,
            barcode = barcode,
            pricePaise = price.paise,
            stock = stock,
            imageUri = imageUri
        )
    }
}
