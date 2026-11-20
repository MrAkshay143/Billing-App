package com.billing.app.core.printer

import com.billing.app.domain.model.InvoiceReceipt
import com.billing.app.domain.model.ReceiptItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterGoldenTest {

    @Test
    fun testTwoLine58mmReceiptLayout() {
        val receipt = InvoiceReceipt(
            shopName = "Maa Tara Bhandar",
            shopAddressLine1 = "Station Road, Bolpur",
            shopPhone = "9876543210",
            invoiceNumber = "INV-2026-001",
            timestamp = 1700000000000L,
            items = listOf(
                ReceiptItem(
                    name = "Sugar Premium Crystal",
                    quantityMilli = 250L,
                    unitCode = "KG",
                    unitPricePaise = 12000L,
                    lineTotalPaise = 3000L
                ),
                ReceiptItem(
                    name = "Parle-G Biscuit Pack",
                    quantityMilli = 3000L,
                    unitCode = "PCS",
                    unitPricePaise = 1000L,
                    lineTotalPaise = 3000L
                )
            ),
            subtotalPaise = 6000L,
            grandTotalPaise = 6000L
        )

        val bytes = EscPosFormatter.formatInvoiceText(receipt, columns = 32)
        val text = String(bytes, Charsets.US_ASCII)

        // Verify shop name and metadata
        assertTrue(text.contains("Maa Tara Bhandar"))
        assertTrue(text.contains("INV-2026-001"))

        // Verify two-line layout elements:
        // Line 1: item name
        assertTrue(text.contains("Sugar Premium Crystal"))
        assertTrue(text.contains("Parle-G Biscuit Pack"))

        // Line 2: quantity in base unit x unit price = line total
        assertTrue(text.contains("0.250 kg x 120.00/kg"))
        assertTrue(text.contains("3 pc x 10.00/pc"))
        assertTrue(text.contains("30.00"))

        // Every line should respect 32-character boundary
        text.lines().filter { it.isNotBlank() }.forEach { rawLine ->
            // Strip ESC/POS control command bytes (ESC @, ESC E n, ESC a n, GS ! n, etc.)
            val lineWithoutEsc = rawLine
                .replace(Regex("\\u001B[Ea!]\\u0000|\\u001B[Ea!]\\u0001|\\u001B@|\\u001D!\\u0000|\\u001D!\\u0011|\\u001DV\\u0041\\u0010"), "")
                .trim()
            assertTrue("Line exceeds 32 chars: '$lineWithoutEsc' (length ${lineWithoutEsc.length})", lineWithoutEsc.length <= 32)
        }
    }

    @Test
    fun testCompositionStatutoryDeclarationTextAndBitmap() {
        val receipt = InvoiceReceipt(
            shopName = "Small Grocery",
            invoiceNumber = "BOS-001",
            timestamp = 1700000000000L,
            items = listOf(
                ReceiptItem(
                    name = "Atta",
                    quantityMilli = 1000L,
                    unitCode = "KG",
                    unitPricePaise = 4000L,
                    lineTotalPaise = 4000L
                )
            ),
            subtotalPaise = 4000L,
            grandTotalPaise = 4000L,
            receiptType = "Bill of Supply"
        )

        val bytesText = EscPosFormatter.formatInvoiceText(receipt, columns = 32)
        val text = String(bytesText, Charsets.US_ASCII)

        // Exact statutory declaration required by GST Composition rules
        assertTrue(text.contains("Composition Taxable Person, not eligible to collect tax on supplies"))
        assertTrue(text.contains("Bill of Supply"))
    }

    @Test
    fun testNonAsciiDetectionForMultilingualPrinting() {
        // Pure ASCII text routes to fast ESC/POS text engine
        assertFalse(EscPosFormatter.containsNonAscii("Maa Tara Store INV-1001 0.250 kg"))

        // Bengali text routes to bitmap raster engine
        assertTrue(EscPosFormatter.containsNonAscii("মা তারা ভাণ্ডার"))
        assertTrue(EscPosFormatter.containsNonAscii("চিনি ১ কেজি"))

        // Devanagari text routes to bitmap raster engine
        assertTrue(EscPosFormatter.containsNonAscii("दुकानदार बिल"))
        assertTrue(EscPosFormatter.containsNonAscii("चीनी"))
    }

    @Test
    fun testReceiptStringsParityAcrossLanguages() {
        val en = com.billing.app.core.localization.EnStrings
        val bn = com.billing.app.core.localization.BnStrings
        val hi = com.billing.app.core.localization.HiStrings

        // Bengali receipt labels verified
        assertEquals("চালান:", bn.receiptInvoice)
        assertEquals("তারিখ:", bn.receiptDate)
        assertEquals("সর্বমোট:", bn.receiptTotalPayable)
        assertEquals("ধন্যবাদ! আবার আসবেন", bn.receiptThankYou)

        // Hindi receipt labels verified
        assertEquals("इनवॉइस:", hi.receiptInvoice)
        assertEquals("तारीख:", hi.receiptDate)
        assertEquals("कुल देय:", hi.receiptTotalPayable)
        assertEquals("धन्यवाद! फिर पधारें", hi.receiptThankYou)

        // English receipt labels verified
        assertEquals("Invoice:", en.receiptInvoice)
        assertEquals("TOTAL:", en.receiptTotalPayable)
    }
}
