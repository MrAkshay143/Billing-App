package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "users",
    indices = [
        Index("role"),
        Index("username", unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String,
    val displayName: String,
    val pinHash: String,
    val pinSalt: String,
    val role: String = "CASHIER",
    val isActive: Boolean = true,
    val failedAttempts: Int = 0,
    val lockedUntil: Long? = null,
    val mustSetPin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
