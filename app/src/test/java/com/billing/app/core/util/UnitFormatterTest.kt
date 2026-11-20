package com.billing.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitFormatterTest {

    @Test
    fun testQuantityFormatDisplay() {
        // 1500 milli KG -> "1.5 kg" (trailing zeros trimmed)
        assertEquals("1.5 kg", Quantity.formatDisplay(1500L, "KG", "en"))
        assertEquals("1.5 কেজি", Quantity.formatDisplay(1500L, "KG", "bn"))
        assertEquals("1.5 किग्रा", Quantity.formatDisplay(1500L, "KG", "hi"))

        // 250 milli KG -> "0.25 kg"
        assertEquals("0.25 kg", Quantity.formatDisplay(250L, "KG", "en"))

        // 3000 milli PCS -> "3 pc" (discrete unit)
        assertEquals("3 pc", Quantity.formatDisplay(3000L, "PCS", "en"))
        assertEquals("3 পিস", Quantity.formatDisplay(3000L, "PCS", "bn"))

        // 2500 milli MTR -> "2.5 m" (meter is fractional)
        assertEquals("2.5 m", Quantity.formatDisplay(2500L, "MTR", "en"))
        assertEquals("2.5 মিটার", Quantity.formatDisplay(2500L, "MTR", "bn"))
    }

    @Test
    fun testQuantityFormatReceipt() {
        // Receipt requires fixed 3 decimal places for fractional units
        assertEquals("1.500 kg", Quantity.formatReceipt(1500L, "KG", "en"))
        assertEquals("0.250 kg", Quantity.formatReceipt(250L, "KG", "en"))
        assertEquals("2.500 m", Quantity.formatReceipt(2500L, "MTR", "en"))

        // Discrete units remain exact integer count
        assertEquals("3 pc", Quantity.formatReceipt(3000L, "PCS", "en"))
        assertEquals("1 pack", Quantity.formatReceipt(1000L, "PACK", "en"))
        assertEquals("5 box", Quantity.formatReceipt(5000L, "BOX", "en"))
    }

    @Test
    fun testUnitRegistryResolution() {
        val kg = UnitRegistry.resolve("kg")
        assertEquals("KG", kg.code)
        assertTrue(kg.allowsFractional)

        val pcs = UnitRegistry.resolve("Piece")
        assertEquals("PCS", pcs.code)
        assertFalse(pcs.allowsFractional)

        val mtr = UnitRegistry.resolve("meter")
        assertEquals("MTR", mtr.code)
        assertTrue(mtr.allowsFractional)

        val pack = UnitRegistry.resolve("PKT")
        assertEquals("PACK", pack.code)
        assertFalse(pack.allowsFractional)

        val box = UnitRegistry.resolve("Carton")
        assertEquals("BOX", box.code)
        assertFalse(box.allowsFractional)
    }

    @Test
    fun testLegacyUnitConversionNormalizer() {
        // Gram -> KG (qty * 0.001, price * 1000.0)
        val gConversion = UnitRegistry.resolveWithConversion("g")
        assertEquals("KG", gConversion.canonicalCode)
        assertEquals(0.001, gConversion.quantityMultiplier, 0.000001)
        assertEquals(1000.0, gConversion.priceMultiplier, 0.000001)
        assertFalse(gConversion.isLossyOrUnknown)

        // Milliliter -> L (qty * 0.001, price * 1000.0)
        val mlConversion = UnitRegistry.resolveWithConversion("ml")
        assertEquals("L", mlConversion.canonicalCode)
        assertEquals(0.001, mlConversion.quantityMultiplier, 0.000001)
        assertEquals(1000.0, mlConversion.priceMultiplier, 0.000001)
        assertFalse(mlConversion.isLossyOrUnknown)

        // Dozen -> PCS (qty * 12.0, price / 12.0)
        val dozenConversion = UnitRegistry.resolveWithConversion("dozen")
        assertEquals("PCS", dozenConversion.canonicalCode)
        assertEquals(12.0, dozenConversion.quantityMultiplier, 0.000001)
        assertEquals(1.0 / 12.0, dozenConversion.priceMultiplier, 0.000001)
        assertFalse(dozenConversion.isLossyOrUnknown)

        // Unknown/lossy unit is flagged to prevent silent corruption
        val unknown = UnitRegistry.resolveWithConversion("xyz_unknown")
        assertTrue(unknown.isLossyOrUnknown)
        assertEquals("Unsupported or unknown unit: 'xyz_unknown'", unknown.errorMessage)
    }

    @Test
    fun testNormalizeInputDigits() {
        // Bengali digits
        assertEquals("120.50", Quantity.normalizeInputDigits("১২০.৫০"))

        // Devanagari digits
        assertEquals("125.75", Quantity.normalizeInputDigits("१२५.७५"))

        // Thousands and Indian lakh commas stripped
        assertEquals("1200", Quantity.normalizeInputDigits("1,200"))
        assertEquals("100000", Quantity.normalizeInputDigits("1,00,000"))

        // Currency symbol stripped
        assertEquals("1500.00", Quantity.normalizeInputDigits("₹ 1,500.00"))
        assertEquals("350", Quantity.normalizeInputDigits("Rs. 350"))
    }
}
