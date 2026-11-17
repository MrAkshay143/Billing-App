package com.billing.app.domain.repository

import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.PurchaseItemEntity
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {
    fun getAllPurchases(): Flow<List<PurchaseEntity>>
    fun getPurchaseCount(): Flow<Int>
    suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItemEntity>
    suspend fun addPurchase(purchase: PurchaseEntity, items: List<PurchaseItemEntity>): Result<Long>
}
