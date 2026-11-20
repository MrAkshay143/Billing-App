package com.billing.app.data.repository

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
import com.billing.app.data.local.dao.ReturnItemBatchDao
import com.billing.app.data.local.dao.ShiftDao
import com.billing.app.data.local.dao.TopProductSummary
import com.billing.app.data.local.entity.AuditLogEntity
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import com.billing.app.data.local.entity.InvoiceCounterEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemBatchEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.data.local.entity.ProductBatchEntity
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.data.local.entity.ReturnEntity
import com.billing.app.data.local.entity.ReturnItemBatchEntity
import com.billing.app.data.local.entity.ReturnItemEntity
import com.billing.app.data.local.entity.ShiftEntity
import com.billing.app.domain.repository.ReturnItemInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SalesReturnAndVoidTest {

    private lateinit var fakeReturnDao: FakeReturnDao
    private lateinit var fakeReturnItemBatchDao: FakeReturnItemBatchDao
    private lateinit var fakeInvoiceDao: FakeInvoiceDao
    private lateinit var fakeInvoiceItemBatchDao: FakeInvoiceItemBatchDao
    private lateinit var fakeProductDao: FakeProductDao
    private lateinit var fakeProductBatchDao: FakeProductBatchDao
    private lateinit var fakeCustomerDao: FakeCustomerDao
    private lateinit var fakeCustomerLedgerDao: FakeCustomerLedgerDao
    private lateinit var fakeShiftDao: FakeShiftDao
    private lateinit var fakeCashMovementDao: FakeCashMovementDao
    private lateinit var fakeAuditLogDao: FakeAuditLogDao

    private lateinit var returnRepository: ReturnRepositoryImpl
    private lateinit var invoiceRepository: InvoiceRepositoryImpl

    @Before
    fun setup() {
        fakeReturnDao = FakeReturnDao()
        fakeReturnItemBatchDao = FakeReturnItemBatchDao()
        fakeInvoiceDao = FakeInvoiceDao()
        fakeInvoiceItemBatchDao = FakeInvoiceItemBatchDao()
        fakeProductDao = FakeProductDao()
        fakeProductBatchDao = FakeProductBatchDao()
        fakeCustomerDao = FakeCustomerDao()
        fakeCustomerLedgerDao = FakeCustomerLedgerDao()
        fakeShiftDao = FakeShiftDao()
        fakeCashMovementDao = FakeCashMovementDao()
        fakeAuditLogDao = FakeAuditLogDao()

        returnRepository = ReturnRepositoryImpl(
            returnDao = fakeReturnDao,
            returnItemBatchDao = fakeReturnItemBatchDao,
            invoiceDao = fakeInvoiceDao,
            invoiceItemBatchDao = fakeInvoiceItemBatchDao,
            productDao = fakeProductDao,
            productBatchDao = fakeProductBatchDao,
            customerDao = fakeCustomerDao,
            customerLedgerDao = fakeCustomerLedgerDao,
            shiftDao = fakeShiftDao,
            cashMovementDao = fakeCashMovementDao,
            auditLogDao = fakeAuditLogDao,
            database = null
        )

        invoiceRepository = InvoiceRepositoryImpl(
            invoiceDao = fakeInvoiceDao,
            productDao = fakeProductDao,
            customerDao = fakeCustomerDao,
            customerLedgerDao = fakeCustomerLedgerDao,
            cashMovementDao = fakeCashMovementDao,
            productBatchDao = fakeProductBatchDao,
            invoiceItemBatchDao = fakeInvoiceItemBatchDao,
            shiftDao = fakeShiftDao,
            returnDao = fakeReturnDao,
            auditLogDao = fakeAuditLogDao,
            database = null
        )
    }

    @Test
    fun testSalesReturn_mixedPaymentRefundPriority_khataFirstThenCashUpi() = runBlocking {
        // Active shift
        val activeShift = ShiftEntity(id = 1L, userId = "cashier_1", openedAt = System.currentTimeMillis(), openingFloatPaise = 10000L, status = "OPEN")
        fakeShiftDao.currentShift = activeShift

        // Customer with balance 50000L
        val customer = CustomerEntity(id = "cust_1", name = "Ramesh", phone = "9876543210", openingBalancePaise = 50000L)
        fakeCustomerDao.insertCustomer(customer)

        // Invoice: Total Rs. 500 (50000L)
        // Mixed Payment:
        // Cash: 20000L, UPI: 10000L, Due: 20000L
        val invoice = InvoiceEntity(
            id = 100L,
            invoiceNumber = "INV-000100",
            shiftId = 1L,
            customerId = "cust_1",
            customerPhone = "9876543210",
            subtotalPaise = 50000L,
            grandTotalPaise = 50000L,
            cashReceivedPaise = 20000L,
            changeReturnedPaise = 0L,
            upiPaidPaise = 10000L,
            duePaise = 20000L,
            status = InvoiceStatus.COMPLETED
        )
        fakeInvoiceDao.insertInvoice(invoice)

        // 5 items of Rs. 100 each
        val invoiceItem = InvoiceItemEntity(
            id = 1001L,
            invoiceId = 100L,
            productId = "prod_1",
            productName = "Product 1",
            unitPricePaise = 10000L,
            quantityMilli = 5000L,
            lineTotalPaise = 50000L
        )
        fakeInvoiceDao.insertInvoiceItem(invoiceItem)

        fakeProductDao.insertProduct(
            ProductEntity(id = "prod_1", name = "Product 1", stockMilli = 10000L)
        )

        // Return 3 items = Rs. 300 (30000L)
        val returnInputs = listOf(
            ReturnItemInput(
                invoiceItemId = 1001L,
                quantityMilli = 3000L,
                reason = "Damaged"
            )
        )

        val result = returnRepository.processSalesReturn(
            invoiceNumber = "INV-000100",
            itemsToReturn = returnInputs,
            reason = "Customer return",
            userId = "cashier_1"
        )

        assertTrue(result.isSuccess)
        val returnEntity = result.getOrThrow()

        // Total refund: 30000L
        assertEquals(30000L, returnEntity.totalRefundPaise)
        // Step 1: Khata Credit Reversal absorbs due = 20000L
        assertEquals(20000L, returnEntity.khataCreditPaise)
        // Step 2: Remaining refund = 10000L split between cash (20000L) and upi (10000L)
        // cashRefundPaise = (10000 * 20000) / 30000 = 6666L
        // upiRefundPaise = 10000 - 6666 = 3334L
        assertEquals(6666L, returnEntity.cashRefundPaise)
        assertEquals(3334L, returnEntity.upiRefundPaise)

        // Verify Invoice due updated to 0L
        val updatedInvoice = fakeInvoiceDao.getInvoiceByNumber("INV-000100")
        assertEquals(0L, updatedInvoice?.duePaise)

        // Verify Customer Khata Ledger Entry created
        val ledgerEntries = fakeCustomerLedgerDao.entries.filter { it.customerId == "cust_1" }
        assertEquals(1, ledgerEntries.size)
        val ledger = ledgerEntries.first()
        assertEquals("RETURN_CREDIT", ledger.transactionType)
        assertEquals(20000L, ledger.creditPaise)
        assertEquals(30000L, ledger.balanceAfterPaise) // 50000 - 20000 = 30000

        // Verify Customer cached balance updated
        val updatedCustomer = fakeCustomerDao.getCustomerById("cust_1")
        assertEquals(30000L, updatedCustomer?.openingBalancePaise)

        // Verify Cash Drawer synced with REFUND_CASH = -6666L
        val cashMovements = fakeCashMovementDao.movements
        assertEquals(1, cashMovements.size)
        val movement = cashMovements.first()
        assertEquals("REFUND_CASH", movement.movementType)
        assertEquals(-6666L, movement.amountPaise)
        assertEquals(1L, movement.shiftId)

        // Verify Product restocked: 10000L + 3000L = 13000L
        val updatedProduct = fakeProductDao.getProductById("prod_1")
        assertEquals(13000L, updatedProduct?.stockMilli)

        // Verify Audit Log
        val auditLogs = fakeAuditLogDao.logs.filter { it.action == "SALES_RETURN" }
        assertEquals(1, auditLogs.size)
    }

    @Test
    fun testSalesReturn_overReturnGuard_blocksExcessQuantity() = runBlocking {
        val activeShift = ShiftEntity(id = 1L, userId = "cashier_1", openedAt = System.currentTimeMillis(), openingFloatPaise = 10000L, status = "OPEN")
        fakeShiftDao.currentShift = activeShift

        val invoice = InvoiceEntity(
            id = 200L,
            invoiceNumber = "INV-000200",
            shiftId = 1L,
            subtotalPaise = 20000L,
            grandTotalPaise = 20000L,
            cashReceivedPaise = 20000L,
            status = InvoiceStatus.COMPLETED
        )
        fakeInvoiceDao.insertInvoice(invoice)

        // 2 items sold (2000L milli)
        val item = InvoiceItemEntity(
            id = 2001L,
            invoiceId = 200L,
            productId = "prod_2",
            productName = "Product 2",
            unitPricePaise = 10000L,
            quantityMilli = 2000L,
            lineTotalPaise = 20000L,
            unit = "KG"
        )
        fakeInvoiceDao.insertInvoiceItem(item)

        fakeProductDao.insertProduct(ProductEntity(id = "prod_2", name = "Product 2", stockMilli = 5000L, unit = "KG"))

        // Return 1: Return 1000 milli (1 item) -> Success
        val return1Result = returnRepository.processSalesReturn(
            invoiceNumber = "INV-000200",
            itemsToReturn = listOf(ReturnItemInput(invoiceItemId = 2001L, quantityMilli = 1000L)),
            reason = "Return 1",
            userId = "cashier_1"
        )
        assertTrue(return1Result.isSuccess)

        // Return 2: Try to return 1500 milli -> 1000 + 1500 = 2500 > 2000 (Sold) -> MUST FAIL
        val return2Result = returnRepository.processSalesReturn(
            invoiceNumber = "INV-000200",
            itemsToReturn = listOf(ReturnItemInput(invoiceItemId = 2001L, quantityMilli = 1500L)),
            reason = "Return 2 excess",
            userId = "cashier_1"
        )
        assertTrue(return2Result.isFailure)
        val ex = return2Result.exceptionOrNull()
        assertTrue(ex is IllegalStateException)
        assertEquals("Return quantity exceeds sold quantity", ex?.message)
    }

    @Test
    fun testSalesReturn_reverseFefoBatchRestocking() = runBlocking {
        val activeShift = ShiftEntity(id = 1L, userId = "cashier_1", openedAt = System.currentTimeMillis(), openingFloatPaise = 10000L, status = "OPEN")
        fakeShiftDao.currentShift = activeShift

        val invoice = InvoiceEntity(
            id = 300L,
            invoiceNumber = "INV-000300",
            shiftId = 1L,
            subtotalPaise = 50000L,
            grandTotalPaise = 50000L,
            cashReceivedPaise = 50000L,
            status = InvoiceStatus.COMPLETED
        )
        fakeInvoiceDao.insertInvoice(invoice)

        val item = InvoiceItemEntity(
            id = 3001L,
            invoiceId = 300L,
            productId = "prod_batch",
            productName = "Batch Product",
            unitPricePaise = 10000L,
            quantityMilli = 5000L,
            lineTotalPaise = 50000L
        )
        fakeInvoiceDao.insertInvoiceItem(item)

        // Batches:
        // Batch 1 (expires early): 2000 milli sold first
        // Batch 2 (expires later): 3000 milli sold second
        fakeProductBatchDao.insertBatch(ProductBatchEntity(id = 11L, productId = "prod_batch", batchNumber = "B1", expiryDate = null, purchasePricePaise = 5000L, mrpPaise = 10000L, initialStockMilli = 5000L, currentStockMilli = 0L))
        fakeProductBatchDao.insertBatch(ProductBatchEntity(id = 12L, productId = "prod_batch", batchNumber = "B2", expiryDate = null, purchasePricePaise = 5000L, mrpPaise = 10000L, initialStockMilli = 5000L, currentStockMilli = 0L))

        fakeInvoiceItemBatchDao.insertAll(
            listOf(
                InvoiceItemBatchEntity(id = 1L, invoiceItemId = 3001L, batchId = 11L, quantityMilli = 2000L, costPaise = 5000L),
                InvoiceItemBatchEntity(id = 2L, invoiceItemId = 3001L, batchId = 12L, quantityMilli = 3000L, costPaise = 5000L)
            )
        )

        fakeProductDao.insertProduct(ProductEntity(id = "prod_batch", name = "Batch Product", stockMilli = 0L, trackBatch = true))

        // Return 4000 milli (4 units)
        // In Reverse FEFO:
        // Batch 12 (sold second) should receive 3000 milli (max sold in B2).
        // Batch 11 (sold first) should receive the remaining 1000 milli.
        val result = returnRepository.processSalesReturn(
            invoiceNumber = "INV-000300",
            itemsToReturn = listOf(ReturnItemInput(invoiceItemId = 3001L, quantityMilli = 4000L)),
            reason = "Return batches",
            userId = "cashier_1"
        )
        assertTrue(result.isSuccess)

        val batch11 = fakeProductBatchDao.getBatchById(11L)
        val batch12 = fakeProductBatchDao.getBatchById(12L)
        assertEquals(1000L, batch11?.currentStockMilli)
        assertEquals(3000L, batch12?.currentStockMilli)

        // Verify return_item_batches
        val returnBatches = fakeReturnItemBatchDao.batches
        assertEquals(2, returnBatches.size)
        val b12Return = returnBatches.find { it.batchId == 12L }
        val b11Return = returnBatches.find { it.batchId == 11L }
        assertEquals(3000L, b12Return?.quantityMilli)
        assertEquals(1000L, b11Return?.quantityMilli)

        // Verify product overall stock
        val product = fakeProductDao.getProductById("prod_batch")
        assertEquals(4000L, product?.stockMilli)
    }

    @Test
    fun testSameShiftVoidRestriction_blocksVoidFromDifferentOrClosedShift() = runBlocking {
        // Shift 1 was active when invoice was made
        // Current open shift is Shift 2
        fakeShiftDao.currentShift = ShiftEntity(id = 2L, userId = "mgr_1", openedAt = System.currentTimeMillis(), openingFloatPaise = 10000L, status = "OPEN")

        val invoice = InvoiceEntity(
            id = 400L,
            invoiceNumber = "INV-000400",
            shiftId = 1L, // Belongs to closed shift 1!
            subtotalPaise = 10000L,
            grandTotalPaise = 10000L,
            cashReceivedPaise = 10000L,
            status = InvoiceStatus.COMPLETED
        )
        fakeInvoiceDao.insertInvoice(invoice)

        val result = invoiceRepository.voidInvoice(
            invoiceNumber = "INV-000400",
            managerUserId = "mgr_1",
            reason = "Customer cancelled"
        )

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is IllegalStateException)
        assertTrue(ex?.message?.contains("Same-Shift Void Rule") == true)

        // Verify invoice was NOT cancelled
        val inv = fakeInvoiceDao.getInvoiceByNumber("INV-000400")
        assertEquals(InvoiceStatus.COMPLETED, inv?.status)
    }

    @Test
    fun testExistingReturnsBlock_blocksVoid() = runBlocking {
        val activeShift = ShiftEntity(id = 1L, userId = "cashier_1", openedAt = System.currentTimeMillis(), openingFloatPaise = 10000L, status = "OPEN")
        fakeShiftDao.currentShift = activeShift

        val invoice = InvoiceEntity(
            id = 500L,
            invoiceNumber = "INV-000500",
            shiftId = 1L,
            subtotalPaise = 10000L,
            grandTotalPaise = 10000L,
            cashReceivedPaise = 10000L,
            status = InvoiceStatus.COMPLETED
        )
        fakeInvoiceDao.insertInvoice(invoice)

        // Existing return for this invoice
        fakeReturnDao.insertReturn(
            ReturnEntity(
                id = 50L,
                returnNumber = "RET-000050",
                invoiceId = 500L,
                shiftId = 1L,
                totalRefundPaise = 5000L,
                reason = "Partial return",
                processedByUserId = "cashier_1"
            )
        )

        val result = invoiceRepository.voidInvoice(
            invoiceNumber = "INV-000500",
            managerUserId = "mgr_1",
            reason = "Void attempt"
        )

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is IllegalStateException)
        assertEquals("Cannot void invoice with existing sales returns.", ex?.message)
    }

    @Test
    fun testVoidInvoice_cashDrawerReversal_and_customerCreditReversal() = runBlocking {
        val activeShift = ShiftEntity(id = 1L, userId = "cashier_1", openedAt = System.currentTimeMillis(), openingFloatPaise = 10000L, status = "OPEN")
        fakeShiftDao.currentShift = activeShift

        val customer = CustomerEntity(id = "cust_void", name = "Kamal", phone = "9123456789", openingBalancePaise = 25000L)
        fakeCustomerDao.insertCustomer(customer)

        // Invoice:
        // Cash: 30000L, Due: 20000L -> Total 50000L
        val invoice = InvoiceEntity(
            id = 600L,
            invoiceNumber = "INV-000600",
            shiftId = 1L,
            customerId = "cust_void",
            customerPhone = "9123456789",
            subtotalPaise = 50000L,
            grandTotalPaise = 50000L,
            cashReceivedPaise = 30000L,
            duePaise = 20000L,
            status = InvoiceStatus.COMPLETED
        )
        fakeInvoiceDao.insertInvoice(invoice)

        val item = InvoiceItemEntity(
            id = 6001L,
            invoiceId = 600L,
            productId = "prod_void",
            productName = "Product Void",
            unitPricePaise = 50000L,
            quantityMilli = 1000L,
            lineTotalPaise = 50000L
        )
        fakeInvoiceDao.insertInvoiceItem(item)

        fakeProductDao.insertProduct(ProductEntity(id = "prod_void", name = "Product Void", stockMilli = 2000L))

        // Void invoice
        val result = invoiceRepository.voidInvoice(
            invoiceNumber = "INV-000600",
            managerUserId = "mgr_1",
            reason = "Wrong item billed"
        )

        assertTrue(result.isSuccess)

        // Verify status and metadata
        val voidedInv = fakeInvoiceDao.getInvoiceByNumber("INV-000600")
        assertNotNull(voidedInv)
        assertEquals(InvoiceStatus.CANCELLED, voidedInv?.status)
        assertEquals("mgr_1", voidedInv?.voidedByUserId)
        assertEquals("Wrong item billed", voidedInv?.voidReason)
        assertNotNull(voidedInv?.voidedAt)

        // Verify Cash Drawer reversal movement: -30000L
        val cashMovements = fakeCashMovementDao.movements
        assertEquals(1, cashMovements.size)
        val movement = cashMovements.first()
        assertEquals("VOID_REFUND", movement.movementType)
        assertEquals(-30000L, movement.amountPaise)
        assertEquals("INV-000600", movement.sourceId)

        // Verify Customer Khata reversal: creditPaise = 20000L
        val ledgerEntries = fakeCustomerLedgerDao.entries.filter { it.customerId == "cust_void" }
        assertEquals(1, ledgerEntries.size)
        val ledger = ledgerEntries.first()
        assertEquals("VOID_CREDIT_REVERSAL", ledger.transactionType)
        assertEquals(20000L, ledger.creditPaise)
        assertEquals(5000L, ledger.balanceAfterPaise) // 25000 - 20000 = 5000

        val updatedCustomer = fakeCustomerDao.getCustomerById("cust_void")
        assertEquals(5000L, updatedCustomer?.openingBalancePaise)

        // Verify Product restocked: 2000L + 1000L = 3000L
        val product = fakeProductDao.getProductById("prod_void")
        assertEquals(3000L, product?.stockMilli)

        // Verify Audit Log
        val auditLogs = fakeAuditLogDao.logs.filter { it.action == "VOID_INVOICE" }
        assertEquals(1, auditLogs.size)
        val log = auditLogs.first()
        assertEquals("mgr_1", log.userId)
        assertEquals("INV-000600", log.entityId)
    }

    // --- Fake DAOs ---

    class FakeReturnDao : ReturnDao {
        val returns = mutableListOf<ReturnEntity>()
        val returnItems = mutableListOf<ReturnItemEntity>()
        private var nextReturnId = 1L
        private var nextReturnItemId = 1L

        override suspend fun insertReturn(returnEntity: ReturnEntity): Long {
            val id = if (returnEntity.id == 0L) nextReturnId++ else returnEntity.id
            returns.add(returnEntity.copy(id = id))
            return id
        }

        override suspend fun insertReturnItem(item: ReturnItemEntity): Long {
            val id = if (item.id == 0L) nextReturnItemId++ else item.id
            returnItems.add(item.copy(id = id))
            return id
        }

        override suspend fun insertReturnItems(items: List<ReturnItemEntity>) {
            items.forEach { insertReturnItem(it) }
        }

        override suspend fun upsertReturn(returnEntity: ReturnEntity) {
            val idx = returns.indexOfFirst { it.id == returnEntity.id }
            if (idx >= 0) returns[idx] = returnEntity else returns.add(returnEntity)
        }

        override suspend fun upsertAllReturns(returns: List<ReturnEntity>) {
            returns.forEach { upsertReturn(it) }
        }

        override suspend fun upsertReturnItems(items: List<ReturnItemEntity>) {
            items.forEach { item ->
                val idx = returnItems.indexOfFirst { it.id == item.id }
                if (idx >= 0) returnItems[idx] = item else returnItems.add(item)
            }
        }

        override suspend fun getReturnById(id: Long): ReturnEntity? =
            returns.find { it.id == id }

        override suspend fun getReturnByNumber(returnNumber: String): ReturnEntity? =
            returns.find { it.returnNumber == returnNumber }

        override fun getReturnsForInvoice(invoiceId: Long): Flow<List<ReturnEntity>> =
            MutableStateFlow(returns.filter { it.invoiceId == invoiceId }).asStateFlow()

        override suspend fun getReturnsForInvoiceList(invoiceId: Long): List<ReturnEntity> =
            returns.filter { it.invoiceId == invoiceId }

        override fun getReturnsForShift(shiftId: Long): Flow<List<ReturnEntity>> =
            MutableStateFlow(returns.filter { it.shiftId == shiftId }).asStateFlow()

        override suspend fun getReturnListForShift(shiftId: Long): List<ReturnEntity> =
            returns.filter { it.shiftId == shiftId }

        override fun getAllReturns(): Flow<List<ReturnEntity>> =
            MutableStateFlow(returns.toList()).asStateFlow()

        override suspend fun getAllReturnList(): List<ReturnEntity> =
            returns.toList()

        override fun getReturnItems(returnId: Long): Flow<List<ReturnItemEntity>> =
            MutableStateFlow(returnItems.filter { it.returnId == returnId }).asStateFlow()

        override suspend fun getReturnItemList(returnId: Long): List<ReturnItemEntity> =
            returnItems.filter { it.returnId == returnId }

        override suspend fun getReturnItemsForInvoiceItem(invoiceItemId: Long): List<ReturnItemEntity> =
            returnItems.filter { it.invoiceItemId == invoiceItemId }

        override suspend fun getAllReturnItems(): List<ReturnItemEntity> =
            returnItems.toList()

        override fun getTotalRefundsForShift(shiftId: Long): Flow<Long> =
            MutableStateFlow(returns.filter { it.shiftId == shiftId }.sumOf { it.totalRefundPaise }).asStateFlow()

        override fun getTotalCashRefundsForShift(shiftId: Long): Flow<Long> =
            MutableStateFlow(returns.filter { it.shiftId == shiftId }.sumOf { it.cashRefundPaise }).asStateFlow()

        override suspend fun deleteReturn(id: Long) {
            returns.removeAll { it.id == id }
        }

        override suspend fun deleteReturnItems(returnId: Long) {
            returnItems.removeAll { it.returnId == returnId }
        }
    }

    class FakeReturnItemBatchDao : ReturnItemBatchDao {
        val batches = mutableListOf<ReturnItemBatchEntity>()
        private var nextId = 1L

        override suspend fun insertReturnItemBatch(batch: ReturnItemBatchEntity): Long {
            val id = if (batch.id == 0L) nextId++ else batch.id
            batches.add(batch.copy(id = id))
            return id
        }

        override suspend fun insertAll(batches: List<ReturnItemBatchEntity>) {
            batches.forEach { insertReturnItemBatch(it) }
        }

        override suspend fun upsertReturnItemBatch(batch: ReturnItemBatchEntity) {
            val idx = batches.indexOfFirst { it.id == batch.id }
            if (idx >= 0) batches[idx] = batch else batches.add(batch)
        }

        override suspend fun upsertAll(batches: List<ReturnItemBatchEntity>) {
            batches.forEach { upsertReturnItemBatch(it) }
        }

        override fun getBatchesForReturnItem(returnItemId: Long): Flow<List<ReturnItemBatchEntity>> =
            MutableStateFlow(batches.filter { it.returnItemId == returnItemId }).asStateFlow()

        override suspend fun getBatchListForReturnItem(returnItemId: Long): List<ReturnItemBatchEntity> =
            batches.filter { it.returnItemId == returnItemId }

        override fun getAllReturnItemBatches(): Flow<List<ReturnItemBatchEntity>> =
            MutableStateFlow(batches.toList()).asStateFlow()

        override suspend fun getAllReturnItemBatchList(): List<ReturnItemBatchEntity> =
            batches.toList()

        override suspend fun deleteForReturnItem(returnItemId: Long) {
            batches.removeAll { it.returnItemId == returnItemId }
        }

        override suspend fun deleteById(id: Long) {
            batches.removeAll { it.id == id }
        }
    }

    class FakeInvoiceDao : InvoiceDao {
        val invoices = mutableListOf<InvoiceEntity>()
        val invoiceItems = mutableListOf<InvoiceItemEntity>()
        private var nextInvoiceId = 1L
        private var nextItemId = 1L

        override suspend fun insertInvoice(invoice: InvoiceEntity): Long {
            val id = if (invoice.id == 0L) nextInvoiceId++ else invoice.id
            invoices.add(invoice.copy(id = id))
            return id
        }

        override suspend fun insertInvoiceItem(item: InvoiceItemEntity): Long {
            val id = if (item.id == 0L) nextItemId++ else item.id
            invoiceItems.add(item.copy(id = id))
            return id
        }

        override suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>) {
            items.forEach { insertInvoiceItem(it) }
        }

        override suspend fun upsertInvoice(invoice: InvoiceEntity) {
            val idx = invoices.indexOfFirst { it.invoiceNumber == invoice.invoiceNumber }
            if (idx >= 0) invoices[idx] = invoice else invoices.add(invoice)
        }

        override suspend fun upsertInvoiceItems(items: List<InvoiceItemEntity>) {
            invoiceItems.addAll(items)
        }

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
            MutableStateFlow(invoices.filter { it.duePaise > 0 && it.status == InvoiceStatus.COMPLETED }).asStateFlow()

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

        override suspend fun getInvoiceItems(invoiceNumber: String): List<InvoiceItemEntity> {
            val inv = getInvoiceByNumber(invoiceNumber) ?: return emptyList()
            return invoiceItems.filter { it.invoiceId == inv.id }
        }

        override suspend fun getInvoiceItemsByInvoiceId(invoiceId: Long): List<InvoiceItemEntity> =
            invoiceItems.filter { it.invoiceId == invoiceId }

        override suspend fun getAllInvoiceItems(): List<InvoiceItemEntity> =
            invoiceItems.toList()

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

        override suspend fun deleteInvoiceItems(invoiceNumber: String) {
            val inv = getInvoiceByNumber(invoiceNumber)
            if (inv != null) invoiceItems.removeAll { it.invoiceId == inv.id }
        }

        override suspend fun deleteInvoiceItemsByInvoiceId(invoiceId: Long) {
            invoiceItems.removeAll { it.invoiceId == invoiceId }
        }

        override suspend fun updateInvoiceDue(invoiceNumber: String, duePaise: Long) {
            val idx = invoices.indexOfFirst { it.invoiceNumber == invoiceNumber }
            if (idx >= 0) invoices[idx] = invoices[idx].copy(duePaise = duePaise)
        }

        override suspend fun getLastInvoiceNumber(): Long? =
            invoices.mapNotNull { it.invoiceNumber.removePrefix("INV-").toLongOrNull() }.maxOrNull()

        override suspend fun setInvoiceCounter(counter: InvoiceCounterEntity) {}

        override suspend fun getInvoicesByShiftId(shiftId: Long): List<InvoiceEntity> =
            invoices.filter { it.shiftId == shiftId && it.status == InvoiceStatus.COMPLETED }

        override suspend fun getInvoiceItemsByShiftId(shiftId: Long): List<InvoiceItemEntity> =
            invoiceItems.filter { item -> invoices.any { it.id == item.invoiceId && it.shiftId == shiftId && it.status == InvoiceStatus.COMPLETED } }
    }

    class FakeInvoiceItemBatchDao : InvoiceItemBatchDao {
        val itemBatches = mutableListOf<InvoiceItemBatchEntity>()
        private var nextId = 1L

        override suspend fun insertInvoiceItemBatch(itemBatch: InvoiceItemBatchEntity): Long {
            val id = if (itemBatch.id == 0L) nextId++ else itemBatch.id
            itemBatches.add(itemBatch.copy(id = id))
            return id
        }

        override suspend fun insertItemBatch(itemBatch: InvoiceItemBatchEntity): Long =
            insertInvoiceItemBatch(itemBatch)

        override suspend fun insertAll(itemBatches: List<InvoiceItemBatchEntity>) {
            itemBatches.forEach { insertInvoiceItemBatch(it) }
        }

        override suspend fun upsertInvoiceItemBatch(itemBatch: InvoiceItemBatchEntity) {
            val idx = itemBatches.indexOfFirst { it.id == itemBatch.id }
            if (idx >= 0) itemBatches[idx] = itemBatch else itemBatches.add(itemBatch)
        }

        override suspend fun upsertAll(itemBatches: List<InvoiceItemBatchEntity>) {
            itemBatches.forEach { upsertInvoiceItemBatch(it) }
        }

        override fun getItemBatchesForInvoiceItem(invoiceItemId: Long): Flow<List<InvoiceItemBatchEntity>> =
            MutableStateFlow(itemBatches.filter { it.invoiceItemId == invoiceItemId }).asStateFlow()

        override suspend fun getItemBatchListForInvoiceItem(invoiceItemId: Long): List<InvoiceItemBatchEntity> =
            itemBatches.filter { it.invoiceItemId == invoiceItemId }

        override fun getItemBatchesForBatch(batchId: Long): Flow<List<InvoiceItemBatchEntity>> =
            MutableStateFlow(itemBatches.filter { it.batchId == batchId }).asStateFlow()

        override fun getAllItemBatches(): Flow<List<InvoiceItemBatchEntity>> =
            MutableStateFlow(itemBatches.toList()).asStateFlow()

        override suspend fun getAllItemBatchList(): List<InvoiceItemBatchEntity> =
            itemBatches.toList()

        override suspend fun deleteForItem(invoiceItemId: Long) {
            itemBatches.removeAll { it.invoiceItemId == invoiceItemId }
        }

        override suspend fun deleteById(id: Long) {
            itemBatches.removeAll { it.id == id }
        }
    }

    class FakeProductDao : ProductDao {
        val products = mutableListOf<ProductEntity>()

        override fun getAllProducts(): Flow<List<ProductEntity>> =
            MutableStateFlow(products.toList()).asStateFlow()

        override suspend fun getProductByBarcode(barcode: String): ProductEntity? =
            products.find { it.barcode == barcode }

        override suspend fun getProductById(id: String): ProductEntity? =
            products.find { it.id == id }

        override suspend fun getProductBySku(sku: String): ProductEntity? =
            products.find { it.sku == sku }

        override fun getProductsByCategory(category: String): Flow<List<ProductEntity>> =
            MutableStateFlow(products.filter { it.category == category }).asStateFlow()

        override fun searchProducts(query: String): Flow<List<ProductEntity>> =
            MutableStateFlow(products.filter { it.name.contains(query, true) }).asStateFlow()

        override fun searchProductsByCategory(query: String, category: String): Flow<List<ProductEntity>> =
            MutableStateFlow(products.filter { it.category == category && it.name.contains(query, true) }).asStateFlow()

        override suspend fun insertProduct(product: ProductEntity) {
            val idx = products.indexOfFirst { it.id == product.id }
            if (idx >= 0) products[idx] = product else products.add(product)
        }

        override suspend fun upsertProduct(product: ProductEntity) {
            insertProduct(product)
        }

        override suspend fun insertAll(products: List<ProductEntity>) {
            products.forEach { insertProduct(it) }
        }

        override suspend fun updateProduct(product: ProductEntity) {
            insertProduct(product)
        }

        override suspend fun deleteProduct(id: String) {
            products.removeAll { it.id == id }
        }

        override suspend fun decrementProductStockMilli(productId: String, quantityMilli: Long): Int {
            val idx = products.indexOfFirst { it.id == productId }
            if (idx >= 0 && products[idx].stockMilli >= quantityMilli) {
                products[idx] = products[idx].copy(stockMilli = products[idx].stockMilli - quantityMilli)
                return 1
            }
            return 0
        }

        override suspend fun forceDecrementProductStockMilli(productId: String, quantityMilli: Long): Int {
            val idx = products.indexOfFirst { it.id == productId }
            if (idx >= 0) {
                products[idx] = products[idx].copy(stockMilli = products[idx].stockMilli - quantityMilli)
                return 1
            }
            return 0
        }

        override suspend fun restoreProductStockMilli(productId: String, quantityMilli: Long): Int {
            val idx = products.indexOfFirst { it.id == productId }
            if (idx >= 0) {
                products[idx] = products[idx].copy(stockMilli = products[idx].stockMilli + quantityMilli)
                return 1
            }
            return 0
        }

        override suspend fun updateStockMilli(productId: String, newStockMilli: Long): Int {
            val idx = products.indexOfFirst { it.id == productId }
            if (idx >= 0) {
                products[idx] = products[idx].copy(stockMilli = newStockMilli)
                return 1
            }
            return 0
        }

        override fun getTotalCount(): Flow<Int> =
            MutableStateFlow(products.size).asStateFlow()

        override fun getLowStockCount(): Flow<Int> =
            MutableStateFlow(0).asStateFlow()

        override fun getOutOfStockCount(): Flow<Int> =
            MutableStateFlow(0).asStateFlow()

        override suspend fun getLowAndOutOfStockProducts(): List<ProductEntity> =
            emptyList()

        override fun getAllCategories(): Flow<List<String>> =
            MutableStateFlow(emptyList<String>()).asStateFlow()

        override suspend fun getProductTransactionCount(productId: String): Int = 0
    }

    class FakeProductBatchDao : ProductBatchDao {
        val batches = mutableListOf<ProductBatchEntity>()
        private var nextId = 1L

        override suspend fun insertBatch(batch: ProductBatchEntity): Long {
            val id = if (batch.id == 0L) nextId++ else batch.id
            batches.add(batch.copy(id = id))
            return id
        }

        override suspend fun upsertBatch(batch: ProductBatchEntity) {
            val idx = batches.indexOfFirst { it.id == batch.id }
            if (idx >= 0) batches[idx] = batch else batches.add(batch)
        }

        override suspend fun upsertAll(batches: List<ProductBatchEntity>) {
            batches.forEach { upsertBatch(it) }
        }

        override suspend fun getBatchById(id: Long): ProductBatchEntity? =
            batches.find { it.id == id }

        override fun getBatchesForProduct(productId: String): Flow<List<ProductBatchEntity>> =
            MutableStateFlow(batches.filter { it.productId == productId }).asStateFlow()

        override suspend fun getBatchListForProduct(productId: String): List<ProductBatchEntity> =
            batches.filter { it.productId == productId }

        override suspend fun getActiveBatchesFEFO(productId: String): List<ProductBatchEntity> =
            batches.filter { it.productId == productId && it.currentStockMilli > 0 }

        override fun getAllBatches(): Flow<List<ProductBatchEntity>> =
            MutableStateFlow(batches.toList()).asStateFlow()

        override suspend fun getAllBatchList(): List<ProductBatchEntity> =
            batches.toList()

        override suspend fun updateBatchStock(id: Long, newStockMilli: Long): Int {
            val idx = batches.indexOfFirst { it.id == id }
            if (idx >= 0) {
                batches[idx] = batches[idx].copy(currentStockMilli = newStockMilli)
                return 1
            }
            return 0
        }

        override suspend fun decrementBatchStock(id: Long, quantityMilli: Long): Int {
            val idx = batches.indexOfFirst { it.id == id }
            if (idx >= 0 && batches[idx].currentStockMilli >= quantityMilli) {
                batches[idx] = batches[idx].copy(currentStockMilli = batches[idx].currentStockMilli - quantityMilli)
                return 1
            }
            return 0
        }

        override suspend fun incrementBatchStock(id: Long, quantityMilli: Long): Int {
            val idx = batches.indexOfFirst { it.id == id }
            if (idx >= 0) {
                batches[idx] = batches[idx].copy(currentStockMilli = batches[idx].currentStockMilli + quantityMilli)
                return 1
            }
            return 0
        }

        override fun getExpiringBatches(thresholdTimestamp: Long): Flow<List<ProductBatchEntity>> =
            MutableStateFlow(emptyList<ProductBatchEntity>()).asStateFlow()

        override suspend fun deleteBatch(id: Long) {
            batches.removeAll { it.id == id }
        }

        override suspend fun deleteBatchesForProduct(productId: String) {
            batches.removeAll { it.productId == productId }
        }
    }

    class FakeCustomerDao : CustomerDao {
        val customers = mutableListOf<CustomerEntity>()

        override fun getAllCustomers(): Flow<List<CustomerEntity>> =
            MutableStateFlow(customers.toList()).asStateFlow()

        override fun searchCustomers(query: String): Flow<List<CustomerEntity>> =
            MutableStateFlow(customers.filter { it.name.contains(query, true) }).asStateFlow()

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
            entries.add(entry.copy(id = id))
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

    class FakeShiftDao : ShiftDao {
        var currentShift: ShiftEntity? = null
        val shifts = mutableListOf<ShiftEntity>()

        override suspend fun insertShift(shift: ShiftEntity): Long {
            shifts.add(shift)
            currentShift = shift
            return shift.id
        }

        override suspend fun upsertShift(shift: ShiftEntity) {
            val idx = shifts.indexOfFirst { it.id == shift.id }
            if (idx >= 0) shifts[idx] = shift else shifts.add(shift)
            currentShift = shift
        }

        override suspend fun upsertAll(shifts: List<ShiftEntity>) {
            this.shifts.addAll(shifts)
        }

        override suspend fun updateShift(shift: ShiftEntity) {
            upsertShift(shift)
        }

        override suspend fun getShiftById(id: Long): ShiftEntity? =
            shifts.find { it.id == id } ?: if (currentShift?.id == id) currentShift else null

        override suspend fun getOpenShiftForUser(userId: String): ShiftEntity? =
            if (currentShift?.userId == userId && currentShift?.status == "OPEN") currentShift else null

        override suspend fun getCurrentOpenShift(): ShiftEntity? =
            if (currentShift?.status == "OPEN") currentShift else null

        override fun getCurrentOpenShiftFlow(): Flow<ShiftEntity?> =
            MutableStateFlow(if (currentShift?.status == "OPEN") currentShift else null).asStateFlow()

        override fun getAllShifts(): Flow<List<ShiftEntity>> =
            MutableStateFlow(shifts.toList()).asStateFlow()

        override fun getShiftsForUser(userId: String): Flow<List<ShiftEntity>> =
            MutableStateFlow(shifts.filter { it.userId == userId }).asStateFlow()

        override suspend fun closeShift(
            id: Long,
            closedAt: Long,
            actualCashPaise: Long,
            expectedCashPaise: Long,
            variancePaise: Long,
            note: String?,
            status: String
        ) {
            val s = getShiftById(id)
            if (s != null) {
                val updated = s.copy(
                    closedAt = closedAt,
                    actualCashPaise = actualCashPaise,
                    expectedCashPaise = expectedCashPaise,
                    variancePaise = variancePaise,
                    note = note,
                    status = status
                )
                upsertShift(updated)
            }
        }

        override suspend fun deleteShift(id: Long) {
            shifts.removeAll { it.id == id }
        }

        override fun getShiftCount(): Flow<Int> =
            MutableStateFlow(shifts.size).asStateFlow()
    }

    class FakeCashMovementDao : CashMovementDao {
        val movements = mutableListOf<CashMovementEntity>()
        private var nextId = 1L

        override suspend fun insertCashMovement(movement: CashMovementEntity): Long {
            val id = nextId++
            val saved = movement.copy(id = id).apply {
                this.source = movement.source
                this.sourceId = movement.sourceId
                this.notes = movement.notes
            }
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
            movements.filter { it.shiftId == shiftId }.sumOf { it.amountPaise }

        override fun getNetCashForShiftFlow(shiftId: Long): Flow<Long> =
            MutableStateFlow(movements.filter { it.shiftId == shiftId }.sumOf { it.amountPaise }).asStateFlow()

        override suspend fun deleteCashMovement(id: Long) {
            movements.removeAll { it.id == id }
        }

        override suspend fun deleteCashMovementsForShift(shiftId: Long) {
            movements.removeAll { it.shiftId == shiftId }
        }
    }

    class FakeAuditLogDao : AuditLogDao {
        val logs = mutableListOf<AuditLogEntity>()
        private var nextId = 1L

        override suspend fun insertAuditLog(log: AuditLogEntity): Long {
            val id = nextId++
            logs.add(log.copy(id = id))
            return id
        }

        override suspend fun upsertAuditLog(log: AuditLogEntity) {
            val idx = logs.indexOfFirst { it.id == log.id }
            if (idx >= 0) logs[idx] = log else logs.add(log)
        }

        override suspend fun upsertAll(logs: List<AuditLogEntity>) {
            logs.forEach { upsertAuditLog(it) }
        }

        override fun getAllAuditLogs(): Flow<List<AuditLogEntity>> =
            MutableStateFlow(logs.toList()).asStateFlow()

        override fun getRecentLogs(limit: Int): Flow<List<AuditLogEntity>> =
            MutableStateFlow(logs.take(limit)).asStateFlow()

        override suspend fun getAllAuditLogList(): List<AuditLogEntity> =
            logs.toList()

        override fun getAuditLogsForUser(userId: String): Flow<List<AuditLogEntity>> =
            MutableStateFlow(logs.filter { it.userId == userId }).asStateFlow()

        override fun getAuditLogsForEntity(entityType: String, entityId: String): Flow<List<AuditLogEntity>> =
            MutableStateFlow(logs.filter { it.entityType == entityType && it.entityId == entityId }).asStateFlow()

        override fun getAuditLogsBetween(startTime: Long, endTime: Long): Flow<List<AuditLogEntity>> =
            MutableStateFlow(logs.filter { it.timestamp in startTime..endTime }).asStateFlow()

        override suspend fun getAuditLogById(id: Long): AuditLogEntity? =
            logs.find { it.id == id }

        override suspend fun deleteAuditLog(id: Long) {
            logs.removeAll { it.id == id }
        }

        override suspend fun deleteAuditLogsOlderThan(cutoffTimestamp: Long) {
            logs.removeAll { it.timestamp < cutoffTimestamp }
        }
    }
}
