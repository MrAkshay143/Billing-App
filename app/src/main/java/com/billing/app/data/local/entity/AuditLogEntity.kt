package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index("userId"),
        Index("timestamp"),
        Index("action")
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val action: String,
    val entityType: String = "",
    val entityId: String = "",
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
