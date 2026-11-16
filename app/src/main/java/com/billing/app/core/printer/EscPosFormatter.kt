package com.billing.app.core.printer

import com.billing.app.core.util.Money
import com.billing.app.domain.model.CartItem
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
    private val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x10) // GS V 65 16: Partial cut with feed

    fun formatReceipt(
        shop: ShopProfile,
        items: List<CartItem>,
        total: Money,
        columns: Int = 32 // 32 for 58mm, 48 for 80mm
    ): ByteArray {
        val out = ByteArrayOutputStream()

        fun write(bytes: ByteArray) = out.write(bytes)
        fun writeLine(text: String) {
            // Replace ₹ with "Rs." to prevent corrupt bytes on thermal printers
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
        writeLine(shop.name.ifBlank { "SHOP" })

        write(TEXT_NORMAL)
        write(BOLD_OFF)
        if (shop.addressLine1.isNotBlank()) writeLine(shop.addressLine1)
        if (shop.addressLine2.isNotBlank()) writeLine(shop.addressLine2)
        if (shop.phoneNumber.isNotBlank()) writeLine("Ph: ${shop.phoneNumber}")

        val dateStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US).format(Date())
        writeLine(dateStr)
        writeLine(separator())

        // Column Titles
        write(ALIGN_LEFT)
        val headerFmt = when (columns) {
            48 -> String.format("%-26s %10s %10s", "Item", "Price", "Total")
            else -> String.format("%-16s %7s %7s", "Item", "Price", "Total")
        }
        writeLine(headerFmt)
        writeLine(separator())

        // Line Items
        for (item in items) {
            val name = "${item.quantity}x ${item.product.name}"
            val priceStr = item.product.price.format(includeSymbol = false)
            val totalStr = item.total.format(includeSymbol = false)

            if (columns == 48) {
                writeLine(String.format("%-26.26s %10s %10s", name, priceStr, totalStr))
            } else {
                writeLine(String.format("%-16.16s %7s %7s", name, priceStr, totalStr))
            }
        }

        writeLine(separator())

        // Total
        write(ALIGN_RIGHT)
        write(BOLD_ON)
        writeLine("TOTAL: Rs. ${total.format(includeSymbol = false)}")
        write(BOLD_OFF)
        write(LF)

        // Footer
        write(ALIGN_CENTER)
        if (shop.footerText.isNotBlank()) {
            writeLine(shop.footerText)
        }
        writeLine("Thank You! Visit Again")
        write(LF)
        write(LF)
        write(CUT_PAPER)

        return out.toByteArray()
    }
}
