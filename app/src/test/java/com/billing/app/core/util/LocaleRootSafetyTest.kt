package com.billing.app.core.util

import com.billing.app.core.qr.UpiQrGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LocaleRootSafetyTest {

    @Test
    fun testIndianDigitNormalization() {
        // Bengali digits
        assertEquals("120.50", Quantity.normalizeInputDigits("১২০.৫০"))
        assertEquals("9876543210", Quantity.normalizeInputDigits("৯৮৭৬৫৪৩২১০"))

        // Devanagari digits
        assertEquals("120.50", Quantity.normalizeInputDigits("१२०.५०"))
        assertEquals("9876543210", Quantity.normalizeInputDigits("९८७६५४३२१०"))

        // Mixed with ASCII and commas: commas stripped for numeric safety
        assertEquals("1250.00", Quantity.normalizeInputDigits("১,২৫০.০০"))
    }

    @Test
    fun testMoneyParsingWithIndianDigitsAndCommas() {
        // Bengali input
        val m1 = Money.fromRupees("১২০.৫০")
        assertEquals(12050L, m1.paise)

        // Devanagari input
        val m2 = Money.fromRupees("१,२५०")
        assertEquals(125000L, m2.paise)

        // Formatted money should always produce ASCII digits
        val formatted = m1.format(includeSymbol = false)
        assertEquals("1234.50", Money(123450L).format(includeSymbol = false))
        assertTrue(formatted.all { it.code < 128 })
    }

    @Test
    fun testUpiQrAmountStrictLocaleRoot() {
        val originalLocale = Locale.getDefault()
        try {
            // Set default locale to French (which uses comma as decimal separator)
            Locale.setDefault(Locale.FRANCE)

            val money = Money(123450L) // 1234.50
            // Money format must not use comma
            val formatted = money.format(includeSymbol = false)
            assertEquals("1234.50", formatted)

            val upiUri = UpiQrGenerator.buildUpiUri(
                upiId = "merchant@upi",
                shopName = "Kirana Store",
                amount = money
            )
            assertTrue(upiUri != null)
            assertTrue("UPI URI must have am=1234.50 and not am=1234,50", upiUri!!.contains("am=1234.50"))
            assertTrue(upiUri.contains("cu=INR"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
