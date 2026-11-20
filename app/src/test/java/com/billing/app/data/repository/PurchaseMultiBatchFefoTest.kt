package com.billing.app.data.repository

import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.CategorySalesSummary
import com.billing.app.data.local.dao.CustomerDao
import com.billing.app.data.local.dao.CustomerLedgerDao
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.InvoiceItemBatchDao
import com.billing.app.data.local.dao.ProductBatchDao
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.dao.PurchaseDao
import com.billing.app.data.local.dao.SupplierDao
import com.billing.app.data.local.dao.SupplierLedgerDao
import com.billing.app.data.local.dao.TopProductSummary
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
import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.PurchaseItemEntity
import com.billing.app.data.local.entity.SupplierEntity
import com.billing.app.data.local.entity.SupplierLedgerEntity
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

class PurchaseMultiBatchFefoTest {

    private lateinit var fakePurchaseDao: FakePurchaseDao
    private lateinit var fakeProductDao: FakeProductDao
    private lateinit var fakeProductBatchDao: FakeProductBatchDao
    private lateinit var fakeSupplierDao: FakeSupplierDao
    private lateinit var fakeSupplierLedgerDao: FakeSupplierLedgerDao
    private lateinit var fakeCashMovementDao: FakeCashMovementDao
    private lateinit var fakeInvoiceDao: FakeInvoiceDao
    private lateinit var fakeCustomerDao: FakeCustomerDao
    private lateinit var fakeCustomerLedgerDao: FakeCustomerLedgerDao
    private lateinit var fakeInvoiceItemBatchDao: FakeInvoiceItemBatchDao

    private lateinit var purchaseRepository: PurchaseRepositoryImpl
    private lateinit var invoiceRepository: InvoiceRepositoryImpl

    @Before
    fun setup() {
        fakePurchaseDao = FakePurchaseDao()
        fakeProductDao = FakeProductDao()
        fakeProductBatchDao = FakeProductBatchDao()
        fakeSupplierDao = FakeSupplierDao()
        fakeSupplierLedgerDao = FakeSupplierLedgerDao()
        fakeCashMovementDao = FakeCashMovementDao()
        fakeInvoiceDao = FakeInvoiceDao()
        fakeCustomerDao = FakeCustomerDao()
        fakeCustomerLedgerDao = FakeCustomerLedgerDao()
        fakeInvoiceItemBatchDao = FakeInvoiceItemBatchDao()

        purchaseRepository = PurchaseRepositoryImpl(
            purchaseDao = fakePurchaseDao,
            productDao = fakeProductDao,
            productBatchDao = fakeProductBatchDao,
            supplierDao = fakeSupplierDao,
            supplierLedgerDao = fakeSupplierLedgerDao,
            cashMovementDao = fakeCashMovementDao,
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
            database = null
        )
    }

    // --- Task 4 Tests ---

    @Test
    fun testBatchCreationAndBatchIdLinkingOnPurchaseOfTrackBatchProduct() = runBlocking {
        // Arrange
        val product = ProductEntity(
            id = "prod_pharma_1",
            name = "Paracetamol 500mg",
            barcode = "PARA500",
            pricePaise = 3000L,
            stockMilli = 0L,
            trackBatch = true,
            purchasePricePaise = 1500L,
            mrpPaise = 3000L
        )
        fakeProductDao.insertProduct(product)

        val purchase = PurchaseEntity(
            purchaseNumber = "PO-2026-001",
            supplierId = "sup_1",
            supplierName = "Cipla Pharma",
            totalPaise = 150000L,
            paidPaise = 150000L,
            duePaise = 0L,
            paymentStatus = "PAID",
            shiftId = 1L
        )
        val purchaseItem = PurchaseItemEntity(
            purchaseId = 0L,
            productId = product.id,
            productName = product.name,
            quantityMilli = 10000L, // 10 units
            unitPricePaise = 1500L,
            lineTotalPaise = 150000L,
            batchNumber = "BATCH-XY12",
            expiryDate = 1790000000000L,
            mrpPaise = 3200L
        )

        // Act
        val result = purchaseRepository.addPurchase(purchase, listOf(purchaseItem), paymentMode = "CASH")

        // Assert
        assertTrue("Purchase add should succeed", result.isSuccess)
        val purchaseId = result.getOrThrow()
        assertTrue("Generated purchaseId should be > 0", purchaseId > 0L)

        // Verify batch was created in product_batches
        val batches = fakeProductBatchDao.getBatchListForProduct(product.id)
        assertEquals(1, batches.size)
        val createdBatch = batches[0]
        assertEquals("BATCH-XY12", createdBatch.batchNumber)
        assertEquals(1790000000000L, createdBatch.expiryDate)
        assertEquals(1500L, createdBatch.purchasePricePaise)
        assertEquals(3200L, createdBatch.mrpPaise)
        assertEquals(10000L, createdBatch.initialStockMilli)
        assertEquals(10000L, createdBatch.currentStockMilli)

        // Verify purchase_items has batchId linked
        val savedItems = fakePurchaseDao.getPurchaseItems(purchaseId)
        assertEquals(1, savedItems.size)
        assertEquals(createdBatch.id, savedItems[0].batchId)
        assertEquals("BATCH-XY12", savedItems[0].batchNumber)

        // Verify parent product stock was restored
        val updatedProduct = fakeProductDao.getProductById(product.id)
        assertEquals(10000L, updatedProduct?.stockMilli)
    }

