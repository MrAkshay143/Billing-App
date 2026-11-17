package com.billing.app.domain.repository

import com.billing.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    fun getExpensesBetween(start: Long, end: Long): Flow<List<ExpenseEntity>>
    fun getTotalExpenseBetween(start: Long, end: Long): Flow<Long>
    suspend fun addExpense(expense: ExpenseEntity): Result<Unit>
    suspend fun updateExpense(expense: ExpenseEntity): Result<Unit>
    suspend fun deleteExpense(id: String): Result<Unit>
}
