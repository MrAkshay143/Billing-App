package com.billing.app.core.ui.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ToastType {
    INFO,
    SUCCESS,
    ERROR
}

data class DokandaarToastData(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: ToastType = ToastType.INFO,
    val durationMillis: Long = 2500L
)

/**
 * Centralized, branded Toast and feedback manager for Dokandaar POS.
 * Replaces ugly platform default gray bubble toasts with standard styled Dokandaar toasts.
 */
object FeedbackManager {
    private val _activeToast = MutableStateFlow<DokandaarToastData?>(null)
    val activeToast = _activeToast.asStateFlow()

    fun showToast(
        context: Context? = null,
        message: String,
        duration: Int = 0
    ) {
        if (message.isBlank()) return
        val lower = message.lowercase()
        val type = when {
            lower.contains("success") || lower.contains("saved") || lower.contains("copied") ||
            lower.contains("imported") || lower.contains("restored") || lower.contains("applied") ||
            lower.contains("verified") || lower.contains("selected") || lower.contains("added") -> ToastType.SUCCESS

            lower.contains("fail") || lower.contains("error") || lower.contains("required") ||
            lower.contains("cannot") || lower.contains("not found") || lower.contains("deleted") ||
            lower.contains("disabled") || lower.contains("please enter") -> ToastType.ERROR

            else -> ToastType.INFO
        }

        val durationMillis = if (message.length > 40 || duration > 0) 3500L else 2500L
        _activeToast.value = DokandaarToastData(
            id = System.currentTimeMillis(),
            message = message.trim(),
            type = type,
            durationMillis = durationMillis
        )
    }

    fun dismissToast() {
        _activeToast.value = null
    }
}
