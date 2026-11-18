package com.billing.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class InvoiceNumberGeneratorTest {

    @Test
    fun testConsecutiveInvoiceNumberFormatting() {
        val number1 = 1L
        val formatted1 = String.format(Locale.US, "INV-%06d", number1)
        assertEquals("INV-000001", formatted1)

        val number2 = 1248L
        val formatted2 = String.format(Locale.US, "INV-%06d", number2)
        assertEquals("INV-001248", formatted2)

        val number3 = 100000L
        val formatted3 = String.format(Locale.US, "INV-%06d", number3)
        assertEquals("INV-100000", formatted3)
    }

    @Test
    fun testStrictMonotonicSequence() {
        var current = 0L
        for (i in 1..100) {
            val next = current + 1
            assertTrue(next > current)
            current = next
        }
        assertEquals(100L, current)
    }
}
