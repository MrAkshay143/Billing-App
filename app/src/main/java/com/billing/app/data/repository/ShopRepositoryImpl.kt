package com.billing.app.data.repository

import com.billing.app.data.local.dao.ShopDao
import com.billing.app.data.local.entity.ShopEntity
import com.billing.app.domain.model.ShopProfile
import com.billing.app.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShopRepositoryImpl(
    private val shopDao: ShopDao
) : ShopRepository {

    override fun getShopProfile(): Flow<ShopProfile> {
        return shopDao.getShopProfile().map { entity ->
            entity?.toDomain() ?: ShopProfile()
        }
    }

    override suspend fun getShopProfileSync(): ShopProfile {
        return shopDao.getShopProfileSync()?.toDomain() ?: ShopProfile()
    }

    override suspend fun updateShopProfile(profile: ShopProfile): Result<Unit> {
        return try {
            shopDao.insertOrUpdate(profile.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ShopEntity.toDomain(): ShopProfile {
        return ShopProfile(
            id = id,
            name = name,
            addressLine1 = addressLine1,
            addressLine2 = addressLine2,
            phoneNumber = phoneNumber,
            upiId = upiId,
            footerText = footerText
        )
    }

    private fun ShopProfile.toEntity(): ShopEntity {
        return ShopEntity(
            id = id,
            name = name,
            addressLine1 = addressLine1,
            addressLine2 = addressLine2,
            phoneNumber = phoneNumber,
            upiId = upiId,
            footerText = footerText
        )
    }
}
