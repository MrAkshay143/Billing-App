package com.billing.app.core.util

import com.billing.app.domain.model.ProductUnit
import com.billing.app.domain.model.QuantityStepChip
import com.billing.app.domain.model.UnitCategory

data class UnitConversionResult(
    val canonicalCode: String,
    val quantityMultiplier: Double,
    val priceMultiplier: Double,
    val isLossyOrUnknown: Boolean = false,
    val errorMessage: String? = null
)

object UnitRegistry {
    val PCS = ProductUnit(
        code = "PCS",
        category = UnitCategory.COUNT,
        allowsFractional = false,
        defaultStepMilli = 1000L,
        quickChips = listOf(
            QuantityStepChip(1000L, "1PCS"),
            QuantityStepChip(2000L, "2PCS"),
            QuantityStepChip(5000L, "5PCS")
        )
    )

    val KG = ProductUnit(
        code = "KG",
        category = UnitCategory.WEIGHT,
        allowsFractional = true,
        defaultStepMilli = 500L,
        quickChips = listOf(
            QuantityStepChip(100L, "100G"),
            QuantityStepChip(250L, "250G"),
            QuantityStepChip(500L, "500G"),
            QuantityStepChip(1000L, "1KG")
        )
    )

    val L = ProductUnit(
        code = "L",
        category = UnitCategory.VOLUME,
        allowsFractional = true,
        defaultStepMilli = 500L,
        quickChips = listOf(
            QuantityStepChip(200L, "200ML"),
            QuantityStepChip(500L, "500ML"),
            QuantityStepChip(1000L, "1L")
        )
    )

    val PACK = ProductUnit(
        code = "PACK",
        category = UnitCategory.COUNT,
        allowsFractional = false,
        defaultStepMilli = 1000L,
        quickChips = listOf(
            QuantityStepChip(1000L, "1PACK"),
            QuantityStepChip(2000L, "2PACK"),
            QuantityStepChip(5000L, "5PACK")
        )
    )

    val BOX = ProductUnit(
        code = "BOX",
        category = UnitCategory.COUNT,
        allowsFractional = false,
        defaultStepMilli = 1000L,
        quickChips = listOf(
            QuantityStepChip(1000L, "1BOX"),
            QuantityStepChip(2000L, "2BOX"),
            QuantityStepChip(5000L, "5BOX")
        )
    )

    val MTR = ProductUnit(
        code = "MTR",
        category = UnitCategory.LENGTH,
        allowsFractional = true,
        defaultStepMilli = 1000L,
        quickChips = listOf(
            QuantityStepChip(500L, "0.5MTR"),
            QuantityStepChip(1000L, "1MTR"),
            QuantityStepChip(2000L, "2MTR")
        )
    )

    /**
     * Storable 6 Base Units
     */
    val STANDARD_UNITS: List<ProductUnit> = listOf(PCS, KG, L, PACK, BOX, MTR)

    private val UNIT_MAP: Map<String, ProductUnit> = STANDARD_UNITS.associateBy { it.code }

    /**
     * Resolves standard unit by canonical code or fallback to PCS
     */
    fun resolve(rawCode: String?): ProductUnit {
        val clean = rawCode?.trim()?.uppercase() ?: return PCS
        return when (clean) {
            "PCS", "PC", "PIECE", "PIECES" -> PCS
            "KG", "KILOGRAM", "KGS" -> KG
            "L", "LTR", "LITER", "LITRE", "LITRES" -> L
            "PACK", "PKT", "PACKET", "PACKETS" -> PACK
            "BOX", "BOXES", "CARTON" -> BOX
            "MTR", "METER", "METRE", "M" -> MTR
            else -> UNIT_MAP[clean] ?: PCS
        }
    }

    /**
     * Resolves legacy unit strings with quantity & price conversion factors.
     * Flagged as lossyOrUnknown if unknown or lossy mapping to prevent silent corruption in CSV / Restore.
     */
    fun resolveWithConversion(rawCode: String?): UnitConversionResult {
        val clean = rawCode?.trim()?.uppercase()
        if (clean.isNullOrBlank()) {
            return UnitConversionResult("PCS", 1.0, 1.0)
        }
        return when (clean) {
            "PCS", "PC", "PIECE", "PIECES" -> UnitConversionResult("PCS", 1.0, 1.0)
            "KG", "KILOGRAM", "KGS" -> UnitConversionResult("KG", 1.0, 1.0)
            "G", "GM", "GRAM", "GRAMS" -> UnitConversionResult("KG", 0.001, 1000.0)
            "L", "LTR", "LITER", "LITRE", "LITRES" -> UnitConversionResult("L", 1.0, 1.0)
            "ML", "MILLILITER", "MILLILITRE" -> UnitConversionResult("L", 0.001, 1000.0)
            "PACK", "PKT", "PACKET", "PACKETS" -> UnitConversionResult("PACK", 1.0, 1.0)
            "BOX", "BOXES", "CARTON" -> UnitConversionResult("BOX", 1.0, 1.0)
            "MTR", "METER", "METRE", "M" -> UnitConversionResult("MTR", 1.0, 1.0)
            "DOZEN", "DZ" -> UnitConversionResult("PCS", 12.0, 1.0 / 12.0)
            else -> UnitConversionResult(
                canonicalCode = clean,
                quantityMultiplier = 1.0,
                priceMultiplier = 1.0,
                isLossyOrUnknown = true,
                errorMessage = "Unsupported or unknown unit: '$rawCode'"
            )
        }
    }
}