    @Test
    fun testBatchAutoGeneratedBatchNumberWhenBlank() = runBlocking {
        val product = ProductEntity(
            id = "prod_pharma_2",
            name = "Amoxicillin",
            barcode = "AMOX500",
            pricePaise = 5000L,
            stockMilli = 0L,
            trackBatch = true
        )
        fakeProductDao.insertProduct(product)

        val purchase = PurchaseEntity(
            purchaseNumber = "PO-2026-002",
            totalPaise = 50000L,
            paidPaise = 50000L,
            paymentStatus = "PAID"
        )
        val purchaseItem = PurchaseItemEntity(
            purchaseId = 0L,
            productId = product.id,
            productName = product.name,
            quantityMilli = 5000L,
            unitPricePaise = 1000L,
            lineTotalPaise = 50000L,
            batchNumber = "", // blank batch number
            expiryDate = null
        )

        val result = purchaseRepository.addPurchase(purchase, listOf(purchaseItem))
        assertTrue(result.isSuccess)

        val batches = fakeProductBatchDao.getBatchListForProduct(product.id)
        assertEquals(1, batches.size)
        assertTrue(
            "Auto-generated batchNumber should start with BATCH-",
            batches[0].batchNumber.startsWith("BATCH-")
        )
    }

    @Test
    fun testLppUpdateOnStandardProductPurchase() = runBlocking {
        // Standard product (trackBatch = false)
        val product = ProductEntity(
            id = "prod_std_1",
            name = "Basmati Rice 1kg",
            barcode = "RICE1KG",
            pricePaise = 12000L,
            stockMilli = 2000L,
            trackBatch = false,
            purchasePricePaise = 8000L,
            mrpPaise = 13000L
        )
        fakeProductDao.insertProduct(product)

        val purchase = PurchaseEntity(
            purchaseNumber = "PO-2026-003",
            totalPaise = 95000L,
            paidPaise = 95000L
        )
        val purchaseItem = PurchaseItemEntity(
            purchaseId = 0L,
            productId = product.id,
            productName = product.name,
            quantityMilli = 10000L, // 10 kg
            unitPricePaise = 9500L, // New LPP: 95.00
            lineTotalPaise = 95000L,
            mrpPaise = 14000L // New MRP: 140.00
        )

        val result = purchaseRepository.addPurchase(purchase, listOf(purchaseItem))
        assertTrue(result.isSuccess)

        // Verify product was updated with latest purchase price (LPP) and new MRP
        val updatedProduct = fakeProductDao.getProductById(product.id)
        assertNotNull(updatedProduct)
        assertEquals(9500L, updatedProduct!!.purchasePricePaise)
        assertEquals(14000L, updatedProduct.mrpPaise)
        assertEquals(12000L, updatedProduct.stockMilli) // 2000 + 10000

        // Verify no product_batches were created for standard product
        val batches = fakeProductBatchDao.getBatchListForProduct(product.id)
        assertTrue("No batches should be created for non-batch tracked product", batches.isEmpty())

        // Verify purchase item has batchId == null
        val savedItems = fakePurchaseDao.getPurchaseItems(result.getOrThrow())
        assertEquals(1, savedItems.size)
        assertNull(savedItems[0].batchId)
    }

