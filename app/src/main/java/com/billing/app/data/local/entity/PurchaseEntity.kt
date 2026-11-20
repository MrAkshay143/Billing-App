package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchases",
    indices = [
        Index(value = ["purchaseNumber"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["supplierId"]),
        Index(value = ["shiftId"])
    ]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val purchaseNumber: String,
    val supplierId: String? = null,
    val supplierName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val totalPaise: Long,
    val paymentStatus: String = "PAID",
    val shiftId: Long? = null,
    val createdByUserId: String? = null,
    val paidPaise: Long = 0L,
    val duePaise: Long = 0L,
    val status: String = "RECEIVED"
)
