package com.billing.app.domain.repository

import com.billing.app.data.local.entity.CustomUnitEntity
import kotlinx.coroutines.flow.Flow

interface CustomUnitRepository {
    fun getAllCustomUnits(): Flow<List<CustomUnitEntity>>
    suspend fun getAllCustomUnitsSync(): List<CustomUnitEntity>
    suspend fun getByCode(code: String): CustomUnitEntity?
    suspend fun addCustomUnit(
        code: String,
        name: String,
        baseUnitCode: String,
        conversionFactor: Double,
        allowsFractional: Boolean = false
    ): Result<Long>
    suspend fun deleteCustomUnit(id: Long)
}
