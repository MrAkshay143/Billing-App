package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "returns",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("invoiceId"),
        Index("shiftId"),
        Index(value = ["returnNumber"], unique = true)
    ]
)
data class ReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnNumber: String,
    val invoiceId: Long,
    val shiftId: Long,
    val totalRefundPaise: Long,
    val cashRefundPaise: Long = 0L,
    val upiRefundPaise: Long = 0L,
    val khataCreditPaise: Long = 0L,
    val reason: String,
    val processedByUserId: String,
    val createdAt: Long = System.currentTimeMillis()
)
