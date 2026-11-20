package com.billing.app.data.repository

import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.CategorySalesSummary
import com.billing.app.data.local.dao.CustomerDao
import com.billing.app.data.local.dao.CustomerLedgerDao
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.PaymentAllocationDao
import com.billing.app.data.local.dao.TopProductSummary
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import com.billing.app.data.local.entity.InvoiceCounterEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.data.local.entity.PaymentAllocationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URLDecoder

class CustomerKhataFifoTest {

    private lateinit var fakeCustomerDao: FakeCustomerDao
    private lateinit var fakeCustomerLedgerDao: FakeCustomerLedgerDao
    private lateinit var fakePaymentAllocationDao: FakePaymentAllocationDao
    private lateinit var fakeInvoiceDao: FakeInvoiceDao
    private lateinit var fakeCashMovementDao: FakeCashMovementDao
    private lateinit var customerRepository: CustomerRepositoryImpl

    @Before
    fun setup() {
        fakeCustomerDao = FakeCustomerDao()
        fakeCustomerLedgerDao = FakeCustomerLedgerDao()
        fakePaymentAllocationDao = FakePaymentAllocationDao()
        fakeInvoiceDao = FakeInvoiceDao()
        fakeCashMovementDao = FakeCashMovementDao()

        customerRepository = CustomerRepositoryImpl(
            customerDao = fakeCustomerDao,
            customerLedgerDao = fakeCustomerLedgerDao,
            paymentAllocationDao = fakePaymentAllocationDao,
            invoiceDao = fakeInvoiceDao,
            cashMovementDao = fakeCashMovementDao,
            database = null
        )
    }

    @Test
    fun testFifoPaymentAllocationAcrossMultipleInvoices() = runBlocking {
        // Customer with Rs. 800 total due (Inv 1: 500, Inv 2: 300)
        val customer = CustomerEntity(
            id = "cust_fifo_1",
            name = "Ramesh Kumar",
            phone = "9876543210",
            openingBalancePaise = 80000L
        )
        fakeCustomerDao.insertCustomer(customer)
        fakeCustomerLedgerDao.insertEntry(
            CustomerLedgerEntity(
                customerId = customer.id,
                transactionType = "OPENING_BALANCE",
                debitPaise = 80000L,
                creditPaise = 0L,
                balanceAfterPaise = 80000L
            )
        )

        // Invoice 1: 500 due (older timestamp)
        val inv1 = InvoiceEntity(
            id = 101L,
            invoiceNumber = "INV-001",
            customerId = customer.id,
            customerPhone = customer.phone,
            subtotalPaise = 50000L,
            grandTotalPaise = 50000L,
            duePaise = 50000L,
            status = InvoiceStatus.COMPLETED,
            timestamp = 1000L
        )
        // Invoice 2: 300 due (newer timestamp)
        val inv2 = InvoiceEntity(
            id = 102L,
            invoiceNumber = "INV-002",
            customerId = customer.id,
            customerPhone = customer.phone,
            subtotalPaise = 30000L,
            grandTotalPaise = 30000L,
            duePaise = 30000L,
            status = InvoiceStatus.COMPLETED,
            timestamp = 2000L
        )
        fakeInvoiceDao.insertInvoice(inv1)
        fakeInvoiceDao.insertInvoice(inv2)

        // Payment: Rs. 600 (60000 paise) via UPI
        val result = customerRepository.recordKhataPayment(
            customerId = customer.id,
            amountPaise = 60000L,
            paymentMode = "UPI",
            shiftId = null,
            note = "Partial Khata Settlement"
        )
        assertTrue(result.isSuccess)

        // 1. Inv 1 should be completely paid (due = 0)
        val updatedInv1 = fakeInvoiceDao.getInvoiceById(101L)
        assertNotNull(updatedInv1)
        assertEquals(0L, updatedInv1!!.duePaise)

        // 2. Inv 2 should have remaining 200 due (300 - 100 = 200)
        val updatedInv2 = fakeInvoiceDao.getInvoiceById(102L)
        assertNotNull(updatedInv2)
        assertEquals(20000L, updatedInv2!!.duePaise)

        // 3. Allocations check: 2 allocations created
        val allocations = fakePaymentAllocationDao.allocations
        assertEquals(2, allocations.size)
        assertEquals(101L, allocations[0].invoiceId)
        assertEquals(50000L, allocations[0].allocatedAmountPaise)
        assertEquals("FIFO payment allocation", allocations[0].note)

        assertEquals(102L, allocations[1].invoiceId)
        assertEquals(10000L, allocations[1].allocatedAmountPaise)
        assertEquals("FIFO payment allocation", allocations[1].note)

        // 4. Customer balance should be updated: 800 - 600 = 200 (20000 paise)
        val updatedCustomer = fakeCustomerDao.getCustomerById(customer.id)
        assertNotNull(updatedCustomer)
        assertEquals(20000L, updatedCustomer!!.openingBalancePaise)

        // 5. Customer ledger entry check
        val latestLedger = fakeCustomerLedgerDao.getLatestEntryForCustomer(customer.id)
        assertNotNull(latestLedger)
        assertEquals("PAYMENT", latestLedger!!.transactionType)
        assertEquals(60000L, latestLedger.creditPaise)
        assertEquals(0L, latestLedger.debitPaise)
        assertEquals(20000L, latestLedger.balanceAfterPaise)
        assertEquals("UPI", latestLedger.paymentMode)
        assertEquals("Partial Khata Settlement", latestLedger.note)
    }

