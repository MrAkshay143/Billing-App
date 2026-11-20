package com.billing.app.feature.purchases

import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.PurchaseItemEntity
import com.billing.app.data.local.entity.ShiftEntity
import com.billing.app.data.local.entity.SupplierEntity
import com.billing.app.data.local.entity.SupplierLedgerEntity
import com.billing.app.domain.model.Product
import com.billing.app.domain.model.ShiftZReport
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.PurchaseRepository
import com.billing.app.domain.repository.ShiftRepository
import com.billing.app.domain.repository.SupplierRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PurchasesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePurchaseRepo: FakePurchaseRepository
    private lateinit var fakeProductRepo: FakeProductRepository
    private lateinit var fakeSupplierRepo: FakeSupplierRepository
    private lateinit var fakeShiftRepo: FakeShiftRepository
    private lateinit var viewModel: PurchasesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePurchaseRepo = FakePurchaseRepository()
        fakeProductRepo = FakeProductRepository()
        fakeSupplierRepo = FakeSupplierRepository()
        fakeShiftRepo = FakeShiftRepository()

        viewModel = PurchasesViewModel(
            purchaseRepository = fakePurchaseRepo,
            productRepository = fakeProductRepo,
            supplierRepository = fakeSupplierRepo,
            shiftRepository = fakeShiftRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createProduct(id: String, name: String, trackBatch: Boolean = false): Product {
        return Product(
            id = id,
            name = name,
            barcode = "8901234567890",
            price = Money.fromRupees(150.0),
            stockMilli = 10000L,
            unit = "PCS",
            purchasePrice = Money.fromRupees(100.0),
            mrp = Money.fromRupees(160.0),
            trackBatch = trackBatch
        )
    }

    @Test
    fun addingItem_calculatesTotalsCorrectly_taxInclusive() {
        val product = createProduct("prod_1", "Sunflower Oil", trackBatch = false)
        viewModel.setTaxInclusive(true)

        val draft = PurchaseItemDraft(
            product = product,
            quantityMilli = 1000L, // 1 unit
            unitPricePaise = 11800L, // ₹118.00 inclusive of 18% GST
            gstRateBp = 1800
        )

        val added = viewModel.addItem(draft)
        assertTrue(added)

        val (subtotal, tax, grandTotal) = viewModel.calculateTotals()
        assertEquals(11800L, grandTotal)
        assertEquals(10000L, subtotal)
        assertEquals(1800L, tax)
        assertEquals(11800L, viewModel.paidAmountPaise.value)
    }

    @Test
    fun addingItem_calculatesTotalsCorrectly_taxExclusive() {
        val product = createProduct("prod_1", "Basmati Rice", trackBatch = false)
        viewModel.setTaxInclusive(false)

        val draft = PurchaseItemDraft(
            product = product,
            quantityMilli = 2000L, // 2 units
            unitPricePaise = 10000L, // ₹100.00 base price
            gstRateBp = 500 // 5% GST
        )

        val added = viewModel.addItem(draft)
        assertTrue(added)

        val (subtotal, tax, grandTotal) = viewModel.calculateTotals()
        // Base = 2 * 10000 = 20000 paise
        // Tax = 5% of 20000 = 1000 paise
        // GrandTotal = 21000 paise
        assertEquals(20000L, subtotal)
        assertEquals(1000L, tax)
        assertEquals(21000L, grandTotal)
    }

    @Test
    fun batchProduct_requiresCompleteBatchDetails() {
        val product = createProduct("prod_batch", "Paracetamol 500mg", trackBatch = true)

        // Missing batch number
        val draftNoBatch = PurchaseItemDraft(
            product = product,
            quantityMilli = 1000L,
            unitPricePaise = 5000L,
            batchNumber = "",
            expiryDate = System.currentTimeMillis() + 86400000L * 30,
            mrpPaise = 6500L
        )
        assertFalse(viewModel.addItem(draftNoBatch))
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Batch number"))

        // Missing expiry date
        val draftNoExpiry = PurchaseItemDraft(
            product = product,
            quantityMilli = 1000L,
            unitPricePaise = 5000L,
            batchNumber = "BATCH-001",
            expiryDate = null,
            mrpPaise = 6500L
        )
        assertFalse(viewModel.addItem(draftNoExpiry))
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Expiry date"))

        // Missing MRP
        val draftNoMrp = PurchaseItemDraft(
            product = product,
            quantityMilli = 1000L,
            unitPricePaise = 5000L,
            batchNumber = "BATCH-001",
            expiryDate = System.currentTimeMillis() + 86400000L * 30,
            mrpPaise = 0L
        )
        assertFalse(viewModel.addItem(draftNoMrp))
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("MRP"))

        // Valid complete batch
        val draftValid = PurchaseItemDraft(
            product = product,
            quantityMilli = 1000L,
            unitPricePaise = 5000L,
            batchNumber = "BATCH-001",
            expiryDate = System.currentTimeMillis() + 86400000L * 30,
            mrpPaise = 6500L
        )
        assertTrue(viewModel.addItem(draftValid))
        assertNull(viewModel.errorMessage.value)
        assertEquals(1, viewModel.items.value.size)
    }

    @Test
    fun savePurchase_requiresActiveShift_whenPayingCash() = runTest(testDispatcher) {
        val supplier = SupplierEntity(id = "sup_1", name = "City Wholesalers", phone = "9876543210")
        val product = createProduct("prod_1", "Sugar", trackBatch = false)

        viewModel.setSelectedSupplier(supplier)
        viewModel.setInvoiceNumber("INV-1001")
        viewModel.setPaymentMode("CASH")
        viewModel.addItem(PurchaseItemDraft(product = product, quantityMilli = 1000L, unitPricePaise = 4000L))

        // Ensure no active shift exists
        fakeShiftRepo.activeShift = null

        var successCalled = false
        viewModel.savePurchase { successCalled = true }
        advanceUntilIdle()

        assertFalse(successCalled)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Active shift"))
        assertNull(fakePurchaseRepo.lastAddedPurchase)
    }

    @Test
    fun savePurchase_succeeds_whenActiveShiftPresent_forCash() = runTest(testDispatcher) {
        val supplier = SupplierEntity(id = "sup_1", name = "City Wholesalers", phone = "9876543210")
        val product = createProduct("prod_1", "Sugar", trackBatch = false)

        viewModel.setSelectedSupplier(supplier)
        viewModel.setInvoiceNumber("INV-1002")
        viewModel.setPaymentMode("CASH")
        viewModel.addItem(PurchaseItemDraft(product = product, quantityMilli = 1000L, unitPricePaise = 4000L))

        // Set active shift
        val activeShift = ShiftEntity(id = 88L, userId = "user_admin", openingFloatPaise = 50000L)
        fakeShiftRepo.activeShift = activeShift

        var successCalled = false
        viewModel.savePurchase { successCalled = true }
        advanceUntilIdle()

        assertTrue(successCalled)
        assertNull(viewModel.errorMessage.value)
        assertNotNull(fakePurchaseRepo.lastAddedPurchase)

        val saved = fakePurchaseRepo.lastAddedPurchase!!
        assertEquals("INV-1002", saved.purchaseNumber)
        assertEquals("sup_1", saved.supplierId)
        assertEquals(88L, saved.shiftId)
        assertEquals(4000L, saved.totalPaise)
        assertEquals(4000L, saved.paidPaise)
        assertEquals(0L, saved.duePaise)
        assertEquals("PAID", saved.paymentStatus)
        assertEquals("CASH", fakePurchaseRepo.lastPaymentMode)

        // Draft should be cleared
        assertTrue(viewModel.items.value.isEmpty())
        assertNull(viewModel.selectedSupplier.value)
        assertEquals("", viewModel.invoiceNumber.value)
    }

    @Test
    fun savePurchase_succeeds_forCredit_withoutActiveShift() = runTest(testDispatcher) {
        val supplier = SupplierEntity(id = "sup_2", name = "Grain Merchant", phone = "9123456780")
        val product = createProduct("prod_2", "Wheat", trackBatch = false)

        viewModel.setSelectedSupplier(supplier)
        viewModel.setInvoiceNumber("INV-CREDIT-99")
        viewModel.addItem(PurchaseItemDraft(product = product, quantityMilli = 2000L, unitPricePaise = 3000L))
        viewModel.setPaymentMode("CREDIT")

        // No active shift needed for credit
        fakeShiftRepo.activeShift = null

        var successCalled = false
        viewModel.savePurchase { successCalled = true }
        advanceUntilIdle()

        assertTrue(successCalled)
        val saved = fakePurchaseRepo.lastAddedPurchase!!
        assertEquals("INV-CREDIT-99", saved.purchaseNumber)
        assertEquals(6000L, saved.totalPaise)
        assertEquals(0L, saved.paidPaise)
        assertEquals(6000L, saved.duePaise)
        assertEquals("DUE", saved.paymentStatus)
        assertEquals("CREDIT", fakePurchaseRepo.lastPaymentMode)
        assertNull(saved.shiftId)
    }

    @Test
    fun savePurchase_delegatesBatchDetails_toPurchaseItems() = runTest(testDispatcher) {
        val supplier = SupplierEntity(id = "sup_3", name = "Pharma Dist", phone = "9988776655")
        val product = createProduct("prod_med", "Cough Syrup", trackBatch = true)
        val expDate = System.currentTimeMillis() + 86400000L * 60

        viewModel.setSelectedSupplier(supplier)
        viewModel.setInvoiceNumber("INV-BATCH-01")
        viewModel.setPaymentMode("UPI")
        viewModel.addItem(
            PurchaseItemDraft(
                product = product,
                quantityMilli = 5000L, // 5 units
                unitPricePaise = 8000L, // ₹80.00
                gstRateBp = 1200,
                mrpPaise = 11000L,
                batchNumber = "BATCH-MED-99",
                expiryDate = expDate
            )
        )

        var successCalled = false
        viewModel.savePurchase { successCalled = true }
        advanceUntilIdle()

        assertTrue(successCalled)
        assertNotNull(fakePurchaseRepo.lastAddedItems)
        val items = fakePurchaseRepo.lastAddedItems!!
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("prod_med", item.productId)
        assertEquals("Cough Syrup", item.productName)
        assertEquals(5000L, item.quantityMilli)
        assertEquals(8000L, item.unitPricePaise)
        assertEquals("BATCH-MED-99", item.batchNumber)
        assertEquals(expDate, item.expiryDate)
        assertEquals(11000L, item.mrpPaise)
    }

    // Fake Repositories
    class FakePurchaseRepository : PurchaseRepository {
        val purchasesList = mutableListOf<PurchaseEntity>()
        val purchaseItemsMap = mutableMapOf<Long, List<PurchaseItemEntity>>()
        var lastAddedPurchase: PurchaseEntity? = null
        var lastAddedItems: List<PurchaseItemEntity>? = null
        var lastPaymentMode: String? = null
        private var nextId = 1L

        override fun getAllPurchases(): Flow<List<PurchaseEntity>> = flowOf(purchasesList)
        override fun getPurchaseCount(): Flow<Int> = flowOf(purchasesList.size)
        override suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItemEntity> =
            purchaseItemsMap[purchaseId] ?: emptyList()

        override suspend fun addPurchase(
            purchase: PurchaseEntity,
            items: List<PurchaseItemEntity>,
            paymentMode: String
        ): Result<Long> {
            val id = nextId++
            val saved = purchase.copy(id = id)
            purchasesList.add(saved)
            purchaseItemsMap[id] = items.map { it.copy(purchaseId = id) }
            lastAddedPurchase = saved
            lastAddedItems = items
            lastPaymentMode = paymentMode
            return Result.success(id)
        }

        override suspend fun recordSupplierPayment(
            supplierId: String,
            amountPaise: Long,
            paymentMode: String,
            shiftId: Long?,
            note: String?
        ): Result<Unit> = Result.success(Unit)

        override fun getSupplierLedger(supplierId: String): Flow<List<SupplierLedgerEntity>> = flowOf(emptyList())
        override fun getSupplierBalance(supplierId: String): Flow<Long> = flowOf(0L)
    }

    class FakeProductRepository : ProductRepository {
        val products = mutableListOf<Product>()
        override fun getProducts(): Flow<List<Product>> = flowOf(products)
        override fun getProductsByCategory(category: String): Flow<List<Product>> = flowOf(emptyList())
        override fun searchProducts(query: String, category: String?): Flow<List<Product>> = flowOf(emptyList())
        override fun getTotalCount(): Flow<Int> = flowOf(products.size)
        override fun getLowStockCount(): Flow<Int> = flowOf(0)
        override fun getOutOfStockCount(): Flow<Int> = flowOf(0)
        override fun getAllCategories(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getProductByBarcode(barcode: String): Product? = products.find { it.barcode == barcode }
        override suspend fun getProductById(id: String): Product? = products.find { it.id == id }
        override suspend fun getProductBySku(sku: String): Product? = products.find { it.sku == sku }
        override suspend fun getLowAndOutOfStockProducts(): List<Product> = emptyList()
        override suspend fun addProduct(product: Product): Result<Unit> { products.add(product); return Result.success(Unit) }
        override suspend fun addProducts(products: List<Product>): Result<Unit> { this.products.addAll(products); return Result.success(Unit) }
        override suspend fun updateProduct(product: Product): Result<Unit> = Result.success(Unit)
        override suspend fun adjustStock(id: String, newStock: Int): Result<Unit> = Result.success(Unit)
        override suspend fun deleteProduct(id: String): Result<Unit> = Result.success(Unit)
    }

    class FakeSupplierRepository : SupplierRepository {
        val suppliers = mutableListOf<SupplierEntity>()
        override fun getAllSuppliers(): Flow<List<SupplierEntity>> = flowOf(suppliers)
        override fun searchSuppliers(query: String): Flow<List<SupplierEntity>> = flowOf(emptyList())
        override fun getSupplierCount(): Flow<Int> = flowOf(suppliers.size)
        override suspend fun getSupplierById(id: String): SupplierEntity? = suppliers.find { it.id == id }
        override suspend fun addSupplier(supplier: SupplierEntity): Result<Unit> { suppliers.add(supplier); return Result.success(Unit) }
        override suspend fun updateSupplier(supplier: SupplierEntity): Result<Unit> = Result.success(Unit)
        override suspend fun deleteSupplier(id: String): Result<Unit> = Result.success(Unit)
    }

    class FakeShiftRepository : ShiftRepository {
        var activeShift: ShiftEntity? = null
        override fun getActiveShiftFlow(): Flow<ShiftEntity?> = flowOf(activeShift)
        override suspend fun getActiveShift(): ShiftEntity? = activeShift
        override suspend fun openShift(userId: String, openingFloatPaise: Long, note: String?): Result<ShiftEntity> {
            val s = ShiftEntity(id = 1L, userId = userId, openingFloatPaise = openingFloatPaise)
            activeShift = s
            return Result.success(s)
        }
        override suspend fun closeShift(actualCashPaise: Long, note: String?): Result<ShiftEntity> {
            val s = activeShift ?: return Result.failure(Exception("No active shift"))
            activeShift = null
            return Result.success(s)
        }
        override suspend fun recordCashMovement(
            movementType: String,
            amountPaise: Long,
            referenceId: String?,
            reason: String?
        ): Result<Long> = Result.success(1L)
        override fun getMovementsForActiveShift(): Flow<List<CashMovementEntity>> = flowOf(emptyList())
        override suspend fun getExpectedCashForActiveShift(): Long = 0L
        override suspend fun generateZReport(shiftId: Long): Result<ShiftZReport> = Result.failure(NotImplementedError())
    }
}