    @Test
    fun testSupplierLedgerPurchaseDueCreationAndSupplierBalanceUpdate() = runBlocking {
        val supplier = SupplierEntity(
            id = "sup_agro_1",
            name = "Agro Foods Ltd",
            phone = "9876543210",
            openingBalancePaise = 0L,
            duePaise = 0L
        )
        fakeSupplierDao.insertSupplier(supplier)

        val purchase = PurchaseEntity(
            purchaseNumber = "PO-2026-004",
            supplierId = supplier.id,
            supplierName = supplier.name,
            totalPaise = 500000L, // 5000.00
            paidPaise = 200000L,  // 2000.00 paid
            duePaise = 300000L,   // 3000.00 due
            paymentStatus = "PARTIAL",
            shiftId = 1L
        )
        val item = PurchaseItemEntity(
            purchaseId = 0L,
            productId = "prod_any",
            productName = "Any Product",
            quantityMilli = 5000L,
            unitPricePaise = 1000L,
            lineTotalPaise = 500000L
        )

        val result = purchaseRepository.addPurchase(purchase, listOf(item), paymentMode = "CASH")
        assertTrue(result.isSuccess)

        // Verify SupplierLedger PURCHASE_DUE entry
        val ledgerEntries = fakeSupplierLedgerDao.getEntryListForSupplier(supplier.id)
        assertEquals(1, ledgerEntries.size)
        val entry = ledgerEntries[0]
        assertEquals("PURCHASE_DUE", entry.transactionType)
        assertEquals(0L, entry.debitPaise)
        assertEquals(300000L, entry.creditPaise)
        assertEquals(300000L, entry.balanceAfterPaise)
        assertEquals("Purchase: PO-2026-004", entry.note)

        // Verify supplier duePaise updated
        val updatedSupplier = fakeSupplierDao.getSupplierById(supplier.id)
        assertNotNull(updatedSupplier)
        assertEquals(300000L, updatedSupplier!!.duePaise)

        // Verify Flow balance
        val currentBalance = purchaseRepository.getSupplierBalance(supplier.id).first()
        assertEquals(300000L, currentBalance)
    }

    @Test
    fun testCashMovementPurchasePaymentRecordedInDrawerWhenPaidCashInActiveShift() = runBlocking {
        val shiftId = 77L
        val purchase = PurchaseEntity(
            purchaseNumber = "PO-2026-005",
            totalPaise = 150000L,
            paidPaise = 150000L,
            duePaise = 0L,
            shiftId = shiftId
        )
        val item = PurchaseItemEntity(
            purchaseId = 0L,
            productId = "p_1",
            productName = "Product 1",
            quantityMilli = 1000L,
            unitPricePaise = 150000L,
            lineTotalPaise = 150000L
        )

        // Paid with CASH in active shift
        val result = purchaseRepository.addPurchase(purchase, listOf(item), paymentMode = "CASH")
        assertTrue(result.isSuccess)

        val movements = fakeCashMovementDao.getCashMovementListForShift(shiftId)
        assertEquals(1, movements.size)
        val movement = movements[0]
        assertEquals("PURCHASE_PAYMENT", movement.movementType)
        assertEquals(-150000L, movement.amountPaise)
        assertEquals("PO-2026-005", movement.referenceId)

        // When paid with UPI, cash drawer should NOT have a cash movement
        val upiPurchase = PurchaseEntity(
            purchaseNumber = "PO-2026-006",
            totalPaise = 50000L,
            paidPaise = 50000L,
            duePaise = 0L,
            shiftId = shiftId
        )
        val resultUpi = purchaseRepository.addPurchase(upiPurchase, listOf(item), paymentMode = "UPI")
        assertTrue(resultUpi.isSuccess)

        val movementsAfterUpi = fakeCashMovementDao.getCashMovementListForShift(shiftId)
        assertEquals("Still only 1 cash movement because UPI doesn't affect cash drawer", 1, movementsAfterUpi.size)
    }

    @Test
    fun testRecordSupplierPaymentUpdatesLedgerAndCashMovement() = runBlocking {
        val supplier = SupplierEntity(
            id = "sup_99",
            name = "Vendor 99",
            phone = "9112233445",
            openingBalancePaise = 500000L,
            duePaise = 500000L
        )
        fakeSupplierDao.insertSupplier(supplier)

        // Initial PURCHASE_DUE entry
        fakeSupplierLedgerDao.insertEntry(
            SupplierLedgerEntity(
                supplierId = supplier.id,
                transactionType = "PURCHASE_DUE",
                creditPaise = 500000L,
                balanceAfterPaise = 500000L
            )
        )

        // Record supplier payment of 2000.00
        val paymentResult = purchaseRepository.recordSupplierPayment(
            supplierId = supplier.id,
            amountPaise = 200000L,
            paymentMode = "CASH",
            shiftId = 12L,
            note = "Part payment via cash"
        )
        assertTrue(paymentResult.isSuccess)

        // Check ledger
        val entries = fakeSupplierLedgerDao.getEntryListForSupplier(supplier.id)
        assertEquals(2, entries.size)
        val payEntry = entries[1]
        assertEquals("PAYMENT", payEntry.transactionType)
        assertEquals(200000L, payEntry.debitPaise)
        assertEquals(0L, payEntry.creditPaise)
        assertEquals(300000L, payEntry.balanceAfterPaise)
        assertEquals("CASH", payEntry.paymentMode)

        // Check supplier entity
        val updatedSupplier = fakeSupplierDao.getSupplierById(supplier.id)
        assertEquals(300000L, updatedSupplier?.duePaise)

        // Check cash movement
        val cashMovements = fakeCashMovementDao.getCashMovementListForShift(12L)
        assertEquals(1, cashMovements.size)
        assertEquals("PURCHASE_PAYMENT", cashMovements[0].movementType)
        assertEquals(-200000L, cashMovements[0].amountPaise)
    }

