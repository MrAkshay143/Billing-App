package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.billing.app.data.local.entity.CashMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashMovementDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCashMovement(movement: CashMovementEntity): Long

    @Upsert
    suspend fun upsertCashMovement(movement: CashMovementEntity)

    @Upsert
    suspend fun upsertAll(movements: List<CashMovementEntity>)

    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY createdAt ASC")
    fun getCashMovementsForShift(shiftId: Long): Flow<List<CashMovementEntity>>

    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY createdAt ASC")
    suspend fun getCashMovementListForShift(shiftId: Long): List<CashMovementEntity>

    @Query("SELECT * FROM cash_movements ORDER BY createdAt DESC")
    fun getAllCashMovements(): Flow<List<CashMovementEntity>>

    @Query("SELECT * FROM cash_movements WHERE id = :id LIMIT 1")
    suspend fun getCashMovementById(id: Long): CashMovementEntity?

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM cash_movements WHERE shiftId = :shiftId AND movementType = :movementType")
    fun getTotalByMovementType(shiftId: Long, movementType: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM cash_movements WHERE shiftId = :shiftId AND (reason != 'Opening Float' OR reason IS NULL)")
    suspend fun getNetCashForShift(shiftId: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM cash_movements WHERE shiftId = :shiftId AND (reason != 'Opening Float' OR reason IS NULL)")
    fun getNetCashForShiftFlow(shiftId: Long): Flow<Long>

    @Query("DELETE FROM cash_movements WHERE id = :id")
    suspend fun deleteCashMovement(id: Long)

    @Query("DELETE FROM cash_movements WHERE shiftId = :shiftId")
    suspend fun deleteCashMovementsForShift(shiftId: Long)
}
