package com.billing.app.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object DokandaarTheme {
    val isDark: Boolean
        @Composable
        get() = LocalIsDarkTheme.current

    val background: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFF0B1120) else Color(0xFFF8FAFC)

    val cardBackground: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFF1E293B) else Color.White

    val textPrimary: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    val textMuted: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF64748B)

    val border: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFF334155) else Color(0xFFE2E8F0)

    val navBackground: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFF1E293B) else Color.White
}
