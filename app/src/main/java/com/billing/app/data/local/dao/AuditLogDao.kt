package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.billing.app.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    suspend fun insertLog(log: AuditLogEntity): Long = insertAuditLog(log)

    @Upsert
    suspend fun upsertAuditLog(log: AuditLogEntity)

    @Upsert
    suspend fun upsertAll(logs: List<AuditLogEntity>)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllAuditLogList(): List<AuditLogEntity>

    @Query("SELECT * FROM audit_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAuditLogsForUser(userId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE entityType = :entityType AND entityId = :entityId ORDER BY timestamp DESC")
    fun getAuditLogsForEntity(entityType: String, entityId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getAuditLogsBetween(startTime: Long, endTime: Long): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE id = :id LIMIT 1")
    suspend fun getAuditLogById(id: Long): AuditLogEntity?

    @Query("DELETE FROM audit_logs WHERE id = :id")
    suspend fun deleteAuditLog(id: Long)

    @Query("DELETE FROM audit_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteAuditLogsOlderThan(cutoffTimestamp: Long)
}
