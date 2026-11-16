package com.billing.app.core.ui.util

import android.content.Context
import android.widget.Toast

/**
 * Centralized feedback manager that eliminates the Android Toast queue delay
 * by cancelling any active toast before showing a new one.
 */
object FeedbackManager {
    private var activeToast: Toast? = null

    fun showToast(
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        if (message.isBlank()) return
        try {
            activeToast?.cancel()
            activeToast = Toast.makeText(context.applicationContext, message, duration).also {
                it.show()
            }
        } catch (_: Exception) {}
    }
}
