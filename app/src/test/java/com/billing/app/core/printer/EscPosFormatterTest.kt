package com.billing.app.core.printer

import com.billing.app.core.util.Money
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.InvoiceReceipt
import com.billing.app.domain.model.Product
import com.billing.app.domain.model.ReceiptItem
import com.billing.app.domain.model.ShopProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EscPosFormatterTest {

    @Test
    fun testReceiptContainsEssentialEscPosCommandsAndText() {
        val shop = ShopProfile(name = "SuperMart", phoneNumber = "9876543210")
        val product = Product(id = "1", name = "Biscuits", barcode = "12345", price = Money.fromRupees(25.0))
        val items = listOf(CartItem(product = product, quantity = 2))
        val total = Money.fromRupees(50.0)

        val bytes = EscPosFormatter.formatReceipt(shop, items, total)
        val text = String(bytes, Charsets.US_ASCII)

        assertTrue(text.contains("SuperMart"))
        assertTrue(text.contains("9876543210"))
        assertTrue(text.contains("Biscuits"))
        assertTrue(text.contains("TOTAL: Rs. 50.00"))
        // Check ESC/POS Init header bytes
        assertTrue(bytes[0] == 0x1B.toByte() && bytes[1] == 0x40.toByte())
    }

    @Test
    fun testReceiptWithDiscountTaxAndPaymentDetails() {
        val receipt = InvoiceReceipt(
            shopName = "Dokandaar Store",
            shopAddressLine1 = "12 Station Road",
            shopPhone = "9876543210",
            shopGstin = "27AAPFU0939F1ZV",
            invoiceNumber = "INV-2026-000042",
            timestamp = 1700000000000L,
            items = listOf(
                ReceiptItem(name = "Atta 5kg", quantityMilli = 1000L, unitCode = "PCS", unitPricePaise = 25000L, lineTotalPaise = 25000L),
                ReceiptItem(name = "Rice 1kg", quantityMilli = 2000L, unitCode = "PCS", unitPricePaise = 6000L, lineTotalPaise = 12000L)
            ),
            subtotalPaise = 37000L,
            discountPaise = 2000L, // Rs. 20 discount
            taxPaise = 1750L, // Rs. 17.50 GST
            grandTotalPaise = 36750L,
            paymentMethod = "CASH",
            cashReceivedPaise = 40000L,
            changeReturnedPaise = 3250L,
            duePaise = 0L
        )

        val bytes = EscPosFormatter.formatInvoiceText(receipt, columns = 32)
        val text = String(bytes, Charsets.US_ASCII)

        assertTrue(text.contains("Dokandaar Store"))
        assertTrue(text.contains("INV-2026-000042"))
        assertTrue(text.contains("GSTIN: 27AAPFU0939F1ZV"))
        assertTrue(text.contains("Discount: -Rs. 20.00"))
        assertTrue(text.contains("Tax / GST: Rs. 17.50"))
        assertTrue(text.contains("TOTAL: Rs. 367.50"))
        assertTrue(text.contains("Cash Received: Rs. 400.00"))
        assertTrue(text.contains("Change Returned: Rs. 32.50"))
    }

    @Test
    fun testReceipt80mmColumnFormatting() {
        val receipt = InvoiceReceipt(
            shopName = "Mega Store",
            invoiceNumber = "INV-80MM-1",
            timestamp = 1700000000000L,
            items = listOf(ReceiptItem(name = "Item 1", quantityMilli = 1000L, unitCode = "PCS", unitPricePaise = 1000L, lineTotalPaise = 1000L)),
            subtotalPaise = 1000L,
            grandTotalPaise = 1000L
        )

        val bytes48 = EscPosFormatter.formatInvoiceText(receipt, columns = 48)
        val text48 = String(bytes48, Charsets.US_ASCII)

        // 48 column separator line has length 48
        assertTrue(text48.contains("-".repeat(48)))
    }

    @Test
    fun testIndicLanguageDetection() {
        assertFalse(EscPosFormatter.containsNonAscii("English Store Name 123"))
        assertTrue(EscPosFormatter.containsNonAscii("দোকানদার স্টোর")) // Bengali
        assertTrue(EscPosFormatter.containsNonAscii("दुकानदार किराना")) // Hindi
    }
}
