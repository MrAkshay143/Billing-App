package com.billing.app.core.printer

import com.billing.app.core.util.Money
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.Product
import com.billing.app.domain.model.ShopProfile
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
}
