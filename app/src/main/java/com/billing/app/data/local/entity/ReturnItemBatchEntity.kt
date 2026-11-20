package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "return_item_batches",
    foreignKeys = [
        ForeignKey(
            entity = ReturnItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["returnItemId"],
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
        Index("returnItemId"),
        Index("batchId")
    ]
)
data class ReturnItemBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnItemId: Long,
    val batchId: Long,
    val quantityMilli: Long
)