    @Test
    fun testFefoStockDeductionInCompleteSaleAcrossMultipleBatches() = runBlocking {
        val product = ProductEntity(
            id = "prod_fefo_1",
            name = "Syrup 100ml",
            barcode = "SYR100",
            pricePaise = 15000L,
            stockMilli = 8000L,
            trackBatch = true
        )
        fakeProductDao.insertProduct(product)

        // Batch 1: Expiring earlier (T=1000), Stock = 3000 milli (3 units), Cost = 9000 paise
        val b1 = ProductBatchEntity(
            id = 101L,
            productId = product.id,
            batchNumber = "BATCH-EARLY",
            expiryDate = 1000L,
            purchasePricePaise = 9000L,
            mrpPaise = 15000L,
            initialStockMilli = 3000L,
            currentStockMilli = 3000L,
            createdAt = 100L
        )
        // Batch 2: Expiring later (T=2000), Stock = 5000 milli (5 units), Cost = 9500 paise
        val b2 = ProductBatchEntity(
            id = 102L,
            productId = product.id,
            batchNumber = "BATCH-LATER",
            expiryDate = 2000L,
            purchasePricePaise = 9500L,
            mrpPaise = 15000L,
            initialStockMilli = 5000L,
            currentStockMilli = 5000L,
            createdAt = 200L
        )
        fakeProductBatchDao.insertBatch(b1)
        fakeProductBatchDao.insertBatch(b2)

        // Sale of 5000 milli (5 units)
        // Expected FEFO:
        // - 3000 milli from B1 (depletes B1 to 0)
        // - 2000 milli from B2 (reduces B2 to 3000)
        val invoice = InvoiceEntity(
            invoiceNumber = "INV-FEFO-001",
            timestamp = System.currentTimeMillis(),
            subtotalPaise = 75000L,
            grandTotalPaise = 75000L,
            status = InvoiceStatus.COMPLETED,
            paymentMethod = "CASH",
            shiftId = 1L
        )
        val saleItem = InvoiceItemEntity(
            invoiceId = 0L,
            productId = product.id,
            productName = product.name,
            quantityMilli = 5000L,
            unitPricePaise = 15000L,
            lineTotalPaise = 75000L
        )

        val saleResult = invoiceRepository.completeSale(invoice, listOf(saleItem))
        assertTrue("Sale should complete successfully", saleResult.isSuccess)

        // Verify batch stocks after FEFO deduction
        val updatedB1 = fakeProductBatchDao.getBatchById(101L)
        val updatedB2 = fakeProductBatchDao.getBatchById(102L)
        assertEquals("Batch 1 should be completely consumed", 0L, updatedB1?.currentStockMilli)
        assertEquals("Batch 2 should have 3000 milli remaining", 3000L, updatedB2?.currentStockMilli)

        // Verify invoice_item_batches tracking
        val itemBatches = fakeInvoiceItemBatchDao.getAllItemBatchList()
        assertEquals("Should create 2 item batch deduction entries", 2, itemBatches.size)

        val entry1 = itemBatches.find { it.batchId == 101L }
        assertNotNull(entry1)
        assertEquals(3000L, entry1!!.quantityMilli)
        assertEquals(9000L, entry1.costPaise)

        val entry2 = itemBatches.find { it.batchId == 102L }
        assertNotNull(entry2)
        assertEquals(2000L, entry2!!.quantityMilli)
        assertEquals(9500L, entry2.costPaise)

        // Verify total product stock was decremented
        val updatedProduct = fakeProductDao.getProductById(product.id)
        assertEquals(3000L, updatedProduct?.stockMilli)
    }

