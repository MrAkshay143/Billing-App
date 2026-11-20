package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.billing.app.data.local.entity.CustomUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomUnitDao {

    @Query("SELECT * FROM custom_units ORDER BY code ASC")
    fun getAllCustomUnits(): Flow<List<CustomUnitEntity>>

    @Query("SELECT * FROM custom_units ORDER BY code ASC")
    suspend fun getAllCustomUnitsSync(): List<CustomUnitEntity>

    @Query("SELECT * FROM custom_units WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): CustomUnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: CustomUnitEntity): Long

    @Delete
    suspend fun delete(unit: CustomUnitEntity)

    @Query("DELETE FROM custom_units WHERE id = :id")
    suspend fun deleteById(id: Long)
}
