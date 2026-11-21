package com.billing.app.core.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.domain.model.Product
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
    product: Product,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
    categoryImageUri: String? = null
) {
    ProductThumbnail(
        imageUri = product.imageUri,
        categoryName = product.category,
        categoryImageUri = categoryImageUri,
        modifier = modifier,
        size = size,
        shape = shape
    )
}

@Composable
fun ProductThumbnail(
    imageUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
    categoryName: String? = null,
    categoryIconName: String? = null,
    categoryImageUri: String? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val targetPx = with(density) { size.roundToPx() }.coerceAtLeast(64)

    // Choose primary source: product imageUri, or fallback to categoryImageUri if product has none
    val effectiveUri = imageUri?.trim()?.ifBlank { null } ?: categoryImageUri?.trim()?.ifBlank { null }
    var bitmap by remember(effectiveUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(effectiveUri, targetPx) {
        if (effectiveUri == null) {
            bitmap = null
            return@LaunchedEffect
        }

        val cacheKey = "$effectiveUri-$targetPx"
        val cached = ThumbnailCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            bitmap = cached
            return@LaunchedEffect
        }

        bitmap = withContext(Dispatchers.IO) {
            try {
                val cleanPath = effectiveUri.removePrefix("file://")
                val file = File(cleanPath)
                if (file.exists() && file.isFile) {
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                    val rawWidth = boundsOptions.outWidth
                    val rawHeight = boundsOptions.outHeight
                    if (rawWidth > 0 && rawHeight > 0) {
                        var sampleSize = 1
                        while (rawWidth / (sampleSize * 2) >= targetPx && rawHeight / (sampleSize * 2) >= targetPx) {
                            sampleSize *= 2
                        }
                        val decodeOptions = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                        if (decoded != null) {
                            ThumbnailCache.put(cacheKey, decoded)
                        }
                        decoded
                    } else null
                } else {
                    // Try ContentResolver for content:// or other URIs
                    val uri = Uri.parse(effectiveUri)
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, boundsOptions)
                    }
                    val rawWidth = boundsOptions.outWidth
                    val rawHeight = boundsOptions.outHeight
                    if (rawWidth > 0 && rawHeight > 0) {
                        var sampleSize = 1
                        while (rawWidth / (sampleSize * 2) >= targetPx && rawHeight / (sampleSize * 2) >= targetPx) {
                            sampleSize *= 2
                        }
                        val decodeOptions = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, decodeOptions)
                        }
                        if (decoded != null) {
                            ThumbnailCache.put(cacheKey, decoded)
                        }
                        decoded
                    } else null
                }
            } catch (_: OutOfMemoryError) {
                null
            } catch (_: Throwable) {
                null
            }
        }
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(if (DokandaarTheme.isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Product Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Fallback: If category is provided, show category preset icon & background!
        val categoryKey = categoryIconName ?: categoryName
        if (!categoryKey.isNullOrBlank()) {
            val preset = CategoryPresets.getPreset(categoryKey)
            Box(
                modifier = modifier
                    .size(size)
                    .clip(shape)
                    .background(preset.badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = preset.icon,
                    contentDescription = preset.title,
                    tint = preset.iconColor,
                    modifier = Modifier.size(size * 0.58f)
                )
            }
        } else {
            // Ultimate fallback: generic inventory icon
            Box(
                modifier = modifier
                    .size(size)
                    .clip(shape)
                    .background(if (DokandaarTheme.isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = "Product Placeholder",
                    tint = DokandaarTheme.textMuted,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}