    @Test
    fun testFefoStockDeductionThrowsWhenBatchStockInsufficient() = runBlocking {
        val product = ProductEntity(
            id = "prod_fefo_insufficient",
            name = "Scarce Medicine",
            barcode = "MED1",
            pricePaise = 5000L,
            stockMilli = 2000L,
            trackBatch = true
        )
        fakeProductDao.insertProduct(product)

        val b1 = ProductBatchEntity(
            id = 201L,
            productId = product.id,
            batchNumber = "B-ONLY",
            expiryDate = 1000L,
            purchasePricePaise = 3000L,
            mrpPaise = 5000L,
            initialStockMilli = 2000L,
            currentStockMilli = 2000L
        )
        fakeProductBatchDao.insertBatch(b1)

        val invoice = InvoiceEntity(
            invoiceNumber = "INV-FAIL-001",
            timestamp = System.currentTimeMillis(),
            subtotalPaise = 25000L,
            grandTotalPaise = 25000L,
            status = InvoiceStatus.COMPLETED,
            paymentMethod = "CASH",
            shiftId = 1L
        )
        val saleItem = InvoiceItemEntity(
            invoiceId = 0L,
            productId = product.id,
            productName = product.name,
            quantityMilli = 5000L, // Requesting 5000 milli but only 2000 available
            unitPricePaise = 5000L,
            lineTotalPaise = 25000L
        )

        val saleResult = invoiceRepository.completeSale(invoice, listOf(saleItem))
        assertTrue("Sale should fail due to insufficient batch stock", saleResult.isFailure)
        assertTrue(
            "Exception message should mention insufficient batch stock",
            saleResult.exceptionOrNull()?.message?.contains("Insufficient batch stock") == true
        )
    }

    // --- Fake DAOs ---

    class FakePurchaseDao : PurchaseDao {
        val purchases = mutableListOf<PurchaseEntity>()
        val purchaseItems = mutableListOf<PurchaseItemEntity>()
        private var nextPurchaseId = 1L
        private var nextItemId = 1L

        override fun getAllPurchases(): Flow<List<PurchaseEntity>> =
            MutableStateFlow(purchases.toList()).asStateFlow()

        override suspend fun insertPurchase(purchase: PurchaseEntity): Long {
            val id = if (purchase.id > 0) purchase.id else nextPurchaseId++
            purchases.add(purchase.copy(id = id))
            return id
        }

        override suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>) {
            for (item in items) {
                val id = if (item.id > 0) item.id else nextItemId++
                purchaseItems.add(item.copy(id = id))
            }
        }

        override suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItemEntity> =
            purchaseItems.filter { it.purchaseId == purchaseId }

        override suspend fun getAllPurchaseItems(): List<PurchaseItemEntity> =
            purchaseItems.toList()