    @Test
    fun testAdvancePaymentAllocationWhenPaymentExceedsDue() = runBlocking {
        // Customer with Rs. 400 due
        val customer = CustomerEntity(
            id = "cust_fifo_2",
            name = "Suresh Patel",
            phone = "9876543211",
            openingBalancePaise = 40000L
        )
        fakeCustomerDao.insertCustomer(customer)
        fakeCustomerLedgerDao.insertEntry(
            CustomerLedgerEntity(
                customerId = customer.id,
                transactionType = "OPENING_BALANCE",
                debitPaise = 40000L,
                creditPaise = 0L,
                balanceAfterPaise = 40000L
            )
        )

        val inv = InvoiceEntity(
            id = 201L,
            invoiceNumber = "INV-010",
            customerId = customer.id,
            customerPhone = customer.phone,
            subtotalPaise = 40000L,
            grandTotalPaise = 40000L,
            duePaise = 40000L,
            status = InvoiceStatus.COMPLETED,
            timestamp = 1000L
        )
        fakeInvoiceDao.insertInvoice(inv)

        // Customer pays Rs. 700 (70000 paise) -> 400 clears inv, 300 becomes advance credit
        val result = customerRepository.recordKhataPayment(
            customerId = customer.id,
            amountPaise = 70000L,
            paymentMode = "BANK_TRANSFER",
            shiftId = null,
            note = "Advance Payment"
        )
        assertTrue(result.isSuccess)

        // Invoice cleared
        val updatedInv = fakeInvoiceDao.getInvoiceById(201L)
        assertNotNull(updatedInv)
        assertEquals(0L, updatedInv!!.duePaise)

        // Allocations: 1 invoice allocation (400) + 1 advance allocation (300, invoiceId = null)
        val allocations = fakePaymentAllocationDao.allocations
        assertEquals(2, allocations.size)
        assertEquals(201L, allocations[0].invoiceId)
        assertEquals(40000L, allocations[0].allocatedAmountPaise)

        assertNull(allocations[1].invoiceId)
        assertEquals(30000L, allocations[1].allocatedAmountPaise)
        assertEquals("Advance credit", allocations[1].note)

        // Customer balance should be -30000 paise (negative means store owes customer / advance)
        val updatedCustomer = fakeCustomerDao.getCustomerById(customer.id)
        assertNotNull(updatedCustomer)
        assertEquals(-30000L, updatedCustomer!!.openingBalancePaise)

        val balanceSnapshot = customerRepository.getCustomerBalanceSnapshot(customer.id)
        assertEquals(-30000L, balanceSnapshot)
    }

