package com.billing.app.core.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeDebounceManagerTest {

    @Test
    fun testDebounceSuppressesImmediateDuplicate() {
        val manager = BarcodeDebounceManager(cooldownMs = 1000L)
        val barcode = "8901030382341"

        assertTrue(manager.shouldProcess(barcode))
        assertFalse(manager.shouldProcess(barcode)) // Immediate duplicate must be false
    }

    @Test
    fun testDifferentBarcodesAreBothAllowed() {
        val manager = BarcodeDebounceManager(cooldownMs = 1000L)
        assertTrue(manager.shouldProcess("11111"))
        assertTrue(manager.shouldProcess("22222"))
    }
}
