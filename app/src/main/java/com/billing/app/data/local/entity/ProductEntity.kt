package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.billing.app.core.util.Money
import java.util.UUID

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["category"]),
        Index(value = ["sku"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val barcode: String = "",
    val pricePaise: Long = 0L,
    val stockMilli: Long = 0L,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val sku: String = "",
    val category: String = "Grocery",
    val unit: String = "PCS",
    val purchasePricePaise: Long = 0L,
    val mrpPaise: Long = 0L,
    val minStockAlertMilli: Long = 5000L,
    val gstRateBp: Int = 0,
    val description: String = "",
    val trackBatch: Boolean = false,
    val hsnCode: String? = null,
    val isActive: Boolean = true
) {
    @get:androidx.room.Ignore
    val stock: Int get() = (stockMilli / 1000).toInt()

    @get:androidx.room.Ignore
    val gstRate: Int get() = gstRateBp / 100

    @get:androidx.room.Ignore
    val minStockAlert: Int get() = (minStockAlertMilli / 1000).toInt()

    @get:androidx.room.Ignore
    val mrp: Money get() = Money(mrpPaise)

    @Ignore
    constructor(
        id: String = UUID.randomUUID().toString(),
        name: String,
        barcode: String,
        pricePaise: Long,
        stock: Int = 0,
        imageUri: String? = null,
        createdAt: Long = System.currentTimeMillis(),
        sku: String = "",
        category: String = "Grocery",
        unit: String = "Piece",
        purchasePricePaise: Long = 0L,
        mrpPaise: Long = 0L,
        minStockAlert: Int = 5,
        gstRate: Int = 0,
        description: String = "",
        trackBatch: Boolean = false,
        hsnCode: String? = null,
        isActive: Boolean = true
    ) : this(
        id = id,
        name = name,
        barcode = barcode,
        pricePaise = pricePaise,
        stockMilli = stock.toLong() * 1000L,
        imageUri = imageUri,
        createdAt = createdAt,
        sku = sku,
        category = category,
        unit = if (unit == "Piece") "PCS" else unit,
        purchasePricePaise = purchasePricePaise,
        mrpPaise = mrpPaise,
        minStockAlertMilli = minStockAlert.toLong() * 1000L,
        gstRateBp = gstRate * 100,
        description = description,
        trackBatch = trackBatch,
        hsnCode = hsnCode,
        isActive = isActive
    )
}
