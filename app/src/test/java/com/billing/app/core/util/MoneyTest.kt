package com.billing.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun testMoneyAdditionAndMultiplication() {
        val price1 = Money.fromRupees(15.50)
        val price2 = Money.fromRupees(4.50)
        val sum = price1 + price2

        assertEquals(2000L, sum.paise)
        assertEquals("₹20.00", sum.format())

        val multiple = price1 * 3
        assertEquals(4650L, multiple.paise)
        assertEquals("₹46.50", multiple.format())
    }

    @Test
    fun testMoneyFormattingWithoutSymbol() {
        val amount = Money(12550L) // 125.50
        assertEquals("125.50", amount.format(includeSymbol = false))
    }

    @Test
    fun testMoneyComparison() {
        val m1 = Money.fromRupees(100.0)
        val m2 = Money.fromRupees(50.0)
        assertTrue(m1 > m2)
    }
}
