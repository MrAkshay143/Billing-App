package com.billing.app.domain.repository

import com.billing.app.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun getAllCustomers(): Flow<List<CustomerEntity>>
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>
    fun getCustomerCount(): Flow<Int>
    suspend fun getCustomerById(id: String): CustomerEntity?
    suspend fun getCustomerByPhone(phone: String): CustomerEntity?
    suspend fun addCustomer(customer: CustomerEntity): Result<Unit>
    suspend fun addCustomers(customers: List<CustomerEntity>): Result<Unit>
    suspend fun updateCustomer(customer: CustomerEntity): Result<Unit>
    suspend fun deleteCustomer(id: String): Result<Unit>
}
