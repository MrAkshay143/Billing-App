package com.billing.app.domain.repository

import com.billing.app.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

interface AuditRepository {
    suspend fun recordLog(
        userId: String,
        action: String,
        entityType: String,
        entityId: String,
        details: String
    ): Long

    suspend fun log(
        action: String,
        details: String,
        userId: String = "SYSTEM",
        entityType: String = "PRODUCT",
        entityId: String = ""
    ): Long = recordLog(userId, action, entityType, entityId, details)

    fun getRecentLogs(limit: Int = 100): Flow<List<AuditLogEntity>>
}
