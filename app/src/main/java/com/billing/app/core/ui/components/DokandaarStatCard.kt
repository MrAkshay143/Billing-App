package com.billing.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.theme.DokandaarTheme

/**
 * Standardized 4-in-a-row KPI Statistic Card matching Reference UI Snapshot.
 *
 * Guarantees:
 * 1. Single-line non-wrapping label (fontSize = 10.sp, softWrap = false)
 * 2. Single-line bold value (fontSize = 12.5.sp - 13.sp, softWrap = false)
 * 3. Soft pastel container background with dark theme adaptation
 * 4. Centered vertical stack: Icon -> Label -> Value
 */
@Composable
fun DokandaarStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    containerBg: Color,
    modifier: Modifier = Modifier,
    valueColor: Color = DokandaarTheme.textPrimary,
    borderColor: Color = if (DokandaarTheme.isDark) DokandaarTheme.border else containerBg.copy(alpha = 0.5f),
    iconSize: Dp = 20.dp,
    onClick: (() -> Unit)? = null
) {
    val effectiveBg = if (DokandaarTheme.isDark) {
        DokandaarTheme.cardBackground
    } else {
        containerBg
    }

    val effectiveBorder = if (DokandaarTheme.isDark) {
        BorderStroke(1.dp, iconTint.copy(alpha = 0.35f))
    } else {
        BorderStroke(1.dp, borderColor)
    }

    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = effectiveBg),
        border = effectiveBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = DokandaarTheme.textMuted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Standardized 4-item horizontal row for KPI stats cards.
 */
@Composable
fun DokandaarKpiFourRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
