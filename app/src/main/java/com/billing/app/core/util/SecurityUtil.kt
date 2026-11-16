package com.billing.app.core.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtil {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 65536
    private const val PBKDF2_KEY_LENGTH = 256
    private const val PBKDF2_PREFIX = "pbkdf2:"

    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes PIN using PBKDF2-HMAC-SHA256 (65,536 iterations, 256-bit key).
     */
    fun hashPin(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = skf.generateSecret(spec).encoded
        return PBKDF2_PREFIX + hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Legacy single-iteration SHA-256 for backward compatibility with existing stored PINs.
     */
    private fun legacySha256Hash(pin: String, salt: String): String {
        val combined = "$salt:$pin"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Constant-time verification of PIN against stored hash.
     */
    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        if (expectedHash.isBlank()) return false

        val calculated = if (expectedHash.startsWith(PBKDF2_PREFIX)) {
            hashPin(pin, salt)
        } else {
            legacySha256Hash(pin, salt)
        }

        return MessageDigest.isEqual(
            calculated.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }
}