    @Test
    fun testCashMovementRecordedOnCashPayment() = runBlocking {
        val customer = CustomerEntity(
            id = "cust_fifo_3",
            name = "Amit Shah",
            phone = "9876543212",
            openingBalancePaise = 15000L
        )
        fakeCustomerDao.insertCustomer(customer)
        fakeCustomerLedgerDao.insertEntry(
            CustomerLedgerEntity(
                customerId = customer.id,
                transactionType = "OPENING_BALANCE",
                debitPaise = 15000L,
                creditPaise = 0L,
                balanceAfterPaise = 15000L
            )
        )

        // Cash payment of Rs. 150 with active shiftId = 77L
        val result = customerRepository.recordKhataPayment(
            customerId = customer.id,
            amountPaise = 15000L,
            paymentMode = "CASH",
            shiftId = 77L,
            note = "Counter Cash Payment"
        )
        assertTrue(result.isSuccess)

        // Verify CashMovementEntity recorded
        val movements = fakeCashMovementDao.movements
        assertEquals(1, movements.size)
        val m = movements.first()
        assertEquals(77L, m.shiftId)
        assertEquals("KHATA_COLLECTION", m.movementType)
        assertEquals(15000L, m.amountPaise)
        assertEquals(customer.id, m.referenceId)
        assertEquals("Khata payment received from Amit Shah", m.reason)
    }

    @Test
    fun testCashMovementNotRecordedForNonCashPayment() = runBlocking {
        val customer = CustomerEntity(
            id = "cust_fifo_4",
            name = "Priya Sharma",
            phone = "9876543213",
            openingBalancePaise = 10000L
        )
        fakeCustomerDao.insertCustomer(customer)

        val result = customerRepository.recordKhataPayment(
            customerId = customer.id,
            amountPaise = 10000L,
            paymentMode = "UPI",
            shiftId = 88L
        )
        assertTrue(result.isSuccess)

        // No cash movement recorded for UPI
        assertTrue(fakeCashMovementDao.movements.isEmpty())
    }

    @Test
    fun testGeneratePaymentReminderWhatsAppUrl() {
        val customer = CustomerEntity(
            id = "cust_fifo_5",
            name = "Vikram Singh",
            phone = "+91 98765 43210"
        )
        val duePaise = 45000L // Rs. 450.00
        val upiId = "kirana@upi"
        val shopName = "Dokandaar Supermarket"

        val url = customerRepository.generatePaymentReminderWhatsAppUrl(
            customer = customer,
            duePaise = duePaise,
            upiId = upiId,
            shopName = shopName
        )

        // Clean phone verified in URL path: 91 + 10-digit phone
        assertTrue(url.startsWith("https://wa.me/919876543210?text="))

        val encodedText = url.substringAfter("text=")
        val decodedMessage = URLDecoder.decode(encodedText, "UTF-8")

        // Contains customer name, shop name, formatted rupee amount, and UPI link
        assertTrue(decodedMessage.contains("Vikram Singh"))
        assertTrue(decodedMessage.contains("Dokandaar Supermarket"))
        assertTrue(decodedMessage.contains("Rs. 450.00"))
        assertTrue(decodedMessage.contains("upi://pay?pa=kirana@upi&pn=Dokandaar Supermarket&am=450.00&tn=Khata%20Payment"))
    }

    @Test
    fun testGetCustomerBalanceFlow() = runBlocking {
        val customerId = "cust_flow_test"

        fakeCustomerLedgerDao.insertEntry(
            CustomerLedgerEntity(
                customerId = customerId,
                transactionType = "CREDIT_SALE",
                debitPaise = 100000L,
                creditPaise = 0L,
                balanceAfterPaise = 100000L
            )
        )
        fakeCustomerLedgerDao.insertEntry(
            CustomerLedgerEntity(
                customerId = customerId,
                transactionType = "PAYMENT",
                debitPaise = 0L,
                creditPaise = 30000L,
                balanceAfterPaise = 70000L
            )
        )

        val balance = customerRepository.getCustomerBalance(customerId).first()
        assertEquals(70000L, balance)
    }

