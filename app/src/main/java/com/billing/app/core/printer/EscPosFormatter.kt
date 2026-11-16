package com.billing.app.core.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.billing.app.core.util.Money
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.InvoiceReceipt
import com.billing.app.domain.model.ReceiptItem
import com.billing.app.domain.model.ShopProfile
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EscPosFormatter {
    private val INIT = byteArrayOf(0x1B, 0x40)
    private val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    private val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val TEXT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)
    private val TEXT_LARGE = byteArrayOf(0x1D, 0x21, 0x11)
    private val LF = byteArrayOf(0x0A)
    private val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x10) // Partial cut with feed

    fun containsNonAscii(text: String): Boolean {
        return text.any { it.code > 127 }
    }

    fun formatMoneyPaise(paise: Long): String {
        return Money(paise).format(includeSymbol = false)
    }

    /**
     * Formats an invoice receipt automatically choosing between clean raster bitmap (for Indic/non-ASCII fonts)
     * or standard ESC/POS text bytes.
     */
    fun formatReceiptAuto(receipt: InvoiceReceipt, paperWidth: String): ByteArray {
        val is80mm = paperWidth.contains("80")
        val columns = if (is80mm) 48 else 32
        val widthPx = if (is80mm) 576 else 384

        // Check if any field contains Hindi or Bengali or Indic characters
        val allText = buildString {
            append(receipt.shopName).append(receipt.shopAddressLine1).append(receipt.shopAddressLine2)
            append(receipt.footerText).append(receipt.customerName ?: "")
            receipt.items.forEach { append(it.name) }
        }

        return if (containsNonAscii(allText)) {
            formatInvoiceBitmap(receipt, widthPx)
        } else {
            formatInvoiceText(receipt, columns)
        }
    }

    /**
     * ESC/POS text-based receipt formatting
     */
    fun formatInvoiceText(receipt: InvoiceReceipt, columns: Int = 32): ByteArray {
        val out = ByteArrayOutputStream()

        fun write(bytes: ByteArray) = out.write(bytes)
        fun writeLine(text: String) {
            val sanitized = text.replace("₹", "Rs. ")
            out.write(sanitized.toByteArray(Charset.forName("CP437")))
            out.write(LF)
        }

        fun separator(): String = "-".repeat(columns)

        write(INIT)

        // Shop Header
        write(ALIGN_CENTER)
        write(BOLD_ON)
        write(TEXT_LARGE)
        writeLine(receipt.shopName.ifBlank { "STORE" })

        write(TEXT_NORMAL)
        write(BOLD_OFF)
        if (receipt.shopAddressLine1.isNotBlank()) writeLine(receipt.shopAddressLine1)
        if (receipt.shopAddressLine2.isNotBlank()) writeLine(receipt.shopAddressLine2)
        if (receipt.shopPhone.isNotBlank()) writeLine("Ph: ${receipt.shopPhone}")
        if (receipt.shopGstin.isNotBlank()) writeLine("GSTIN: ${receipt.shopGstin}")

        writeLine(separator())

        // Invoice metadata
        write(ALIGN_LEFT)
        writeLine("Invoice: ${receipt.invoiceNumber}")
        val dateStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US).format(Date(receipt.timestamp))
        writeLine("Date: $dateStr")
        if (!receipt.customerName.isNullOrBlank()) {
            writeLine("Customer: ${receipt.customerName}")
        }
        if (!receipt.customerPhone.isNullOrBlank()) {
            writeLine("Phone: ${receipt.customerPhone}")
        }
        writeLine(separator())

        // Column Titles
        val headerFmt = when (columns) {
            48 -> String.format(Locale.US, "%-26s %10s %10s", "Item", "Price", "Total")
            else -> String.format(Locale.US, "%-16s %7s %7s", "Item", "Price", "Total")
        }
        writeLine(headerFmt)
        writeLine(separator())

        // Line Items
        for (item in receipt.items) {
            val name = "${item.quantity}x ${item.name}"
            val priceStr = formatMoneyPaise(item.unitPricePaise)
            val totalStr = formatMoneyPaise(item.lineTotalPaise)

            if (columns == 48) {
                writeLine(String.format(Locale.US, "%-26.26s %10s %10s", name, priceStr, totalStr))
            } else {
                writeLine(String.format(Locale.US, "%-16.16s %7s %7s", name, priceStr, totalStr))
            }
        }

        writeLine(separator())

        // Totals & Financial Breakdown
        write(ALIGN_RIGHT)
        writeLine("Subtotal: Rs. ${formatMoneyPaise(receipt.subtotalPaise)}")

        if (receipt.discountPaise > 0) {
            writeLine("Discount: -Rs. ${formatMoneyPaise(receipt.discountPaise)}")
        }

        if (receipt.taxPaise > 0) {
            writeLine("Tax / GST: Rs. ${formatMoneyPaise(receipt.taxPaise)}")
        }

        write(BOLD_ON)
        writeLine("TOTAL: Rs. ${formatMoneyPaise(receipt.grandTotalPaise)}")
        write(BOLD_OFF)

        writeLine(separator())

        // Payment info
        write(ALIGN_LEFT)
        writeLine("Payment: ${receipt.paymentMethod}")
        if (receipt.paymentMethod == "CASH" && receipt.cashReceivedPaise > 0) {
            writeLine("Cash Received: Rs. ${formatMoneyPaise(receipt.cashReceivedPaise)}")
            writeLine("Change Returned: Rs. ${formatMoneyPaise(receipt.changeReturnedPaise)}")
        }
        if (receipt.duePaise > 0) {
            write(BOLD_ON)
            writeLine("Balance Due: Rs. ${formatMoneyPaise(receipt.duePaise)}")
            write(BOLD_OFF)
        }

        writeLine(separator())

        // Footer
        write(ALIGN_CENTER)
        if (receipt.footerText.isNotBlank()) {
            writeLine(receipt.footerText)
        }
        writeLine("Thank You! Visit Again")
        write(LF)
        write(LF)
        write(CUT_PAPER)

        return out.toByteArray()
    }

    /**
     * Converts an Android Bitmap into standard ESC/POS raster bit image command:
     * GS v 0 m xL xH yL yH [data...]
     */
    fun convertBitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerLine = (width + 7) / 8

        val out = ByteArrayOutputStream()
        out.write(INIT)
        out.write(ALIGN_CENTER)

        // GS v 0 m xL xH yL yH
        out.write(0x1D)
        out.write(0x76)
        out.write(0x30)
        out.write(0x00) // Mode: normal

        out.write(bytesPerLine and 0xFF)
        out.write((bytesPerLine shr 8) and 0xFF)
        out.write(height and 0xFF)
        out.write((height shr 8) and 0xFF)

        // Bulk pixel fetch avoids millions of JNI calls (Fixes P3)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            val yOffset = y * width
            for (byteIdx in 0 until bytesPerLine) {
                var byteVal = 0
                for (bit in 0 until 8) {
                    val x = byteIdx * 8 + bit
                    if (x < width) {
                        val pixel = pixels[yOffset + x]
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        val luminance = (299 * r + 587 * g + 114 * b) / 1000
                        if (luminance < 160) {
                            byteVal = byteVal or (1 shl (7 - bit))
                        }
                    }
                }
                out.write(byteVal)
            }
        }

        out.write(LF)
        out.write(LF)
        out.write(CUT_PAPER)
        return out.toByteArray()
    }

    /**
     * Renders a full invoice into a high-resolution monochrome Bitmap for native Indic (Hindi/Bengali) printing
     */
    fun formatInvoiceBitmap(receipt: InvoiceReceipt, widthPx: Int = 384): ByteArray {
        val textSize = if (widthPx > 400) 24f else 20f
        val headerSize = textSize * 1.4f
        val lineHeight = textSize * 1.45f
        val padding = 16f

        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
            typeface = Typeface.DEFAULT
        }

        val paintHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = headerSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val paintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
        }

        // Measure content height
        var totalLines = 12 + (receipt.items.size * 2)
        if (receipt.shopAddressLine1.isNotBlank()) totalLines++
        if (receipt.shopAddressLine2.isNotBlank()) totalLines++
        if (receipt.shopPhone.isNotBlank()) totalLines++
        if (receipt.shopGstin.isNotBlank()) totalLines++
        if (!receipt.customerName.isNullOrBlank()) totalLines++
        if (!receipt.customerPhone.isNullOrBlank()) totalLines++
        if (receipt.discountPaise > 0) totalLines++
        if (receipt.taxPaise > 0) totalLines++
        if (receipt.duePaise > 0) totalLines++

        val totalHeight = (totalLines * lineHeight + padding * 3).toInt()

        val bitmap = Bitmap.createBitmap(widthPx, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = padding + headerSize

        // Shop Header
        canvas.drawText(receipt.shopName.ifBlank { "STORE" }, widthPx / 2f, y, paintHeader)
        y += lineHeight

        paintText.textAlign = Paint.Align.CENTER
        if (receipt.shopAddressLine1.isNotBlank()) {
            canvas.drawText(receipt.shopAddressLine1, widthPx / 2f, y, paintText)
            y += lineHeight
        }
        if (receipt.shopAddressLine2.isNotBlank()) {
            canvas.drawText(receipt.shopAddressLine2, widthPx / 2f, y, paintText)
            y += lineHeight
        }
        if (receipt.shopPhone.isNotBlank()) {
            canvas.drawText("Ph: ${receipt.shopPhone}", widthPx / 2f, y, paintText)
            y += lineHeight
        }
        if (receipt.shopGstin.isNotBlank()) {
            canvas.drawText("GSTIN: ${receipt.shopGstin}", widthPx / 2f, y, paintText)
            y += lineHeight
        }

        // Divider
        fun drawDivider(currentY: Float) {
            canvas.drawLine(padding, currentY, widthPx - padding, currentY, paintText)
        }

        drawDivider(y)
        y += lineHeight * 0.8f

        // Metadata
        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("Invoice: ${receipt.invoiceNumber}", padding, y, paintText)
        y += lineHeight
        val dateStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US).format(Date(receipt.timestamp))
        canvas.drawText("Date: $dateStr", padding, y, paintText)
        y += lineHeight

        if (!receipt.customerName.isNullOrBlank()) {
            canvas.drawText("Customer: ${receipt.customerName}", padding, y, paintText)
            y += lineHeight
        }
        if (!receipt.customerPhone.isNullOrBlank()) {
            canvas.drawText("Phone: ${receipt.customerPhone}", padding, y, paintText)
            y += lineHeight
        }

        drawDivider(y)
        y += lineHeight * 0.8f

        // Items Header
        canvas.drawText("Item", padding, y, paintBold)
        paintBold.textAlign = Paint.Align.RIGHT
        canvas.drawText("Price", widthPx - padding - 90f, y, paintBold)
        canvas.drawText("Total", widthPx - padding, y, paintBold)
        y += lineHeight * 0.9f
        drawDivider(y)
        y += lineHeight * 0.8f

        // Item Rows
        for (item in receipt.items) {
            paintText.textAlign = Paint.Align.LEFT
            val itemName = "${item.quantity}x ${item.name}"
            val truncatedName = if (itemName.length > 20) itemName.take(18) + ".." else itemName
            canvas.drawText(truncatedName, padding, y, paintText)

            paintText.textAlign = Paint.Align.RIGHT
            val priceStr = formatMoneyPaise(item.unitPricePaise)
            val totalStr = formatMoneyPaise(item.lineTotalPaise)
            canvas.drawText(priceStr, widthPx - padding - 90f, y, paintText)
            canvas.drawText(totalStr, widthPx - padding, y, paintText)
            y += lineHeight
        }

        drawDivider(y)
        y += lineHeight * 0.8f

        // Totals
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("Subtotal: Rs. ${formatMoneyPaise(receipt.subtotalPaise)}", widthPx - padding, y, paintText)
        y += lineHeight

        if (receipt.discountPaise > 0) {
            canvas.drawText("Discount: -Rs. ${formatMoneyPaise(receipt.discountPaise)}", widthPx - padding, y, paintText)
            y += lineHeight
        }

        if (receipt.taxPaise > 0) {
            canvas.drawText("Tax: Rs. ${formatMoneyPaise(receipt.taxPaise)}", widthPx - padding, y, paintText)
            y += lineHeight
        }

        paintBold.textAlign = Paint.Align.RIGHT
        canvas.drawText("TOTAL: Rs. ${formatMoneyPaise(receipt.grandTotalPaise)}", widthPx - padding, y, paintBold)
        y += lineHeight * 1.1f

        drawDivider(y)
        y += lineHeight * 0.8f

        // Payment
        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("Payment: ${receipt.paymentMethod}", padding, y, paintText)
        y += lineHeight

        if (receipt.paymentMethod == "CASH" && receipt.cashReceivedPaise > 0) {
            canvas.drawText("Cash: Rs. ${formatMoneyPaise(receipt.cashReceivedPaise)}", padding, y, paintText)
            y += lineHeight
            canvas.drawText("Change: Rs. ${formatMoneyPaise(receipt.changeReturnedPaise)}", padding, y, paintText)
            y += lineHeight
        }

        if (receipt.duePaise > 0) {
            canvas.drawText("Due Balance: Rs. ${formatMoneyPaise(receipt.duePaise)}", padding, y, paintBold)
            y += lineHeight
        }

        drawDivider(y)
        y += lineHeight * 0.8f

        // Footer
        paintText.textAlign = Paint.Align.CENTER
        if (receipt.footerText.isNotBlank()) {
            canvas.drawText(receipt.footerText, widthPx / 2f, y, paintText)
            y += lineHeight
        }
        canvas.drawText("Thank You! Visit Again", widthPx / 2f, y, paintText)

        try {
            return convertBitmapToEscPosRaster(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Backward-compatible simple format method for legacy callers
     */
    fun formatReceipt(
        shop: ShopProfile,
        items: List<CartItem>,
        total: Money,
        columns: Int = 32
    ): ByteArray {
        val receiptItems = items.map {
            ReceiptItem(
                name = it.product.name,
                quantity = it.quantity,
                unitPricePaise = it.product.price.paise,
                lineTotalPaise = it.total.paise
            )
        }
        val receipt = InvoiceReceipt(
            shopName = shop.name,
            shopAddressLine1 = shop.addressLine1,
            shopAddressLine2 = shop.addressLine2,
            shopPhone = shop.phoneNumber,
            shopGstin = "",
            footerText = shop.footerText,
            invoiceNumber = "INV-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()),
            timestamp = System.currentTimeMillis(),
            items = receiptItems,
            subtotalPaise = total.paise,
            discountPaise = 0L,
            taxPaise = 0L,
            grandTotalPaise = total.paise,
            paymentMethod = "CASH"
        )
        return formatInvoiceText(receipt, columns)
    }
}
