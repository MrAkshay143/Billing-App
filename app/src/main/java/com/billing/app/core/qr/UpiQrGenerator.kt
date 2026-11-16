package com.billing.app.core.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.billing.app.core.util.Money
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.net.URLEncoder

object UpiQrGenerator {
    fun generateUpiQrBitmap(
        upiId: String,
        shopName: String,
        amount: Money,
        sizePx: Int = 360
    ): Bitmap? {
        if (upiId.isBlank()) return null

        val encodedName = URLEncoder.encode(shopName, "UTF-8").replace("+", "%20")
        val amountStr = amount.format(includeSymbol = false)
        val upiUri = "upi://pay?pa=$upiId&pn=$encodedName&am=$amountStr&cu=INR"

        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                upiUri,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
