package com.billing.app.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.theme.ScannerLaserGreen

@Composable
fun ViewfinderLaserOverlay(
    boxWidth: Dp = 230.dp,
    boxHeight: Dp = 130.dp,
    cornerLength: Dp = 28.dp,
    strokeWidth: Dp = 3.5.dp,
    laserColor: Color = ScannerLaserGreen,
    showInstruction: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserSweep"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = boxWidth, height = boxHeight)) {
            val length = cornerLength.toPx()
            val stroke = strokeWidth.toPx()
            val w = size.width
            val h = size.height

            drawLine(laserColor, Offset(0f, 0f), Offset(length, 0f), stroke, StrokeCap.Round)
            drawLine(laserColor, Offset(0f, 0f), Offset(0f, length), stroke, StrokeCap.Round)

            drawLine(laserColor, Offset(w, 0f), Offset(w - length, 0f), stroke, StrokeCap.Round)
            drawLine(laserColor, Offset(w, 0f), Offset(w, length), stroke, StrokeCap.Round)

            drawLine(laserColor, Offset(0f, h), Offset(length, h), stroke, StrokeCap.Round)
            drawLine(laserColor, Offset(0f, h), Offset(0f, h - length), stroke, StrokeCap.Round)

            drawLine(laserColor, Offset(w, h), Offset(w - length, h), stroke, StrokeCap.Round)
            drawLine(laserColor, Offset(w, h), Offset(w, h - length), stroke, StrokeCap.Round)

            val currentLaserY = h * laserProgress
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        laserColor.copy(alpha = 0.4f),
                        laserColor,
                        laserColor.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, currentLaserY),
                end = Offset(w, currentLaserY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        if (showInstruction) {
            Text(
                text = "Align barcode or QR code within the frame",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}
