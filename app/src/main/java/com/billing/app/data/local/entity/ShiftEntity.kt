package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shifts",
    indices = [
        Index("userId"),
        Index("openedAt")
    ]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val openingFloatPaise: Long,
    val actualCashPaise: Long? = null,
    val expectedCashPaise: Long? = null,
    val variancePaise: Long? = null,
    val note: String? = null,
    val status: String = "OPEN"
)
