package com.billing.app.domain.model

enum class UnitCategory {
    COUNT,      // PCS, PACK, BOX
    WEIGHT,     // KG
    VOLUME,     // L
    LENGTH      // MTR
}

data class QuantityStepChip(
    val deltaMilli: Long,     // e.g. 100L for 100g (milli-KG), 1000L for 1 pc
    val chipCode: String      // "100G", "250G", "500G", "1KG", "1PCS", "2PCS", "5PCS"
)

data class ProductUnit(
    val code: String,                 // Canonical: "KG", "L", "PCS", "PACK", "BOX", "MTR"
    val category: UnitCategory,
    val allowsFractional: Boolean,    // true for KG, L, MTR; false for PCS, PACK, BOX
    val defaultStepMilli: Long,       // e.g. 1000L for PCS, 500L for KG
    val quickChips: List<QuantityStepChip> = emptyList()
)
