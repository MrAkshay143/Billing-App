package com.billing.app.data.repository

import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.core.util.UnitRegistry
import com.billing.app.data.local.dao.AuditLogDao
import com.billing.app.data.local.dao.ProductBatchDao
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.dao.StockAdjustmentDao
import com.billing.app.data.local.entity.AuditLogEntity
import com.billing.app.data.local.entity.StockAdjustmentEntity
import com.billing.app.domain.repository.StockAdjustmentInput
import com.billing.app.domain.repository.StockAdjustmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class StockAdjustmentRepositoryImpl(
    private val stockAdjustmentDao: StockAdjustmentDao,
    private val productDao: ProductDao,
    private val productBatchDao: ProductBatchDao,
    private val auditLogDao: AuditLogDao,
    private val database: AppDatabase? = null
) : StockAdjustmentRepository {

    constructor(database: AppDatabase) : this(
        stockAdjustmentDao = database.stockAdjustmentDao(),
        productDao = database.productDao(),
        productBatchDao = database.productBatchDao(),
        auditLogDao = database.auditLogDao(),
        database = database
    )

    override fun getAllAdjustments(): Flow<List<StockAdjustmentEntity>> =
        stockAdjustmentDao.getAllStockAdjustments()

    override fun getAdjustmentsForProduct(productId: String): Flow<List<StockAdjustmentEntity>> =
        stockAdjustmentDao.getAdjustmentsForProduct(productId)

    override fun getAdjustmentsBetween(startTime: Long, endTime: Long): Flow<List<StockAdjustmentEntity>> =
        stockAdjustmentDao.getAdjustmentsBetween(startTime, endTime)

    override suspend fun adjustStock(input: StockAdjustmentInput): Result<StockAdjustmentEntity> =
        withContext(Dispatchers.IO) {
            try {
                val action: suspend () -> StockAdjustmentEntity = {
                    val product = productDao.getProductById(input.productId)
                        ?: throw IllegalArgumentException("Product not found: ${input.productId}")

                    val unitDef = UnitRegistry.resolve(product.unit)
                    if (!unitDef.allowsFractional && input.adjustedQuantityMilli % 1000L != 0L) {
                        throw IllegalArgumentException("Fractional adjustment quantity is not allowed for discrete unit '${unitDef.code}'")
                    }

                    if (product.trackBatch && input.batchId == null) {
                        throw IllegalArgumentException("Batch selection is required for batch-tracked product: ${product.name}")
                    }

                    // 1. Update product batch if batchId is provided
                    if (input.batchId != null) {
                        val batch = productBatchDao.getBatchById(input.batchId)
                            ?: throw IllegalArgumentException("Batch not found: ${input.batchId}")
                        val newBatchStock = batch.currentStockMilli + input.adjustedQuantityMilli
                        if (newBatchStock < 0L) {
                            throw IllegalStateException("Batch stock cannot be negative: resulting $newBatchStock milli")
                        }
                        productBatchDao.updateBatchStock(batch.id, newBatchStock)
                    }

                    // 2. Update product total stock
                    val newStockMilli = product.stockMilli + input.adjustedQuantityMilli
                    productDao.updateStockMilli(product.id, newStockMilli)

                    // 3. Insert stock adjustment entry
                    val entity = StockAdjustmentEntity(
                        productId = input.productId,
                        batchId = input.batchId,
                        adjustedQuantityMilli = input.adjustedQuantityMilli,
                        reason = input.reason,
                        adjustedByUserId = input.managerUserId,
                        adjustedAt = System.currentTimeMillis()
                    )
                    val id = stockAdjustmentDao.insertStockAdjustment(entity)

                    // 4. Record audit log
                    val sign = if (input.adjustedQuantityMilli >= 0) "+" else ""
                    val details = "Stock adjusted: $sign${input.adjustedQuantityMilli / 1000.0} units for ${product.name}. Reason: ${input.reason}. Notes: ${input.notes}"
                    auditLogDao.insertAuditLog(
                        AuditLogEntity(
                            userId = input.managerUserId,
                            action = "STOCK_ADJUSTMENT",
                            entityType = "PRODUCT",
                            entityId = input.productId,
                            details = details
                        )
                    )

                    entity.copy(id = id)
                }

                val resultEntity = if (database != null) {
                    database.withTransaction { action() }
                } else {
                    action()
                }

                Result.success(resultEntity)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
