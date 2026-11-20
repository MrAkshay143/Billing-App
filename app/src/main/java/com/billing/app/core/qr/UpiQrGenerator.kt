package com.billing.app.core.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.billing.app.core.util.Money
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.net.URLEncoder
import java.util.Locale

object UpiQrGenerator {
    private val UPI_VPA_REGEX = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$")

    fun isValidUpiId(upiId: String): Boolean {
        return UPI_VPA_REGEX.matches(upiId.trim())
    }

    fun buildUpiUri(
        upiId: String,
        shopName: String,
        amount: Money,
        transactionRef: String? = null,
        transactionNote: String = "Store Payment"
    ): String? {
        val cleanUpi = upiId.trim()
        if (!isValidUpiId(cleanUpi)) return null

        val encodedPa = URLEncoder.encode(cleanUpi, "UTF-8").replace("+", "%20")
        val cleanShop = shopName.ifBlank { "Store" }
        val encodedPn = URLEncoder.encode(cleanShop, "UTF-8").replace("+", "%20")
        val amountStr = String.format(Locale.ROOT, "%.2f", amount.paise / 100.0)
        val encodedTn = URLEncoder.encode(transactionNote, "UTF-8").replace("+", "%20")

        val sb = StringBuilder("upi://pay?")
        sb.append("pa=").append(encodedPa)
        sb.append("&pn=").append(encodedPn)
        sb.append("&am=").append(amountStr)
        sb.append("&cu=INR")
        sb.append("&tn=").append(encodedTn)

        if (!transactionRef.isNullOrBlank()) {
            val encodedTr = URLEncoder.encode(transactionRef.trim(), "UTF-8").replace("+", "%20")
            sb.append("&tr=").append(encodedTr)
        }

        return sb.toString()
    }

    fun generateUpiQrBitmap(
        upiId: String,
        shopName: String,
        amount: Money,
        transactionRef: String? = null,
        transactionNote: String = "Store Payment",
        sizePx: Int = 360
    ): Bitmap? {
        val upiUri = buildUpiUri(upiId, shopName, amount, transactionRef, transactionNote) ?: return null

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
