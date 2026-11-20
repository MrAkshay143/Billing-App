package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cash_movements",
    foreignKeys = [
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("shiftId"),
        Index("createdAt")
    ]
)
data class CashMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val movementType: String,
    val amountPaise: Long,
    val referenceId: String? = null,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    @androidx.room.Ignore
    var source: String? = null

    @androidx.room.Ignore
    var sourceId: String? = null

    @androidx.room.Ignore
    var notes: String? = null

    @androidx.room.Ignore
    constructor(
        shiftId: Long,
        movementType: String,
        amountPaise: Long,
        source: String? = null,
        sourceId: String? = null,
        notes: String? = null
    ) : this(
        id = 0,
        shiftId = shiftId,
        movementType = movementType,
        amountPaise = amountPaise,
        referenceId = sourceId,
        reason = notes ?: source,
        createdAt = System.currentTimeMillis()
    ) {
        this.source = source
        this.sourceId = sourceId
        this.notes = notes
    }
}
