package com.billing.app.core.util

import com.billing.app.domain.model.AppError
import java.util.Locale

data class WholesaleConversion(
    val wholesaleUnit: String,
    val unitsPerPackage: Int,
    val packageCount: Double,
    val pricePerPackagePaise: Long,
    val baseUnitCode: String,
    val baseQuantityMilli: Long,
    val unitPricePaise: Long,
    val totalCostPaise: Long,
    val displayText: String
)

object WholesaleConverter {

    val PRESET_PACKAGING_UNITS = listOf("DOZEN", "CARTON", "BOX", "BAG", "CUSTOM")

    /**
     * Converts wholesale purchase into base unit quantities and prices.
     * Enforces domain fractional rules if the base unit is discrete (PCS, PACK, BOX).
     */
    fun convert(
        wholesaleUnit: String,
        unitsPerPackage: Int,
        packageCount: Double,
        pricePerPackagePaise: Long,
        baseUnitCode: String
    ): Result<WholesaleConversion> {
        if (unitsPerPackage <= 0) {
            return Result.failure(IllegalArgumentException("Units per package must be greater than 0"))
        }
        if (packageCount <= 0.0) {
            return Result.failure(IllegalArgumentException("Package count must be greater than 0"))
        }
        if (pricePerPackagePaise < 0L) {
            return Result.failure(IllegalArgumentException("Price per package cannot be negative"))
        }

        val totalUnits = packageCount * unitsPerPackage
        val baseQuantityMilli = (totalUnits * 1000.0 + 0.5).toLong()

        // Verify fractional constraints on discrete units
        val unitDef = UnitRegistry.resolve(baseUnitCode)
        if (!unitDef.allowsFractional && baseQuantityMilli % 1000L != 0L) {
            return Result.failure(AppError.FractionalNotAllowed(unitDef.code))
        }

        // Unit buy price in base unit
        val unitPricePaise = (pricePerPackagePaise + (unitsPerPackage / 2)) / unitsPerPackage
        val totalCostPaise = (packageCount * pricePerPackagePaise + 0.5).toLong()

        val packageCountStr = if (packageCount % 1.0 == 0.0) packageCount.toInt().toString() else String.format(Locale.ROOT, "%.2f", packageCount)
        val unitPriceRupees = String.format(Locale.ROOT, "%.2f", unitPricePaise / 100.0)
        val totalUnitsStr = if (totalUnits % 1.0 == 0.0) totalUnits.toInt().toString() else String.format(Locale.ROOT, "%.3f", totalUnits)

        val displayText = "$packageCountStr $wholesaleUnit x $unitsPerPackage = $totalUnitsStr $baseUnitCode @ Rs. $unitPriceRupees/$baseUnitCode"

        return Result.success(
            WholesaleConversion(
                wholesaleUnit = wholesaleUnit.trim().uppercase(Locale.ROOT),
                unitsPerPackage = unitsPerPackage,
                packageCount = packageCount,
                pricePerPackagePaise = pricePerPackagePaise,
                baseUnitCode = unitDef.code,
                baseQuantityMilli = baseQuantityMilli,
                unitPricePaise = unitPricePaise,
                totalCostPaise = totalCostPaise,
                displayText = displayText
            )
        )
    }
}
