package com.billing.app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.billing.app.core.ui.theme.GreenAccentColor

@Composable
fun ViewfinderOverlay(
    boxSize: Dp = 250.dp,
    cornerLength: Dp = 32.dp,
    strokeWidth: Dp = 4.dp,
    cornerColor: Color = GreenAccentColor
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(boxSize)) {
            val length = cornerLength.toPx()
            val stroke = strokeWidth.toPx()
            val w = size.width
            val h = size.height

            // Top-Left
            drawLine(cornerColor, Offset(0f, 0f), Offset(length, 0f), stroke, StrokeCap.Round)
            drawLine(cornerColor, Offset(0f, 0f), Offset(0f, length), stroke, StrokeCap.Round)

            // Top-Right
            drawLine(cornerColor, Offset(w, 0f), Offset(w - length, 0f), stroke, StrokeCap.Round)
            drawLine(cornerColor, Offset(w, 0f), Offset(w, length), stroke, StrokeCap.Round)

            // Bottom-Left
            drawLine(cornerColor, Offset(0f, h), Offset(length, h), stroke, StrokeCap.Round)
            drawLine(cornerColor, Offset(0f, h), Offset(0f, h - length), stroke, StrokeCap.Round)

            // Bottom-Right
            drawLine(cornerColor, Offset(w, h), Offset(w - length, h), stroke, StrokeCap.Round)
            drawLine(cornerColor, Offset(w, h), Offset(w, h - length), stroke, StrokeCap.Round)
        }
    }
}
