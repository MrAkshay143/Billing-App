package com.billing.app.feature.more.subscreens

import android.widget.Toast
import com.billing.app.core.ui.util.FeedbackManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.billing.app.core.ui.components.SubPageHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.TextMutedColor
import com.billing.app.feature.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = com.billing.app.core.localization.LocalAppStrings.current
    val currentTheme by viewModel.appTheme.collectAsState()

    val themes = listOf(
        ThemeOption(
            key = "system",
            title = strings.systemDefault,
            description = strings.themeSystemDesc,
            icon = Icons.Default.BrightnessAuto
        ),
        ThemeOption(
            key = "light",
            title = strings.lightMode,
            description = strings.themeLightDesc,
            icon = Icons.Default.LightMode
        ),
        ThemeOption(
            key = "dark",
            title = strings.darkMode,
            description = strings.themeDarkDesc,
            icon = Icons.Default.DarkMode
        )
    )

    Scaffold(
        topBar = {
            SubPageHeader(
                title = strings.appearanceTheme,
                subtitle = strings.appearanceSubtitle,
                icon = Icons.Default.Palette,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AddProductBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = HeaderGradientStart,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = strings.themeDisplay,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                        Text(
                            text = strings.themeDisplayDesc,
                            fontSize = 13.sp,
                            color = DokandaarTheme.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = strings.selectApplicationTheme,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DokandaarTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    themes.forEachIndexed { index, option ->
                        val isSelected = currentTheme == option.key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAppTheme(option.key)
                                    FeedbackManager.showToast(context, "${option.title} applied")
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) (if (DokandaarTheme.isDark) HeaderGradientStart.copy(alpha = 0.22f) else AddProductBg) else DokandaarTheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) HeaderGradientStart else DokandaarTheme.textMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = DokandaarTheme.textPrimary
                                )
                                Text(
                                    text = option.description,
                                    fontSize = 12.sp,
                                    color = DokandaarTheme.textMuted
                                )
                            }

                            // Radio Check circle
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) HeaderGradientStart else DokandaarTheme.border,
                                        shape = CircleShape
                                    )
                                    .background(if (isSelected) HeaderGradientStart else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (index < themes.size - 1) {
                            HorizontalDivider(color = DokandaarTheme.border.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class ThemeOption(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)
