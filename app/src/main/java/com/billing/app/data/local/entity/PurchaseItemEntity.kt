package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["purchaseId"])]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val purchaseId: Long,
    val productId: String,
    val productName: String,
    val quantityMilli: Long = 1000L,
    val unitPricePaise: Long,
    val lineTotalPaise: Long,
    val unit: String = "PCS",
    val gstRateBp: Int = 0,
    val batchNumber: String? = null,
    val expiryDate: Long? = null,
    val mrpPaise: Long = 0L,
    val batchId: Long? = null
) {
    @get:androidx.room.Ignore
    val quantity: Int get() = (quantityMilli / 1000).toInt()

    @Ignore
    constructor(
        id: Long = 0L,
        purchaseId: Long,
        productId: String,
        productName: String,
        quantity: Int,
        unitPricePaise: Long,
        lineTotalPaise: Long
    ) : this(
        id = id,
        purchaseId = purchaseId,
        productId = productId,
        productName = productName,
        quantityMilli = quantity.toLong() * 1000L,
        unitPricePaise = unitPricePaise,
        lineTotalPaise = lineTotalPaise
    )
}
