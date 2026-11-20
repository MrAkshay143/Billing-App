package com.billing.app.data.repository

import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.core.util.Quantity
import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.CustomerDao
import com.billing.app.data.local.dao.CustomerLedgerDao
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.PaymentAllocationDao
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.data.local.entity.PaymentAllocationEntity
import com.billing.app.domain.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Locale

class CustomerRepositoryImpl(
    private val customerDao: CustomerDao,
    private val customerLedgerDao: CustomerLedgerDao? = null,
    private val paymentAllocationDao: PaymentAllocationDao? = null,
    private val invoiceDao: InvoiceDao? = null,
    private val cashMovementDao: CashMovementDao? = null,
    private val database: AppDatabase? = null
) : CustomerRepository {

    constructor(database: AppDatabase) : this(
        customerDao = database.customerDao(),
        customerLedgerDao = database.customerLedgerDao(),
        paymentAllocationDao = database.paymentAllocationDao(),
        invoiceDao = database.invoiceDao(),
        cashMovementDao = database.cashMovementDao(),
        database = database
    )

    private suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
    }

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
            if (customer.openingBalancePaise > 0L && customerLedgerDao != null) {
                customerLedgerDao.insertEntry(
                    CustomerLedgerEntity(
                        customerId = customer.id,
                        transactionType = "OPENING_BALANCE",
                        debitPaise = customer.openingBalancePaise,
                        creditPaise = 0L,
                        balanceAfterPaise = customer.openingBalancePaise,
                        note = "Opening Balance"
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCustomers(customers: List<CustomerEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            customerDao.insertAll(customers)
            if (customerLedgerDao != null) {
                for (customer in customers) {
                    if (customer.openingBalancePaise > 0L) {
                        customerLedgerDao.insertEntry(
                            CustomerLedgerEntity(
                                customerId = customer.id,
                                transactionType = "OPENING_BALANCE",
                                debitPaise = customer.openingBalancePaise,
                                creditPaise = 0L,
                                balanceAfterPaise = customer.openingBalancePaise,
                                note = "Opening Balance"
                            )
                        )
                    }
                }
            }
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

    override fun getCustomerLedger(customerId: String): Flow<List<CustomerLedgerEntity>> {
        return customerLedgerDao?.getEntriesForCustomer(customerId) ?: emptyFlow()
    }

    override fun getCustomerBalance(customerId: String): Flow<Long> {
        val ledgerDao = customerLedgerDao ?: return flowOf(0L)
        return combine(
            ledgerDao.getTotalDebitForCustomer(customerId),
            ledgerDao.getTotalCreditForCustomer(customerId)
        ) { debit, credit ->
            debit - credit
        }
    }

    override suspend fun getCustomerBalanceSnapshot(customerId: String): Long = withContext(Dispatchers.IO) {
        customerLedgerDao?.getLatestEntryForCustomer(customerId)?.balanceAfterPaise
            ?: customerDao.getCustomerById(customerId)?.openingBalancePaise
            ?: 0L
    }

    override suspend fun recordKhataPayment(
        customerId: String,
        amountPaise: Long,
        paymentMode: String,
        shiftId: Long?,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            runInTransaction {
                // 1. Calculate running balance:
                val currentBal = getCustomerBalanceSnapshot(customerId)
                val newBal = currentBal - amountPaise

                // 2. Insert CustomerLedgerEntity:
                val ledgerEntry = CustomerLedgerEntity(
                    customerId = customerId,
                    transactionType = "PAYMENT",
                    debitPaise = 0L,
                    creditPaise = amountPaise,
                    balanceAfterPaise = newBal,
                    paymentMode = paymentMode,
                    note = note ?: "Khata Payment"
                )
                val ledgerEntryId = customerLedgerDao?.insertEntry(ledgerEntry) ?: 0L

                // 3. FIFO Allocation across pending invoices (payment_allocations):
                val customer = customerDao.getCustomerById(customerId)
                val phone = customer?.phone?.trim().orEmpty()
                val phoneInvoices = if (phone.isNotEmpty() && invoiceDao != null) {
                    invoiceDao.getInvoicesByCustomerPhone(phone).firstOrNull() ?: emptyList()
                } else {
                    emptyList()
                }
                val rawInvoices = if (phoneInvoices.isNotEmpty()) {
                    phoneInvoices
                } else if (invoiceDao != null) {
                    invoiceDao.getAllInvoices().firstOrNull() ?: emptyList()
                } else {
                    emptyList()
                }

                val pendingInvoices = rawInvoices
                    .filter { inv ->
                        (inv.customerId == customerId || (phone.isNotEmpty() && inv.customerPhone == phone)) &&
                            inv.status == InvoiceStatus.COMPLETED &&
                            inv.duePaise > 0L
                    }
                    .sortedBy { it.timestamp }

                var remaining = amountPaise
                for (inv in pendingInvoices) {
                    if (remaining <= 0L) break
                    val alloc = minOf(remaining, inv.duePaise)
                    paymentAllocationDao?.insertAllocation(
                        PaymentAllocationEntity(
                            ledgerEntryId = ledgerEntryId,
                            invoiceId = inv.id,
                            allocatedAmountPaise = alloc,
                            note = "FIFO payment allocation"
                        )
                    )
                    invoiceDao?.updateInvoiceDue(inv.invoiceNumber, inv.duePaise - alloc)
                    remaining -= alloc
                }

                if (remaining > 0L) {
                    paymentAllocationDao?.insertAllocation(
                        PaymentAllocationEntity(
                            ledgerEntryId = ledgerEntryId,
                            invoiceId = null,
                            allocatedAmountPaise = remaining,
                            note = "Advance credit"
                        )
                    )
                }

                // 4. Sync with Cash Drawer (cash_movements):
                if (paymentMode.equals("CASH", ignoreCase = true) && shiftId != null) {
                    cashMovementDao?.insertCashMovement(
                        CashMovementEntity(
                            shiftId = shiftId,
                            movementType = "KHATA_COLLECTION",
                            amountPaise = amountPaise,
                            source = "CUSTOMER_PAYMENT",
                            sourceId = customerId,
                            notes = "Khata payment received from ${customer?.name}"
                        )
                    )
                }

                // 5. Sync cached balance on CustomerEntity:
                customer?.let {
                    customerDao.updateCustomer(it.copy(openingBalancePaise = newBal))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun generatePaymentReminderWhatsAppUrl(
        customer: CustomerEntity,
        duePaise: Long,
        upiId: String,
        shopName: String
    ): String {
        val normalizedPhone = Quantity.normalizeInputDigits(customer.phone)
        val digits = normalizedPhone.filter { it.isDigit() }
        val cleanPhone = if (digits.length >= 10) digits.takeLast(10) else digits

        val amount = String.format(Locale.ROOT, "%.2f", duePaise / 100.0)
        val formattedRupees = "Rs. $amount"

        val upiUri = "upi://pay?pa=$upiId&pn=$shopName&am=$amount&tn=Khata%20Payment"
        val message = "Dear ${customer.name}, you have a pending payment of $formattedRupees at $shopName. Please pay using UPI: $upiUri\nThank you!"
        val encodedMessage = URLEncoder.encode(message, "UTF-8")

        return "https://wa.me/91$cleanPhone?text=$encodedMessage"
    }
}
