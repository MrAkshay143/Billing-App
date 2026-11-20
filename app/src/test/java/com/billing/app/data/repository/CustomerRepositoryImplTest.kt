package com.billing.app.data.repository

import com.billing.app.data.local.dao.CustomerDao
import com.billing.app.data.local.dao.CustomerLedgerDao
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomerRepositoryImplTest {

    private lateinit var fakeCustomerDao: FakeCustomerDao
    private lateinit var fakeCustomerLedgerDao: FakeCustomerLedgerDao
    private lateinit var customerRepository: CustomerRepositoryImpl

    @Before
    fun setup() {
        fakeCustomerDao = FakeCustomerDao()
        fakeCustomerLedgerDao = FakeCustomerLedgerDao()
        customerRepository = CustomerRepositoryImpl(
            customerDao = fakeCustomerDao,
            customerLedgerDao = fakeCustomerLedgerDao
        )
    }

    @Test
    fun addCustomer_withOpeningBalance_insertsOpeningBalanceLedgerEntry() = runBlocking {
        val customer = CustomerEntity(
            id = "cust_101",
            name = "Ramesh Kumar",
            phone = "9876543210",
            openingBalancePaise = 25000L // Rs 250.00
        )

        val result = customerRepository.addCustomer(customer)
        assertTrue(result.isSuccess)

        // Customer stored in DAO
        assertEquals(1, fakeCustomerDao.customers.size)
        assertEquals(customer, fakeCustomerDao.customers.first())

        // Ledger entry verified
        val ledgerEntries = fakeCustomerLedgerDao.entries
        assertEquals(1, ledgerEntries.size)
        val entry = ledgerEntries.first()
        assertEquals("cust_101", entry.customerId)
        assertEquals("OPENING_BALANCE", entry.transactionType)
        assertEquals(25000L, entry.debitPaise)
        assertEquals(0L, entry.creditPaise)
        assertEquals(25000L, entry.balanceAfterPaise)
        assertEquals("Opening Balance", entry.note)
    }

    @Test
    fun addCustomer_withZeroOpeningBalance_doesNotInsertLedgerEntry() = runBlocking {
        val customer = CustomerEntity(
            id = "cust_102",
            name = "Suresh Patel",
            phone = "9876543211",
            openingBalancePaise = 0L
        )

        val result = customerRepository.addCustomer(customer)
        assertTrue(result.isSuccess)

        assertEquals(1, fakeCustomerDao.customers.size)
        assertTrue(fakeCustomerLedgerDao.entries.isEmpty())
    }

    // --- Fake DAOs ---

    class FakeCustomerDao : CustomerDao {
        val customers = mutableListOf<CustomerEntity>()

        override fun getAllCustomers(): Flow<List<CustomerEntity>> =
            MutableStateFlow(customers.toList()).asStateFlow()

        override fun searchCustomers(query: String): Flow<List<CustomerEntity>> =
            MutableStateFlow(customers.filter { it.name.contains(query, true) || it.phone.contains(query) }).asStateFlow()

        override suspend fun getCustomerById(id: String): CustomerEntity? =
            customers.find { it.id == id }

        override suspend fun getCustomerByPhone(phone: String): CustomerEntity? =
            customers.find { it.phone == phone }

        override suspend fun insertCustomer(customer: CustomerEntity) {
            val idx = customers.indexOfFirst { it.id == customer.id }
            if (idx >= 0) customers[idx] = customer else customers.add(customer)
        }

        override suspend fun insertAll(customers: List<CustomerEntity>) {
            customers.forEach { insertCustomer(it) }
        }

        override suspend fun updateCustomer(customer: CustomerEntity) {
            insertCustomer(customer)
        }

        override suspend fun deleteCustomer(id: String) {
            customers.removeAll { it.id == id }
        }

        override fun getCustomerCount(): Flow<Int> =
            MutableStateFlow(customers.size).asStateFlow()
    }

    class FakeCustomerLedgerDao : CustomerLedgerDao {
        val entries = mutableListOf<CustomerLedgerEntity>()
        private var nextId = 1L

        override suspend fun insertEntry(entry: CustomerLedgerEntity): Long {
            val id = nextId++
            val saved = entry.copy(id = id)
            entries.add(saved)
            return id
        }

        override suspend fun upsertEntry(entry: CustomerLedgerEntity) {
            val idx = entries.indexOfFirst { it.id == entry.id }
            if (idx >= 0) entries[idx] = entry else entries.add(entry)
        }

        override suspend fun upsertAll(entries: List<CustomerLedgerEntity>) {
            entries.forEach { upsertEntry(it) }
        }

        override fun getEntriesForCustomer(customerId: String): Flow<List<CustomerLedgerEntity>> =
            MutableStateFlow(entries.filter { it.customerId == customerId }).asStateFlow()

        override suspend fun getEntryListForCustomer(customerId: String): List<CustomerLedgerEntity> =
            entries.filter { it.customerId == customerId }

        override fun getAllEntries(): Flow<List<CustomerLedgerEntity>> =
            MutableStateFlow(entries.toList()).asStateFlow()

        override suspend fun getEntryById(id: Long): CustomerLedgerEntity? =
            entries.find { it.id == id }

        override suspend fun getLatestEntryForCustomer(customerId: String): CustomerLedgerEntity? =
            entries.filter { it.customerId == customerId }.maxByOrNull { it.id }

        override fun getEntriesForInvoice(invoiceId: Long): Flow<List<CustomerLedgerEntity>> =
            MutableStateFlow(entries.filter { it.invoiceId == invoiceId }).asStateFlow()

        override fun getEntriesBetween(customerId: String, startTime: Long, endTime: Long): Flow<List<CustomerLedgerEntity>> =
            MutableStateFlow(entries.filter { it.customerId == customerId && it.createdAt in startTime..endTime }).asStateFlow()

        override fun getTotalDebitForCustomer(customerId: String): Flow<Long> =
            MutableStateFlow(entries.filter { it.customerId == customerId }.sumOf { it.debitPaise }).asStateFlow()

        override fun getTotalCreditForCustomer(customerId: String): Flow<Long> =
            MutableStateFlow(entries.filter { it.customerId == customerId }.sumOf { it.creditPaise }).asStateFlow()

        override suspend fun deleteEntry(id: Long) {
            entries.removeAll { it.id == id }
        }

        override suspend fun deleteEntriesForCustomer(customerId: String) {
            entries.removeAll { it.customerId == customerId }
        }
    }
}
