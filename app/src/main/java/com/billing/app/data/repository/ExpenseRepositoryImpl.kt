package com.billing.app.data.repository

import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.ExpenseDao
import com.billing.app.data.local.dao.ShiftDao
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.ExpenseEntity
import com.billing.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val shiftDao: ShiftDao? = null,
    private val cashMovementDao: CashMovementDao? = null,
    private val database: AppDatabase? = null
) : ExpenseRepository {

    constructor(database: AppDatabase) : this(
        expenseDao = database.expenseDao(),
        shiftDao = database.shiftDao(),
        cashMovementDao = database.cashMovementDao(),
        database = database
    )

    private suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
    }

    override fun getAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    override fun getExpensesBetween(start: Long, end: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesBetween(start, end)

    override fun getTotalExpenseBetween(start: Long, end: Long): Flow<Long> =
        expenseDao.getTotalExpenseBetween(start, end)

    override suspend fun addExpense(expense: ExpenseEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            runInTransaction {
                val activeShift = shiftDao?.getCurrentOpenShift()
                val expenseToInsert = if (expense.shiftId == null && activeShift != null) {
                    expense.copy(shiftId = activeShift.id)
                } else {
                    expense
                }
                expenseDao.insertExpense(expenseToInsert)

                if (expenseToInsert.paymentMethod.equals("CASH", ignoreCase = true) && activeShift != null && cashMovementDao != null) {
                    val movement = CashMovementEntity(
                        shiftId = activeShift.id,
                        movementType = "EXPENSE_OUT",
                        amountPaise = -expenseToInsert.amountPaise,
                        referenceId = expenseToInsert.id,
                        reason = expenseToInsert.title
                    )
                    cashMovementDao.insertCashMovement(movement)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExpense(expense: ExpenseEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            expenseDao.updateExpense(expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            expenseDao.deleteExpense(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
