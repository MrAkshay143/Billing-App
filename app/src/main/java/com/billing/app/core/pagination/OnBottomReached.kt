package com.billing.app.core.pagination

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Triggers [onLoadMore] approximately [buffer] items before reaching the bottom of the LazyList.
 * Enables automatic infinite scroll pagination without user interaction.
 */
@Composable
fun LazyListState.OnBottomReached(
    buffer: Int = 5,
    enabled: Boolean = true,
    onLoadMore: () -> Unit
) {
    val shouldLoadMore = remember(enabled, buffer) {
        derivedStateOf {
            if (!enabled) return@derivedStateOf false
            val layout = layoutInfo
            val total = layout.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val lastVisible = layout.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisible.index >= total - 1 - buffer
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                onLoadMore()
            }
    }
}
