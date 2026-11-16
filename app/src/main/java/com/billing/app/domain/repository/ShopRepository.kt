package com.billing.app.domain.repository

import com.billing.app.domain.model.ShopProfile
import kotlinx.coroutines.flow.Flow

interface ShopRepository {
    fun getShopProfile(): Flow<ShopProfile>
    suspend fun getShopProfileSync(): ShopProfile
    suspend fun updateShopProfile(profile: ShopProfile): Result<Unit>
}
