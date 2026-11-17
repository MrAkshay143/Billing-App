package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["category"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val amountPaise: Long,
    val date: Long = System.currentTimeMillis(),
    val paymentMethod: String = "CASH",
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
