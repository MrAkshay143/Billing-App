package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["phone"], unique = true),
        Index(value = ["name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val gstin: String? = null,
    val openingBalancePaise: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val creditLimitPaise: Long = 0L
)
