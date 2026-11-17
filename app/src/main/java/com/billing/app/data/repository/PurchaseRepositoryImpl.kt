package com.billing.app.data.repository

import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.PurchaseItemEntity
import com.billing.app.domain.repository.PurchaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PurchaseRepositoryImpl(
    private val database: AppDatabase
) : PurchaseRepository {

    override fun getAllPurchases(): Flow<List<PurchaseEntity>> =
        database.purchaseDao().getAllPurchases()

    override fun getPurchaseCount(): Flow<Int> =
        database.purchaseDao().getPurchaseCount()

    override suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItemEntity> = withContext(Dispatchers.IO) {
        database.purchaseDao().getPurchaseItems(purchaseId)
    }

    override suspend fun addPurchase(
        purchase: PurchaseEntity,
        items: List<PurchaseItemEntity>
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = database.withTransaction {
                val purchaseId = database.purchaseDao().insertPurchase(purchase)
                val mappedItems = items.map { it.copy(purchaseId = purchaseId) }
                database.purchaseDao().insertPurchaseItems(mappedItems)

                // Increase stock for purchased items
                for (item in items) {
                    database.productDao().restoreProductStock(item.productId, item.quantity)
                }
                purchaseId
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
