package com.billing.app.data.repository

import com.billing.app.data.local.dao.AuditLogDao
import com.billing.app.data.local.entity.AuditLogEntity
import com.billing.app.domain.repository.AuditRepository
import kotlinx.coroutines.flow.Flow

class AuditRepositoryImpl(
    private val auditLogDao: AuditLogDao
) : AuditRepository {

    override suspend fun recordLog(
        userId: String,
        action: String,
        entityType: String,
        entityId: String,
        details: String
    ): Long {
        val log = AuditLogEntity(
            userId = userId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        return auditLogDao.insertAuditLog(log)
    }

    override fun getRecentLogs(limit: Int): Flow<List<AuditLogEntity>> {
        val effectiveLimit = if (limit > 0) limit else 100
        return auditLogDao.getRecentLogs(effectiveLimit)
    }
}
