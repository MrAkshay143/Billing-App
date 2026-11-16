package com.billing.app.core.ui.util

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Clickable modifier that prevents multi-click / double-tap race conditions
 * by dropping subsequent clicks occurring within [debounceMs] window.
 */
fun Modifier.debouncedClickable(
    debounceMs: Long = 600L,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    this.clickable(enabled = enabled) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime >= debounceMs) {
            lastClickTime = now
            onClick()
        }
    }
}
