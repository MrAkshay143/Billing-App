package com.billing.app.data.repository

import com.billing.app.data.local.dao.ExpenseDao
import com.billing.app.data.local.entity.ExpenseEntity
import com.billing.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    override fun getExpensesBetween(start: Long, end: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesBetween(start, end)

    override fun getTotalExpenseBetween(start: Long, end: Long): Flow<Long> =
        expenseDao.getTotalExpenseBetween(start, end)

    override suspend fun addExpense(expense: ExpenseEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            expenseDao.insertExpense(expense)
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