        override fun getPurchaseCount(): Flow<Int> =
            MutableStateFlow(purchases.size).asStateFlow()
    }

    class FakeProductDao : ProductDao {
        val products = mutableMapOf<String, ProductEntity>()

        override fun getAllProducts(): Flow<List<ProductEntity>> =
            MutableStateFlow(products.values.toList()).asStateFlow()

        override suspend fun getProductByBarcode(barcode: String): ProductEntity? =
            products.values.find { it.barcode == barcode }

        override suspend fun getProductById(id: String): ProductEntity? =
            products[id]

        override suspend fun getProductBySku(sku: String): ProductEntity? =
            products.values.find { it.sku == sku }

        override fun getProductsByCategory(category: String): Flow<List<ProductEntity>> =
            MutableStateFlow(products.values.filter { it.category == category }).asStateFlow()

        override fun searchProducts(query: String): Flow<List<ProductEntity>> =
            MutableStateFlow(products.values.filter { it.name.contains(query, ignoreCase = true) }).asStateFlow()

        override fun searchProductsByCategory(query: String, category: String): Flow<List<ProductEntity>> =
            MutableStateFlow(products.values.filter { it.category == category && it.name.contains(query, ignoreCase = true) }).asStateFlow()

        override suspend fun insertProduct(product: ProductEntity) {
            products[product.id] = product
        }

        override suspend fun upsertProduct(product: ProductEntity) {
            products[product.id] = product
        }

        override suspend fun insertAll(products: List<ProductEntity>) {
            products.forEach { this.products[it.id] = it }
        }

        override suspend fun updateProduct(product: ProductEntity) {
            products[product.id] = product
        }

        override suspend fun deleteProduct(id: String) {
            products.remove(id)
        }

        override suspend fun decrementProductStockMilli(productId: String, quantityMilli: Long): Int {
            val p = products[productId] ?: return 0
            if (p.stockMilli >= quantityMilli) {
                products[productId] = p.copy(stockMilli = p.stockMilli - quantityMilli)
                return 1
            }
            return 0
        }

        override suspend fun forceDecrementProductStockMilli(productId: String, quantityMilli: Long): Int {
            val p = products[productId] ?: return 0
            products[productId] = p.copy(stockMilli = p.stockMilli - quantityMilli)
            return 1
        }

        override suspend fun restoreProductStockMilli(productId: String, quantityMilli: Long): Int {
            val p = products[productId] ?: return 0
            products[productId] = p.copy(stockMilli = p.stockMilli + quantityMilli)
            return 1
        }

        override suspend fun updateStockMilli(productId: String, newStockMilli: Long): Int {
            val p = products[productId] ?: return 0
            products[productId] = p.copy(stockMilli = newStockMilli)
            return 1
        }

        override fun getTotalCount(): Flow<Int> = MutableStateFlow(products.size).asStateFlow()
        override fun getLowStockCount(): Flow<Int> = MutableStateFlow(0).asStateFlow()
        override fun getOutOfStockCount(): Flow<Int> = MutableStateFlow(0).asStateFlow()
        override suspend fun getLowAndOutOfStockProducts(): List<ProductEntity> = emptyList()
        override fun getAllCategories(): Flow<List<String>> = MutableStateFlow(emptyList<String>()).asStateFlow()
        override suspend fun getProductTransactionCount(productId: String): Int = 0
    }

    class FakeProductBatchDao : ProductBatchDao {
        val batches = mutableListOf<ProductBatchEntity>()
        private var nextId = 1L

        override suspend fun insertBatch(batch: ProductBatchEntity): Long {
            val id = if (batch.id > 0) batch.id else nextId++
            val saved = batch.copy(id = id)
            batches.add(saved)
            return id
        }

        override suspend fun upsertBatch(batch: ProductBatchEntity) {
            val idx = batches.indexOfFirst { it.id == batch.id }
            if (idx >= 0) batches[idx] = batch else insertBatch(batch)
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
            batches
                .filter { it.productId == productId && it.currentStockMilli > 0 }
                .sortedWith(
                    compareBy<ProductBatchEntity> { it.expiryDate == null }
                        .thenBy { it.expiryDate }
                        .thenBy { it.createdAt }
                )

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

    class FakeSupplierDao : SupplierDao {
        val suppliers = mutableMapOf<String, SupplierEntity>()

        override fun getAllSuppliers(): Flow<List<SupplierEntity>> =
            MutableStateFlow(suppliers.values.toList()).asStateFlow()

        override fun searchSuppliers(query: String): Flow<List<SupplierEntity>> =
            MutableStateFlow(suppliers.values.filter { it.name.contains(query, ignoreCase = true) }).asStateFlow()

        override suspend fun getSupplierById(id: String): SupplierEntity? =
            suppliers[id]

        override suspend fun insertSupplier(supplier: SupplierEntity) {
            suppliers[supplier.id] = supplier
        }

        override suspend fun insertAll(suppliers: List<SupplierEntity>) {
            suppliers.forEach { this.suppliers[it.id] = it }
        }

        override suspend fun updateSupplier(supplier: SupplierEntity) {
            suppliers[supplier.id] = supplier
        }

        override suspend fun deleteSupplier(id: String) {
            suppliers.remove(id)
        }

        override fun getSupplierCount(): Flow<Int> =
            MutableStateFlow(suppliers.size).asStateFlow()
    }

    class FakeSupplierLedgerDao : SupplierLedgerDao {
        val entries = mutableListOf<SupplierLedgerEntity>()
        private var nextId = 1L

        override suspend fun insertEntry(entry: SupplierLedgerEntity): Long {
            val id = if (entry.id > 0) entry.id else nextId++
            val saved = entry.copy(id = id)
            entries.add(saved)
            return id
        }

        override suspend fun upsertEntry(entry: SupplierLedgerEntity) {
            val idx = entries.indexOfFirst { it.id == entry.id }
            if (idx >= 0) entries[idx] = entry else insertEntry(entry)
        }

        override suspend fun upsertAll(entries: List<SupplierLedgerEntity>) {
            entries.forEach { upsertEntry(it) }
        }

        override fun getEntriesForSupplier(supplierId: String): Flow<List<SupplierLedgerEntity>> =
            MutableStateFlow(entries.filter { it.supplierId == supplierId }).asStateFlow()

        override suspend fun getEntryListForSupplier(supplierId: String): List<SupplierLedgerEntity> =
            entries.filter { it.supplierId == supplierId }

        override fun getAllEntries(): Flow<List<SupplierLedgerEntity>> =
            MutableStateFlow(entries.toList()).asStateFlow()

        override suspend fun getEntryById(id: Long): SupplierLedgerEntity? =
            entries.find { it.id == id }

        override suspend fun getLatestEntryForSupplier(supplierId: String): SupplierLedgerEntity? =
            entries.filter { it.supplierId == supplierId }.maxByOrNull { it.id }

        override fun getEntriesForPurchase(purchaseId: Long): Flow<List<SupplierLedgerEntity>> =
            MutableStateFlow(entries.filter { it.purchaseId == purchaseId }).asStateFlow()

        override fun getEntriesBetween(supplierId: String, startTime: Long, endTime: Long): Flow<List<SupplierLedgerEntity>> =
            MutableStateFlow(entries.filter { it.supplierId == supplierId && it.createdAt in startTime..endTime }).asStateFlow()

        override fun getTotalDebitForSupplier(supplierId: String): Flow<Long> =
            MutableStateFlow(entries.filter { it.supplierId == supplierId }.sumOf { it.debitPaise }).asStateFlow()

        override fun getTotalCreditForSupplier(supplierId: String): Flow<Long> =
            MutableStateFlow(entries.filter { it.supplierId == supplierId }.sumOf { it.creditPaise }).asStateFlow()

        override suspend fun deleteEntry(id: Long) {
            entries.removeAll { it.id == id }
        }

        override suspend fun deleteEntriesForSupplier(supplierId: String) {
            entries.removeAll { it.supplierId == supplierId }
        }
    }

    class FakeCashMovementDao : CashMovementDao {
        val movements = mutableListOf<CashMovementEntity>()
        private var nextId = 1L

        override suspend fun insertCashMovement(movement: CashMovementEntity): Long {
            val id = if (movement.id > 0) movement.id else nextId++
            val saved = movement.copy(id = id)
            movements.add(saved)
            return id
        }

        override suspend fun upsertCashMovement(movement: CashMovementEntity) {
            val idx = movements.indexOfFirst { it.id == movement.id }
            if (idx >= 0) movements[idx] = movement else insertCashMovement(movement)
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

    class FakeInvoiceDao : InvoiceDao {
        val invoices = mutableListOf<InvoiceEntity>()
        val invoiceItems = mutableListOf<InvoiceItemEntity>()
        private var nextId = 1L
        private var nextItemId = 1L

        override suspend fun insertInvoice(invoice: InvoiceEntity): Long {
            val id = if (invoice.id == 0L) nextId++ else invoice.id
            val saved = invoice.copy(id = id)
            invoices.add(saved)
            return id
        }

        override suspend fun insertInvoiceItem(item: InvoiceItemEntity): Long {
            val id = if (item.id == 0L) nextItemId++ else item.id
            val saved = item.copy(id = id)
            invoiceItems.add(saved)
            return id
        }

        override suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>) {
            for (item in items) {
                val id = if (item.id == 0L) nextItemId++ else item.id
                invoiceItems.add(item.copy(id = id))
            }
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
            MutableStateFlow(invoices.filter { it.invoiceNumber.contains(query, ignoreCase = true) }).asStateFlow()

        override fun searchInvoicesByStatus(query: String, status: InvoiceStatus): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.filter { it.status == status && it.invoiceNumber.contains(query, ignoreCase = true) }).asStateFlow()

        override fun searchInvoicesByType(query: String, type: String): Flow<List<InvoiceEntity>> =
            MutableStateFlow(invoices.filter { it.invoiceType == type && it.invoiceNumber.contains(query, ignoreCase = true) }).asStateFlow()

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
            MutableStateFlow(invoices.filter { it.status == InvoiceStatus.COMPLETED && it.timestamp in startTime..endTime }.sumOf { it.duePaise }).asStateFlow()

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

        override suspend fun getLastInvoiceNumber(): Long? = invoices.size.toLong()
        override suspend fun setInvoiceCounter(counter: InvoiceCounterEntity) {}

        override suspend fun getInvoicesByShiftId(shiftId: Long): List<InvoiceEntity> =
            invoices.filter { it.shiftId == shiftId && it.status == InvoiceStatus.COMPLETED }

        override suspend fun getInvoiceItemsByShiftId(shiftId: Long): List<InvoiceItemEntity> =
            invoiceItems.filter { item -> invoices.any { it.id == item.invoiceId && it.shiftId == shiftId && it.status == InvoiceStatus.COMPLETED } }
    }

    class FakeCustomerDao : CustomerDao {
        val customers = mutableMapOf<String, CustomerEntity>()
        override fun getAllCustomers(): Flow<List<CustomerEntity>> = MutableStateFlow(customers.values.toList()).asStateFlow()
        override fun searchCustomers(query: String): Flow<List<CustomerEntity>> = MutableStateFlow(emptyList<CustomerEntity>()).asStateFlow()
        override suspend fun getCustomerById(id: String): CustomerEntity? = customers[id]
        override suspend fun getCustomerByPhone(phone: String): CustomerEntity? = customers.values.find { it.phone == phone }
        override suspend fun insertCustomer(customer: CustomerEntity) { customers[customer.id] = customer }
        override suspend fun insertAll(customers: List<CustomerEntity>) { customers.forEach { this.customers[it.id] = it } }
        override suspend fun updateCustomer(customer: CustomerEntity) { customers[customer.id] = customer }
        override suspend fun deleteCustomer(id: String) { customers.remove(id) }
        override fun getCustomerCount(): Flow<Int> = MutableStateFlow(customers.size).asStateFlow()
    }

    class FakeCustomerLedgerDao : CustomerLedgerDao {
        val entries = mutableListOf<CustomerLedgerEntity>()
        override suspend fun insertEntry(entry: CustomerLedgerEntity): Long {
            entries.add(entry)
            return entries.size.toLong()
        }
        override suspend fun upsertEntry(entry: CustomerLedgerEntity) { entries.add(entry) }
        override suspend fun upsertAll(entries: List<CustomerLedgerEntity>) { this.entries.addAll(entries) }
        override fun getEntriesForCustomer(customerId: String): Flow<List<CustomerLedgerEntity>> = MutableStateFlow(entries.filter { it.customerId == customerId }).asStateFlow()
        override suspend fun getEntryListForCustomer(customerId: String): List<CustomerLedgerEntity> = entries.filter { it.customerId == customerId }
        override fun getAllEntries(): Flow<List<CustomerLedgerEntity>> = MutableStateFlow(entries.toList()).asStateFlow()
        override suspend fun getEntryById(id: Long): CustomerLedgerEntity? = entries.find { it.id == id }
        override suspend fun getLatestEntryForCustomer(customerId: String): CustomerLedgerEntity? = entries.filter { it.customerId == customerId }.lastOrNull()
        override fun getEntriesForInvoice(invoiceId: Long): Flow<List<CustomerLedgerEntity>> = MutableStateFlow(entries.filter { it.invoiceId == invoiceId }).asStateFlow()
        override fun getEntriesBetween(customerId: String, startTime: Long, endTime: Long): Flow<List<CustomerLedgerEntity>> = MutableStateFlow(emptyList<CustomerLedgerEntity>()).asStateFlow()
        override fun getTotalDebitForCustomer(customerId: String): Flow<Long> = MutableStateFlow(entries.filter { it.customerId == customerId }.sumOf { it.debitPaise }).asStateFlow()
        override fun getTotalCreditForCustomer(customerId: String): Flow<Long> = MutableStateFlow(entries.filter { it.customerId == customerId }.sumOf { it.creditPaise }).asStateFlow()
        override suspend fun deleteEntry(id: Long) { entries.removeAll { it.id == id } }
        override suspend fun deleteEntriesForCustomer(customerId: String) { entries.removeAll { it.customerId == customerId } }
    }

    class FakeInvoiceItemBatchDao : InvoiceItemBatchDao {
        val itemBatches = mutableListOf<InvoiceItemBatchEntity>()
        private var nextId = 1L

        override suspend fun insertInvoiceItemBatch(itemBatch: InvoiceItemBatchEntity): Long {
            val id = if (itemBatch.id > 0) itemBatch.id else nextId++
            val saved = itemBatch.copy(id = id)
            itemBatches.add(saved)
            return id
        }

        override suspend fun insertItemBatch(itemBatch: InvoiceItemBatchEntity): Long =
            insertInvoiceItemBatch(itemBatch)

        override suspend fun insertAll(itemBatches: List<InvoiceItemBatchEntity>) {
            itemBatches.forEach { insertInvoiceItemBatch(it) }
        }

        override suspend fun upsertInvoiceItemBatch(itemBatch: InvoiceItemBatchEntity) {
            val idx = itemBatches.indexOfFirst { it.id == itemBatch.id }
            if (idx >= 0) itemBatches[idx] = itemBatch else insertInvoiceItemBatch(itemBatch)
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
}
