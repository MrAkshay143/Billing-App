package com.billing.app.domain.model

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPricePaise: Long,
    val lineTotalPaise: Long
)

data class InvoiceReceipt(
    val shopName: String,
    val shopAddressLine1: String = "",
    val shopAddressLine2: String = "",
    val shopPhone: String = "",
    val shopGstin: String = "",
    val footerText: String = "",
    val invoiceNumber: String,
    val timestamp: Long,
    val items: List<ReceiptItem>,
    val subtotalPaise: Long,
    val discountPaise: Long = 0L,
    val taxPaise: Long = 0L,
    val grandTotalPaise: Long,
    val paymentMethod: String = "CASH",
    val cashReceivedPaise: Long = 0L,
    val changeReturnedPaise: Long = 0L,
    val duePaise: Long = 0L,
    val customerName: String? = null,
    val customerPhone: String? = null
)
