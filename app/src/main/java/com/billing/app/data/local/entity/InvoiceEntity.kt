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

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["status"]),
        Index(value = ["customerName"]),
        Index(value = ["invoiceType"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotalPaise: Long,
    val taxPaise: Long = 0L,
    val discountPaise: Long = 0L,
    val grandTotalPaise: Long,
    val duePaise: Long = 0L,
    val status: InvoiceStatus = InvoiceStatus.COMPLETED,
    val paymentMethod: String = "UPI_OR_CASH",
    val customerName: String? = null,
    val customerPhone: String? = null,
    val cashReceivedPaise: Long = 0L,
    val changeReturnedPaise: Long = 0L,
    val invoiceType: String = "SALE",
    val itemsCount: Int = 1
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
