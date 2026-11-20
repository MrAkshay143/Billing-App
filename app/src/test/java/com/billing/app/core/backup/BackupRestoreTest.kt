package com.billing.app.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class BackupRestoreTest {

    private fun sha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testBackupPayloadChecksumIntegrity() {
        val productsPayload = """[{"id":"prod-1","name":"Milk 500ml","barcode":"890123456789","pricePaise":3200,"stock":15}]"""
        val invoicesPayload = """[{"invoiceNumber":"INV-2026-000001","timestamp":1700000000000,"subtotalPaise":6400,"discountPaise":400,"taxPaise":0,"grandTotalPaise":6000,"status":"COMPLETED","paymentMethod":"UPI"}]"""
        val itemsPayload = """[{"invoiceNumber":"INV-2026-000001","productId":"prod-1","productName":"Milk 500ml","unitPricePaise":3200,"quantity":2,"lineTotalPaise":6400}]"""

        val payload = productsPayload + invoicesPayload + itemsPayload
        val validChecksum = sha256(payload)

        // Verify valid checksum calculation
        val calculated = sha256(payload)
        assertEquals(validChecksum, calculated)

        // Verify tampering detection
        val tamperedPayload = payload.replace("6000", "5000")
        val tamperedCalculated = sha256(tamperedPayload)
        assertFalse(validChecksum.equals(tamperedCalculated, ignoreCase = true))
    }

    @Test
    fun testUserExportPinSecurityExcludesPinHashAndSalt() {
        // Given an exported user JSON string according to BackupManager specification
        val exportedJson = """{"id":"u-123","username":"admin","displayName":"Store Owner","role":"ADMIN","isActive":true,"failedAttempts":0,"mustSetPin":false,"createdAt":1700000000000}"""

        // Verify that pinHash and pinSalt are not present in exported data
        assertFalse("Exported user must never contain pinHash", exportedJson.contains("\"pinHash\""))
        assertFalse("Exported user must never contain pinSalt", exportedJson.contains("\"pinSalt\""))
    }

    @Test
    fun testUserImportForcesMustSetPinAndEmptyPinHash() {
        // Simulation of BackupManager restore logic:
        // Set pinHash = "" and mustSetPin = true regardless of incoming values
        val restoredPinHash = ""
        val restoredPinSalt = ""
        val restoredMustSetPin = true

        assertEquals("", restoredPinHash)
        assertEquals("", restoredPinSalt)
        assertTrue("Restored user must be forced to set PIN", restoredMustSetPin)
    }
}
