package com.billing.app.domain.model

import com.billing.app.core.util.Money

data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val total: Money get() = product.price * quantity
}
