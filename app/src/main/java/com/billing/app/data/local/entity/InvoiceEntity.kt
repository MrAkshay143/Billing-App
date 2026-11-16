package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class InvoiceStatus {
    DRAFT,
    COMPLETED,
    CANCELLED
}

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotalPaise: Long,
    val taxPaise: Long = 0L,
    val discountPaise: Long = 0L,
    val grandTotalPaise: Long,
    val status: InvoiceStatus = InvoiceStatus.COMPLETED,
    val paymentMethod: String = "UPI_OR_CASH",
    val customerName: String? = null,
    val customerPhone: String? = null,
    val cashReceivedPaise: Long = 0L,
    val changeReturnedPaise: Long = 0L
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["invoiceNumber"],
            childColumns = ["invoiceNumber"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceNumber"])]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceNumber: String,
    val productId: String,
    val productName: String,
    val barcode: String,
    val unitPricePaise: Long,
    val quantity: Int,
    val lineTotalPaise: Long
)
