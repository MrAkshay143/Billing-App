package com.billing.app.domain.repository

import com.billing.app.data.local.entity.ReturnEntity
import com.billing.app.data.local.entity.ReturnItemEntity
import kotlinx.coroutines.flow.Flow

data class ReturnItemInput(
    val invoiceItemId: Long,
    val quantityMilli: Long,
    val reason: String = ""
)

interface ReturnRepository {
    fun getAllReturns(): Flow<List<ReturnEntity>>
    fun getReturnsForInvoice(invoiceId: Long): Flow<List<ReturnEntity>>
    suspend fun getReturnItems(returnId: Long): List<ReturnItemEntity>
    suspend fun processSalesReturn(
        invoiceNumber: String,
        itemsToReturn: List<ReturnItemInput>,
        reason: String,
        userId: String
    ): Result<ReturnEntity>
}
