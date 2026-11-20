package com.billing.app.domain.repository

import com.billing.app.data.local.entity.StockAdjustmentEntity
import kotlinx.coroutines.flow.Flow

enum class StockAdjustmentReason(val code: String, val displayNameBn: String, val displayNameEn: String) {
    DAMAGE("DAMAGE", "নষ্ট বা ক্ষতি", "Damage / Broken"),
    EXPIRED("EXPIRED", "মেয়াদোত্তীর্ণ", "Expired"),
    AUDIT_COUNT_MISMATCH("AUDIT_COUNT_MISMATCH", "স্টক গণনা অমিল", "Audit Count Mismatch"),
    THEFT_OR_LOSS("THEFT_OR_LOSS", "হারিয়ে যাওয়া বা চুরি", "Theft or Loss"),
    CORRECTION("CORRECTION", "ভুল সংশোধন", "Correction")
}

data class StockAdjustmentInput(
    val productId: String,
    val batchId: Long? = null,
    val adjustedQuantityMilli: Long, // Difference: e.g. -1000L for -1 unit, +2000L for +2 units
    val reason: String,
    val notes: String = "",
    val managerUserId: String
)

interface StockAdjustmentRepository {
    fun getAllAdjustments(): Flow<List<StockAdjustmentEntity>>
    fun getAdjustmentsForProduct(productId: String): Flow<List<StockAdjustmentEntity>>
    fun getAdjustmentsBetween(startTime: Long, endTime: Long): Flow<List<StockAdjustmentEntity>>
    suspend fun adjustStock(input: StockAdjustmentInput): Result<StockAdjustmentEntity>
}
