package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_counter")
data class InvoiceCounterEntity(
    @PrimaryKey val id: Int = 1,
    val lastNumber: Long = 0L
)
