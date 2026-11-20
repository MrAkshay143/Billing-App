package com.billing.app.domain.repository

import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.ShiftEntity
import com.billing.app.domain.model.ShiftZReport
import kotlinx.coroutines.flow.Flow

interface ShiftRepository {
    fun getActiveShiftFlow(): Flow<ShiftEntity?>
    suspend fun getActiveShift(): ShiftEntity?
    suspend fun openShift(userId: String, openingFloatPaise: Long, note: String? = null): Result<ShiftEntity>
    suspend fun closeShift(actualCashPaise: Long, note: String? = null): Result<ShiftEntity>
    suspend fun recordCashMovement(
        movementType: String,
        amountPaise: Long,
        referenceId: String? = null,
        reason: String? = null
    ): Result<Long>
    fun getMovementsForActiveShift(): Flow<List<CashMovementEntity>>
    suspend fun getExpectedCashForActiveShift(): Long
    suspend fun generateZReport(shiftId: Long): Result<ShiftZReport>
}
