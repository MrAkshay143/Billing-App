package com.billing.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.PrimaryPurple

/**
 * Centralized Dokandaar Search Bar
 *
 * Guarantees:
 * - Single-line search input (singleLine = true, maxLines = 1)
 * - Strict non-wrapping placeholder with ellipsis (maxLines = 1, softWrap = false)
 * - Standardized height matching adjacent filter buttons (default 44.dp)
 * - Leading search icon
 * - Trailing clear button when searchQuery is not empty
 * - Optional custom trailing content (e.g. barcode scanner button)
 */
@Composable
fun DokandaarSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    leadingIcon: ImageVector = Icons.Default.Search,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DokandaarTheme.cardBackground,
        border = BorderStroke(1.dp, DokandaarTheme.border),
        modifier = modifier.height(height)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = "Search",
                tint = DokandaarTheme.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 11.5.sp,
                        color = DokandaarTheme.textMuted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    maxLines = 1,
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        color = DokandaarTheme.textPrimary
                    ),
                    cursorBrush = SolidColor(PrimaryPurple),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = DokandaarTheme.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

/**
 * Centralized Date Filter Capsule Pill matching the Search Bar
 */
@Composable
fun DokandaarDateFilterPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    icon: ImageVector = Icons.Default.CalendarMonth,
    iconTint: Color = PrimaryPurple
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DokandaarTheme.cardBackground,
        border = BorderStroke(1.dp, DokandaarTheme.border),
        modifier = modifier
            .height(height)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DokandaarTheme.textPrimary,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = DokandaarTheme.textPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
