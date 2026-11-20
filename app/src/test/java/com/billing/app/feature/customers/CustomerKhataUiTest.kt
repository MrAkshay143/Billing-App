package com.billing.app.feature.customers

import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import com.billing.app.data.repository.CustomerKhataFifoTest
import com.billing.app.data.repository.CustomerRepositoryImpl
import com.billing.app.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URLDecoder

class CustomerKhataUiTest {

    private lateinit var fakeCustomerDao: CustomerKhataFifoTest.FakeCustomerDao
    private lateinit var fakeCustomerLedgerDao: CustomerKhataFifoTest.FakeCustomerLedgerDao
    private lateinit var fakePaymentAllocationDao: CustomerKhataFifoTest.FakePaymentAllocationDao
    private lateinit var fakeInvoiceDao: CustomerKhataFifoTest.FakeInvoiceDao
    private lateinit var fakeCashMovementDao: CustomerKhataFifoTest.FakeCashMovementDao
    private lateinit var customerRepository: CustomerRepository

    @Before
    fun setup() {
        fakeCustomerDao = CustomerKhataFifoTest.FakeCustomerDao()
        fakeCustomerLedgerDao = CustomerKhataFifoTest.FakeCustomerLedgerDao()
        fakePaymentAllocationDao = CustomerKhataFifoTest.FakePaymentAllocationDao()
        fakeInvoiceDao = CustomerKhataFifoTest.FakeInvoiceDao()
        fakeCashMovementDao = CustomerKhataFifoTest.FakeCashMovementDao()

        customerRepository = CustomerRepositoryImpl(
            customerDao = fakeCustomerDao,
            customerLedgerDao = fakeCustomerLedgerDao,
            paymentAllocationDao = fakePaymentAllocationDao,
            invoiceDao = fakeInvoiceDao,
            cashMovementDao = fakeCashMovementDao,
            database = null
        )
    }

    // =========================================================================
    // 1. Point 23: Cash Payment Drawer Sync (KHATA_COLLECTION in active shift)
    // =========================================================================

    @Test
    fun testCashPaymentSyncsWithCashMovementDrawer() = runBlocking {
        val customer = CustomerEntity(
            id = "cust_khata_1",
            name = "Ramesh Kumar",
            phone = "9876543210",
            openingBalancePaise = 50000L // ₹500.00
        )
        fakeCustomerDao.insertCustomer(customer)
        fakeCustomerLedgerDao.insertEntry(
            CustomerLedgerEntity(
                customerId = customer.id,
                transactionType = "OPENING_BALANCE",
                debitPaise = 50000L,
                creditPaise = 0L,
                balanceAfterPaise = 50000L
            )
        )

        // Customer pays ₹200 (20000 paise) via Cash on active shift #42
        val activeShiftId = 42L
        val paymentAmountPaise = 20000L
        val result = customerRepository.recordKhataPayment(
            customerId = customer.id,
            amountPaise = paymentAmountPaise,
            paymentMode = "CASH",
            shiftId = activeShiftId,
            note = "Khata Cash Collection"
        )

        assertTrue(result.isSuccess)

        // Verify Cash Movement created in cash drawer
        val movements = fakeCashMovementDao.movements
        assertEquals(1, movements.size)
        val movement = movements.first()
        assertEquals(activeShiftId, movement.shiftId)
        assertEquals("KHATA_COLLECTION", movement.movementType)
        assertEquals(paymentAmountPaise, movement.amountPaise)
        assertEquals("CUSTOMER_PAYMENT", movement.source)
        assertEquals(customer.id, movement.sourceId)
        assertEquals("Khata payment received from Ramesh Kumar", movement.notes)

        // Verify Customer Balance reduced
        val updatedCustomer = fakeCustomerDao.getCustomerById(customer.id)
        assertNotNull(updatedCustomer)
        assertEquals(30000L, updatedCustomer!!.openingBalancePaise)

        // Verify Customer Ledger Entry
        val ledger = fakeCustomerLedgerDao.entries.filter { it.customerId == customer.id }
        assertEquals(2, ledger.size)
        val paymentEntry = ledger.last()
        assertEquals("PAYMENT", paymentEntry.transactionType)
        assertEquals(20000L, paymentEntry.creditPaise)
        assertEquals(0L, paymentEntry.debitPaise)
        assertEquals(30000L, paymentEntry.balanceAfterPaise)
        assertEquals("CASH", paymentEntry.paymentMode)
    }

