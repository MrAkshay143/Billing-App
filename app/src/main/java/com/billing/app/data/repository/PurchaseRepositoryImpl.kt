package com.billing.app.data.repository

import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.ProductBatchDao
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.dao.PurchaseDao
import com.billing.app.data.local.dao.SupplierDao
import com.billing.app.data.local.dao.SupplierLedgerDao
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.ProductBatchEntity
import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.PurchaseItemEntity
import com.billing.app.data.local.entity.SupplierLedgerEntity
import com.billing.app.domain.repository.PurchaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

class PurchaseRepositoryImpl(
    private val purchaseDao: PurchaseDao,
    private val productDao: ProductDao,
    private val productBatchDao: ProductBatchDao? = null,
    private val supplierDao: SupplierDao? = null,
    private val supplierLedgerDao: SupplierLedgerDao? = null,
    private val cashMovementDao: CashMovementDao? = null,
    private val database: AppDatabase? = null
) : PurchaseRepository {

    constructor(database: AppDatabase) : this(
        purchaseDao = database.purchaseDao(),
        productDao = database.productDao(),
        productBatchDao = database.productBatchDao(),
        supplierDao = database.supplierDao(),
        supplierLedgerDao = database.supplierLedgerDao(),
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

    override fun getAllPurchases(): Flow<List<PurchaseEntity>> =
        purchaseDao.getAllPurchases()

    override fun getPurchaseCount(): Flow<Int> =
        purchaseDao.getPurchaseCount()

    override suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItemEntity> = withContext(Dispatchers.IO) {
        purchaseDao.getPurchaseItems(purchaseId)
    }

    override suspend fun addPurchase(
        purchase: PurchaseEntity,
        items: List<PurchaseItemEntity>,
        paymentMode: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = runInTransaction {
                val purchaseId = purchaseDao.insertPurchase(purchase)
                val processedItems = mutableListOf<PurchaseItemEntity>()

                for (item in items) {
                    val product = productDao.getProductById(item.productId)
                    if (product?.trackBatch == true) {
                        val batchNumber = item.batchNumber?.ifBlank { null }
                            ?: "BATCH-${System.currentTimeMillis().toString().takeLast(6)}"
                        val mrpPaise = if (item.mrpPaise > 0L) item.mrpPaise else product.mrp.paise
                        val batch = ProductBatchEntity(
                            productId = item.productId,
                            batchNumber = batchNumber,
                            expiryDate = item.expiryDate,
                            purchasePricePaise = item.unitPricePaise,
                            mrpPaise = mrpPaise,
                            initialStockMilli = item.quantityMilli,
                            currentStockMilli = item.quantityMilli
                        )
                        val batchId = productBatchDao?.insertBatch(batch) ?: 0L
                        processedItems.add(
                            item.copy(
                                purchaseId = purchaseId,
                                batchId = if (batchId > 0L) batchId else null,
                                batchNumber = batchNumber,
                                mrpPaise = mrpPaise
                            )
                        )
                    } else {
                        // Standard product (trackBatch == false): LPP Update
                        if (product != null) {
                            val newMrpPaise = if (item.mrpPaise > 0L) item.mrpPaise else product.mrp.paise
                            productDao.updateProduct(
                                product.copy(
                                    purchasePricePaise = item.unitPricePaise,
                                    mrpPaise = newMrpPaise
                                )
                            )
                        }
                        processedItems.add(item.copy(purchaseId = purchaseId))
                    }
                    productDao.restoreProductStockMilli(item.productId, item.quantityMilli)
                }

                purchaseDao.insertPurchaseItems(processedItems)

                // 4. Supplier Ledger on Credit/Due
                if (purchase.duePaise > 0L && purchase.supplierId != null) {
                    val curBal = supplierLedgerDao?.getLatestEntryForSupplier(purchase.supplierId)?.balanceAfterPaise
                        ?: supplierDao?.getSupplierById(purchase.supplierId)?.openingBalancePaise
                        ?: 0L
                    val newBal = curBal + purchase.duePaise
                    val ledgerEntry = SupplierLedgerEntity(
                        supplierId = purchase.supplierId,
                        transactionType = "PURCHASE_DUE",
                        purchaseId = purchaseId,
                        debitPaise = 0L,
                        creditPaise = purchase.duePaise,
                        balanceAfterPaise = newBal,
                        note = "Purchase: ${purchase.purchaseNumber}"
                    )
                    supplierLedgerDao?.insertEntry(ledgerEntry)
                    supplierDao?.getSupplierById(purchase.supplierId)?.let {
                        supplierDao.updateSupplier(it.copy(duePaise = newBal, openingBalancePaise = newBal))
                    }
                }

                // 5. Cash Drawer Sync
                if (purchase.paidPaise > 0L && paymentMode.equals("CASH", ignoreCase = true) && purchase.shiftId != null) {
                    cashMovementDao?.insertCashMovement(
                        CashMovementEntity(
                            shiftId = purchase.shiftId,
                            movementType = "PURCHASE_PAYMENT",
                            amountPaise = -purchase.paidPaise,
                            source = "PURCHASE",
                            sourceId = purchase.purchaseNumber,
                            notes = "Payment for purchase ${purchase.purchaseNumber}"
                        )
                    )
                }

                purchaseId
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordSupplierPayment(
        supplierId: String,
        amountPaise: Long,
        paymentMode: String,
        shiftId: Long?,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            runInTransaction {
                val curBal = supplierLedgerDao?.getLatestEntryForSupplier(supplierId)?.balanceAfterPaise
                    ?: supplierDao?.getSupplierById(supplierId)?.openingBalancePaise
                    ?: 0L
                val newBal = (curBal - amountPaise).coerceAtLeast(0L)
                val ledgerEntry = SupplierLedgerEntity(
                    supplierId = supplierId,
                    transactionType = "PAYMENT",
                    debitPaise = amountPaise,
                    creditPaise = 0L,
                    balanceAfterPaise = newBal,
                    paymentMode = paymentMode,
                    note = note ?: "Payment to supplier"
                )
                supplierLedgerDao?.insertEntry(ledgerEntry)
                supplierDao?.getSupplierById(supplierId)?.let {
                    supplierDao.updateSupplier(it.copy(duePaise = newBal, openingBalancePaise = newBal))
                }
                if (paymentMode.equals("CASH", ignoreCase = true) && shiftId != null) {
                    cashMovementDao?.insertCashMovement(
                        CashMovementEntity(
                            shiftId = shiftId,
                            movementType = "PURCHASE_PAYMENT",
                            amountPaise = -amountPaise,
                            source = "SUPPLIER_PAYMENT",
                            sourceId = supplierId,
                            notes = "Supplier payment"
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSupplierLedger(supplierId: String): Flow<List<SupplierLedgerEntity>> {
        return supplierLedgerDao?.getEntriesForSupplier(supplierId) ?: emptyFlow()
    }

    override fun getSupplierBalance(supplierId: String): Flow<Long> {
        val ledgerDao = supplierLedgerDao ?: return flowOf(0L)
        return combine(
            ledgerDao.getTotalCreditForSupplier(supplierId),
            ledgerDao.getTotalDebitForSupplier(supplierId)
        ) { credit, debit ->
            (credit - debit).coerceAtLeast(0L)
        }
    }
}
