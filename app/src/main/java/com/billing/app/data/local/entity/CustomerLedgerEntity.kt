package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_ledger",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("customerId"),
        Index("createdAt")
    ]
)
data class CustomerLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: String,
    val transactionType: String,
    val invoiceId: Long? = null,
    val debitPaise: Long = 0L,
    val creditPaise: Long = 0L,
    val balanceAfterPaise: Long,
    val paymentMode: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
