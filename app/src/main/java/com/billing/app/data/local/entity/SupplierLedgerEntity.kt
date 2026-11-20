package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "supplier_ledger",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("supplierId"),
        Index("createdAt")
    ]
)
data class SupplierLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: String,
    val transactionType: String,
    val purchaseId: Long? = null,
    val debitPaise: Long = 0L,
    val creditPaise: Long = 0L,
    val balanceAfterPaise: Long,
    val paymentMode: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
