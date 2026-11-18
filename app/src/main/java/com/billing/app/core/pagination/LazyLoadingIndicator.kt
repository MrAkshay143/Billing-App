package com.billing.app.core.pagination

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.PrimaryPurple

@Composable
fun LazyLoadingFooter(
    isLoadingMore: Boolean,
    hasMore: Boolean,
    totalCount: Int,
    itemLabel: String = "items",
    onRetry: (() -> Unit)? = null,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingMore -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryPurple
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Loading more $itemLabel...",
                        fontSize = 12.sp,
                        color = DokandaarTheme.textMuted
                    )
                }
            }

            errorMessage != null && onRetry != null -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = DokandaarTheme.textMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Retry",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple,
                        modifier = Modifier.clickable { onRetry() }
                    )
                }
            }

            !hasMore && totalCount > 0 -> {
                Text(
                    text = "Showing all $totalCount $itemLabel",
                    fontSize = 11.5.sp,
                    color = DokandaarTheme.textMuted.copy(alpha = 0.8f)
                )
            }
        }
    }
}
