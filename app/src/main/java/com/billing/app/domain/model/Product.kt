package com.billing.app.domain.model

import com.billing.app.core.util.Money

data class Product(
    val id: String,
    val name: String,
    val barcode: String,
    val price: Money,
    val stock: Int = 0,
    val imageUri: String? = null
)
