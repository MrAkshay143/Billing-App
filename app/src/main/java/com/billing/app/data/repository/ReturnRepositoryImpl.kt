package com.billing.app.data.repository

import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.core.util.UnitRegistry
import com.billing.app.data.local.dao.AuditLogDao
import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.CustomerDao
import com.billing.app.data.local.dao.CustomerLedgerDao
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.InvoiceItemBatchDao
import com.billing.app.data.local.dao.ProductBatchDao
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.dao.ReturnDao
import com.billing.app.data.local.dao.ReturnItemBatchDao
import com.billing.app.data.local.dao.ShiftDao
import com.billing.app.data.local.entity.AuditLogEntity
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.data.local.entity.ReturnEntity
import com.billing.app.data.local.entity.ReturnItemBatchEntity
import com.billing.app.data.local.entity.ReturnItemEntity
import com.billing.app.domain.repository.ReturnItemInput
import com.billing.app.domain.repository.ReturnRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale

class ReturnRepositoryImpl(
    private val returnDao: ReturnDao,
    private val returnItemBatchDao: ReturnItemBatchDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemBatchDao: InvoiceItemBatchDao,
    private val productDao: ProductDao,
    private val productBatchDao: ProductBatchDao,
    private val customerDao: CustomerDao,
    private val customerLedgerDao: CustomerLedgerDao,
    private val shiftDao: ShiftDao,
    private val cashMovementDao: CashMovementDao,
    private val auditLogDao: AuditLogDao,
    private val database: AppDatabase? = null
) : ReturnRepository {

    constructor(database: AppDatabase) : this(
        returnDao = database.returnDao(),
        returnItemBatchDao = database.returnItemBatchDao(),
        invoiceDao = database.invoiceDao(),
        invoiceItemBatchDao = database.invoiceItemBatchDao(),
        productDao = database.productDao(),
        productBatchDao = database.productBatchDao(),
        customerDao = database.customerDao(),
        customerLedgerDao = database.customerLedgerDao(),
        shiftDao = database.shiftDao(),
        cashMovementDao = database.cashMovementDao(),
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

    override fun getAllReturns(): Flow<List<ReturnEntity>> =
        returnDao.getAllReturns()

    override fun getReturnsForInvoice(invoiceId: Long): Flow<List<ReturnEntity>> =
        returnDao.getReturnsForInvoice(invoiceId)

    override suspend fun getReturnItems(returnId: Long): List<ReturnItemEntity> =
        returnDao.getReturnItemList(returnId)

    override suspend fun processSalesReturn(
        invoiceNumber: String,
        itemsToReturn: List<ReturnItemInput>,
        reason: String,
        userId: String
    ): Result<ReturnEntity> = withContext(Dispatchers.IO) {
        try {
            if (itemsToReturn.isEmpty()) {
                throw IllegalArgumentException("No items provided for return")
            }

            val savedReturn = runInTransaction {
                // 1. Look up invoice and active shift
                val activeShift = shiftDao.getCurrentOpenShift()
                    ?: throw IllegalStateException("Active shift required for processing sales return")

                val invoice = invoiceDao.getInvoiceByNumber(invoiceNumber)
                    ?: throw IllegalArgumentException("Invoice $invoiceNumber not found")

                // 2. Invoice validation: status must be COMPLETED (cannot return from CANCELLED or DRAFT)
                if (invoice.status != InvoiceStatus.COMPLETED) {
                    throw IllegalStateException("Cannot return items from invoice with status ${invoice.status}")
                }

                // 3. Fetch invoice items and check for over-return
                val invoiceItems = invoiceDao.getInvoiceItemsByInvoiceId(invoice.id)
                val invoiceItemMap = invoiceItems.associateBy { it.id }

                for (item in itemsToReturn) {
                    if (item.quantityMilli <= 0L) {
                        throw IllegalArgumentException("Return quantity must be positive")
                    }
                    val invoiceItem = invoiceItemMap[item.invoiceItemId]
                        ?: throw IllegalArgumentException("Invoice item ${item.invoiceItemId} not found in invoice $invoiceNumber")

                    val unitDef = UnitRegistry.resolve(invoiceItem.unit)
                    if (!unitDef.allowsFractional && item.quantityMilli % 1000L != 0L) {
                        throw IllegalArgumentException("Fractional return quantity is not allowed for discrete unit '${unitDef.code}'")
                    }

                    val existingReturns = returnDao.getReturnItemsForInvoiceItem(item.invoiceItemId)
                    val existingReturnedMilli = existingReturns.sumOf { it.quantityMilli }

                    if (existingReturnedMilli + item.quantityMilli > invoiceItem.quantityMilli) {
                        throw IllegalStateException("Return quantity exceeds sold quantity")
                    }
                }

                // 4. Financial Refund Calculation:
                // Line refund: unitPricePaise * quantityMilli / 1000L
                // Pro-rata tax calculation for returned item: taxableRefundPaise and taxRefundPaise
                val lineRefundList = itemsToReturn.map { item ->
                    val invoiceItem = invoiceItemMap.getValue(item.invoiceItemId)
                    val lineRefundPaise = (invoiceItem.unitPricePaise * item.quantityMilli) / 1000L
                    val taxableRefundPaise = if (invoiceItem.quantityMilli > 0L) {
                        (invoiceItem.taxableValuePaise * item.quantityMilli) / invoiceItem.quantityMilli
                    } else {
                        0L
                    }
                    val taxRefundPaise = if (invoiceItem.quantityMilli > 0L) {
                        (invoiceItem.taxPaise * item.quantityMilli) / invoiceItem.quantityMilli
                    } else {
                        0L
                    }
                    Triple(item, lineRefundPaise, Pair(taxableRefundPaise, taxRefundPaise))
                }

                val totalRefundPaise = lineRefundList.sumOf { it.second }

                val targetCustomerId = invoice.customerId ?: invoice.customerPhone?.let { phone ->
                    customerDao.getCustomerByPhone(phone.trim())?.id
                }

                // Mixed-Payment Refund Priority:
                // Step 1: Khata Credit Reversal for invoice due:
                val invoiceDueReduction = if (invoice.duePaise > 0L) {
                    minOf(totalRefundPaise, invoice.duePaise)
                } else {
                    0L
                }
                if (invoiceDueReduction > 0L) {
                    invoiceDao.updateInvoiceDue(invoice.invoiceNumber, invoice.duePaise - invoiceDueReduction)
                }

                // Step 2: Cash & UPI Split Refund vs Khata Post-Allocation:
                val remainingRefund = totalRefundPaise - invoiceDueReduction
                val origCash = (invoice.cashReceivedPaise - invoice.changeReturnedPaise).coerceAtLeast(0L)
                val origUpi = invoice.upiPaidPaise
                val totalOrigPaid = origCash + origUpi

                val cashRefundPaise: Long
                val upiRefundPaise: Long
                var totalKhataCreditPaise = invoiceDueReduction

                if (totalOrigPaid > 0L) {
                    val calculatedCashRefund = (remainingRefund * origCash) / totalOrigPaid
                    val calculatedUpiRefund = remainingRefund - calculatedCashRefund

                    // Cap cash and UPI refunds by the original amounts paid on the invoice
                    val actualCashRefund = minOf(calculatedCashRefund, origCash)
                    val actualUpiRefund = minOf(calculatedUpiRefund, origUpi)
                    val excessRefund = remainingRefund - actualCashRefund - actualUpiRefund

                    if (excessRefund > 0L && targetCustomerId != null) {
                        // Customer has Khata: credit excess to Khata rather than leaking cash
                        totalKhataCreditPaise += excessRefund
                        cashRefundPaise = actualCashRefund
                        upiRefundPaise = actualUpiRefund
                    } else {
                        cashRefundPaise = calculatedCashRefund
                        upiRefundPaise = calculatedUpiRefund
                    }
                } else {
                    // Prevent cash drawer leakage:
                    // If no cash/UPI was originally paid on this invoice, check if the customer has outstanding debt across their Khata.
                    // Prioritize crediting the refund to Khata (RETURN_CREDIT) rather than paying cash out of the register!
                    val currentBal = if (targetCustomerId != null) {
                        customerLedgerDao.getLatestEntryForCustomer(targetCustomerId)?.balanceAfterPaise
                            ?: customerDao.getCustomerById(targetCustomerId)?.openingBalancePaise
                            ?: 0L
                    } else 0L

                    val remainingDebt = (currentBal - invoiceDueReduction).coerceAtLeast(0L)
                    val additionalKhataCredit = if (targetCustomerId != null) {
                        if (remainingDebt > 0L) {
                            minOf(remainingRefund, remainingDebt)
                        } else {
                            remainingRefund
                        }
                    } else {
                        0L
                    }

                    totalKhataCreditPaise += additionalKhataCredit
                    cashRefundPaise = remainingRefund - additionalKhataCredit
                    upiRefundPaise = 0L
                }

                val khataCreditPaise = totalKhataCreditPaise

                if (khataCreditPaise > 0L && targetCustomerId != null) {
                    val currentBal = customerLedgerDao.getLatestEntryForCustomer(targetCustomerId)?.balanceAfterPaise
                        ?: customerDao.getCustomerById(targetCustomerId)?.openingBalancePaise
                        ?: 0L
                    val newBal = currentBal - khataCreditPaise
                    customerLedgerDao.insertEntry(
                        CustomerLedgerEntity(
                            customerId = targetCustomerId,
                            transactionType = "RETURN_CREDIT",
                            invoiceId = invoice.id,
                            debitPaise = 0L,
                            creditPaise = khataCreditPaise,
                            balanceAfterPaise = newBal,
                            note = "Return credit: for ${invoice.invoiceNumber}"
                        )
                    )
                    val customer = customerDao.getCustomerById(targetCustomerId)
                    if (customer != null) {
                        customerDao.updateCustomer(customer.copy(openingBalancePaise = newBal))
                    }
                }

                // Generate unique return number
                val nextNum = (returnDao.getAllReturnList().maxOfOrNull { it.id } ?: 0L) + 1L
                val returnNumber = String.format(Locale.US, "RET-%06d", nextNum)

                // Step 3: Cash Drawer Sync:
                if (cashRefundPaise > 0L) {
                    cashMovementDao.insertCashMovement(
                        CashMovementEntity(
                            shiftId = activeShift.id,
                            movementType = "REFUND_CASH",
                            amountPaise = -cashRefundPaise,
                            source = "RETURN",
                            sourceId = returnNumber
                        )
                    )
                }

                // 6. Insert ReturnEntity
                val returnEntity = ReturnEntity(
                    returnNumber = returnNumber,
                    invoiceId = invoice.id,
                    shiftId = activeShift.id,
                    totalRefundPaise = totalRefundPaise,
                    cashRefundPaise = cashRefundPaise,
                    upiRefundPaise = upiRefundPaise,
                    khataCreditPaise = khataCreditPaise,
                    reason = reason,
                    processedByUserId = userId
                )
                val returnId = returnDao.insertReturn(returnEntity)
                val savedReturnEntity = returnEntity.copy(id = returnId)

                // 5. Stock Restocking & Return Items
                for ((item, lineRefundPaise, taxPair) in lineRefundList) {
                    val (taxableRefund, taxRefund) = taxPair
                    val invoiceItem = invoiceItemMap.getValue(item.invoiceItemId)
                    val returnItem = ReturnItemEntity(
                        returnId = returnId,
                        invoiceItemId = item.invoiceItemId,
                        quantityMilli = item.quantityMilli,
                        unit = invoiceItem.unit,
                        refundAmountPaise = lineRefundPaise,
                        taxableRefundPaise = taxableRefund,
                        taxRefundPaise = taxRefund,
                        restock = true
                    )
                    val returnItemId = returnDao.insertReturnItem(returnItem)

                    // Restock into batches in reverse proportion (Reverse FEFO)
                    val itemBatches = invoiceItemBatchDao.getItemBatchListForInvoiceItem(item.invoiceItemId)
                    if (itemBatches.isNotEmpty()) {
                        val prevReturnItems = returnDao.getReturnItemsForInvoiceItem(item.invoiceItemId)
                            .filter { it.id != returnItemId }
                        val prevReturnBatches = prevReturnItems.flatMap {
                            returnItemBatchDao.getBatchListForReturnItem(it.id)
                        }

                        var qtyToRestock = item.quantityMilli
                        for (itemBatch in itemBatches.reversed()) {
                            if (qtyToRestock <= 0L) break
                            val previouslyReturned = prevReturnBatches
                                .filter { it.batchId == itemBatch.batchId }
                                .sumOf { it.quantityMilli }
                            val availableFromBatch = (itemBatch.quantityMilli - previouslyReturned).coerceAtLeast(0L)
                            if (availableFromBatch > 0L) {
                                val restockBatchQty = minOf(qtyToRestock, availableFromBatch)
                                productBatchDao.incrementBatchStock(itemBatch.batchId, restockBatchQty)
                                returnItemBatchDao.insertReturnItemBatch(
                                    ReturnItemBatchEntity(
                                        returnItemId = returnItemId,
                                        batchId = itemBatch.batchId,
                                        quantityMilli = restockBatchQty
                                    )
                                )
                                qtyToRestock -= restockBatchQty
                            }
                        }
                    }

                    // Restock overall product
                    if (invoiceItem.productId != null && !invoiceItem.isCustomItem) {
                        productDao.restoreProductStockMilli(invoiceItem.productId, item.quantityMilli)
                    }
                }

                // 7. Insert AuditLogEntity
                auditLogDao.insertAuditLog(
                    AuditLogEntity(
                        userId = userId,
                        action = "SALES_RETURN",
                        entityType = "RETURN",
                        entityId = returnNumber,
                        details = "Return of Rs. ${totalRefundPaise / 100} for ${invoice.invoiceNumber}"
                    )
                )

                savedReturnEntity
            }
            Result.success(savedReturn)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
