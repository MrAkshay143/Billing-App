package com.billing.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.core.ui.util.ToastType
import kotlinx.coroutines.delay

@Composable
fun DokandaarToastHost(
    modifier: Modifier = Modifier
) {
    val toastData by FeedbackManager.activeToast.collectAsState()

    LaunchedEffect(toastData?.id) {
        toastData?.let { data ->
            delay(data.durationMillis)
            FeedbackManager.dismissToast()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toastData != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.padding(bottom = 78.dp, start = 20.dp, end = 20.dp)
        ) {
            toastData?.let { data ->
                val (borderColor, iconVector, iconTint) = when (data.type) {
                    ToastType.SUCCESS -> Triple(
                        Color(0xFF22C55E).copy(alpha = 0.5f),
                        Icons.Default.CheckCircle,
                        Color(0xFF22C55E)
                    )
                    ToastType.ERROR -> Triple(
                        Color(0xFFEF4444).copy(alpha = 0.5f),
                        Icons.Default.ErrorOutline,
                        Color(0xFFEF4444)
                    )
                    ToastType.INFO -> Triple(
                        Color(0xFF7629D9).copy(alpha = 0.5f),
                        Icons.Default.Info,
                        Color(0xFFA855F7)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF131127),
                    border = BorderStroke(1.dp, borderColor),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .widthIn(min = 160.dp, max = 340.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            FeedbackManager.dismissToast()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = data.message,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
