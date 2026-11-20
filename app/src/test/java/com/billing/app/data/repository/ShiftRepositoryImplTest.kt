package com.billing.app.data.repository

import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.ShiftDao
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShiftRepositoryImplTest {

    private lateinit var fakeShiftDao: FakeShiftDao
    private lateinit var fakeCashMovementDao: FakeCashMovementDao
    private lateinit var shiftRepository: ShiftRepositoryImpl

    @Before
    fun setup() {
        fakeShiftDao = FakeShiftDao()
        fakeCashMovementDao = FakeCashMovementDao()
        shiftRepository = ShiftRepositoryImpl(
            shiftDao = fakeShiftDao,
            cashMovementDao = fakeCashMovementDao,
            database = null
        )
    }

    @Test
    fun openShift_succeeds_whenNoShiftIsOpen() = runBlocking {
        val result = shiftRepository.openShift(
            userId = "cashier_1",
            openingFloatPaise = 50000L,
            note = "Morning shift"
        )

        assertTrue(result.isSuccess)
        val shift = result.getOrThrow()
        assertEquals("cashier_1", shift.userId)
        assertEquals(50000L, shift.openingFloatPaise)
        assertEquals("OPEN", shift.status)
        assertEquals("Morning shift", shift.note)

        // Verify initial cash movement was created
        val movements = fakeCashMovementDao.movements
        assertEquals(1, movements.size)
        val initialMovement = movements.first()
        assertEquals(shift.id, initialMovement.shiftId)
        assertEquals("CASH_IN", initialMovement.movementType)
        assertEquals(50000L, initialMovement.amountPaise)
        assertEquals("Opening Float", initialMovement.reason)
    }

    @Test
    fun openShift_fails_whenShiftAlreadyOpen() = runBlocking {
        shiftRepository.openShift("cashier_1", 10000L)

        val secondResult = shiftRepository.openShift("cashier_2", 20000L)
        assertTrue(secondResult.isFailure)
        assertTrue(secondResult.exceptionOrNull() is IllegalStateException)
        assertEquals("A shift is already open", secondResult.exceptionOrNull()?.message)
    }

    @Test
    fun recordCashMovement_succeeds_whenShiftIsOpen() = runBlocking {
        val openResult = shiftRepository.openShift("cashier_1", 10000L)
        val shift = openResult.getOrThrow()

        val moveResult = shiftRepository.recordCashMovement(
            movementType = "EXPENSE_OUT",
            amountPaise = -2000L,
            referenceId = "exp_1",
            reason = "Tea and snacks"
        )

        assertTrue(moveResult.isSuccess)
        val moveId = moveResult.getOrThrow()
        assertEquals(2L, moveId) // 1 was opening float, 2 is expense

        val movements = fakeCashMovementDao.movements
        assertEquals(2, movements.size)
        val expenseMove = movements[1]
        assertEquals(shift.id, expenseMove.shiftId)
        assertEquals("EXPENSE_OUT", expenseMove.movementType)
        assertEquals(-2000L, expenseMove.amountPaise)
        assertEquals("exp_1", expenseMove.referenceId)
        assertEquals("Tea and snacks", expenseMove.reason)
    }

    @Test
    fun recordCashMovement_fails_whenNoShiftOpen() = runBlocking {
        val moveResult = shiftRepository.recordCashMovement(
            movementType = "CASH_IN",
            amountPaise = 5000L
        )

        assertTrue(moveResult.isFailure)
        assertEquals(
            "No active shift open to record cash movement",
            moveResult.exceptionOrNull()?.message
        )
    }

    @Test
    fun getExpectedCashForActiveShift_calculatesCorrectly() = runBlocking {
        // Open shift with 500.00 float (50000 paise)
        shiftRepository.openShift("cashier_1", 50000L)

        // Record a cash sale of 150.00 (+15000 paise)
        shiftRepository.recordCashMovement("SALE_NET_CASH", 15000L)

        // Record cash expense of 50.00 (-5000 paise)
        shiftRepository.recordCashMovement("EXPENSE_OUT", -5000L)

        val expectedCash = shiftRepository.getExpectedCashForActiveShift()
        // 50000 + (15000 - 5000) = 60000 paise (600.00)
        assertEquals(60000L, expectedCash)
    }

    @Test
    fun closeShift_computesVarianceAndClosesShift() = runBlocking {
        shiftRepository.openShift("cashier_1", 50000L)
        shiftRepository.recordCashMovement("SALE_NET_CASH", 10000L)

        // Expected cash: 50000 + 10000 = 60000 paise
        // Actual counted cash: 58000 paise (Short by 2000 paise)
        val closeResult = shiftRepository.closeShift(
            actualCashPaise = 58000L,
            note = "Short by 20 rupees"
        )

        assertTrue(closeResult.isSuccess)
        val closedShift = closeResult.getOrThrow()
        assertEquals("CLOSED", closedShift.status)
        assertNotNull(closedShift.closedAt)
        assertEquals(58000L, closedShift.actualCashPaise)
        assertEquals(60000L, closedShift.expectedCashPaise)
        assertEquals(-2000L, closedShift.variancePaise)
        assertEquals("Short by 20 rupees", closedShift.note)

        // Verify active shift is now null
        assertNull(shiftRepository.getActiveShift())
    }

    @Test
    fun closeShift_fails_whenNoShiftOpen() = runBlocking {
        val closeResult = shiftRepository.closeShift(50000L)
        assertTrue(closeResult.isFailure)
        assertEquals("No active shift open", closeResult.exceptionOrNull()?.message)
    }

    @Test
    fun getMovementsForActiveShift_emitsMovements() = runBlocking {
        shiftRepository.openShift("cashier_1", 10000L)
        shiftRepository.recordCashMovement("SALE_NET_CASH", 5000L)

        val movements = shiftRepository.getMovementsForActiveShift().first()
        assertEquals(2, movements.size)
    }

    // --- Fake DAOs ---

    class FakeShiftDao : ShiftDao {
        val shifts = mutableListOf<ShiftEntity>()
        private val _openShiftFlow = MutableStateFlow<ShiftEntity?>(null)
        private var nextId = 1L

        override suspend fun insertShift(shift: ShiftEntity): Long {
            val id = nextId++
            val saved = shift.copy(id = id)
            shifts.add(saved)
            if (saved.status == "OPEN") {
                _openShiftFlow.value = saved
            }
            return id
        }

        override suspend fun upsertShift(shift: ShiftEntity) {
            val idx = shifts.indexOfFirst { it.id == shift.id }
            if (idx >= 0) shifts[idx] = shift else shifts.add(shift)
            _openShiftFlow.value = shifts.firstOrNull { it.status == "OPEN" }
        }

        override suspend fun upsertAll(shifts: List<ShiftEntity>) {
            shifts.forEach { upsertShift(it) }
        }

        override suspend fun updateShift(shift: ShiftEntity) {
            val idx = shifts.indexOfFirst { it.id == shift.id }
            if (idx >= 0) {
                shifts[idx] = shift
            }
            _openShiftFlow.value = shifts.firstOrNull { it.status == "OPEN" }
        }

        override suspend fun getShiftById(id: Long): ShiftEntity? =
            shifts.find { it.id == id }

        override suspend fun getOpenShiftForUser(userId: String): ShiftEntity? =
            shifts.find { it.userId == userId && it.status == "OPEN" }

        override suspend fun getCurrentOpenShift(): ShiftEntity? =
            shifts.find { it.status == "OPEN" }

        override fun getCurrentOpenShiftFlow(): Flow<ShiftEntity?> =
            _openShiftFlow.asStateFlow()

        override fun getAllShifts(): Flow<List<ShiftEntity>> =
            MutableStateFlow(shifts.toList()).asStateFlow()

        override fun getShiftsForUser(userId: String): Flow<List<ShiftEntity>> =
            MutableStateFlow(shifts.filter { it.userId == userId }).asStateFlow()

        override suspend fun closeShift(
            id: Long,
            closedAt: Long,
            actualCashPaise: Long,
            expectedCashPaise: Long,
            variancePaise: Long,
            note: String?,
            status: String
        ) {
            val idx = shifts.indexOfFirst { it.id == id }
            if (idx >= 0) {
                shifts[idx] = shifts[idx].copy(
                    closedAt = closedAt,
                    actualCashPaise = actualCashPaise,
                    expectedCashPaise = expectedCashPaise,
                    variancePaise = variancePaise,
                    note = note,
                    status = status
                )
                _openShiftFlow.value = shifts.firstOrNull { it.status == "OPEN" }
            }
        }

        override suspend fun deleteShift(id: Long) {
            shifts.removeAll { it.id == id }
            _openShiftFlow.value = shifts.firstOrNull { it.status == "OPEN" }
        }

        override fun getShiftCount(): Flow<Int> =
            MutableStateFlow(shifts.size).asStateFlow()
    }

    class FakeCashMovementDao : CashMovementDao {
        val movements = mutableListOf<CashMovementEntity>()
        private val _movementsFlow = MutableStateFlow<List<CashMovementEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insertCashMovement(movement: CashMovementEntity): Long {
            val id = nextId++
            val saved = movement.copy(id = id)
            movements.add(saved)
            _movementsFlow.value = movements.toList()
            return id
        }

        override suspend fun upsertCashMovement(movement: CashMovementEntity) {
            val idx = movements.indexOfFirst { it.id == movement.id }
            if (idx >= 0) movements[idx] = movement else movements.add(movement)
            _movementsFlow.value = movements.toList()
        }

        override suspend fun upsertAll(movements: List<CashMovementEntity>) {
            movements.forEach { upsertCashMovement(it) }
        }

        override fun getCashMovementsForShift(shiftId: Long): Flow<List<CashMovementEntity>> =
            MutableStateFlow(movements.filter { it.shiftId == shiftId }).asStateFlow()

        override suspend fun getCashMovementListForShift(shiftId: Long): List<CashMovementEntity> =
            movements.filter { it.shiftId == shiftId }

        override fun getAllCashMovements(): Flow<List<CashMovementEntity>> =
            _movementsFlow.asStateFlow()

        override suspend fun getCashMovementById(id: Long): CashMovementEntity? =
            movements.find { it.id == id }

        override fun getTotalByMovementType(shiftId: Long, movementType: String): Flow<Long> =
            MutableStateFlow(
                movements.filter { it.shiftId == shiftId && it.movementType == movementType }
                    .sumOf { it.amountPaise }
            ).asStateFlow()

        private fun calculateNetCash(shiftId: Long): Long =
            movements.filter { it.shiftId == shiftId && it.reason != "Opening Float" }
                .sumOf { it.amountPaise }

        override suspend fun getNetCashForShift(shiftId: Long): Long =
            calculateNetCash(shiftId)

        override fun getNetCashForShiftFlow(shiftId: Long): Flow<Long> =
            MutableStateFlow(calculateNetCash(shiftId)).asStateFlow()

        override suspend fun deleteCashMovement(id: Long) {
            movements.removeAll { it.id == id }
            _movementsFlow.value = movements.toList()
        }

        override suspend fun deleteCashMovementsForShift(shiftId: Long) {
            movements.removeAll { it.shiftId == shiftId }
            _movementsFlow.value = movements.toList()
        }
    }
}
