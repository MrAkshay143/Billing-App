package com.billing.app.data.repository

import com.billing.app.data.local.dao.SupplierDao
import com.billing.app.data.local.entity.SupplierEntity
import com.billing.app.domain.repository.SupplierRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SupplierRepositoryImpl(
    private val supplierDao: SupplierDao
) : SupplierRepository {

    override fun getAllSuppliers(): Flow<List<SupplierEntity>> = supplierDao.getAllSuppliers()

    override fun searchSuppliers(query: String): Flow<List<SupplierEntity>> = supplierDao.searchSuppliers(query)

    override fun getSupplierCount(): Flow<Int> = supplierDao.getSupplierCount()

    override suspend fun getSupplierById(id: String): SupplierEntity? = withContext(Dispatchers.IO) {
        supplierDao.getSupplierById(id)
    }

    override suspend fun addSupplier(supplier: SupplierEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supplierDao.insertSupplier(supplier)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSupplier(supplier: SupplierEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supplierDao.updateSupplier(supplier)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSupplier(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supplierDao.deleteSupplier(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
