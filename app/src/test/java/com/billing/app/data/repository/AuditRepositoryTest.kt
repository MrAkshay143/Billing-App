package com.billing.app.data.repository

import com.billing.app.data.local.dao.AuditLogDao
import com.billing.app.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuditRepositoryTest {

    private lateinit var fakeAuditLogDao: FakeAuditLogDao
    private lateinit var auditRepository: AuditRepositoryImpl

    @Before
    fun setup() {
        fakeAuditLogDao = FakeAuditLogDao()
        auditRepository = AuditRepositoryImpl(fakeAuditLogDao)
    }

    @Test
    fun testRecordLogAndGetRecentLogs() = runBlocking {
        val id1 = auditRepository.recordLog(
            userId = "admin_1",
            action = "PRICE_OVERRIDE",
            entityType = "PRODUCT",
            entityId = "prod_123",
            details = "Overrode price from 1000 to 800"
        )

        assertTrue(id1 > 0)
        assertEquals(1, fakeAuditLogDao.logs.size)

        val log = fakeAuditLogDao.logs.first()
        assertEquals("admin_1", log.userId)
        assertEquals("PRICE_OVERRIDE", log.action)
        assertEquals("PRODUCT", log.entityType)
        assertEquals("prod_123", log.entityId)
        assertEquals("Overrode price from 1000 to 800", log.details)
        assertTrue(log.timestamp > 0)

        // Record a second log
        val id2 = auditRepository.recordLog(
            userId = "cashier_2",
            action = "DISCOUNT_APPLIED",
            entityType = "INVOICE",
            entityId = "inv_456",
            details = "Bill discount 50 applied"
        )
        assertTrue(id2 > id1)

        val recentLogs = auditRepository.getRecentLogs(limit = 1).first()
        assertEquals(1, recentLogs.size)
        assertEquals("DISCOUNT_APPLIED", recentLogs.first().action)

        val allRecentLogs = auditRepository.getRecentLogs(limit = 10).first()
        assertEquals(2, allRecentLogs.size)
    }

    private class FakeAuditLogDao : AuditLogDao {
        val logs = mutableListOf<AuditLogEntity>()
        private var nextId = 1L
        private val logsFlow = MutableStateFlow<List<AuditLogEntity>>(emptyList())

        private fun updateFlow() {
            logsFlow.value = logs.sortedByDescending { it.timestamp }.toList()
        }

        override suspend fun insertAuditLog(log: AuditLogEntity): Long {
            val assignedId = nextId++
            val entry = log.copy(id = assignedId)
            logs.add(0, entry)
            updateFlow()
            return assignedId
        }

        override suspend fun upsertAuditLog(log: AuditLogEntity) {
            insertAuditLog(log)
        }

        override suspend fun upsertAll(logs: List<AuditLogEntity>) {
            logs.forEach { insertAuditLog(it) }
        }

        override fun getAllAuditLogs(): Flow<List<AuditLogEntity>> = logsFlow

        override fun getRecentLogs(limit: Int): Flow<List<AuditLogEntity>> =
            logsFlow.map { it.take(limit) }

        override suspend fun getAllAuditLogList(): List<AuditLogEntity> = logs.toList()

        override fun getAuditLogsForUser(userId: String): Flow<List<AuditLogEntity>> =
            logsFlow.map { list -> list.filter { it.userId == userId } }

        override fun getAuditLogsForEntity(entityType: String, entityId: String): Flow<List<AuditLogEntity>> =
            logsFlow.map { list -> list.filter { it.entityType == entityType && it.entityId == entityId } }

        override fun getAuditLogsBetween(startTime: Long, endTime: Long): Flow<List<AuditLogEntity>> =
            logsFlow.map { list -> list.filter { it.timestamp in startTime..endTime } }

        override suspend fun getAuditLogById(id: Long): AuditLogEntity? =
            logs.find { it.id == id }

        override suspend fun deleteAuditLog(id: Long) {
            logs.removeAll { it.id == id }
            updateFlow()
        }

        override suspend fun deleteAuditLogsOlderThan(cutoffTimestamp: Long) {
            logs.removeAll { it.timestamp < cutoffTimestamp }
            updateFlow()
        }
    }
}
