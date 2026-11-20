package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_allocations",
    foreignKeys = [
        ForeignKey(
            entity = CustomerLedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledgerEntryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PaymentAllocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["reversesAllocationId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("ledgerEntryId"),
        Index("invoiceId"),
        Index("reversesAllocationId")
    ]
)
data class PaymentAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ledgerEntryId: Long,
    val invoiceId: Long? = null,
    val allocatedAmountPaise: Long,
    val reversesAllocationId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    @Ignore
    var note: String? = null

    @Ignore
    constructor(
        ledgerEntryId: Long,
        invoiceId: Long?,
        allocatedAmountPaise: Long,
        note: String? = null
    ) : this(
        id = 0,
        ledgerEntryId = ledgerEntryId,
        invoiceId = invoiceId,
        allocatedAmountPaise = allocatedAmountPaise,
        reversesAllocationId = null,
        createdAt = System.currentTimeMillis()
    ) {
        this.note = note
    }
}
