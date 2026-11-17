package com.billing.app.domain.model

import com.billing.app.core.util.Money

data class Product(
    val id: String,
    val name: String,
    val barcode: String,
    val price: Money,
    val stock: Int = 0,
    val imageUri: String? = null,
    val sku: String = "",
    val category: String = "Grocery",
    val unit: String = "Piece",
    val purchasePrice: Money = Money.ZERO,
    val mrp: Money = Money.ZERO,
    val minStockAlert: Int = 5,
    val gstRate: Int = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
