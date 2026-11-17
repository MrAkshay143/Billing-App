package com.billing.app.data.repository

import com.billing.app.data.local.dao.CategoryDao
import com.billing.app.data.local.entity.CategoryEntity
import com.billing.app.domain.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override suspend fun seedDefaultCategoriesIfEmpty() = withContext(Dispatchers.IO) {
        if (categoryDao.getCategoryCount() == 0) {
            val defaults = listOf(
                CategoryEntity(name = "Grocery", displayOrder = 1),
                CategoryEntity(name = "Oil & Ghee", displayOrder = 2),
                CategoryEntity(name = "Pulses & Dal", displayOrder = 3),
                CategoryEntity(name = "Spices & Masala", displayOrder = 4),
                CategoryEntity(name = "Snacks & Biscuits", displayOrder = 5),
                CategoryEntity(name = "Beverages", displayOrder = 6),
                CategoryEntity(name = "Household Items", displayOrder = 7),
                CategoryEntity(name = "Dairy Products", displayOrder = 8),
                CategoryEntity(name = "Personal Care", displayOrder = 9),
                CategoryEntity(name = "Fruits & Vegetables", displayOrder = 10),
                CategoryEntity(name = "Others", displayOrder = 11)
            )
            categoryDao.insertAll(defaults)
        }
    }

    override suspend fun addCategory(name: String, iconName: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val count = categoryDao.getCategoryCount()
            categoryDao.insertCategory(CategoryEntity(name = name.trim(), iconName = iconName, displayOrder = count + 1))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(category: CategoryEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            categoryDao.updateCategory(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            categoryDao.deleteCategory(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
