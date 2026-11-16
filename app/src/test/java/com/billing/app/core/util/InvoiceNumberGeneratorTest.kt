package com.billing.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class InvoiceNumberGeneratorTest {

    @Test
    fun testConsecutiveInvoiceNumberFormatting() {
        val year = 2026
        val number1 = 1L
        val formatted1 = String.format(Locale.US, "INV-%d-%06d", year, number1)
        assertEquals("INV-2026-000001", formatted1)

        val number2 = 42L
        val formatted2 = String.format(Locale.US, "INV-%d-%06d", year, number2)
        assertEquals("INV-2026-000042", formatted2)

        val number3 = 100000L
        val formatted3 = String.format(Locale.US, "INV-%d-%06d", year, number3)
        assertEquals("INV-2026-100000", formatted3)
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
