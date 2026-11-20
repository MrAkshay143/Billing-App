package com.billing.app.domain.model

import com.billing.app.core.util.Money

data class CartItem(
    val product: Product,
    val quantityMilli: Long = 1000L,
    val customPricePaise: Long? = null,
    val discountPaise: Long = 0L,
    val isCustomItem: Boolean = false,
    val customName: String? = null,
    val isCostUnknown: Boolean = false
) {
    val effectiveUnitPricePaise: Long get() = customPricePaise ?: product.price.paise
    val effectiveUnitPrice: Money get() = Money(effectiveUnitPricePaise)
    val grossLinePaise: Long get() = (effectiveUnitPricePaise * quantityMilli + 500L) / 1000L
    val netLinePaise: Long get() = (grossLinePaise - discountPaise).coerceAtLeast(0L)
    val total: Money get() = Money(netLinePaise)
    val quantity: Int get() = (quantityMilli / 1000L).toInt()
    val displayName: String get() = if (isCustomItem && !customName.isNullOrBlank()) customName else product.name

    constructor(product: Product, quantity: Int) : this(
        product = product,
        quantityMilli = quantity.toLong() * 1000L
    )
}
