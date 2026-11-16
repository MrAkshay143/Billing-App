package com.billing.app.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class SecurityUtilTest {

    @Test
    fun testPbkdf2HashAndVerifyPinSuccess() {
        val pin = "1234"
        val salt = SecurityUtil.generateSalt()
        val hash = SecurityUtil.hashPin(pin, salt)

        assertTrue(hash.startsWith("pbkdf2:"))
        assertTrue(SecurityUtil.verifyPin("1234", salt, hash))
        assertFalse(SecurityUtil.verifyPin("0000", salt, hash))
        assertFalse(SecurityUtil.verifyPin("1235", salt, hash))
    }

    @Test
    fun testLegacySha256BackwardCompatibility() {
        val pin = "4321"
        val salt = "abcd1234ef567890"

        // Generate old legacy hash: SHA-256("$salt:$pin")
        val combined = "$salt:$pin"
        val digest = MessageDigest.getInstance("SHA-256")
        val legacyHash = digest.digest(combined.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        // Must successfully verify with legacy hash
        assertTrue(SecurityUtil.verifyPin("4321", salt, legacyHash))
        assertFalse(SecurityUtil.verifyPin("0000", salt, legacyHash))
    }

    @Test
    fun testEmptyOrInvalidHashReturnsFalse() {
        assertFalse(SecurityUtil.verifyPin("1234", "salt", ""))
        assertFalse(SecurityUtil.verifyPin("1234", "salt", "   "))
    }
}
