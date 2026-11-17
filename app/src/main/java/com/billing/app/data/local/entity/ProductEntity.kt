package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
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
    val barcode: String,
    val pricePaise: Long,
    val stock: Int = 0,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val sku: String = "",
    val category: String = "Grocery",
    val unit: String = "Piece",
    val purchasePricePaise: Long = 0L,
    val mrpPaise: Long = 0L,
    val minStockAlert: Int = 5,
    val gstRate: Int = 0,
    val description: String = ""
)
