package com.billing.app.core.qr

import com.billing.app.core.util.Money
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiQrGeneratorTest {

    @Test
    fun testUpiVpaValidation() {
        assertTrue(UpiQrGenerator.isValidUpiId("merchant@okhdfcbank"))
        assertTrue(UpiQrGenerator.isValidUpiId("dokandaar.store@upi"))
        assertTrue(UpiQrGenerator.isValidUpiId("user-123_abc@paytm"))

        // Invalid VPAs
        assertFalse(UpiQrGenerator.isValidUpiId(""))
        assertFalse(UpiQrGenerator.isValidUpiId("notanupi"))
        assertFalse(UpiQrGenerator.isValidUpiId("bad@"))
        assertFalse(UpiQrGenerator.isValidUpiId("@bad"))
        assertFalse(UpiQrGenerator.isValidUpiId("bad@upi&hack=true"))
        assertFalse(UpiQrGenerator.isValidUpiId("bad vpa@upi"))
    }

    @Test
    fun testGenerateUpiQrWithInvalidIdReturnsNull() {
        val result = UpiQrGenerator.generateUpiQrBitmap(
            upiId = "invalid_id_without_at",
            shopName = "Test Shop",
            amount = Money.fromRupees(150.0)
        )
        assertNull(result)
    }
}
