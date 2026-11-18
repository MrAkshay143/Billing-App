package com.billing.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.PrimaryPurple

/**
 * Centralized Header Action Capsule Pill (e.g. "+ Add Product", "+ New Invoice", "+ Add")
 *
 * Automatically guards against double '++':
 * Strips any accidental leading '+' character from text while rendering the single standardized '+' icon.
 * Enforces strict single-line constraints (maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
 * to prevent text from wrapping into broken lines on compact mobile screens.
 */
@Composable
fun DokandaarActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Default.Add,
    backgroundColor: Color = Color.White,
    contentColor: Color = PrimaryPurple,
    fontSize: TextUnit = 12.sp,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 6.dp
) {
    // Strip any accidental leading '+' or extra spaces so double '++' can NEVER occur
    val cleanText = text.trimStart('+', ' ').trim()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = cleanText,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Centralized Single-Line Text Component
 * Guarantees that any text in buttons, headers, chips, and badges never breaks across multiple lines.
 */
@Composable
fun DokandaarSingleLineText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        letterSpacing = letterSpacing,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Centralized Bottom Bar CTA Button (e.g. "Proceed to Pay", "Done", "Complete Sale")
 * Optimally spaces leading icon, title, amount, and trailing arrow with guaranteed single-line rendering.
 */
@Composable
fun DokandaarBottomBarCta(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    enabled: Boolean = true,
    backgroundColor: Color = PrimaryPurple,
    contentColor: Color = Color.White
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = title,
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingText != null || trailingIcon != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (trailingText != null) {
                        Text(
                            text = trailingText,
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Centralized Compact Bottom Action Button (e.g. "Hold", "Discard", "Clear All")
 */
@Composable
fun DokandaarBottomIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = DokandaarTheme.textPrimary,
    borderColor: Color = DokandaarTheme.border
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = DokandaarTheme.cardBackground,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Centralized Dialog Action Button with single-line enforcement
 */
@Composable
fun DokandaarDialogButton(
    text: String,
    onClick: () -> Unit,
    color: Color = PrimaryPurple,
    fontWeight: FontWeight = FontWeight.Bold
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            color = color,
            fontWeight = fontWeight,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
