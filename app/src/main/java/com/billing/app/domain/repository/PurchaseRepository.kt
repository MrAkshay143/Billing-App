package com.billing.app.domain.repository

import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.PurchaseItemEntity
import com.billing.app.data.local.entity.SupplierLedgerEntity
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {
    fun getAllPurchases(): Flow<List<PurchaseEntity>>
    fun getPurchaseCount(): Flow<Int>
    suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItemEntity>
    suspend fun addPurchase(purchase: PurchaseEntity, items: List<PurchaseItemEntity>, paymentMode: String = "CASH"): Result<Long>
    suspend fun recordSupplierPayment(supplierId: String, amountPaise: Long, paymentMode: String, shiftId: Long?, note: String? = null): Result<Unit>
    fun getSupplierLedger(supplierId: String): Flow<List<SupplierLedgerEntity>>
    fun getSupplierBalance(supplierId: String): Flow<Long>
}
