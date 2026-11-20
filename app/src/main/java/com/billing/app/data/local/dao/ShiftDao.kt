package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.billing.app.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Upsert
    suspend fun upsertShift(shift: ShiftEntity)

    @Upsert
    suspend fun upsertAll(shifts: List<ShiftEntity>)

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: Long): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE userId = :userId AND status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getOpenShiftForUser(userId: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getCurrentOpenShift(): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    fun getCurrentOpenShiftFlow(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts ORDER BY openedAt DESC")
    fun getAllShifts(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE userId = :userId ORDER BY openedAt DESC")
    fun getShiftsForUser(userId: String): Flow<List<ShiftEntity>>

    @Query("""
        UPDATE shifts 
        SET closedAt = :closedAt, actualCashPaise = :actualCashPaise, 
            expectedCashPaise = :expectedCashPaise, variancePaise = :variancePaise, 
            note = :note, status = :status 
        WHERE id = :id
    """)
    suspend fun closeShift(
        id: Long,
        closedAt: Long,
        actualCashPaise: Long,
        expectedCashPaise: Long,
        variancePaise: Long,
        note: String?,
        status: String = "CLOSED"
    )

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteShift(id: Long)

    @Query("SELECT COUNT(*) FROM shifts")
    fun getShiftCount(): Flow<Int>
}
