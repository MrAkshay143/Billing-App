package com.billing.app.core.util

import com.billing.app.domain.model.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WholesaleConverterTest {

    @Test
    fun testDozenToPcsConversion() {
        // 5 dozens of eggs, 12 per dozen, Rs. 72.00 per dozen
        val result = WholesaleConverter.convert(
            wholesaleUnit = "DOZEN",
            unitsPerPackage = 12,
            packageCount = 5.0,
            pricePerPackagePaise = 7200L,
            baseUnitCode = "PCS"
        )

        assertTrue(result.isSuccess)
        val conv = result.getOrThrow()
        assertEquals(60000L, conv.baseQuantityMilli) // 60 pcs
        assertEquals(600L, conv.unitPricePaise)       // Rs. 6.00 per pc
        assertEquals(36000L, conv.totalCostPaise)     // Rs. 360.00 total
        assertEquals("5 DOZEN x 12 = 60 PCS @ Rs. 6.00/PCS", conv.displayText)
    }

    @Test
    fun testCartonToPcsConversionWithRounding() {
        // 2 cartons of soap, 72 pcs per carton, Rs. 1440.00 per carton
        val result = WholesaleConverter.convert(
            wholesaleUnit = "CARTON",
            unitsPerPackage = 72,
            packageCount = 2.0,
            pricePerPackagePaise = 144000L,
            baseUnitCode = "PCS"
        )

        assertTrue(result.isSuccess)
        val conv = result.getOrThrow()
        assertEquals(144000L, conv.baseQuantityMilli) // 144 pcs
        assertEquals(2000L, conv.unitPricePaise)       // Rs. 20.00 per pc
        assertEquals(288000L, conv.totalCostPaise)     // Rs. 2,880.00 total
    }

    @Test
    fun testDiscreteUnitRejectsFractionalUnits() {
        // 2.5 cartons with 7 units per carton = 17.5 units -> Discrete PCS must reject
        val result = WholesaleConverter.convert(
            wholesaleUnit = "CARTON",
            unitsPerPackage = 7,
            packageCount = 2.5,
            pricePerPackagePaise = 7000L,
            baseUnitCode = "PCS"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.FractionalNotAllowed)
    }

    @Test
    fun testFractionalUnitAllowsDecimalConversion() {
        // 2.5 bags of sugar, 50 kg per bag, Rs. 2000 per bag
        val result = WholesaleConverter.convert(
            wholesaleUnit = "BAG",
            unitsPerPackage = 50,
            packageCount = 2.5,
            pricePerPackagePaise = 200000L,
            baseUnitCode = "KG"
        )

        assertTrue(result.isSuccess)
        val conv = result.getOrThrow()
        assertEquals(125000L, conv.baseQuantityMilli) // 125 kg
        assertEquals(4000L, conv.unitPricePaise)       // Rs. 40.00 per kg
    }
}
