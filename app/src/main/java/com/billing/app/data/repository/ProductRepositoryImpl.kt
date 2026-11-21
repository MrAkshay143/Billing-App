package com.billing.app.data.repository

import com.billing.app.core.util.Money
import com.billing.app.core.util.UnitRegistry
import com.billing.app.data.local.dao.ProductBatchDao
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.entity.ProductBatchEntity
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.domain.model.AppError
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val productBatchDao: ProductBatchDao? = null
) : ProductRepository {

    private val effectiveBatchDao: ProductBatchDao?
        get() = productBatchDao ?: runCatching { com.billing.app.BillingApplication.instance.database.productBatchDao() }.getOrNull()

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
        val escaped = query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        val flow = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) {
            productDao.searchProducts(escaped)
        } else {
            productDao.searchProductsByCategory(escaped, category)
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

    override suspend fun addProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val unitDef = UnitRegistry.resolve(product.unit)
            if (!unitDef.allowsFractional && product.stockMilli % 1000L != 0L) {
                return@withContext Result.failure(AppError.FractionalNotAllowed(unitDef.code))
            }
            val canonicalProduct = product.copy(unit = unitDef.code)
            productDao.insertProduct(canonicalProduct.toEntity())
            if (canonicalProduct.trackBatch && canonicalProduct.stockMilli > 0L) {
                val initialBatch = ProductBatchEntity(
                    productId = canonicalProduct.id,
                    batchNumber = "BATCH-INIT",
                    expiryDate = null,
                    purchasePricePaise = canonicalProduct.purchasePrice.paise,
                    mrpPaise = canonicalProduct.mrp.paise,
                    initialStockMilli = canonicalProduct.stockMilli,
                    currentStockMilli = canonicalProduct.stockMilli,
                    createdAt = System.currentTimeMillis()
                )
                effectiveBatchDao?.insertBatch(initialBatch)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addProducts(products: List<Product>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val canonicalList = products.map { p ->
                val unitDef = UnitRegistry.resolve(p.unit)
                if (!unitDef.allowsFractional && p.stockMilli % 1000L != 0L) {
                    throw AppError.FractionalNotAllowed(unitDef.code)
                }
                p.copy(unit = unitDef.code)
            }
            productDao.insertAll(canonicalList.map { it.toEntity() })
            canonicalList.forEach { p ->
                if (p.trackBatch && p.stockMilli > 0L) {
                    val initialBatch = ProductBatchEntity(
                        productId = p.id,
                        batchNumber = "BATCH-INIT",
                        expiryDate = null,
                        purchasePricePaise = p.purchasePrice.paise,
                        mrpPaise = p.mrp.paise,
                        initialStockMilli = p.stockMilli,
                        currentStockMilli = p.stockMilli,
                        createdAt = System.currentTimeMillis()
                    )
                    effectiveBatchDao?.insertBatch(initialBatch)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = productDao.getProductById(product.id)
            if (existing != null) {
                val existingCanonicalUnit = UnitRegistry.resolve(existing.unit).code
                val newCanonicalUnit = UnitRegistry.resolve(product.unit).code
                if (existingCanonicalUnit != newCanonicalUnit) {
                    val hasStock = existing.stockMilli != 0L
                    val txCount = productDao.getProductTransactionCount(product.id)
                    if (hasStock || txCount > 0) {
                        return@withContext Result.failure(
                            AppError.UnitChangeNotAllowed(
                                product.name,
                                "Product has existing stock or transaction history"
                            )
                        )
                    }
                }
            }
            val unitDef = UnitRegistry.resolve(product.unit)
            if (!unitDef.allowsFractional && product.stockMilli % 1000L != 0L) {
                return@withContext Result.failure(AppError.FractionalNotAllowed(unitDef.code))
            }
            val entity = product.copy(unit = unitDef.code).toEntity().copy(
                createdAt = existing?.createdAt ?: product.createdAt
            )
            productDao.updateProduct(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductTransactionCount(productId: String): Int = withContext(Dispatchers.IO) {
        productDao.getProductTransactionCount(productId)
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
            barcode = barcode ?: "",
            price = Money(pricePaise),
            stockMilli = stockMilli,
            imageUri = imageUri,
            sku = sku,
            category = category,
            unit = unit,
            purchasePrice = Money(purchasePricePaise),
            mrp = Money(mrpPaise),
            minStockAlertMilli = minStockAlertMilli,
            gstRateBp = gstRateBp,
            description = description,
            trackBatch = trackBatch,
            hsnCode = hsnCode,
            isActive = isActive,
            createdAt = createdAt
        )
    }

    private fun Product.toEntity(): ProductEntity {
        return ProductEntity(
            id = id,
            name = name,
            barcode = barcode,
            pricePaise = price.paise,
            stockMilli = stockMilli,
            imageUri = imageUri,
            createdAt = createdAt,
            sku = sku,
            category = category,
            unit = unit,
            purchasePricePaise = purchasePrice.paise,
            mrpPaise = mrp.paise,
            minStockAlertMilli = minStockAlertMilli,
            gstRateBp = gstRateBp,
            description = description,
            trackBatch = trackBatch,
            hsnCode = hsnCode,
            isActive = isActive
        )
    }
}
