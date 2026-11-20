package com.billing.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_units",
    indices = [Index(value = ["code"], unique = true)]
)
data class CustomUnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val code: String,
    val name: String,
    val baseUnitCode: String,
    val conversionFactor: Double,
    val allowsFractional: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
