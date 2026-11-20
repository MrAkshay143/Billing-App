package com.billing.app.core.util

import java.util.Locale

object Quantity {

    fun getUnitSymbol(unitCode: String?, lang: String = "en"): String {
        val unit = UnitRegistry.resolve(unitCode)
        return com.billing.app.core.localization.getAppStrings(lang).unitSymbol(unit.code)
    }

    /**
     * Display format: Trims unnecessary trailing decimal zeros for a clean UI.
     * Always uses ASCII digits (Locale.ROOT).
     * e.g., 1500 milli KG -> "1.5 kg", 250 milli KG -> "0.25 kg", 3000 milli PCS -> "3 pc"
     */
    fun formatDisplay(quantityMilli: Long, unitCode: String?, lang: String = "en"): String {
        val unit = UnitRegistry.resolve(unitCode)
        val symbol = getUnitSymbol(unit.code, lang)

        if (!unit.allowsFractional) {
            val count = quantityMilli / 1000L
            return "$count $symbol"
        }

        val value = quantityMilli / 1000.0
        val formattedNumber = String.format(Locale.ROOT, "%.3f", value)
            .trimEnd('0')
            .trimEnd('.')
        return "$formattedNumber $symbol"
    }

    /**
     * Receipt format: Fixed 3 decimal places for fractional units, exact integer for discrete units.
     * Always uses ASCII digits (Locale.ROOT).
     * e.g., 1500 milli KG -> "1.500 kg", 250 milli KG -> "0.250 kg", 3000 milli PCS -> "3 pc"
     */
    fun formatReceipt(quantityMilli: Long, unitCode: String?, lang: String = "en"): String {
        val unit = UnitRegistry.resolve(unitCode)
        val symbol = getUnitSymbol(unit.code, lang)

        if (!unit.allowsFractional) {
            val count = quantityMilli / 1000L
            return "$count $symbol"
        }

        val value = quantityMilli / 1000.0
        val formattedNumber = String.format(Locale.ROOT, "%.3f", value)
        return "$formattedNumber $symbol"
    }

    /**
     * Formats price per unit for receipts/UI: e.g. "120.00/kg"
     */
    fun formatPricePerUnit(pricePaise: Long, unitCode: String?, lang: String = "en"): String {
        val symbol = getUnitSymbol(unitCode, lang)
        val priceStr = String.format(Locale.ROOT, "%.2f", pricePaise / 100.0)
        return "$priceStr/$symbol"
    }

    /**
     * Normalizes typed numeric text:
     * - Converts Bengali digits ('০'..'৯') to ASCII '0'..'9'
     * - Converts Devanagari digits ('०'..'९') to ASCII '0'..'9'
     * - Removes thousands separators/commas (e.g. "1,200" -> "1200", "1,00,000" -> "100000")
     * - Removes currency symbols ('₹', "Rs.", "Rs")
     */
    fun normalizeInputDigits(input: String): String {
        return buildString {
            for (ch in input.trim()) {
                when (ch) {
                    in '০'..'৯' -> append('0' + (ch - '০'))
                    in '०'..'९' -> append('0' + (ch - '०'))
                    ',', '₹' -> { /* strip grouping comma and rupee sign */ }
                    else -> append(ch)
                }
            }
        }.replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .trim()
    }
}
