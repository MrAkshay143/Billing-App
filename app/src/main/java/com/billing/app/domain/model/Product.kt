package com.billing.app.domain.model

import com.billing.app.core.util.Money
import com.billing.app.core.util.Quantity

data class Product(
    val id: String,
    val name: String,
    val barcode: String,
    val price: Money,
    val stockMilli: Long = 0L,
    val imageUri: String? = null,
    val sku: String = "",
    val category: String = "Grocery",
    val unit: String = "PCS",
    val purchasePrice: Money = Money.ZERO,
    val mrp: Money = Money.ZERO,
    val minStockAlertMilli: Long = 5000L,
    val gstRateBp: Int = 0,
    val description: String = "",
    val trackBatch: Boolean = false,
    val hsnCode: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val stock: Int get() = (stockMilli / 1000L).toInt()
    val minStockAlert: Int get() = (minStockAlertMilli / 1000L).toInt()
    val gstRate: Int get() = gstRateBp / 100
    fun formatStock(lang: String = "en"): String = Quantity.formatDisplay(stockMilli, unit, lang)

    constructor(
        id: String,
        name: String,
        barcode: String,
        price: Money,
        stock: Int,
        imageUri: String? = null,
        sku: String = "",
        category: String = "Grocery",
        unit: String = "PCS",
        purchasePrice: Money = Money.ZERO,
        mrp: Money = Money.ZERO,
        minStockAlert: Int = 5,
        gstRate: Int = 0,
        description: String = "",
        trackBatch: Boolean = false,
        hsnCode: String? = null,
        isActive: Boolean = true,
        createdAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        name = name,
        barcode = barcode,
        price = price,
        stockMilli = stock.toLong() * 1000L,
        imageUri = imageUri,
        sku = sku,
        category = category,
        unit = unit,
        purchasePrice = purchasePrice,
        mrp = mrp,
        minStockAlertMilli = minStockAlert.toLong() * 1000L,
        gstRateBp = gstRate * 100,
        description = description,
        trackBatch = trackBatch,
        hsnCode = hsnCode,
        isActive = isActive,
        createdAt = createdAt
    )
}
