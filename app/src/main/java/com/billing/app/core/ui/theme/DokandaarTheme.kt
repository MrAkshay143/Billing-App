package com.billing.app.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object DokandaarTheme {
    val isDark: Boolean
        @Composable
        get() = LocalIsDarkTheme.current

    val primary: Color
        get() = PrimaryPurple

    val secondary: Color
        get() = SecondaryPurple

    val background: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFF0B1120) else Color(0xFFF7F7FC)

    val cardBackground: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFF1E293B) else Color.White

    val textPrimary: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFFF8FAFC) else Color(0xFF111827)

    val textMuted: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF64748B)

    val border: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFF334155) else Color(0xFFE2E8F0)

    val surfaceVariant: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFF0F172A) else Color(0xFFF1F5F9)

    val navBackground: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) Color(0xFF1E293B) else Color.White
}
