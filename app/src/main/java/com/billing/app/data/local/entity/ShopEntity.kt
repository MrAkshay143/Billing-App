package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_profile")
data class ShopEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "My Store",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val phoneNumber: String = "",
    val upiId: String = "",
    val footerText: String = "Thank you! Visit again."
)
