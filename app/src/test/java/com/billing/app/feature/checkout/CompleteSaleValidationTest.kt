package com.billing.app.feature.checkout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompleteSaleValidationTest {

    @Test
    fun testDiscountCannotExceedSubtotal() {
        val subtotalPaise = 50000L // Rs. 500
        val attemptedDiscount = 60000L // Rs. 600

        val clampedDiscount = attemptedDiscount.coerceIn(0L, subtotalPaise)
        assertEquals(50000L, clampedDiscount)

        val grandTotal = (subtotalPaise - clampedDiscount).coerceAtLeast(0L)
        assertEquals(0L, grandTotal)
    }

    @Test
    fun testGstCalculation() {
        val subtotalPaise = 100000L // Rs. 1000
        val discountPaise = 10000L // Rs. 100
        val taxablePaise = subtotalPaise - discountPaise // Rs. 900
        val gstRatePercent = 18.0

        val taxPaise = (taxablePaise * (gstRatePercent / 100.0)).toLong()
        assertEquals(16200L, taxPaise) // Rs. 162.00

        val grandTotalPaise = taxablePaise + taxPaise
        assertEquals(106200L, grandTotalPaise) // Rs. 1062.00
    }

    @Test
    fun testCashPaymentDueAndChangeCalculation() {
        val grandTotalPaise = 105000L // Rs. 1050

        // Case 1: Exact payment
        val cashExact = 105000L
        val changeExact = (cashExact - grandTotalPaise).coerceAtLeast(0L)
        val dueExact = (grandTotalPaise - cashExact).coerceAtLeast(0L)
        assertEquals(0L, changeExact)
        assertEquals(0L, dueExact)

        // Case 2: Excess payment (Customer gives Rs. 2000)
        val cashExcess = 200000L
        val changeExcess = (cashExcess - grandTotalPaise).coerceAtLeast(0L)
        val dueExcess = (grandTotalPaise - cashExcess).coerceAtLeast(0L)
        assertEquals(95000L, changeExcess) // Rs. 950 change
        assertEquals(0L, dueExcess)

        // Case 3: Partial payment / credit (Customer gives Rs. 500)
        val cashPartial = 50000L
        val changePartial = (cashPartial - grandTotalPaise).coerceAtLeast(0L)
        val duePartial = (grandTotalPaise - cashPartial).coerceAtLeast(0L)
        assertEquals(0L, changePartial)
        assertEquals(55000L, duePartial) // Rs. 550 due balance
    }

    @Test
    fun testConditionalStockDecrementBoundary() {
        val currentStock = 5
        val requestedQty = 5
        val canDecrement = currentStock >= requestedQty
        assertTrue(canDecrement)

        val excessQty = 6
        val canDecrementExcess = currentStock >= excessQty
        assertTrue(!canDecrementExcess)
    }
}
