package com.billing.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxEngineTest {

    @Test
    fun proRataDiscountDistribution_remainderAbsorbedByLastItem() {
        val items = listOf(
            TaxEngine.LineItem(lineTotalPaise = 1000L, gstRateBp = 1800),
            TaxEngine.LineItem(lineTotalPaise = 2000L, gstRateBp = 1800),
            TaxEngine.LineItem(lineTotalPaise = 3000L, gstRateBp = 1800)
        )
        val billDiscount = 1000L // Rs 10.00 discount on Rs 60.00 subtotal

        val result = TaxEngine.calculate(items = items, billDiscountPaise = billDiscount, isGstEnabled = true)

        assertEquals(6000L, result.subtotalPaise)
        assertEquals(1000L, result.billDiscountPaise)

        // Item 1: (1000 * 1000) / 6000 = 166 paise
        assertEquals(166L, result.lineCalculations[0].billDiscountSharePaise)
        assertEquals(834L, result.lineCalculations[0].netLineGrossPaise)

        // Item 2: (1000 * 2000) / 6000 = 333 paise
        assertEquals(333L, result.lineCalculations[1].billDiscountSharePaise)
        assertEquals(1667L, result.lineCalculations[1].netLineGrossPaise)

        // Item 3 (last): 1000 - (166 + 333) = 501 paise absorbs remainder
        assertEquals(501L, result.lineCalculations[2].billDiscountSharePaise)
        assertEquals(2499L, result.lineCalculations[2].netLineGrossPaise)

        // Verify total discount shares sum to exact bill discount
        val totalShares = result.lineCalculations.sumOf { it.billDiscountSharePaise }
        assertEquals(billDiscount, totalShares)

        // Verify net line gross sum equals subtotal - billDiscount
        val totalNetGross = result.lineCalculations.sumOf { it.netLineGrossPaise }
        assertEquals(result.subtotalPaise - billDiscount, totalNetGross)
    }

    @Test
    fun proRataDiscountDistribution_singleItemAbsorbsFullDiscount() {
        val items = listOf(
            TaxEngine.LineItem(lineTotalPaise = 5000L, gstRateBp = 1800)
        )
        val result = TaxEngine.calculate(items = items, billDiscountPaise = 500L, isGstEnabled = true)

        assertEquals(5000L, result.subtotalPaise)
        assertEquals(500L, result.billDiscountPaise)
        assertEquals(500L, result.lineCalculations[0].billDiscountSharePaise)
        assertEquals(4500L, result.lineCalculations[0].netLineGrossPaise)
    }

    @Test
    fun emptyItemsList_returnsSafeZeroes() {
        val result = TaxEngine.calculate(items = emptyList(), billDiscountPaise = 100L)
        assertEquals(0L, result.subtotalPaise)
        assertEquals(0L, result.totalTaxPaise)
        assertEquals(0L, result.grandTotalPaise)
        assertEquals(0L, result.roundOffPaise)
        assertTrue(result.lineCalculations.isEmpty())
    }

    @Test
    fun gstExtraction_zeroPercentGst() {
        val items = listOf(
            TaxEngine.LineItem(lineTotalPaise = 10000L, gstRateBp = 0)
        )
        val result = TaxEngine.calculate(items = items, billDiscountPaise = 0L, isGstEnabled = true)

        val line = result.lineCalculations.first()
        assertEquals(10000L, line.taxableValuePaise)
        assertEquals(0L, line.taxPaise)
        assertEquals(0L, line.cgstPaise)
        assertEquals(0L, line.sgstPaise)
    }

    @Test
    fun gstExtraction_fivePercentGst() {
        // Rs 105.00 display price with 5% GST (500 bps)
        val items = listOf(
            TaxEngine.LineItem(lineTotalPaise = 10500L, gstRateBp = 500)
        )
        val result = TaxEngine.calculate(items = items, billDiscountPaise = 0L, isGstEnabled = true)

        val line = result.lineCalculations.first()
        assertEquals(10000L, line.taxableValuePaise) // Taxable: Rs 100.00
        assertEquals(500L, line.taxPaise)           // Tax: Rs 5.00
        assertEquals(250L, line.cgstPaise)          // CGST: Rs 2.50
        assertEquals(250L, line.sgstPaise)          // SGST: Rs 2.50
        assertEquals(line.taxPaise, line.cgstPaise + line.sgstPaise)

        // Tax-inclusive pricing: Grand total must NOT add tax again (no double-taxation)
        assertEquals(10500L, result.subtotalPaise)
        assertEquals(500L, result.totalTaxPaise)
        assertEquals(10500L, result.grandTotalPaise)
        assertEquals(0L, result.roundOffPaise)
    }

    @Test
    fun gstExtraction_eighteenPercentGst() {
        // Rs 118.00 display price with 18% GST (1800 bps)
        val items = listOf(
            TaxEngine.LineItem(lineTotalPaise = 11800L, gstRateBp = 1800)
        )
        val result = TaxEngine.calculate(items = items, billDiscountPaise = 0L, isGstEnabled = true)

        val line = result.lineCalculations.first()
        assertEquals(10000L, line.taxableValuePaise) // Taxable: Rs 100.00
        assertEquals(1800L, line.taxPaise)          // Tax: Rs 18.00
        assertEquals(900L, line.cgstPaise)          // CGST: Rs 9.00
        assertEquals(900L, line.sgstPaise)          // SGST: Rs 9.00
        assertEquals(line.taxPaise, line.cgstPaise + line.sgstPaise)

        // Tax-inclusive pricing: Grand total must equal shelf price
        assertEquals(11800L, result.subtotalPaise)
        assertEquals(1800L, result.totalTaxPaise)
        assertEquals(11800L, result.grandTotalPaise)
        assertEquals(0L, result.roundOffPaise)
    }

    @Test
    fun gstExtraction_fortyPercentGst() {
        // Rs 140.00 display price with 40% GST (4000 bps)
        val items = listOf(
            TaxEngine.LineItem(lineTotalPaise = 14000L, gstRateBp = 4000)
        )
        val result = TaxEngine.calculate(items = items, billDiscountPaise = 0L, isGstEnabled = true)

        val line = result.lineCalculations.first()
        assertEquals(10000L, line.taxableValuePaise) // Taxable: Rs 100.00
        assertEquals(4000L, line.taxPaise)          // Tax: Rs 40.00
        assertEquals(2000L, line.cgstPaise)         // CGST: Rs 20.00
        assertEquals(2000L, line.sgstPaise)         // SGST: Rs 20.00
        assertEquals(line.taxPaise, line.cgstPaise + line.sgstPaise)

        // Tax-inclusive pricing: Grand total must equal shelf price
        assertEquals(14000L, result.subtotalPaise)
        assertEquals(4000L, result.totalTaxPaise)
        assertEquals(14000L, result.grandTotalPaise)
        assertEquals(0L, result.roundOffPaise)
    }

    @Test
    fun taxInclusive_retailItems_grandTotalEqualsSubtotalMinusDiscount_taxItemized() {
        // Scenario: 3 retail products with GST 0%, 5%, 18%
        // MRP includes GST.
        val items = listOf(
            TaxEngine.LineItem(lineTotalPaise = 5000L, gstRateBp = 0),     // Rs 50.00 exempt
            TaxEngine.LineItem(lineTotalPaise = 10500L, gstRateBp = 500),  // Rs 105.00 (Rs 100 taxable + Rs 5 GST)
            TaxEngine.LineItem(lineTotalPaise = 11800L, gstRateBp = 1800)  // Rs 118.00 (Rs 100 taxable + Rs 18 GST)
        )
        // Subtotal = 5000 + 10500 + 11800 = 27300 paise (Rs 273.00)
        // Bill discount = Rs 23.00 (2300 paise)
        val discountPaise = 2300L
        val result = TaxEngine.calculate(items = items, billDiscountPaise = discountPaise, isGstEnabled = true)

        assertEquals(27300L, result.subtotalPaise)
        assertEquals(2300L, result.billDiscountPaise)

        // Unrounded total must be exactly subtotal - discount (27300 - 2300 = 25000 paise = Rs 250.00)
        assertEquals(25000L, result.unroundedTotalPaise)
        assertEquals(25000L, result.grandTotalPaise)
        assertEquals(0L, result.roundOffPaise)

        // Tax must be itemized from netLineGross without inflating grand total
        assertTrue(result.totalTaxPaise > 0L)
        val sumTaxable = result.lineCalculations.sumOf { it.taxableValuePaise }
        val sumTax = result.lineCalculations.sumOf { it.taxPaise }
        val sumNetGross = result.lineCalculations.sumOf { it.netLineGrossPaise }

        // Taxable + Tax must equal Net Line Gross for each item
        for (line in result.lineCalculations) {
            assertEquals(line.netLineGrossPaise, line.taxableValuePaise + line.taxPaise)
            assertEquals(line.taxPaise, line.cgstPaise + line.sgstPaise)
        }
        assertEquals(sumNetGross, sumTaxable + sumTax)
        assertEquals(27300L - 2300L, sumNetGross)
    }

    @Test
    fun gstExtraction_disabledOrComposition_exemptsTax() {
        val items = listOf(
            TaxEngine.LineItem(lineTotalPaise = 11800L, gstRateBp = 1800)
        )
        val result = TaxEngine.calculate(items = items, billDiscountPaise = 0L, isGstEnabled = false)

        val line = result.lineCalculations.first()
        assertEquals(11800L, line.taxableValuePaise)
        assertEquals(0L, line.taxPaise)
        assertEquals(0L, line.cgstPaise)
        assertEquals(0L, line.sgstPaise)
    }

    @Test
    fun cgstSgstBalancing_onePaiseDifferenceAbsorbedBySgst() {
        // Gross = 100 paise (Rs 1.00) at 18% GST (1800 bps)
        // Taxable = (100 * 10000 + 5900) / 11800 = 85 paise
        // TotalTax = 100 - 85 = 15 paise (odd!)
        // CGST = 15 / 2 = 7 paise
        // SGST = 15 - 7 = 8 paise (absorbs the 1 paise odd remainder)
        val line = TaxEngine.extractLineTax(netLineGrossPaise = 100L, gstRateBp = 1800, isGstEnabled = true)

        assertEquals(85L, line.taxableValuePaise)
        assertEquals(15L, line.taxPaise)
        assertEquals(7L, line.cgstPaise)
        assertEquals(8L, line.sgstPaise)
        assertEquals(15L, line.cgstPaise + line.sgstPaise)
    }

    @Test
    fun nearestRupeeRoundOff_roundingScenarios() {
        // Case 1: unroundedTotal is 1040 (Rs 10.40) -> grandTotal is 1000 (Rs 10.00), roundOff is -40 paise
        val (grandTotal1, roundOff1) = TaxEngine.roundToNearestRupee(1040L)
        assertEquals(1000L, grandTotal1)
        assertEquals(-40L, roundOff1)

        // Case 2: unroundedTotal is 1060 (Rs 10.60) -> grandTotal is 1100 (Rs 11.00), roundOff is +40 paise
        val (grandTotal2, roundOff2) = TaxEngine.roundToNearestRupee(1060L)
        assertEquals(1100L, grandTotal2)
        assertEquals(40L, roundOff2)

        // Case 3: unroundedTotal is 1050 (Rs 10.50) -> rounds up to 1100 (Rs 11.00), roundOff is +50 paise
        val (grandTotal3, roundOff3) = TaxEngine.roundToNearestRupee(1050L)
        assertEquals(1100L, grandTotal3)
        assertEquals(50L, roundOff3)

        // Case 4: unroundedTotal is exactly 1000 (Rs 10.00) -> roundOff is 0 paise
        val (grandTotal4, roundOff4) = TaxEngine.roundToNearestRupee(1000L)
        assertEquals(1000L, grandTotal4)
        assertEquals(0L, roundOff4)

        // Case 5: unroundedTotal is 1001 (Rs 10.01) -> grandTotal is 1000, roundOff is -1 paise
        val (grandTotal5, roundOff5) = TaxEngine.roundToNearestRupee(1001L)
        assertEquals(1000L, grandTotal5)
        assertEquals(-1L, roundOff5)

        // Case 6: unroundedTotal is 1099 (Rs 10.99) -> grandTotal is 1100, roundOff is +1 paise
        val (grandTotal6, roundOff6) = TaxEngine.roundToNearestRupee(1099L)
        assertEquals(1100L, grandTotal6)
        assertEquals(1L, roundOff6)
    }
}
