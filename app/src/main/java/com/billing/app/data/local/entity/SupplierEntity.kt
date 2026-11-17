package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["phone"]),
        Index(value = ["name"])
    ]
)
data class SupplierEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val contactPerson: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val gstin: String? = null,
    val openingBalancePaise: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
