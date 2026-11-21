package com.billing.app.core.util

data class CalculatedLineTax(
    val lineTotalPaise: Long,
    val billDiscountSharePaise: Long,
    val netLineGrossPaise: Long,
    val taxableValuePaise: Long,
    val taxPaise: Long,
    val cgstPaise: Long,
    val sgstPaise: Long
)

data class BillCalculationResult(
    val subtotalPaise: Long,
    val billDiscountPaise: Long,
    val totalTaxPaise: Long,
    val unroundedTotalPaise: Long,
    val roundOffPaise: Long,
    val grandTotalPaise: Long,
    val lineCalculations: List<CalculatedLineTax>
)

object TaxEngine {

    data class LineItem(
        val lineTotalPaise: Long,
        val gstRateBp: Int = 0
    )

    /**
     * Calculates line taxes and bill totals following the Dokandaar master plan v6.4:
     * 1. Pro-rata bill discount distribution with remainder absorbed by the last item.
     * 2. Net line gross calculation: (lineTotalPaise - billDiscountSharePaise).coerceAtLeast(0L).
     * 3. Tax-inclusive GST extraction:
     *    - If disabled/composition (or gstRateBp <= 0): taxableValue = netLineGross, tax = 0, cgst = 0, sgst = 0.
     *    - If enabled:
     *        Taxable = (netLineGross * 10000 + (10000 + gstRateBp) / 2) / (10000 + gstRateBp)
     *        TotalTax = netLineGross - Taxable
     *        CGST = TotalTax / 2
     *        SGST = TotalTax - CGST (absorbing any 1 paise odd difference)
     * 4. Nearest Rupee Round-Off:
     *    unroundedTotal = (subtotalPaise - effectiveDiscountPaise).coerceAtLeast(0L)
     *    grandTotalPaise = ((unroundedTotal + 50) / 100) * 100
     *    roundOffPaise = grandTotalPaise - unroundedTotal
     */
    fun calculate(
        items: List<LineItem>,
        billDiscountPaise: Long = 0L,
        isGstEnabled: Boolean = true
    ): BillCalculationResult {
        if (items.isEmpty()) {
            return BillCalculationResult(
                subtotalPaise = 0L,
                billDiscountPaise = billDiscountPaise,
                totalTaxPaise = 0L,
                unroundedTotalPaise = 0L,
                roundOffPaise = 0L,
                grandTotalPaise = 0L,
                lineCalculations = emptyList()
            )
        }

        val subtotalPaise = items.sumOf { it.lineTotalPaise }
        val effectiveDiscountPaise = billDiscountPaise.coerceAtLeast(0L)

        val lineCalculations = ArrayList<CalculatedLineTax>(items.size)
        var allocatedDiscount = 0L
        val n = items.size

        for (i in 0 until n) {
            val item = items[i]
            val discountShare: Long = if (subtotalPaise <= 0L || effectiveDiscountPaise == 0L) {
                0L
            } else if (i == n - 1) {
                // Last item absorbs remainder
                effectiveDiscountPaise - allocatedDiscount
            } else {
                (effectiveDiscountPaise * item.lineTotalPaise) / subtotalPaise
            }
            allocatedDiscount += discountShare

            val netLineGrossPaise = (item.lineTotalPaise - discountShare).coerceAtLeast(0L)

            val taxableValuePaise: Long
            val taxPaise: Long
            val cgstPaise: Long
            val sgstPaise: Long

            if (!isGstEnabled || item.gstRateBp <= 0) {
                taxableValuePaise = netLineGrossPaise
                taxPaise = 0L
                cgstPaise = 0L
                sgstPaise = 0L
            } else {
                val denom = 10000L + item.gstRateBp
                taxableValuePaise = (netLineGrossPaise * 10000L + denom / 2L) / denom
                taxPaise = netLineGrossPaise - taxableValuePaise
                cgstPaise = taxPaise / 2L
                sgstPaise = taxPaise - cgstPaise
            }

            lineCalculations.add(
                CalculatedLineTax(
                    lineTotalPaise = item.lineTotalPaise,
                    billDiscountSharePaise = discountShare,
                    netLineGrossPaise = netLineGrossPaise,
                    taxableValuePaise = taxableValuePaise,
                    taxPaise = taxPaise,
                    cgstPaise = cgstPaise,
                    sgstPaise = sgstPaise
                )
            )
        }

        val totalTaxPaise = lineCalculations.sumOf { it.taxPaise }
        val unroundedTotalPaise = (subtotalPaise - effectiveDiscountPaise).coerceAtLeast(0L)
        val grandTotalPaise = ((unroundedTotalPaise + 50L) / 100L) * 100L
        val roundOffPaise = grandTotalPaise - unroundedTotalPaise

        return BillCalculationResult(
            subtotalPaise = subtotalPaise,
            billDiscountPaise = billDiscountPaise,
            totalTaxPaise = totalTaxPaise,
            unroundedTotalPaise = unroundedTotalPaise,
            roundOffPaise = roundOffPaise,
            grandTotalPaise = grandTotalPaise,
            lineCalculations = lineCalculations
        )
    }

    fun calculateBill(
        items: List<LineItem>,
        billDiscountPaise: Long = 0L,
        isGstEnabled: Boolean = true
    ): BillCalculationResult = calculate(items, billDiscountPaise, isGstEnabled)

    /**
     * Single line tax extraction helper.
     */
    fun extractLineTax(
        netLineGrossPaise: Long,
        gstRateBp: Int,
        isGstEnabled: Boolean = true
    ): CalculatedLineTax {
        val gross = netLineGrossPaise.coerceAtLeast(0L)
        if (!isGstEnabled || gstRateBp <= 0) {
            return CalculatedLineTax(
                lineTotalPaise = gross,
                billDiscountSharePaise = 0L,
                netLineGrossPaise = gross,
                taxableValuePaise = gross,
                taxPaise = 0L,
                cgstPaise = 0L,
                sgstPaise = 0L
            )
        }
        val denom = 10000L + gstRateBp
        val taxable = (gross * 10000L + denom / 2L) / denom
        val totalTax = gross - taxable
        val cgst = totalTax / 2L
        val sgst = totalTax - cgst
        return CalculatedLineTax(
            lineTotalPaise = gross,
            billDiscountSharePaise = 0L,
            netLineGrossPaise = gross,
            taxableValuePaise = taxable,
            taxPaise = totalTax,
            cgstPaise = cgst,
            sgstPaise = sgst
        )
    }

    /**
     * Nearest Rupee Round-Off calculation helper.
     */
    fun roundToNearestRupee(unroundedTotalPaise: Long): Pair<Long, Long> {
        val unrounded = unroundedTotalPaise.coerceAtLeast(0L)
        val grandTotal = ((unrounded + 50L) / 100L) * 100L
        val roundOff = grandTotal - unrounded
        return Pair(grandTotal, roundOff)
    }
}
