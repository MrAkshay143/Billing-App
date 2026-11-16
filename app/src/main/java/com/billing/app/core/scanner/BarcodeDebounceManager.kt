package com.billing.app.core.scanner

import java.util.concurrent.ConcurrentHashMap

class BarcodeDebounceManager(
    private val cooldownMs: Long = 2000L,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val lastScanned = ConcurrentHashMap<String, Long>()

    fun shouldProcess(barcode: String): Boolean {
        val now = timeProvider()
        var processed = false

        lastScanned.compute(barcode) { _, prevTime ->
            val last = prevTime ?: 0L
            if (now - last >= cooldownMs) {
                processed = true
                now
            } else {
                processed = false
                last
            }
        }

        // Evict entries older than 30 seconds if map grows past 200 items
        if (lastScanned.size > 200) {
            lastScanned.entries.removeIf { (now - it.value) > 30_000L }
        }

        return processed
    }

    fun clear() = lastScanned.clear()
}