    @Test
    fun testUpiPaymentDoesNotInsertCashMovementDrawer() = runBlocking {
        val customer = CustomerEntity(
            id = "cust_khata_2",
            name = "Priya Sen",
            phone = "9123456789",
            openingBalancePaise = 30000L
        )
        fakeCustomerDao.insertCustomer(customer)

        val result = customerRepository.recordKhataPayment(
            customerId = customer.id,
            amountPaise = 15000L,
            paymentMode = "UPI",
            shiftId = 99L,
            note = "GooglePay Payment"
        )

        assertTrue(result.isSuccess)

        // Cash drawer should NOT have a movement for UPI payments
        assertTrue(fakeCashMovementDao.movements.isEmpty())

        // Customer ledger still updated
        val ledger = fakeCustomerLedgerDao.entries.filter { it.customerId == customer.id }
        assertEquals(1, ledger.size)
        assertEquals("UPI", ledger.first().paymentMode)
        assertEquals(15000L, ledger.first().creditPaise)
    }

    // =========================================================================
    // 2. Point 26: WhatsApp Payment Reminder URL Generation & UPI Deep Link
    // =========================================================================

    @Test
    fun testGeneratePaymentReminderWhatsAppUrlWithUpiDeepLink() {
        val customer = CustomerEntity(
            id = "cust_khata_3",
            name = "Subhash Bose",
            phone = "+91 98300 12345"
        )
        val duePaise = 125050L // ₹1250.50
        val upiId = "dokandaar@icici"
        val shopName = "Bose General Store"

        val url = customerRepository.generatePaymentReminderWhatsAppUrl(
            customer = customer,
            duePaise = duePaise,
            upiId = upiId,
            shopName = shopName
        )

        // Verify URL target: clean phone (919830012345)
        assertTrue(url.startsWith("https://wa.me/919830012345?text="))

        // Verify URL-decoded content
        val textEncoded = url.substringAfter("text=")
        val textDecoded = URLDecoder.decode(textEncoded, "UTF-8")

        assertTrue(textDecoded.contains("Dear Subhash Bose"))
        assertTrue(textDecoded.contains("Rs. 1250.50"))
        assertTrue(textDecoded.contains("Bose General Store"))
        assertTrue(textDecoded.contains("upi://pay?pa=dokandaar@icici&pn=Bose General Store&am=1250.50&tn=Khata%20Payment"))
    }

    @Test
    fun testGenerateWhatsAppUrlHandlesVariousPhoneFormats() {
        val customer = CustomerEntity(
            name = "Arjun Das",
            phone = "09876543210"
        )
        val url = customerRepository.generatePaymentReminderWhatsAppUrl(
            customer = customer,
            duePaise = 10000L,
            upiId = "test@upi",
            shopName = "Test Shop"
        )
        // Last 10 digits used: 9876543210
        assertTrue(url.contains("https://wa.me/919876543210?text="))
    }

    // =========================================================================
    // 3. Points 20 & 21: Customer Ledger Double-Entry History & Balance Display
    // =========================================================================

