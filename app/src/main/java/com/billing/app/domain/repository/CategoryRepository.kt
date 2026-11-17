package com.billing.app.domain.repository

import com.billing.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun seedDefaultCategoriesIfEmpty()
    suspend fun addCategory(name: String, iconName: String? = null, imageUri: String? = null): Result<Unit>
    suspend fun updateCategory(category: CategoryEntity): Result<Unit>
    suspend fun deleteCategory(id: String): Result<Unit>
}
