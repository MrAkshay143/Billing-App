package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_batches",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("productId"),
        Index("expiryDate")
    ]
)
data class ProductBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: String,
    val batchNumber: String,
    val expiryDate: Long?,
    val purchasePricePaise: Long,
    val mrpPaise: Long,
    val initialStockMilli: Long,
    val currentStockMilli: Long,
    val createdAt: Long = System.currentTimeMillis()
)