    // --- Fake DAOs ---

    class FakeCustomerDao : CustomerDao {
        val customers = mutableListOf<CustomerEntity>()

        override fun getAllCustomers(): Flow<List<CustomerEntity>> =
            MutableStateFlow(customers.toList()).asStateFlow()

        override fun searchCustomers(query: String): Flow<List<CustomerEntity>> =
            MutableStateFlow(customers.filter { it.name.contains(query, true) || it.phone.contains(query) }).asStateFlow()

        override fun getCustomerCount(): Flow<Int> =
            MutableStateFlow(customers.size).asStateFlow()

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

    class FakePaymentAllocationDao : PaymentAllocationDao {
        val allocations = mutableListOf<PaymentAllocationEntity>()
        private var nextId = 1L

        override suspend fun insertAllocation(allocation: PaymentAllocationEntity): Long {
            val id = nextId++
            val saved = allocation.copy(id = id)
            saved.note = allocation.note
            allocations.add(saved)
            return id
        }

        override suspend fun upsertAllocation(allocation: PaymentAllocationEntity) {
            val idx = allocations.indexOfFirst { it.id == allocation.id }
            if (idx >= 0) allocations[idx] = allocation else allocations.add(allocation)
        }

        override suspend fun upsertAll(allocations: List<PaymentAllocationEntity>) {
            allocations.forEach { upsertAllocation(it) }
        }

        override fun getAllocationsForLedgerEntry(ledgerEntryId: Long): Flow<List<PaymentAllocationEntity>> =
            MutableStateFlow(allocations.filter { it.ledgerEntryId == ledgerEntryId }).asStateFlow()

        override fun getAllocationsForInvoice(invoiceId: Long): Flow<List<PaymentAllocationEntity>> =
            MutableStateFlow(allocations.filter { it.invoiceId == invoiceId }).asStateFlow()

        override suspend fun getAllocationListForInvoice(invoiceId: Long): List<PaymentAllocationEntity> =
            allocations.filter { it.invoiceId == invoiceId }

        override fun getAllocations(): Flow<List<PaymentAllocationEntity>> =
            MutableStateFlow(allocations.toList()).asStateFlow()

        override suspend fun getAllocationById(id: Long): PaymentAllocationEntity? =
            allocations.find { it.id == id }

        override fun getTotalAllocatedForInvoice(invoiceId: Long): Flow<Long> =
            MutableStateFlow(allocations.filter { it.invoiceId == invoiceId }.sumOf { it.allocatedAmountPaise }).asStateFlow()

        override suspend fun deleteAllocation(id: Long) {
            allocations.removeAll { it.id == id }
        }
    }

    class FakeInvoiceDao : InvoiceDao {
        val invoices = mutableListOf<InvoiceEntity>()
        private var nextId = 1L

        override suspend fun insertInvoice(invoice: InvoiceEntity): Long {
            val id = if (invoice.id > 0) invoice.id else nextId++
            val saved = invoice.copy(id = id)
            invoices.add(saved)
            return id
        }

        override suspend fun insertInvoiceItem(item: InvoiceItemEntity): Long = nextId++

        override suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>) {}

        override suspend fun upsertInvoice(invoice: InvoiceEntity) {
            val idx = invoices.indexOfFirst { it.id == invoice.id || it.invoiceNumber == invoice.invoiceNumber }
            if (idx >= 0) invoices[idx] = invoice else invoices.add(invoice)
        }

        override suspend fun upsertInvoiceItems(items: List<InvoiceItemEntity>) {}

        override fun getAllInvoices(): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.toList()).asStateFlow()

