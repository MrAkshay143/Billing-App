package com.billing.app.data.repository

import com.billing.app.core.util.UnitRegistry
import com.billing.app.data.local.dao.CustomUnitDao
import com.billing.app.data.local.entity.CustomUnitEntity
import com.billing.app.domain.model.AppError
import com.billing.app.domain.repository.CustomUnitRepository
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class CustomUnitRepositoryImpl(
    private val customUnitDao: CustomUnitDao
) : CustomUnitRepository {

    override fun getAllCustomUnits(): Flow<List<CustomUnitEntity>> {
        return customUnitDao.getAllCustomUnits()
    }

    override suspend fun getAllCustomUnitsSync(): List<CustomUnitEntity> {
        return customUnitDao.getAllCustomUnitsSync()
    }

    override suspend fun getByCode(code: String): CustomUnitEntity? {
        return customUnitDao.getByCode(code.trim().uppercase(Locale.ROOT))
    }

    override suspend fun addCustomUnit(
        code: String,
        name: String,
        baseUnitCode: String,
        conversionFactor: Double,
        allowsFractional: Boolean
    ): Result<Long> {
        val cleanCode = code.trim().uppercase(Locale.ROOT)
        if (cleanCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Unit code cannot be empty"))
        }
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Unit name cannot be empty"))
        }
        if (conversionFactor <= 0.0) {
            return Result.failure(IllegalArgumentException("Conversion factor must be greater than zero"))
        }

        val baseDef = UnitRegistry.resolve(baseUnitCode)
        if (!UnitRegistry.STANDARD_UNITS.any { it.code == baseDef.code }) {
            return Result.failure(AppError.InvalidUnit(baseUnitCode))
        }

        // Check if standard unit code exists
        if (UnitRegistry.STANDARD_UNITS.any { it.code == cleanCode }) {
            return Result.failure(IllegalArgumentException("Unit code '$cleanCode' is already a standard unit"))
        }

        val existing = customUnitDao.getByCode(cleanCode)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Custom unit '$cleanCode' already exists"))
        }

        val entity = CustomUnitEntity(
            code = cleanCode,
            name = cleanName,
            baseUnitCode = baseDef.code,
            conversionFactor = conversionFactor,
            allowsFractional = if (!baseDef.allowsFractional) false else allowsFractional,
            createdAt = System.currentTimeMillis()
        )

        val id = customUnitDao.insert(entity)
        return Result.success(id)
    }

    override suspend fun deleteCustomUnit(id: Long) {
        customUnitDao.deleteById(id)
    }
}
