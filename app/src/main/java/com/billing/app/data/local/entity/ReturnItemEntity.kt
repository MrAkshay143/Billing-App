package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "return_items",
    foreignKeys = [
        ForeignKey(
            entity = ReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["returnId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InvoiceItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceItemId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("returnId"),
        Index("invoiceItemId")
    ]
)
data class ReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnId: Long,
    val invoiceItemId: Long,
    val quantityMilli: Long,
    val unit: String = "PCS",
    val refundAmountPaise: Long,
    val taxableRefundPaise: Long = 0L,
    val taxRefundPaise: Long = 0L,
    val restock: Boolean = true
)
