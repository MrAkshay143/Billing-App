package com.billing.app.data.repository

import com.billing.app.data.local.dao.CustomUnitDao
import com.billing.app.data.local.entity.CustomUnitEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomUnitRepositoryImplTest {

    private lateinit var fakeDao: FakeCustomUnitDao
    private lateinit var repository: CustomUnitRepositoryImpl

    private class FakeCustomUnitDao : CustomUnitDao {
        val units = mutableListOf<CustomUnitEntity>()
        private var autoId = 1L

        override fun getAllCustomUnits(): Flow<List<CustomUnitEntity>> = flowOf(units.toList())

        override suspend fun getAllCustomUnitsSync(): List<CustomUnitEntity> = units.toList()

        override suspend fun getByCode(code: String): CustomUnitEntity? = units.find { it.code == code }

        override suspend fun insert(unit: CustomUnitEntity): Long {
            val assigned = unit.copy(id = autoId++)
            units.removeIf { it.code == unit.code }
            units.add(assigned)
            return assigned.id
        }

        override suspend fun delete(unit: CustomUnitEntity) {
            units.removeIf { it.id == unit.id }
        }

        override suspend fun deleteById(id: Long) {
            units.removeIf { it.id == id }
        }
    }

    @Before
    fun setUp() {
        fakeDao = FakeCustomUnitDao()
        repository = CustomUnitRepositoryImpl(fakeDao)
    }

    @Test
    fun testAddValidCustomUnit() = runBlocking {
        val result = repository.addCustomUnit(
            code = "BOSTA",
            name = "Bosta (50kg)",
            baseUnitCode = "KG",
            conversionFactor = 50.0,
            allowsFractional = true
        )
        assertTrue(result.isSuccess)
        val list = repository.getAllCustomUnitsSync()
        assertEquals(1, list.size)
        assertEquals("BOSTA", list[0].code)
        assertEquals("KG", list[0].baseUnitCode)
        assertEquals(50.0, list[0].conversionFactor, 0.001)
    }

    @Test
    fun testRejectDuplicateCustomUnit() = runBlocking {
        repository.addCustomUnit("PLATE", "Plate", "PCS", 1.0)
        val duplicate = repository.addCustomUnit("PLATE", "Plate 2", "PCS", 1.0)
        assertTrue(duplicate.isFailure)
        assertTrue(duplicate.exceptionOrNull()?.message?.contains("already exists") == true)
    }

    @Test
    fun testRejectStandardUnitCodeClash() = runBlocking {
        val clash = repository.addCustomUnit("PCS", "Pieces", "PCS", 1.0)
        assertTrue(clash.isFailure)
        assertTrue(clash.exceptionOrNull()?.message?.contains("standard unit") == true)
    }

    @Test
    fun testDeleteCustomUnit() = runBlocking {
        val id = repository.addCustomUnit("TIN", "Tin Can", "L", 5.0).getOrThrow()
        assertEquals(1, repository.getAllCustomUnitsSync().size)

        repository.deleteCustomUnit(id)
        assertEquals(0, repository.getAllCustomUnitsSync().size)
    }
}
