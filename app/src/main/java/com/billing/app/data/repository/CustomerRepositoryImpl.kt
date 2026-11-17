package com.billing.app.data.repository

import com.billing.app.data.local.dao.CustomerDao
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.domain.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CustomerRepositoryImpl(
    private val customerDao: CustomerDao
) : CustomerRepository {

    override fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    override fun searchCustomers(query: String): Flow<List<CustomerEntity>> = customerDao.searchCustomers(query)

    override fun getCustomerCount(): Flow<Int> = customerDao.getCustomerCount()

    override suspend fun getCustomerById(id: String): CustomerEntity? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(id)
    }

    override suspend fun getCustomerByPhone(phone: String): CustomerEntity? = withContext(Dispatchers.IO) {
        customerDao.getCustomerByPhone(phone)
    }

    override suspend fun addCustomer(customer: CustomerEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            customerDao.insertCustomer(customer)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCustomers(customers: List<CustomerEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            customerDao.insertAll(customers)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCustomer(customer: CustomerEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            customerDao.updateCustomer(customer)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCustomer(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            customerDao.deleteCustomer(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
