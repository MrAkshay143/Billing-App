package com.billing.app.domain.repository

import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

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

    fun getCustomerLedger(customerId: String): Flow<List<CustomerLedgerEntity>> = emptyFlow()
    fun getCustomerBalance(customerId: String): Flow<Long> = flowOf(0L)
    suspend fun getCustomerBalanceSnapshot(customerId: String): Long = 0L
    suspend fun recordKhataPayment(
        customerId: String,
        amountPaise: Long,
        paymentMode: String,
        shiftId: Long?,
        note: String? = null
    ): Result<Unit> = Result.success(Unit)
    fun generatePaymentReminderWhatsAppUrl(
        customer: CustomerEntity,
        duePaise: Long,
        upiId: String,
        shopName: String
    ): String = ""
}
