package com.billing.app.data.repository

import com.billing.app.data.local.dao.AuditLogDao
import com.billing.app.data.local.dao.ProductBatchDao
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.dao.StockAdjustmentDao
import com.billing.app.data.local.entity.AuditLogEntity
import com.billing.app.data.local.entity.ProductBatchEntity
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.data.local.entity.StockAdjustmentEntity
import com.billing.app.domain.repository.StockAdjustmentInput
import com.billing.app.domain.repository.StockAdjustmentReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StockAdjustmentRepositoryTest {

    private lateinit var fakeStockAdjustmentDao: FakeStockAdjustmentDao
    private lateinit var fakeProductDao: FakeProductDao
    private lateinit var fakeProductBatchDao: FakeProductBatchDao
    private lateinit var fakeAuditLogDao: FakeAuditLogDao
    private lateinit var repository: StockAdjustmentRepositoryImpl

    @Before
    fun setup() {
        fakeStockAdjustmentDao = FakeStockAdjustmentDao()
        fakeProductDao = FakeProductDao()
        fakeProductBatchDao = FakeProductBatchDao()
        fakeAuditLogDao = FakeAuditLogDao()

        repository = StockAdjustmentRepositoryImpl(
            stockAdjustmentDao = fakeStockAdjustmentDao,
            productDao = fakeProductDao,
            productBatchDao = fakeProductBatchDao,
            auditLogDao = fakeAuditLogDao,
            database = null
        )
    }

    @Test
    fun adjustStock_standardProduct_decrementsStockAndLogsAudit() = runBlocking {
        val product = ProductEntity(
            id = "prod_1",
            name = "Mustard Oil 1L",
            stockMilli = 10000L, // 10 units
            trackBatch = false
        )
        fakeProductDao.products.add(product)

        val input = StockAdjustmentInput(
            productId = "prod_1",
            batchId = null,
            adjustedQuantityMilli = -2000L, // 2 units damaged
            reason = StockAdjustmentReason.DAMAGE.code,
            notes = "Bottle broken on shelf",
            managerUserId = "mgr_001"
        )

        val result = repository.adjustStock(input)

        assertTrue(result.isSuccess)
        val adjustment = result.getOrNull()
        assertNotNull(adjustment)
        assertEquals(-2000L, adjustment?.adjustedQuantityMilli)
        assertEquals("DAMAGE", adjustment?.reason)

        // Verify product stock was updated: 10000 - 2000 = 8000
        val updatedProd = fakeProductDao.getProductById("prod_1")
        assertEquals(8000L, updatedProd?.stockMilli)

        // Verify audit log
        val auditLogs = fakeAuditLogDao.logs
        assertEquals(1, auditLogs.size)
        val log = auditLogs.first()
        assertEquals("STOCK_ADJUSTMENT", log.action)
        assertEquals("PRODUCT", log.entityType)
        assertEquals("prod_1", log.entityId)
        assertEquals("mgr_001", log.userId)
        assertTrue(log.details.contains("DAMAGE"))
    }

    @Test
    fun adjustStock_batchProduct_withoutBatchId_fails() = runBlocking {
        val product = ProductEntity(
            id = "prod_batch_1",
            name = "Paracetamol 500mg",
            stockMilli = 50000L,
            trackBatch = true
        )
        fakeProductDao.products.add(product)

        val input = StockAdjustmentInput(
            productId = "prod_batch_1",
            batchId = null, // Missing required batch!
            adjustedQuantityMilli = -1000L,
            reason = StockAdjustmentReason.EXPIRED.code,
            managerUserId = "mgr_001"
        )

        val result = repository.adjustStock(input)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(50000L, fakeProductDao.getProductById("prod_batch_1")?.stockMilli)
        assertTrue(fakeStockAdjustmentDao.adjustments.isEmpty())
    }

    @Test
    fun adjustStock_batchProduct_updatesBothBatchAndTotalStock() = runBlocking {
        val product = ProductEntity(
            id = "prod_batch_2",
            name = "Amoxicillin 250mg",
            stockMilli = 30000L,
            trackBatch = true
        )
        fakeProductDao.products.add(product)

        val batch = ProductBatchEntity(
            id = 101L,
            productId = "prod_batch_2",
            batchNumber = "B-2026-X",
            expiryDate = System.currentTimeMillis() + 864000000L,
            purchasePricePaise = 1500L,
            mrpPaise = 2500L,
            initialStockMilli = 30000L,
            currentStockMilli = 30000L
        )
        fakeProductBatchDao.batches.add(batch)

        val input = StockAdjustmentInput(
            productId = "prod_batch_2",
            batchId = 101L,
            adjustedQuantityMilli = -5000L, // -5 units expired
            reason = StockAdjustmentReason.EXPIRED.code,
            notes = "Expired batch cleared",
            managerUserId = "mgr_002"
        )

        val result = repository.adjustStock(input)

        assertTrue(result.isSuccess)

        // Verify product total stock updated: 30000 - 5000 = 25000
        val updatedProd = fakeProductDao.getProductById("prod_batch_2")
        assertEquals(25000L, updatedProd?.stockMilli)

        // Verify batch stock updated: 30000 - 5000 = 25000
        val updatedBatch = fakeProductBatchDao.getBatchById(101L)
        assertEquals(25000L, updatedBatch?.currentStockMilli)
    }

    // --- Fakes ---

    class FakeStockAdjustmentDao : StockAdjustmentDao {
        val adjustments = mutableListOf<StockAdjustmentEntity>()
        private var nextId = 1L

        override suspend fun insertStockAdjustment(adjustment: StockAdjustmentEntity): Long {
            val id = if (adjustment.id == 0L) nextId++ else adjustment.id
            val saved = adjustment.copy(id = id)
            adjustments.add(saved)
            return id
        }

        override suspend fun upsertStockAdjustment(adjustment: StockAdjustmentEntity) {
            val idx = adjustments.indexOfFirst { it.id == adjustment.id }
            if (idx >= 0) adjustments[idx] = adjustment else adjustments.add(adjustment)
        }

        override suspend fun upsertAll(adjustments: List<StockAdjustmentEntity>) {
            adjustments.forEach { upsertStockAdjustment(it) }
        }

        override fun getAdjustmentsForProduct(productId: String): Flow<List<StockAdjustmentEntity>> =
            MutableStateFlow(adjustments.filter { it.productId == productId }).asStateFlow()

        override fun getAllStockAdjustments(): Flow<List<StockAdjustmentEntity>> =
            MutableStateFlow(adjustments.toList()).asStateFlow()

        override suspend fun getAllStockAdjustmentList(): List<StockAdjustmentEntity> =
            adjustments.toList()

        override suspend fun getAdjustmentById(id: Long): StockAdjustmentEntity? =
            adjustments.find { it.id == id }

        override fun getAdjustmentsBetween(startTime: Long, endTime: Long): Flow<List<StockAdjustmentEntity>> =
            MutableStateFlow(adjustments.filter { it.adjustedAt in startTime..endTime }).asStateFlow()

        override suspend fun deleteAdjustment(id: Long) {
            adjustments.removeAll { it.id == id }
        }
    }

    class FakeProductDao : ProductDao {
        val products = mutableListOf<ProductEntity>()

        override fun getAllProducts(): Flow<List<ProductEntity>> =
            MutableStateFlow(products.toList()).asStateFlow()

        override suspend fun getProductByBarcode(barcode: String): ProductEntity? =
            products.find { it.barcode == barcode }

        override suspend fun getProductById(id: String): ProductEntity? =
            products.find { it.id == id }

        override suspend fun getProductBySku(sku: String): ProductEntity? =
            products.find { it.sku == sku }

        override fun getProductsByCategory(category: String): Flow<List<ProductEntity>> =
            MutableStateFlow(products.filter { it.category == category }).asStateFlow()

        override fun searchProducts(query: String): Flow<List<ProductEntity>> =
            MutableStateFlow(products.filter { it.name.contains(query, true) }).asStateFlow()

        override fun searchProductsByCategory(query: String, category: String): Flow<List<ProductEntity>> =
            MutableStateFlow(products.filter { it.category == category && it.name.contains(query, true) }).asStateFlow()

        override suspend fun insertProduct(product: ProductEntity) {
            val idx = products.indexOfFirst { it.id == product.id }
            if (idx >= 0) products[idx] = product else products.add(product)
        }

        override suspend fun upsertProduct(product: ProductEntity) {
            insertProduct(product)
        }

        override suspend fun insertAll(products: List<ProductEntity>) {
            products.forEach { insertProduct(it) }
        }

        override suspend fun updateProduct(product: ProductEntity) {
            insertProduct(product)
        }

        override suspend fun deleteProduct(id: String) {
            products.removeAll { it.id == id }
        }

        override suspend fun decrementProductStockMilli(productId: String, quantityMilli: Long): Int = 0
        override suspend fun forceDecrementProductStockMilli(productId: String, quantityMilli: Long): Int = 0
        override suspend fun restoreProductStockMilli(productId: String, quantityMilli: Long): Int = 0

        override suspend fun updateStockMilli(productId: String, newStockMilli: Long): Int {
            val idx = products.indexOfFirst { it.id == productId }
            if (idx >= 0) {
                products[idx] = products[idx].copy(stockMilli = newStockMilli)
                return 1
            }
            return 0
        }

        override fun getTotalCount(): Flow<Int> = MutableStateFlow(products.size).asStateFlow()
        override fun getLowStockCount(): Flow<Int> = MutableStateFlow(0).asStateFlow()
        override fun getOutOfStockCount(): Flow<Int> = MutableStateFlow(0).asStateFlow()
        override suspend fun getLowAndOutOfStockProducts(): List<ProductEntity> = emptyList()
        override fun getAllCategories(): Flow<List<String>> = MutableStateFlow(emptyList<String>()).asStateFlow()
        override suspend fun getProductTransactionCount(productId: String): Int = 0
    }

    class FakeProductBatchDao : ProductBatchDao {
        val batches = mutableListOf<ProductBatchEntity>()
        private var nextId = 1L

        override suspend fun insertBatch(batch: ProductBatchEntity): Long {
            val id = if (batch.id == 0L) nextId++ else batch.id
            val saved = batch.copy(id = id)
            batches.add(saved)
            return id
        }

        override suspend fun upsertBatch(batch: ProductBatchEntity) {
            val idx = batches.indexOfFirst { it.id == batch.id }
            if (idx >= 0) batches[idx] = batch else batches.add(batch)
        }

        override suspend fun upsertAll(batches: List<ProductBatchEntity>) {
            batches.forEach { upsertBatch(it) }
        }

        override suspend fun getBatchById(id: Long): ProductBatchEntity? =
            batches.find { it.id == id }

        override fun getBatchesForProduct(productId: String): Flow<List<ProductBatchEntity>> =
            MutableStateFlow(batches.filter { it.productId == productId }).asStateFlow()

        override suspend fun getBatchListForProduct(productId: String): List<ProductBatchEntity> =
            batches.filter { it.productId == productId }

        override suspend fun getActiveBatchesFEFO(productId: String): List<ProductBatchEntity> =
            batches.filter { it.productId == productId && it.currentStockMilli > 0 }

        override fun getAllBatches(): Flow<List<ProductBatchEntity>> =
            MutableStateFlow(batches.toList()).asStateFlow()

        override suspend fun getAllBatchList(): List<ProductBatchEntity> =
            batches.toList()

        override suspend fun updateBatchStock(id: Long, newStockMilli: Long): Int {
            val idx = batches.indexOfFirst { it.id == id }
            if (idx >= 0) {
                batches[idx] = batches[idx].copy(currentStockMilli = newStockMilli)
                return 1
            }
            return 0
        }

        override suspend fun decrementBatchStock(id: Long, quantityMilli: Long): Int = 0
        override suspend fun incrementBatchStock(id: Long, quantityMilli: Long): Int = 0
        override fun getExpiringBatches(thresholdTimestamp: Long): Flow<List<ProductBatchEntity>> =
            MutableStateFlow(emptyList<ProductBatchEntity>()).asStateFlow()

        override suspend fun deleteBatch(id: Long) {
            batches.removeAll { it.id == id }
        }

        override suspend fun deleteBatchesForProduct(productId: String) {
            batches.removeAll { it.productId == productId }
        }
    }

    class FakeAuditLogDao : AuditLogDao {
        val logs = mutableListOf<AuditLogEntity>()
        private var nextId = 1L

        override suspend fun insertAuditLog(log: AuditLogEntity): Long {
            val id = if (log.id == 0L) nextId++ else log.id
            val saved = log.copy(id = id)
            logs.add(saved)
            return id
        }

        override suspend fun upsertAuditLog(log: AuditLogEntity) {
            val idx = logs.indexOfFirst { it.id == log.id }
            if (idx >= 0) logs[idx] = log else logs.add(log)
        }

        override suspend fun upsertAll(logs: List<AuditLogEntity>) {
            logs.forEach { upsertAuditLog(it) }
        }

        override fun getAllAuditLogs(): Flow<List<AuditLogEntity>> =
            MutableStateFlow(logs.toList()).asStateFlow()

        override fun getRecentLogs(limit: Int): Flow<List<AuditLogEntity>> =
            MutableStateFlow(logs.takeLast(limit)).asStateFlow()

        override suspend fun getAllAuditLogList(): List<AuditLogEntity> = logs.toList()

        override fun getAuditLogsForUser(userId: String): Flow<List<AuditLogEntity>> =
            MutableStateFlow(logs.filter { it.userId == userId }).asStateFlow()

        override fun getAuditLogsForEntity(entityType: String, entityId: String): Flow<List<AuditLogEntity>> =
            MutableStateFlow(logs.filter { it.entityType == entityType && it.entityId == entityId }).asStateFlow()

        override fun getAuditLogsBetween(startTime: Long, endTime: Long): Flow<List<AuditLogEntity>> =
            MutableStateFlow(logs.filter { it.timestamp in startTime..endTime }).asStateFlow()

        override suspend fun getAuditLogById(id: Long): AuditLogEntity? =
            logs.find { it.id == id }

        override suspend fun deleteAuditLog(id: Long) {
            logs.removeAll { it.id == id }
        }

        override suspend fun deleteAuditLogsOlderThan(cutoffTimestamp: Long) {
            logs.removeAll { it.timestamp < cutoffTimestamp }
        }
    }
}
