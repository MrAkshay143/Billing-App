package com.billing.app.domain.model

data class ShopProfile(
    val id: Int = 1,
    val name: String = "My Store",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val phoneNumber: String = "",
    val upiId: String = "",
    val footerText: String = "Thank you! Visit again."
)
