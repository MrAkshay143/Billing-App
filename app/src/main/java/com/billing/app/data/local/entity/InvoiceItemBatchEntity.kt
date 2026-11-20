package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoice_item_batches",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("invoiceItemId"),
        Index("batchId")
    ]
)
data class InvoiceItemBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceItemId: Long,
    val batchId: Long,
    val quantityMilli: Long,
    val costPaise: Long
)
