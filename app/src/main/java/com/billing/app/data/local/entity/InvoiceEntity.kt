package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
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
        Index(value = ["invoiceType"]),
        Index(value = ["shiftId"]),
        Index(value = ["customerId"])
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
    val itemsCount: Int = 1,
    val shiftId: Long? = null,
    val createdByUserId: String? = null,
    val customerId: String? = null,
    val upiPaidPaise: Long = 0L,
    val originalDuePaise: Long = 0L,
    val roundOffPaise: Long = 0L,
    val voidedAt: Long? = null,
    val voidedByUserId: String? = null,
    val voidReason: String? = null
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceId: Long = 0L,
    val productId: String? = null,
    val productName: String,
    val barcode: String = "",
    val unitPricePaise: Long,
    val quantityMilli: Long = 1000L,
    val lineTotalPaise: Long,
    val unit: String = "PCS",
    val discountPaise: Long = 0L,
    val billDiscountSharePaise: Long = 0L,
    val gstRateBp: Int = 0,
    val hsnCode: String? = null,
    val taxableValuePaise: Long = 0L,
    val taxPaise: Long = 0L,
    val unitCostPaise: Long = 0L,
    val isCustomItem: Boolean = false,
    val isCostUnknown: Boolean = false
) {
    @get:Ignore
    val quantity: Int get() = (quantityMilli / 1000).toInt()

    @Ignore
    var invoiceNumber: String = ""

    @Ignore
    constructor(
        id: Long = 0L,
        invoiceNumber: String = "",
        invoiceId: Long = 0L,
        productId: String? = null,
        productName: String,
        barcode: String = "",
        unitPricePaise: Long,
        quantity: Int,
        lineTotalPaise: Long
    ) : this(
        id = id,
        invoiceId = invoiceId,
        productId = productId,
        productName = productName,
        barcode = barcode,
        unitPricePaise = unitPricePaise,
        quantityMilli = quantity.toLong() * 1000L,
        lineTotalPaise = lineTotalPaise
    ) {
        this.invoiceNumber = invoiceNumber
    }
}