    @Test
    fun testCustomerLedgerDoubleEntryHistoryCalculations() = runBlocking {
        val customerId = "cust_khata_ledger"

        // 1. Opening Balance: ₹500 debit
        fakeCustomerLedgerDao.insertEntry(
            CustomerLedgerEntity(
                customerId = customerId,
                transactionType = "OPENING_BALANCE",
                debitPaise = 50000L,
                creditPaise = 0L,
                balanceAfterPaise = 50000L,
                note = "Opening Balance"
            )
        )

        // 2. Credit Sale: ₹300 debit
        fakeCustomerLedgerDao.insertEntry(
            CustomerLedgerEntity(
                customerId = customerId,
                transactionType = "CREDIT_SALE",
                invoiceId = 101L,
                debitPaise = 30000L,
                creditPaise = 0L,
                balanceAfterPaise = 80000L,
                note = "Groceries on Udhaar"
            )
        )

        // 3. Khata Payment: ₹400 credit (Cash)
        fakeCustomerLedgerDao.insertEntry(
            CustomerLedgerEntity(
                customerId = customerId,
                transactionType = "PAYMENT",
                debitPaise = 0L,
                creditPaise = 40000L,
                balanceAfterPaise = 40000L,
                paymentMode = "CASH",
                note = "Partial Khata Payment"
            )
        )

        // 4. Return Credit: ₹100 credit
        fakeCustomerLedgerDao.insertEntry(
            CustomerLedgerEntity(
                customerId = customerId,
                transactionType = "RETURN_CREDIT",
                debitPaise = 0L,
                creditPaise = 10000L,
                balanceAfterPaise = 30000L,
                note = "Damaged Goods Return"
            )
        )

        // Test ledger retrieval
        val ledgerEntries = customerRepository.getCustomerLedger(customerId).first()
        assertEquals(4, ledgerEntries.size)

        // Verify chronological/type checks
        assertEquals("OPENING_BALANCE", ledgerEntries[0].transactionType)
        assertEquals(50000L, ledgerEntries[0].debitPaise)

        assertEquals("CREDIT_SALE", ledgerEntries[1].transactionType)
        assertEquals(101L, ledgerEntries[1].invoiceId)
        assertEquals(30000L, ledgerEntries[1].debitPaise)

        assertEquals("PAYMENT", ledgerEntries[2].transactionType)
        assertEquals("CASH", ledgerEntries[2].paymentMode)
        assertEquals(40000L, ledgerEntries[2].creditPaise)

        assertEquals("RETURN_CREDIT", ledgerEntries[3].transactionType)
        assertEquals(10000L, ledgerEntries[3].creditPaise)

        // Test running balance calculation:
        // Total Debit: 50000 + 30000 = 80000
        // Total Credit: 40000 + 10000 = 50000
        // Net Balance: 80000 - 50000 = 30000
        val liveBalance = customerRepository.getCustomerBalance(customerId).first()
        assertEquals(30000L, liveBalance)

        val latestBalance = customerRepository.getCustomerBalanceSnapshot(customerId)
        assertEquals(30000L, latestBalance)
    }

    // =========================================================================
    // 4. Credit Limit & Available Credit Computation Logic
    // =========================================================================

    @Test
    fun testCreditLimitAndAvailableComputation() {
        val creditLimitPaise = 100000L // ₹1000.00
        val duePaise = 65000L         // ₹650.00

        val availableCredit = maxOf(0L, creditLimitPaise - duePaise)
        assertEquals(35000L, availableCredit)
        assertEquals("₹350.00", Money(availableCredit).format())

        // Exceeded credit limit: Available should be 0, not negative
        val highDuePaise = 150000L // ₹1500.00
        val availableWhenExceeded = maxOf(0L, creditLimitPaise - highDuePaise)
        assertEquals(0L, availableWhenExceeded)
    }

    @Test
    fun testMoneyFormattingForKhataLedger() {
        assertEquals("₹500.00", Money(50000L).format())
        assertEquals("₹0.00", Money(0L).format())
        assertEquals("-₹150.00", Money(-15000L).format())
        assertEquals("₹1250.50", Money(125050L).format())
    }
}