        override fun getInvoicesByType(type: String): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.filter { it.invoiceType == type }).asStateFlow()

        override fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.filter { it.status == status }).asStateFlow()

        override fun getInvoicesSince(startTime: Long): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.filter { it.timestamp >= startTime }).asStateFlow()

        override fun getInvoicesBetween(startTime: Long, endTime: Long): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.filter { it.timestamp in startTime..endTime }).asStateFlow()

        override fun searchInvoices(query: String): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.filter { it.invoiceNumber.contains(query, true) }).asStateFlow()

        override fun searchInvoicesByStatus(query: String, status: InvoiceStatus): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.filter { it.status == status && it.invoiceNumber.contains(query, true) }).asStateFlow()

        override fun searchInvoicesByType(query: String, type: String): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.filter { it.invoiceType == type && it.invoiceNumber.contains(query, true) }).asStateFlow()

        override fun getTotalInvoiceCount(): Flow<Int> =
            MutableStateFlow(invoices.count { it.status == InvoiceStatus.COMPLETED }).asStateFlow()

        override fun getInvoiceCountSince(startTime: Long): Flow<Int> =
            MutableStateFlow(invoices.count { it.status == InvoiceStatus.COMPLETED && it.timestamp >= startTime }).asStateFlow()

        override fun getInvoiceCountBetween(startTime: Long, endTime: Long): Flow<Int> =
            MutableStateFlow(invoices.count { it.status == InvoiceStatus.COMPLETED && it.timestamp in startTime..endTime }).asStateFlow()

        override fun getTotalSalesSince(startTime: Long): Flow<Long> =
            MutableStateFlow(invoices.filter { it.status == InvoiceStatus.COMPLETED && it.timestamp >= startTime }.sumOf { it.grandTotalPaise }).asStateFlow()

        override fun getTotalSalesBetween(startTime: Long, endTime: Long): Flow<Long> =
            MutableStateFlow(invoices.filter { it.status == InvoiceStatus.COMPLETED && it.timestamp in startTime..endTime }.sumOf { it.grandTotalPaise }).asStateFlow()

        override fun getTotalItemsSoldBetween(startTime: Long, endTime: Long): Flow<Int> =
            MutableStateFlow(0).asStateFlow()

        override fun getTotalItemsSoldAllTime(): Flow<Int> =
            MutableStateFlow(0).asStateFlow()

        override fun getAverageBillBetween(startTime: Long, endTime: Long): Flow<Double> =
            MutableStateFlow(0.0).asStateFlow()

        override fun getCreditDuesInvoices(): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.filter { it.duePaise > 0L && it.status == InvoiceStatus.COMPLETED }).asStateFlow()

        override fun getTotalDuePaise(): Flow<Long> =
            MutableStateFlow(invoices.filter { it.status == InvoiceStatus.COMPLETED }.sumOf { it.duePaise }).asStateFlow()

        override fun getPendingBillBetween(startTime: Long, endTime: Long): Flow<Long> =
            MutableStateFlow(0L).asStateFlow()

        override fun getInvoicesByCustomerPhone(phone: String): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.filter { it.customerPhone == phone }).asStateFlow()

        override fun getTopSellingProducts(startTime: Long, endTime: Long, limit: Int): Flow<List<TopProductSummary>> =
            MutableStateFlow(emptyList<TopProductSummary>()).asStateFlow()

        override fun getSalesByCategory(startTime: Long, endTime: Long): Flow<List<CategorySalesSummary>> =
            MutableStateFlow(emptyList<CategorySalesSummary>()).asStateFlow()

        override suspend fun getInvoiceByNumber(invoiceNumber: String): InvoiceEntity? =
            invoices.find { it.invoiceNumber == invoiceNumber }

        override suspend fun getInvoiceById(id: Long): InvoiceEntity? =
            invoices.find { it.id == id }

        override suspend fun getInvoiceItems(invoiceNumber: String): List<InvoiceItemEntity> = emptyList()

        override suspend fun getInvoiceItemsByInvoiceId(invoiceId: Long): List<InvoiceItemEntity> = emptyList()

        override suspend fun getAllInvoiceItems(): List<InvoiceItemEntity> = emptyList()

        override suspend fun updateInvoiceStatus(invoiceNumber: String, status: InvoiceStatus) {
            val idx = invoices.indexOfFirst { it.invoiceNumber == invoiceNumber }
            if (idx >= 0) invoices[idx] = invoices[idx].copy(status = status)
        }

        override suspend fun updateInvoiceVoidStatus(
            invoiceNumber: String,
            voidedAt: Long,
            voidedByUserId: String?,
            voidReason: String?
        ) {
            val idx = invoices.indexOfFirst { it.invoiceNumber == invoiceNumber }
            if (idx >= 0) {
                invoices[idx] = invoices[idx].copy(
                    status = InvoiceStatus.CANCELLED,
                    voidedAt = voidedAt,
                    voidedByUserId = voidedByUserId,
                    voidReason = voidReason
                )
            }
        }

        override suspend fun deleteInvoice(invoiceNumber: String) {
            invoices.removeAll { it.invoiceNumber == invoiceNumber }
        }

        override suspend fun deleteInvoiceItems(invoiceNumber: String) {}

        override suspend fun deleteInvoiceItemsByInvoiceId(invoiceId: Long) {}

        override suspend fun updateInvoiceDue(invoiceNumber: String, duePaise: Long) {
            val idx = invoices.indexOfFirst { it.invoiceNumber == invoiceNumber }
            if (idx >= 0) invoices[idx] = invoices[idx].copy(duePaise = duePaise)
        }

        override suspend fun getLastInvoiceNumber(): Long? =
            invoices.mapNotNull { it.invoiceNumber.removePrefix("INV-").toLongOrNull() }.maxOrNull()

        override suspend fun setInvoiceCounter(counter: InvoiceCounterEntity) {}

        override suspend fun getInvoicesByShiftId(shiftId: Long): List<InvoiceEntity> =
            invoices.filter { it.shiftId == shiftId && it.status == InvoiceStatus.COMPLETED }

        override suspend fun getInvoiceItemsByShiftId(shiftId: Long): List<InvoiceItemEntity> = emptyList()
    }

    class FakeCashMovementDao : CashMovementDao {
        val movements = mutableListOf<CashMovementEntity>()
        private var nextId = 1L

        override suspend fun insertCashMovement(movement: CashMovementEntity): Long {
            val id = nextId++
            val saved = movement.copy(id = id)
            saved.source = movement.source
            saved.sourceId = movement.sourceId
            saved.notes = movement.notes
            movements.add(saved)
            return id
        }

        override suspend fun upsertCashMovement(movement: CashMovementEntity) {
            val idx = movements.indexOfFirst { it.id == movement.id }
            if (idx >= 0) movements[idx] = movement else movements.add(movement)
        }

        override suspend fun upsertAll(movements: List<CashMovementEntity>) {
            movements.forEach { upsertCashMovement(it) }
        }

        override fun getCashMovementsForShift(shiftId: Long): Flow<List<CashMovementEntity>> =
            MutableStateFlow(movements.filter { it.shiftId == shiftId }).asStateFlow()

        override suspend fun getCashMovementListForShift(shiftId: Long): List<CashMovementEntity> =
            movements.filter { it.shiftId == shiftId }

        override fun getAllCashMovements(): Flow<List<CashMovementEntity>> =
            MutableStateFlow(movements.toList()).asStateFlow()

        override suspend fun getCashMovementById(id: Long): CashMovementEntity? =
            movements.find { it.id == id }

        override fun getTotalByMovementType(shiftId: Long, movementType: String): Flow<Long> =
            MutableStateFlow(movements.filter { it.shiftId == shiftId && it.movementType == movementType }.sumOf { it.amountPaise }).asStateFlow()

        override suspend fun getNetCashForShift(shiftId: Long): Long =
            movements.filter { it.shiftId == shiftId && it.reason != "Opening Float" }.sumOf { it.amountPaise }

        override fun getNetCashForShiftFlow(shiftId: Long): Flow<Long> =
            MutableStateFlow(movements.filter { it.shiftId == shiftId && it.reason != "Opening Float" }.sumOf { it.amountPaise }).asStateFlow()

        override suspend fun deleteCashMovement(id: Long) {
            movements.removeAll { it.id == id }
        }

        override suspend fun deleteCashMovementsForShift(shiftId: Long) {
            movements.removeAll { it.shiftId == shiftId }
        }
    }
}
