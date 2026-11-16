package com.billing.app.core.util

import java.util.Locale

data class Money(val paise: Long) : Comparable<Money> {
    operator fun plus(other: Money): Money = Money(this.paise + other.paise)
    operator fun minus(other: Money): Money = Money(this.paise - other.paise)
    operator fun times(quantity: Int): Money = Money(this.paise * quantity)

    override fun compareTo(other: Money): Int = this.paise.compareTo(other.paise)

    fun toRupees(): Double = paise / 100.0

    fun format(includeSymbol: Boolean = true): String {
        val rupees = paise / 100
        val remainder = kotlin.math.abs(paise % 100)
        val formatted = String.format(Locale.US, "%d.%02d", rupees, remainder)
        return if (includeSymbol) "₹$formatted" else formatted
    }

    companion object {
        val ZERO = Money(0L)

        fun fromPaise(paise: Long): Money = Money(paise)

        fun fromRupees(amount: Double): Money {
            return Money(kotlin.math.round(amount * 100.0).toLong())
        }

        fun fromRupees(amount: String): Money {
            val clean = amount.trim().replace(",", "").replace("₹", "")
            val parsed = clean.toDoubleOrNull() ?: 0.0
            return fromRupees(parsed)
        }
    }
}
