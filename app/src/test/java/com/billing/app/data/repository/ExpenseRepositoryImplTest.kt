package com.billing.app.data.repository

import com.billing.app.data.local.dao.ExpenseDao
import com.billing.app.data.local.entity.ExpenseEntity
import com.billing.app.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseRepositoryImplTest {

    private lateinit var fakeExpenseDao: FakeExpenseDao
    private lateinit var fakeShiftDao: ShiftRepositoryImplTest.FakeShiftDao
    private lateinit var fakeCashMovementDao: ShiftRepositoryImplTest.FakeCashMovementDao
    private lateinit var expenseRepository: ExpenseRepositoryImpl

    @Before
    fun setup() {
        fakeExpenseDao = FakeExpenseDao()
        fakeShiftDao = ShiftRepositoryImplTest.FakeShiftDao()
        fakeCashMovementDao = ShiftRepositoryImplTest.FakeCashMovementDao()
        expenseRepository = ExpenseRepositoryImpl(
            expenseDao = fakeExpenseDao,
            shiftDao = fakeShiftDao,
            cashMovementDao = fakeCashMovementDao,
            database = null
        )
    }

    @Test
    fun addExpense_cashWithActiveShift_recordsExpenseOutMovement() = runBlocking {
        // Open a shift first
        val shiftId = fakeShiftDao.insertShift(
            ShiftEntity(userId = "cashier_1", openingFloatPaise = 50000L, status = "OPEN")
        )

        val expense = ExpenseEntity(
            id = "exp_tea_1",
            title = "Office Tea",
            category = "Refreshment",
            amountPaise = 15000L, // Rs 150.00
            paymentMethod = "CASH"
        )

        val result = expenseRepository.addExpense(expense)
        assertTrue(result.isSuccess)

        // Verify expense saved with shiftId
        val savedExpense = fakeExpenseDao.expenses.first()
        assertEquals(shiftId, savedExpense.shiftId)

        // Verify cash movement recorded
        val movements = fakeCashMovementDao.movements
        assertEquals(1, movements.size)
        val movement = movements.first()
        assertEquals(shiftId, movement.shiftId)
        assertEquals("EXPENSE_OUT", movement.movementType)
        assertEquals(-15000L, movement.amountPaise)
        assertEquals("exp_tea_1", movement.referenceId)
        assertEquals("Office Tea", movement.reason)
    }

    @Test
    fun addExpense_upiWithActiveShift_doesNotRecordCashMovement() = runBlocking {
        fakeShiftDao.insertShift(
            ShiftEntity(userId = "cashier_1", openingFloatPaise = 50000L, status = "OPEN")
        )

        val expense = ExpenseEntity(
            id = "exp_upi_1",
            title = "Internet Bill",
            category = "Utilities",
            amountPaise = 80000L,
            paymentMethod = "UPI"
        )

        val result = expenseRepository.addExpense(expense)
        assertTrue(result.isSuccess)

        // No cash movement should be recorded for non-cash expense
        assertEquals(0, fakeCashMovementDao.movements.size)
    }

    @Test
    fun addExpense_cashWithoutActiveShift_savesExpenseWithoutMovement() = runBlocking {
        val expense = ExpenseEntity(
            id = "exp_no_shift",
            title = "Stationery",
            category = "Supplies",
            amountPaise = 20000L,
            paymentMethod = "CASH"
        )

        val result = expenseRepository.addExpense(expense)
        assertTrue(result.isSuccess)

        val saved = fakeExpenseDao.expenses.first()
        assertNull(saved.shiftId)
        assertEquals(0, fakeCashMovementDao.movements.size)
    }

    class FakeExpenseDao : ExpenseDao {
        val expenses = mutableListOf<ExpenseEntity>()
        private val _flow = MutableStateFlow<List<ExpenseEntity>>(emptyList())

        override fun getAllExpenses(): Flow<List<ExpenseEntity>> = _flow.asStateFlow()

        override fun getExpensesBetween(start: Long, end: Long): Flow<List<ExpenseEntity>> =
            MutableStateFlow(expenses.filter { it.date in start..end }).asStateFlow()

        override suspend fun insertExpense(expense: ExpenseEntity) {
            val idx = expenses.indexOfFirst { it.id == expense.id }
            if (idx >= 0) expenses[idx] = expense else expenses.add(expense)
            _flow.value = expenses.toList()
        }

        override suspend fun insertAll(expenses: List<ExpenseEntity>) {
            expenses.forEach { insertExpense(it) }
        }

        override suspend fun updateExpense(expense: ExpenseEntity) {
            val idx = expenses.indexOfFirst { it.id == expense.id }
            if (idx >= 0) {
                expenses[idx] = expense
                _flow.value = expenses.toList()
            }
        }

        override suspend fun deleteExpense(id: String) {
            expenses.removeAll { it.id == id }
            _flow.value = expenses.toList()
        }

        override fun getTotalExpenseBetween(start: Long, end: Long): Flow<Long> =
            MutableStateFlow(expenses.filter { it.date in start..end }.sumOf { it.amountPaise }).asStateFlow()
    }
}
