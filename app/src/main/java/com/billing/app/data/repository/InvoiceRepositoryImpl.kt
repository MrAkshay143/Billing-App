package com.billing.app.data.repository

import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.data.local.dao.AuditLogDao
import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.CategorySalesSummary
import com.billing.app.data.local.dao.CustomerDao
import com.billing.app.data.local.dao.CustomerLedgerDao
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.InvoiceItemBatchDao
import com.billing.app.data.local.dao.ProductBatchDao
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.dao.ReturnDao
import com.billing.app.data.local.dao.ShiftDao
import com.billing.app.data.local.dao.TopProductSummary
import com.billing.app.data.local.entity.AuditLogEntity
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import com.billing.app.data.local.entity.InvoiceCounterEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemBatchEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.domain.repository.InvoiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale

class InvoiceRepositoryImpl(
    private val invoiceDao: InvoiceDao,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val customerLedgerDao: CustomerLedgerDao,
    private val cashMovementDao: CashMovementDao,
    private val productBatchDao: ProductBatchDao? = null,
    private val invoiceItemBatchDao: InvoiceItemBatchDao? = null,
    private val shiftDao: ShiftDao? = null,
    private val returnDao: ReturnDao? = null,
    private val auditLogDao: AuditLogDao? = null,
    private val database: AppDatabase? = null
) : InvoiceRepository {

    constructor(database: AppDatabase) : this(
        invoiceDao = database.invoiceDao(),
        productDao = database.productDao(),
        customerDao = database.customerDao(),
        customerLedgerDao = database.customerLedgerDao(),
        cashMovementDao = database.cashMovementDao(),
        productBatchDao = database.productBatchDao(),
        invoiceItemBatchDao = database.invoiceItemBatchDao(),
        shiftDao = database.shiftDao(),
        returnDao = database.returnDao(),
        auditLogDao = database.auditLogDao(),
        database = database
    )

    private suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
    }

    override fun getAllInvoices(): Flow<List<InvoiceEntity>> =
        invoiceDao.getAllInvoices()

    override fun getInvoicesByType(type: String): Flow<List<InvoiceEntity>> =
        invoiceDao.getInvoicesByType(type)

    override fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>> =
        invoiceDao.getInvoicesByStatus(status)

    override fun getInvoicesSince(startTime: Long): Flow<List<InvoiceEntity>> =
        invoiceDao.getInvoicesSince(startTime)

    override fun getInvoicesBetween(startTime: Long, endTime: Long): Flow<List<InvoiceEntity>> =
        invoiceDao.getInvoicesBetween(startTime, endTime)

    override fun searchInvoices(query: String, status: InvoiceStatus?, type: String?): Flow<List<InvoiceEntity>> {
        val escaped = query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

        return when {
            status != null -> invoiceDao.searchInvoicesByStatus(escaped, status)
            !type.isNullOrBlank() && !type.equals("ALL", ignoreCase = true) -> invoiceDao.searchInvoicesByType(escaped, type)
            else -> invoiceDao.searchInvoices(escaped)
        }
    }

    override fun getTotalInvoiceCount(): Flow<Int> =
        invoiceDao.getTotalInvoiceCount()

    override fun getInvoiceCountBetween(startTime: Long, endTime: Long): Flow<Int> =
        invoiceDao.getInvoiceCountBetween(startTime, endTime)

    override fun getTotalSalesBetween(startTime: Long, endTime: Long): Flow<Long> =
        invoiceDao.getTotalSalesBetween(startTime, endTime)

    override fun getTotalItemsSoldBetween(startTime: Long, endTime: Long): Flow<Int> =
        invoiceDao.getTotalItemsSoldBetween(startTime, endTime)

    override fun getTotalItemsSoldAllTime(): Flow<Int> =
        invoiceDao.getTotalItemsSoldAllTime()

    override fun getAverageBillBetween(startTime: Long, endTime: Long): Flow<Double> =
        invoiceDao.getAverageBillBetween(startTime, endTime)

    override fun getCreditDuesInvoices(): Flow<List<InvoiceEntity>> =
        invoiceDao.getCreditDuesInvoices()

    override fun getTotalDuePaise(): Flow<Long> =
        invoiceDao.getTotalDuePaise()

    override fun getPendingBillBetween(startTime: Long, endTime: Long): Flow<Long> =
        invoiceDao.getPendingBillBetween(startTime, endTime)

    override fun getInvoicesByCustomerPhone(phone: String): Flow<List<InvoiceEntity>> =
        invoiceDao.getInvoicesByCustomerPhone(phone)

    override fun getTopSellingProducts(startTime: Long, endTime: Long, limit: Int): Flow<List<TopProductSummary>> =
        invoiceDao.getTopSellingProducts(startTime, endTime, limit)

    override fun getSalesByCategory(startTime: Long, endTime: Long): Flow<List<CategorySalesSummary>> =
        invoiceDao.getSalesByCategory(startTime, endTime)

    override suspend fun getInvoiceByNumber(invoiceNumber: String): InvoiceEntity? =
        invoiceDao.getInvoiceByNumber(invoiceNumber)

    override suspend fun getInvoiceItems(invoiceNumber: String): List<InvoiceItemEntity> =
        invoiceDao.getInvoiceItems(invoiceNumber)

    override suspend fun getAllInvoiceItems(): List<InvoiceItemEntity> =
        invoiceDao.getAllInvoiceItems()

    override suspend fun generateNextInvoiceNumber(): String = withContext(Dispatchers.IO) {
        runInTransaction {
            val lastNum = invoiceDao.getLastInvoiceNumber() ?: 0L
            val nextNum = lastNum + 1
            invoiceDao.setInvoiceCounter(InvoiceCounterEntity(id = 1, lastNumber = nextNum))
            String.format(Locale.US, "INV-%06d", nextNum)
        }
    }

    override suspend fun completeSale(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        draftInvoiceNumberToDelete: String?
    ): Result<InvoiceEntity> = withContext(Dispatchers.IO) {
        // 1. Mandatory active shift rule
        if (invoice.shiftId == null) {
            return@withContext Result.failure(IllegalStateException("Active shift required for completing sale"))
        }

        try {
            val completedInvoice = runInTransaction {
                // Determine invoice number atomically inside transaction to prevent counter leaks on failure
                val finalInvoiceNumber = if (invoice.invoiceNumber.isBlank() || invoice.invoiceNumber.startsWith("DFT-")) {
                    val lastNum = invoiceDao.getLastInvoiceNumber() ?: 0L
                    val nextNum = lastNum + 1
                    invoiceDao.setInvoiceCounter(InvoiceCounterEntity(id = 1, lastNumber = nextNum))
                    String.format(Locale.US, "INV-%06d", nextNum)
                } else {
                    invoice.invoiceNumber
                }
                val invoiceToInsert = if (invoice.invoiceNumber != finalInvoiceNumber) {
                    invoice.copy(invoiceNumber = finalInvoiceNumber)
                } else {
                    invoice
                }

                // 2. Invoices insertion
                val insertedId = invoiceDao.insertInvoice(invoiceToInsert)
                val finalInvoice = if (invoiceToInsert.id == 0L) invoiceToInsert.copy(id = insertedId) else invoiceToInsert

                // 3. Items insertion & stock deduction (FEFO for batch tracked products)
                val pBatchDao = productBatchDao ?: database?.productBatchDao()
                val iBatchDao = invoiceItemBatchDao ?: database?.invoiceItemBatchDao()

                for (item in items) {
                    val preparedItem = item.copy(
                        invoiceId = finalInvoice.id
                    ).apply {
                        this.invoiceNumber = finalInvoice.invoiceNumber
                    }
                    val invoiceItemId = invoiceDao.insertInvoiceItem(preparedItem)

                    if (item.productId != null && !item.isCustomItem) {
                        val qtyMilli = if (item.quantityMilli > 0L) item.quantityMilli else item.quantity.toLong() * 1000L
                        val product = productDao.getProductById(item.productId)
                        if (product?.trackBatch == true) {
                            if (pBatchDao != null) {
                                val activeBatches = pBatchDao.getActiveBatchesFEFO(item.productId)
                                var qtyLeft = qtyMilli
                                for (batch in activeBatches) {
                                    if (qtyLeft <= 0L) break
                                    val deduct = minOf(qtyLeft, batch.currentStockMilli)
                                    pBatchDao.decrementBatchStock(batch.id, deduct)
                                    if (invoiceItemId > 0L) {
                                        iBatchDao?.insertItemBatch(
                                            InvoiceItemBatchEntity(
                                                invoiceItemId = invoiceItemId,
                                                batchId = batch.id,
                                                quantityMilli = deduct,
                                                costPaise = batch.purchasePricePaise
                                            )
                                        )
                                    }
                                    qtyLeft -= deduct
                                }
                                if (qtyLeft > 0L) {
                                    throw IllegalStateException("Insufficient batch stock for '${item.productName}'")
                                }
                                val rowsUpdated = productDao.decrementProductStockMilli(item.productId, qtyMilli)
                                if (rowsUpdated == 0) {
                                    productDao.forceDecrementProductStockMilli(item.productId, qtyMilli)
                                }
                            } else {
                                val rowsUpdated = productDao.decrementProductStockMilli(item.productId, qtyMilli)
                                if (rowsUpdated == 0) {
                                    throw IllegalStateException("Insufficient stock for batch tracked product '${item.productName}'")
                                }
                            }
                        } else {
                            productDao.forceDecrementProductStockMilli(item.productId, qtyMilli)
                        }
                    }
                }

                // 4. Delete draft if sale was from resumed draft (delete child items before parent invoice)
                if (!draftInvoiceNumberToDelete.isNullOrBlank()) {
                    invoiceDao.deleteInvoiceItems(draftInvoiceNumberToDelete)
                    invoiceDao.deleteInvoice(draftInvoiceNumberToDelete)
                }

                // 5. Net cash drawer movement
                val netCash = finalInvoice.cashReceivedPaise - finalInvoice.changeReturnedPaise
                if (netCash > 0L) {
                    cashMovementDao.insertCashMovement(
                        CashMovementEntity(
                            shiftId = finalInvoice.shiftId!!,
                            movementType = "SALE_CASH",
                            amountPaise = netCash,
                            source = "INVOICE",
                            sourceId = finalInvoice.invoiceNumber
                        )
                    )
                }

                // 6. Customer Khata Double-Entry Ledger (CREDIT_SALE)
                val dueAmount = if (finalInvoice.originalDuePaise > 0L) finalInvoice.originalDuePaise else finalInvoice.duePaise
                if (dueAmount > 0L) {
                    val targetCustomerId = finalInvoice.customerId ?: finalInvoice.customerPhone?.let { phone ->
                        customerDao.getCustomerByPhone(phone.trim())?.id
                    }
                    if (targetCustomerId != null) {
                        val currentBal = customerLedgerDao.getLatestEntryForCustomer(targetCustomerId)?.balanceAfterPaise
                            ?: customerDao.getCustomerById(targetCustomerId)?.openingBalancePaise
                            ?: 0L
                        val newBal = currentBal + dueAmount
                        customerLedgerDao.insertEntry(
                            CustomerLedgerEntity(
                                customerId = targetCustomerId,
                                transactionType = "CREDIT_SALE",
                                invoiceId = finalInvoice.id,
                                debitPaise = dueAmount,
                                creditPaise = 0L,
                                balanceAfterPaise = newBal,
                                note = "Credit sale: ${finalInvoice.invoiceNumber}"
                            )
                        )
                        val customer = customerDao.getCustomerById(targetCustomerId)
                        if (customer != null) {
                            customerDao.updateCustomer(customer.copy(openingBalancePaise = newBal))
                        }
                    }
                }

                finalInvoice
            }
            Result.success(completedInvoice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveDraft(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Result<InvoiceEntity> = withContext(Dispatchers.IO) {
        try {
            val savedDraft = runInTransaction {
                invoiceDao.deleteInvoiceItems(invoice.invoiceNumber)
                val invoiceId = invoiceDao.insertInvoice(invoice)
                invoiceDao.deleteInvoiceItems(invoice.invoiceNumber)
                val linkedItems = items.map { it.copy(invoiceId = invoiceId) }
                invoiceDao.insertInvoiceItems(linkedItems)
                if (invoice.id == 0L) invoice.copy(id = invoiceId) else invoice
            }
            Result.success(savedDraft)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun voidInvoice(invoiceNumber: String): Result<Unit> =
        voidInvoice(invoiceNumber, null, null)

    override suspend fun voidInvoice(
        invoiceNumber: String,
        managerUserId: String?,
        reason: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            runInTransaction {
                val inv = invoiceDao.getInvoiceByNumber(invoiceNumber)
                    ?: throw IllegalArgumentException("Invoice $invoiceNumber not found")
                if (inv.status != InvoiceStatus.COMPLETED) {
                    throw IllegalStateException("Only COMPLETED invoices can be voided (status: ${inv.status})")
                }

                // 1. Same-Shift Void Rule:
                val sDao = shiftDao ?: database?.shiftDao()
                    ?: throw IllegalStateException("Active shift required to void invoice")
                val activeShift = sDao.getCurrentOpenShift()
                    ?: throw IllegalStateException("Active shift required to void invoice")
                if (inv.shiftId != activeShift.id) {
                    throw IllegalStateException("Same-Shift Void Rule: Cannot void invoice from a previous or closed shift. Please issue a Sales Return instead.")
                }

                // 2. Existing Returns Block:
                val rDao = returnDao ?: database?.returnDao()
                val returns = rDao?.getReturnsForInvoiceList(inv.id) ?: emptyList()
                if (returns.isNotEmpty()) {
                    throw IllegalStateException("Cannot void invoice with existing sales returns.")
                }

                // 3. Mark as cancelled with void metadata:
                invoiceDao.updateInvoiceVoidStatus(
                    invoiceNumber = invoiceNumber,
                    voidedAt = System.currentTimeMillis(),
                    voidedByUserId = managerUserId,
                    voidReason = reason
                )

                // 4. Revert Cash Drawer:
                val netCash = (inv.cashReceivedPaise - inv.changeReturnedPaise).coerceAtLeast(0L)
                if (netCash > 0L) {
                    cashMovementDao.insertCashMovement(
                        CashMovementEntity(
                            shiftId = activeShift.id,
                            movementType = "VOID_REFUND",
                            amountPaise = -netCash,
                            source = "INVOICE",
                            sourceId = invoiceNumber
                        )
                    )
                }

                // 5. Revert Customer Khata (check originalDuePaise to handle settled due bills):
                val creditToRevert = if (inv.originalDuePaise > 0L) inv.originalDuePaise else inv.duePaise
                if (creditToRevert > 0L) {
                    val targetCustomerId = inv.customerId ?: inv.customerPhone?.let { phone ->
                        customerDao.getCustomerByPhone(phone.trim())?.id
                    }
                    if (targetCustomerId != null) {
                        val currentBal = customerLedgerDao.getLatestEntryForCustomer(targetCustomerId)?.balanceAfterPaise
                            ?: customerDao.getCustomerById(targetCustomerId)?.openingBalancePaise
                            ?: 0L
                        val newBal = currentBal - creditToRevert
                        customerLedgerDao.insertEntry(
                            CustomerLedgerEntity(
                                customerId = targetCustomerId,
                                transactionType = "VOID_CREDIT_REVERSAL",
                                invoiceId = inv.id,
                                debitPaise = 0L,
                                creditPaise = creditToRevert,
                                balanceAfterPaise = newBal,
                                note = "Void credit reversal: $invoiceNumber"
                            )
                        )
                        val customer = customerDao.getCustomerById(targetCustomerId)
                        if (customer != null) {
                            customerDao.updateCustomer(customer.copy(openingBalancePaise = newBal))
                        }
                    }
                }

                // 6. Restock Products & Batches:
                val items = invoiceDao.getInvoiceItems(invoiceNumber)
                val pBatchDao = productBatchDao ?: database?.productBatchDao()
                val iBatchDao = invoiceItemBatchDao ?: database?.invoiceItemBatchDao()

                for (item in items) {
                    if (item.productId != null && !item.isCustomItem) {
                        val qtyMilli = if (item.quantityMilli > 0L) item.quantityMilli else item.quantity.toLong() * 1000L
                        if (iBatchDao != null && pBatchDao != null) {
                            val itemBatches = iBatchDao.getItemBatchListForInvoiceItem(item.id)
                            for (itemBatch in itemBatches) {
                                pBatchDao.incrementBatchStock(itemBatch.batchId, itemBatch.quantityMilli)
                            }
                        }
                        productDao.restoreProductStockMilli(item.productId, qtyMilli)
                    }
                }

                // 7. Audit log:
                val aDao = auditLogDao ?: database?.auditLogDao()
                aDao?.insertAuditLog(
                    AuditLogEntity(
                        userId = managerUserId ?: "Admin",
                        action = "VOID_INVOICE",
                        entityType = "INVOICE",
                        entityId = invoiceNumber,
                        details = "Voided $invoiceNumber due to $reason"
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun settleInvoiceDue(invoiceNumber: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            runInTransaction {
                val inv = invoiceDao.getInvoiceByNumber(invoiceNumber)
                if (inv != null && inv.duePaise > 0L) {
                    invoiceDao.updateInvoiceDue(invoiceNumber, 0L)
                    if (!inv.customerPhone.isNullOrBlank()) {
                        val customer = customerDao.getCustomerByPhone(inv.customerPhone.trim())
                        if (customer != null) {
                            val newBal = (customer.openingBalancePaise - inv.duePaise).coerceAtLeast(0L)
                            customerDao.insertCustomer(customer.copy(openingBalancePaise = newBal))
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteInvoice(invoiceNumber: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            runInTransaction {
                invoiceDao.deleteInvoiceItems(invoiceNumber)
                invoiceDao.deleteInvoice(invoiceNumber)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun upsertInvoicesAndItems(
        invoices: List<InvoiceEntity>,
        items: List<InvoiceItemEntity>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            runInTransaction {
                for (inv in invoices) {
                    invoiceDao.upsertInvoice(inv)
                }
                invoiceDao.upsertInvoiceItems(items)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
