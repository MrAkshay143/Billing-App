package com.billing.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ImageUtil {

    fun saveAndDownscaleImage(context: Context, uri: Uri, maxDimension: Int = 1024): String? {
        return try {
            val dir = File(context.filesDir, "product_images")
            if (!dir.exists()) dir.mkdirs()

            // 1. Decode bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            val maxOriginal = max(options.outWidth, options.outHeight)
            while (maxOriginal / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            // 2. Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Compress to disk
            val file = File(dir, "prod_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun saveBitmapImage(context: Context, bitmap: Bitmap): String? {
        return try {
            val dir = File(context.filesDir, "product_images")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "prod_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun deleteImageFile(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            val file = File(filePath)
            if (file.exists() && file.parentFile?.name == "product_images") {
                file.delete()
            }
        } catch (_: Exception) {}
    }
}
