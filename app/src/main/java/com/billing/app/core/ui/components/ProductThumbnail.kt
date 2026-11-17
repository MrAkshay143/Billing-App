package com.billing.app.core.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.billing.app.core.ui.theme.DokandaarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceIn(4096, 32768)
    private val lru = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(key: String): Bitmap? = synchronized(this) { lru.get(key) }
    fun put(key: String, bitmap: Bitmap) { synchronized(this) { lru.put(key, bitmap) } }
    fun remove(key: String) { synchronized(this) { lru.remove(key) } }
    fun clear() { synchronized(this) { lru.evictAll() } }
}

@Composable
fun ProductThumbnail(
    imageUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp)
) {
    val density = LocalDensity.current
    val targetPx = with(density) { size.roundToPx() }.coerceAtLeast(64)
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri, targetPx) {
        if (imageUri.isNullOrBlank()) {
            bitmap = null
            return@LaunchedEffect
        }

        val cacheKey = "$imageUri-$targetPx"
        val cached = ThumbnailCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            bitmap = cached
            return@LaunchedEffect
        }

        bitmap = withContext(Dispatchers.IO) {
            try {
                val cleanPath = imageUri.removePrefix("file://")
                val file = File(cleanPath)
                if (!file.exists()) return@withContext null

                // 1. Decode bounds only
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)

                val rawWidth = options.outWidth
                val rawHeight = options.outHeight
                if (rawWidth <= 0 || rawHeight <= 0) return@withContext null

                // 2. Calculate optimal inSampleSize
                var sampleSize = 1
                while (rawWidth / (sampleSize * 2) >= targetPx && rawHeight / (sampleSize * 2) >= targetPx) {
                    sampleSize *= 2
                }

                // 3. Decode scaled bitmap safely in RGB_565 (50% less memory)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                if (decoded != null) {
                    ThumbnailCache.put(cacheKey, decoded)
                }
                decoded
            } catch (_: OutOfMemoryError) {
                null
            } catch (_: Throwable) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (DokandaarTheme.isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Product Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = "Product Placeholder",
                tint = DokandaarTheme.textMuted,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
