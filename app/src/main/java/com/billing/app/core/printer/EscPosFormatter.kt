package com.billing.app.core.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.billing.app.core.util.Money
import com.billing.app.core.util.Quantity
import com.billing.app.core.util.UnitRegistry
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.InvoiceReceipt
import com.billing.app.domain.model.ReceiptItem
import com.billing.app.domain.model.ShiftZReport
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
            append(receipt.footerText).append(receipt.customerName ?: "").append(receipt.cashierName)
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

        // Receipt Type Header
        write(BOLD_ON)
        writeLine(receipt.receiptType.ifBlank { "Tax Invoice" })
        write(BOLD_OFF)
        if (receipt.receiptType.contains("Supply", ignoreCase = true)) {
            writeLine("Composition Taxable Person, not eligible to collect tax on supplies")
        }

        writeLine(separator())

        // Invoice metadata
        write(ALIGN_LEFT)
        writeLine("Invoice: ${receipt.invoiceNumber}")
        val dateStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.ROOT).format(Date(receipt.timestamp))
        writeLine("Date: $dateStr")
        if (receipt.cashierName.isNotBlank()) {
            writeLine("Cashier: ${receipt.cashierName}")
        }
        if (!receipt.customerName.isNullOrBlank()) {
            writeLine("Customer: ${receipt.customerName}")
        }
        if (!receipt.customerPhone.isNullOrBlank()) {
            writeLine("Phone: ${receipt.customerPhone}")
        }
        writeLine(separator())

        // Column Titles
        val headerFmt = when (columns) {
            48 -> String.format(Locale.ROOT, "%-36s %11s", "Item / Details", "Total")
            else -> String.format(Locale.ROOT, "%-22s %9s", "Item / Details", "Total")
        }
        writeLine(headerFmt)
        writeLine(separator())

        // Line Items (Two-line layout)
        for (item in receipt.items) {
            val nameLine = if (item.name.length > columns) item.name.take(columns - 1) else item.name
            writeLine(nameLine)

            val qtyStr = Quantity.formatReceipt(item.quantityMilli, item.unitCode)
            val priceStr = Quantity.formatPricePerUnit(item.unitPricePaise, item.unitCode)
            val detailStr = "$qtyStr x $priceStr"
            val totalStr = formatMoneyPaise(item.lineTotalPaise)

            if (columns == 48) {
                writeLine(String.format(Locale.ROOT, "  %-34s %11s", detailStr.take(34), totalStr))
            } else {
                writeLine(String.format(Locale.ROOT, "  %-20s %9s", detailStr.take(20), totalStr))
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

        if (receipt.roundOffPaise != 0L) {
            val sign = if (receipt.roundOffPaise > 0) "+" else "-"
            writeLine("Round Off: $sign Rs. ${formatMoneyPaise(kotlin.math.abs(receipt.roundOffPaise))}")
        }

        write(BOLD_ON)
        writeLine("TOTAL: Rs. ${formatMoneyPaise(receipt.grandTotalPaise)}")
        write(BOLD_OFF)

        writeLine(separator())

        // Payment info
        write(ALIGN_LEFT)
        writeLine("Payment: ${receipt.paymentMethod}")
        if (receipt.cashReceivedPaise > 0) {
            writeLine("Cash Received: Rs. ${formatMoneyPaise(receipt.cashReceivedPaise)}")
            if (receipt.changeReturnedPaise > 0) {
                writeLine("Change Returned: Rs. ${formatMoneyPaise(receipt.changeReturnedPaise)}")
            }
        }
        if (receipt.upiPaidPaise > 0) {
            writeLine("UPI Paid: Rs. ${formatMoneyPaise(receipt.upiPaidPaise)}")
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
    fun formatInvoiceBitmap(
        receipt: InvoiceReceipt,
        widthPx: Int = 384,
        strings: com.billing.app.core.localization.AppStrings = com.billing.app.core.localization.StringsProvider.receiptStrings
    ): ByteArray {
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
        var totalLines = 14 + (receipt.items.size * 2)
        if (receipt.shopAddressLine1.isNotBlank()) totalLines++
        if (receipt.shopAddressLine2.isNotBlank()) totalLines++
        if (receipt.shopPhone.isNotBlank()) totalLines++
        if (receipt.shopGstin.isNotBlank()) totalLines++
        if (receipt.receiptType.contains("Supply", ignoreCase = true)) totalLines++
        if (receipt.cashierName.isNotBlank()) totalLines++
        if (!receipt.customerName.isNullOrBlank()) totalLines++
        if (!receipt.customerPhone.isNullOrBlank()) totalLines++
        if (receipt.discountPaise > 0) totalLines++
        if (receipt.taxPaise > 0) totalLines++
        if (receipt.roundOffPaise != 0L) totalLines++
        if (receipt.cashReceivedPaise > 0) totalLines++
        if (receipt.changeReturnedPaise > 0) totalLines++
        if (receipt.upiPaidPaise > 0) totalLines++
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

        // Receipt Type
        canvas.drawText(receipt.receiptType.ifBlank { "Tax Invoice" }, widthPx / 2f, y, paintBold)
        y += lineHeight
        if (receipt.receiptType.contains("Supply", ignoreCase = true)) {
            val prevSize = paintText.textSize
            paintText.textSize = textSize * 0.75f
            canvas.drawText("Composition Taxable Person, not eligible to collect tax on supplies", widthPx / 2f, y, paintText)
            paintText.textSize = prevSize
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
        canvas.drawText("${strings.receiptInvoice} ${receipt.invoiceNumber}", padding, y, paintText)
        y += lineHeight
        val dateStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.ROOT).format(Date(receipt.timestamp))
        canvas.drawText("${strings.receiptDate} $dateStr", padding, y, paintText)
        y += lineHeight

        if (receipt.cashierName.isNotBlank()) {
            canvas.drawText("${strings.receiptCashier} ${receipt.cashierName}", padding, y, paintText)
            y += lineHeight
        }
        if (!receipt.customerName.isNullOrBlank()) {
            canvas.drawText("${strings.receiptCustomer} ${receipt.customerName}", padding, y, paintText)
            y += lineHeight
        }
        if (!receipt.customerPhone.isNullOrBlank()) {
            canvas.drawText("${strings.receiptPhone} ${receipt.customerPhone}", padding, y, paintText)
            y += lineHeight
        }

        drawDivider(y)
        y += lineHeight * 0.8f

        // Items Header
        canvas.drawText(strings.receiptItemDetails, padding, y, paintBold)
        paintBold.textAlign = Paint.Align.RIGHT
        canvas.drawText(strings.receiptTotal, widthPx - padding, y, paintBold)
        y += lineHeight * 0.9f
        drawDivider(y)
        y += lineHeight * 0.8f

        val receiptLang = com.billing.app.core.localization.StringsProvider.receiptLang
        // Item Rows (Two-line layout)
        for (item in receipt.items) {
            paintText.textAlign = Paint.Align.LEFT
            val truncatedName = if (item.name.length > 30) item.name.take(28) + ".." else item.name
            canvas.drawText(truncatedName, padding, y, paintText)
            y += lineHeight

            val qtyStr = Quantity.formatReceipt(item.quantityMilli, item.unitCode, receiptLang)
            val priceStr = Quantity.formatPricePerUnit(item.unitPricePaise, item.unitCode, receiptLang)
            val detailStr = "$qtyStr x $priceStr"
            val totalStr = formatMoneyPaise(item.lineTotalPaise)

            canvas.drawText(detailStr, padding + 10f, y, paintText)
            paintText.textAlign = Paint.Align.RIGHT
            canvas.drawText(totalStr, widthPx - padding, y, paintText)
            y += lineHeight
        }

        drawDivider(y)
        y += lineHeight * 0.8f

        // Totals
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("${strings.receiptSubtotal} Rs. ${formatMoneyPaise(receipt.subtotalPaise)}", widthPx - padding, y, paintText)
        y += lineHeight

        if (receipt.discountPaise > 0) {
            canvas.drawText("${strings.receiptDiscount} -Rs. ${formatMoneyPaise(receipt.discountPaise)}", widthPx - padding, y, paintText)
            y += lineHeight
        }

        if (receipt.taxPaise > 0) {
            canvas.drawText("${strings.receiptTax} Rs. ${formatMoneyPaise(receipt.taxPaise)}", widthPx - padding, y, paintText)
            y += lineHeight
        }

        if (receipt.roundOffPaise != 0L) {
            val sign = if (receipt.roundOffPaise > 0) "+" else "-"
            canvas.drawText("${strings.receiptRoundOff} $sign Rs. ${formatMoneyPaise(kotlin.math.abs(receipt.roundOffPaise))}", widthPx - padding, y, paintText)
            y += lineHeight
        }

        paintBold.textAlign = Paint.Align.RIGHT
        canvas.drawText("${strings.receiptTotalPayable} Rs. ${formatMoneyPaise(receipt.grandTotalPaise)}", widthPx - padding, y, paintBold)
        y += lineHeight * 1.1f

        drawDivider(y)
        y += lineHeight * 0.8f

        // Payment
        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("${strings.receiptPayment} ${receipt.paymentMethod}", padding, y, paintText)
        y += lineHeight

        if (receipt.cashReceivedPaise > 0) {
            canvas.drawText("${strings.receiptCashReceived} Rs. ${formatMoneyPaise(receipt.cashReceivedPaise)}", padding, y, paintText)
            y += lineHeight
            if (receipt.changeReturnedPaise > 0) {
                canvas.drawText("${strings.receiptChangeReturned} Rs. ${formatMoneyPaise(receipt.changeReturnedPaise)}", padding, y, paintText)
                y += lineHeight
            }
        }

        if (receipt.upiPaidPaise > 0) {
            canvas.drawText("${strings.receiptUpiPaid} Rs. ${formatMoneyPaise(receipt.upiPaidPaise)}", padding, y, paintText)
            y += lineHeight
        }

        if (receipt.duePaise > 0) {
            canvas.drawText("${strings.receiptDueBalance} Rs. ${formatMoneyPaise(receipt.duePaise)}", padding, y, paintBold)
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
        canvas.drawText(strings.receiptThankYou, widthPx / 2f, y, paintText)

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
                quantityMilli = it.quantityMilli,
                unitCode = UnitRegistry.resolve(it.product.unit).code,
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
            invoiceNumber = "INV-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date()),
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

    /**
     * Formats a Shift Z-Report automatically choosing between clean raster bitmap
     * (for Indic/non-ASCII fonts) or standard ESC/POS text bytes.
     */
    fun formatZReportAuto(zReport: ShiftZReport, shopProfile: ShopProfile, paperWidth: String): ByteArray {
        val is80mm = paperWidth.contains("80")
        val columns = if (is80mm) 48 else 32
        val widthPx = if (is80mm) 576 else 384

        val allText = buildString {
            append(shopProfile.name).append(shopProfile.addressLine1).append(shopProfile.addressLine2)
            append(zReport.managerName)
            zReport.cashierBreakdowns.forEach { append(it.userName) }
        }

        return if (containsNonAscii(allText)) {
            formatZReportBitmap(zReport, shopProfile, widthPx)
        } else {
            formatZReportText(zReport, shopProfile, columns)
        }
    }

    /**
     * Formats a Shift Z-Report into standard ESC/POS text bytes.
     */
    fun formatZReportText(zReport: ShiftZReport, shopProfile: ShopProfile, columns: Int = 32): ByteArray {
        val out = ByteArrayOutputStream()

        fun write(bytes: ByteArray) = out.write(bytes)
        fun writeLine(text: String) {
            val sanitized = text.replace("₹", "Rs. ")
            out.write(sanitized.toByteArray(Charset.forName("CP437")))
            out.write(LF)
        }

        fun separator(): String = "-".repeat(columns)

        fun twoCol(left: String, right: String): String {
            val maxLeft = columns - right.length - 1
            val truncatedLeft = if (left.length > maxLeft) left.take(maxLeft - 1) + "." else left
            val spaces = " ".repeat((columns - truncatedLeft.length - right.length).coerceAtLeast(1))
            return "$truncatedLeft$spaces$right"
        }

        write(INIT)

        // Store Header
        write(ALIGN_CENTER)
        write(BOLD_ON)
        write(TEXT_LARGE)
        writeLine(shopProfile.name.ifBlank { "STORE" })

        write(TEXT_NORMAL)
        write(BOLD_OFF)
        if (shopProfile.addressLine1.isNotBlank()) writeLine(shopProfile.addressLine1)
        if (shopProfile.addressLine2.isNotBlank()) writeLine(shopProfile.addressLine2)
        if (shopProfile.phoneNumber.isNotBlank()) writeLine("Ph: ${shopProfile.phoneNumber}")

        write(LF)
        write(BOLD_ON)
        writeLine("Z-REPORT / SHIFT SUMMARY")
        write(BOLD_OFF)
        writeLine(separator())

        // Shift Metadata
        write(ALIGN_LEFT)
        writeLine("Shift ID: #${zReport.shift.id}")
        val sdf = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US)
        writeLine("Opened: ${sdf.format(Date(zReport.shift.openedAt))}")
        val closedStr = zReport.shift.closedAt?.let { sdf.format(Date(it)) } ?: "STILL OPEN"
        writeLine("Closed: $closedStr")
        writeLine("Manager: ${zReport.managerName}")
        writeLine(separator())

        // CASH RECONCILIATION
        write(BOLD_ON)
        write(ALIGN_CENTER)
        writeLine("CASH RECONCILIATION")
        write(BOLD_OFF)
        write(ALIGN_LEFT)
        writeLine(twoCol("Opening Float:", "Rs. ${formatMoneyPaise(zReport.openingFloatPaise)}"))
        writeLine(twoCol("Cash Sales:", "+Rs. ${formatMoneyPaise(zReport.cashSalesPaise)}"))
        if (zReport.khataCollectionsPaise > 0) {
            writeLine(twoCol("Khata Collection:", "+Rs. ${formatMoneyPaise(zReport.khataCollectionsPaise)}"))
        }
        if (zReport.otherCashInPaise > 0) {
            writeLine(twoCol("Other Cash In:", "+Rs. ${formatMoneyPaise(zReport.otherCashInPaise)}"))
        }
        if (zReport.expenseCashOutPaise > 0) {
            writeLine(twoCol("Expenses Out:", "-Rs. ${formatMoneyPaise(zReport.expenseCashOutPaise)}"))
        }
        if (zReport.supplierPaymentPaise > 0) {
            writeLine(twoCol("Supplier Payments:", "-Rs. ${formatMoneyPaise(zReport.supplierPaymentPaise)}"))
        }
        if (zReport.otherCashOutPaise > 0) {
            writeLine(twoCol("Other Cash Out:", "-Rs. ${formatMoneyPaise(zReport.otherCashOutPaise)}"))
        }

        write(BOLD_ON)
        writeLine(twoCol("Expected Cash:", "Rs. ${formatMoneyPaise(zReport.expectedCashPaise)}"))
        writeLine(twoCol("Actual Cash:", "Rs. ${formatMoneyPaise(zReport.actualCashPaise)}"))

        val varianceStatus = when {
            zReport.variancePaise > 0 -> "EXCESS"
            zReport.variancePaise < 0 -> "SHORTAGE"
            else -> "BALANCED"
        }
        val varianceSign = if (zReport.variancePaise > 0) "+" else if (zReport.variancePaise < 0) "-" else ""
        val varianceStr = "$varianceSign Rs. ${formatMoneyPaise(kotlin.math.abs(zReport.variancePaise))} ($varianceStatus)"
        writeLine(twoCol("Variance:", varianceStr))
        write(BOLD_OFF)
        writeLine(separator())

        // SALES SUMMARY
        write(BOLD_ON)
        write(ALIGN_CENTER)
        writeLine("SALES SUMMARY")
        write(BOLD_OFF)
        write(ALIGN_LEFT)
        writeLine(twoCol("Invoices Count:", "${zReport.invoicesCount}"))
        writeLine(twoCol("Items Sold:", "${zReport.itemsSoldCount}"))
        writeLine(twoCol("Cash Sales:", "Rs. ${formatMoneyPaise(zReport.cashSalesPaise)}"))
        writeLine(twoCol("UPI Sales:", "Rs. ${formatMoneyPaise(zReport.upiSalesPaise)}"))
        writeLine(twoCol("Credit Sales:", "Rs. ${formatMoneyPaise(zReport.creditSalesPaise)}"))
        write(BOLD_ON)
        writeLine(twoCol("Total Turnover:", "Rs. ${formatMoneyPaise(zReport.totalTurnoverPaise)}"))
        write(BOLD_OFF)
        writeLine(separator())

        // CASHIER BREAKDOWN
        if (zReport.cashierBreakdowns.isNotEmpty()) {
            write(BOLD_ON)
            write(ALIGN_CENTER)
            writeLine("CASHIER BREAKDOWN")
            write(BOLD_OFF)
            write(ALIGN_LEFT)
            val headerFmt = when (columns) {
                48 -> String.format(Locale.US, "%-26s %10s %10s", "Cashier", "Bills", "Total")
                else -> String.format(Locale.US, "%-16s %6s %8s", "Cashier", "Bills", "Total")
            }
            writeLine(headerFmt)
            writeLine(separator())

            for (cashier in zReport.cashierBreakdowns) {
                val totalStr = formatMoneyPaise(cashier.totalSalesPaise)
                if (columns == 48) {
                    writeLine(String.format(Locale.US, "%-26.26s %10d %10s", cashier.userName, cashier.invoiceCount, totalStr))
                } else {
                    writeLine(String.format(Locale.US, "%-16.16s %6d %8s", cashier.userName, cashier.invoiceCount, totalStr))
                }
            }
            writeLine(separator())
        }

        // End of Z-Report
        write(ALIGN_CENTER)
        writeLine("*** END OF Z-REPORT ***")
        write(LF)
        write(LF)
        write(CUT_PAPER)

        return out.toByteArray()
    }

    /**
     * Formats a Shift Z-Report into a monochrome Bitmap ESC/POS raster for native Indic script printing.
     */
    fun formatZReportBitmap(zReport: ShiftZReport, shopProfile: ShopProfile, widthPx: Int = 384): ByteArray {
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

        var totalLines = 26 + (zReport.cashierBreakdowns.size * 2)
        if (shopProfile.addressLine1.isNotBlank()) totalLines++
        if (shopProfile.addressLine2.isNotBlank()) totalLines++
        if (shopProfile.phoneNumber.isNotBlank()) totalLines++
        if (zReport.khataCollectionsPaise > 0) totalLines++
        if (zReport.otherCashInPaise > 0) totalLines++
        if (zReport.expenseCashOutPaise > 0) totalLines++
        if (zReport.supplierPaymentPaise > 0) totalLines++
        if (zReport.otherCashOutPaise > 0) totalLines++

        val totalHeight = (totalLines * lineHeight + padding * 4).toInt()

        val bitmap = Bitmap.createBitmap(widthPx, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = padding + headerSize

        // Shop Header
        canvas.drawText(shopProfile.name.ifBlank { "STORE" }, widthPx / 2f, y, paintHeader)
        y += lineHeight

        paintText.textAlign = Paint.Align.CENTER
        if (shopProfile.addressLine1.isNotBlank()) {
            canvas.drawText(shopProfile.addressLine1, widthPx / 2f, y, paintText)
            y += lineHeight
        }
        if (shopProfile.addressLine2.isNotBlank()) {
            canvas.drawText(shopProfile.addressLine2, widthPx / 2f, y, paintText)
            y += lineHeight
        }
        if (shopProfile.phoneNumber.isNotBlank()) {
            canvas.drawText("Ph: ${shopProfile.phoneNumber}", widthPx / 2f, y, paintText)
            y += lineHeight
        }

        canvas.drawText("Z-REPORT / SHIFT SUMMARY", widthPx / 2f, y, paintBold)
        y += lineHeight

        fun drawDivider(currentY: Float) {
            canvas.drawLine(padding, currentY, widthPx - padding, currentY, paintText)
        }

        drawDivider(y)
        y += lineHeight * 0.8f

        // Shift Metadata
        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("Shift ID: #${zReport.shift.id}", padding, y, paintText)
        y += lineHeight
        val sdf = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US)
        canvas.drawText("Opened: ${sdf.format(Date(zReport.shift.openedAt))}", padding, y, paintText)
        y += lineHeight
        val closedStr = zReport.shift.closedAt?.let { sdf.format(Date(it)) } ?: "STILL OPEN"
        canvas.drawText("Closed: $closedStr", padding, y, paintText)
        y += lineHeight
        canvas.drawText("Manager: ${zReport.managerName}", padding, y, paintText)
        y += lineHeight

        drawDivider(y)
        y += lineHeight * 0.8f

        // CASH RECONCILIATION
        paintBold.textAlign = Paint.Align.CENTER
        canvas.drawText("CASH RECONCILIATION", widthPx / 2f, y, paintBold)
        y += lineHeight

        fun drawTwoCol(label: String, value: String, isBold: Boolean = false) {
            val paint = if (isBold) paintBold else paintText
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, padding, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(value, widthPx - padding, y, paint)
            y += lineHeight
        }

        drawTwoCol("Opening Float:", "Rs. ${formatMoneyPaise(zReport.openingFloatPaise)}")
        drawTwoCol("Cash Sales:", "+Rs. ${formatMoneyPaise(zReport.cashSalesPaise)}")
        if (zReport.khataCollectionsPaise > 0) {
            drawTwoCol("Khata Collection:", "+Rs. ${formatMoneyPaise(zReport.khataCollectionsPaise)}")
        }
        if (zReport.otherCashInPaise > 0) {
            drawTwoCol("Other Cash In:", "+Rs. ${formatMoneyPaise(zReport.otherCashInPaise)}")
        }
        if (zReport.expenseCashOutPaise > 0) {
            drawTwoCol("Expenses Out:", "-Rs. ${formatMoneyPaise(zReport.expenseCashOutPaise)}")
        }
        if (zReport.supplierPaymentPaise > 0) {
            drawTwoCol("Supplier Payments:", "-Rs. ${formatMoneyPaise(zReport.supplierPaymentPaise)}")
        }
        if (zReport.otherCashOutPaise > 0) {
            drawTwoCol("Other Cash Out:", "-Rs. ${formatMoneyPaise(zReport.otherCashOutPaise)}")
        }

        drawTwoCol("Expected Cash:", "Rs. ${formatMoneyPaise(zReport.expectedCashPaise)}", isBold = true)
        drawTwoCol("Actual Cash:", "Rs. ${formatMoneyPaise(zReport.actualCashPaise)}", isBold = true)

        val varianceStatus = when {
            zReport.variancePaise > 0 -> "EXCESS"
            zReport.variancePaise < 0 -> "SHORTAGE"
            else -> "BALANCED"
        }
        val varianceSign = if (zReport.variancePaise > 0) "+" else if (zReport.variancePaise < 0) "-" else ""
        val varianceStr = "$varianceSign Rs. ${formatMoneyPaise(kotlin.math.abs(zReport.variancePaise))} ($varianceStatus)"
        drawTwoCol("Variance:", varianceStr, isBold = true)

        drawDivider(y)
        y += lineHeight * 0.8f

        // SALES SUMMARY
        paintBold.textAlign = Paint.Align.CENTER
        canvas.drawText("SALES SUMMARY", widthPx / 2f, y, paintBold)
        y += lineHeight

        drawTwoCol("Invoices Count:", "${zReport.invoicesCount}")
        drawTwoCol("Items Sold:", "${zReport.itemsSoldCount}")
        drawTwoCol("Cash Sales:", "Rs. ${formatMoneyPaise(zReport.cashSalesPaise)}")
        drawTwoCol("UPI Sales:", "Rs. ${formatMoneyPaise(zReport.upiSalesPaise)}")
        drawTwoCol("Credit Sales:", "Rs. ${formatMoneyPaise(zReport.creditSalesPaise)}")
        drawTwoCol("Total Turnover:", "Rs. ${formatMoneyPaise(zReport.totalTurnoverPaise)}", isBold = true)

        drawDivider(y)
        y += lineHeight * 0.8f

        // CASHIER BREAKDOWN
        if (zReport.cashierBreakdowns.isNotEmpty()) {
            paintBold.textAlign = Paint.Align.CENTER
            canvas.drawText("CASHIER BREAKDOWN", widthPx / 2f, y, paintBold)
            y += lineHeight

            paintBold.textAlign = Paint.Align.LEFT
            canvas.drawText("Cashier", padding, y, paintBold)
            paintBold.textAlign = Paint.Align.RIGHT
            canvas.drawText("Bills", widthPx - padding - 90f, y, paintBold)
            canvas.drawText("Total", widthPx - padding, y, paintBold)
            y += lineHeight * 0.9f
            drawDivider(y)
            y += lineHeight * 0.8f

            for (cashier in zReport.cashierBreakdowns) {
                paintText.textAlign = Paint.Align.LEFT
                val name = if (cashier.userName.length > 16) cashier.userName.take(14) + ".." else cashier.userName
                canvas.drawText(name, padding, y, paintText)
                paintText.textAlign = Paint.Align.RIGHT
                canvas.drawText("${cashier.invoiceCount}", widthPx - padding - 90f, y, paintText)
                canvas.drawText(formatMoneyPaise(cashier.totalSalesPaise), widthPx - padding, y, paintText)
                y += lineHeight
            }

            drawDivider(y)
            y += lineHeight * 0.8f
        }

        // Footer
        paintText.textAlign = Paint.Align.CENTER
        canvas.drawText("*** END OF Z-REPORT ***", widthPx / 2f, y, paintText)

        try {
            return convertBitmapToEscPosRaster(bitmap)
        } finally {
            bitmap.recycle()
        }
    }
}
