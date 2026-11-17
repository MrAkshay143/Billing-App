package com.billing.app.domain.repository

import com.billing.app.data.local.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

interface SupplierRepository {
    fun getAllSuppliers(): Flow<List<SupplierEntity>>
    fun searchSuppliers(query: String): Flow<List<SupplierEntity>>
    fun getSupplierCount(): Flow<Int>
    suspend fun getSupplierById(id: String): SupplierEntity?
    suspend fun addSupplier(supplier: SupplierEntity): Result<Unit>
    suspend fun updateSupplier(supplier: SupplierEntity): Result<Unit>
    suspend fun deleteSupplier(id: String): Result<Unit>
}
