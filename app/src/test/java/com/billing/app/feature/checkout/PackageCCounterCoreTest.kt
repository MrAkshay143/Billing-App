package com.billing.app.feature.checkout

import com.billing.app.core.cart.CartManager
import com.billing.app.core.util.Money
import com.billing.app.data.local.dao.CategorySalesSummary
import com.billing.app.data.local.dao.TopProductSummary
import com.billing.app.data.local.entity.AuditLogEntity
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.data.local.entity.UserEntity
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.Product
import com.billing.app.domain.model.ShiftZReport
import com.billing.app.domain.model.ShopProfile
import com.billing.app.domain.repository.AuditRepository
import com.billing.app.domain.repository.CategoryRepository
import com.billing.app.domain.repository.CustomerRepository
import com.billing.app.domain.repository.InvoiceRepository
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.ShopRepository
import com.billing.app.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PackageCCounterCoreTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeShopRepo: FakeShopRepository
    private lateinit var fakePrinterRepo: FakePrinterRepository
    private lateinit var fakeInvoiceRepo: FakeInvoiceRepository
    private lateinit var fakeProductRepo: FakeProductRepository
    private lateinit var fakeCustomerRepo: FakeCustomerRepository
    private lateinit var fakeAuditRepo: FakeAuditRepository
    private lateinit var fakeUserRepo: FakeUserRepository
    private lateinit var cartManager: CartManager
    private lateinit var viewModel: CheckoutViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeShopRepo = FakeShopRepository()
        fakePrinterRepo = FakePrinterRepository()
        fakeInvoiceRepo = FakeInvoiceRepository()
        fakeProductRepo = FakeProductRepository()
        fakeCustomerRepo = FakeCustomerRepository()
        fakeAuditRepo = FakeAuditRepository()
        fakeUserRepo = FakeUserRepository()

        cartManager = CartManager(productRepository = fakeProductRepo)
        viewModel = CheckoutViewModel(
            shopRepository = fakeShopRepo,
            printerRepository = fakePrinterRepo,
            invoiceRepository = fakeInvoiceRepo,
            settingsDataStore = null,
            productRepository = fakeProductRepo,
            customerRepository = fakeCustomerRepo,
            categoryRepository = null,
            auditRepository = fakeAuditRepo,
            userRepository = fakeUserRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPoint2_PriceOverrideAndLineDiscountCalculations() {
        val product = Product(
            id = "p1",
            name = "Test Milk",
            barcode = "1111",
            price = Money(6000L), // ₹60.00
            stockMilli = 10000L
        )
        cartManager.addProduct(product, 2000L) // 2 units -> ₹120.00

        // Unit Price override from ₹60 to ₹55 (5500 paise)
        cartManager.overrideItemPrice("p1", 5500L)
        val item = cartManager.cartItems.value.first()
        assertEquals(5500L, item.effectiveUnitPricePaise)
        assertEquals(11000L, item.grossLinePaise) // 2 * 5500 = 11000

        // Line discount of ₹10 (1000 paise)
        cartManager.setItemDiscount("p1", 1000L)
        val discountedItem = cartManager.cartItems.value.first()
        assertEquals(1000L, discountedItem.discountPaise)
        assertEquals(10000L, discountedItem.netLinePaise) // 11000 - 1000 = 10000 paise (₹100)
        assertEquals(10000L, cartManager.subtotal.paise)
    }

    @Test
    fun testPoint2_AuditLogOnPriceOverride() = runBlocking {
        viewModel.logPriceOverride(
            productId = "prod_99",
            productName = "Basmati Rice",
            oldPricePaise = 10000L,
            newPricePaise = 9000L
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeAuditRepo.recordedLogs.size)
        val log = fakeAuditRepo.recordedLogs.first()
        assertEquals("PRICE_OVERRIDE", log.action)
        assertEquals("PRODUCT", log.entityType)
        assertEquals("prod_99", log.entityId)
        assertTrue(log.details.contains("Basmati Rice"))
        assertTrue(log.details.contains("10000"))
        assertTrue(log.details.contains("9000"))
        assertEquals("user_admin", log.userId)
    }

    @Test
    fun testPoint3_CustomItemCreationAndProperties() {
        val customItem = cartManager.addCustomItem(
            name = "Special Service",
            pricePaise = 25000L, // ₹250.00
            quantityMilli = 1500L, // 1.5 units
            unit = "PCS"
        )

        assertTrue(customItem.isCustomItem)
        assertTrue(customItem.isCostUnknown)
        assertEquals("Special Service", customItem.displayName)
        assertEquals(25000L, customItem.effectiveUnitPricePaise)
        assertEquals(1500L, customItem.quantityMilli)
        // 250.00 * 1.5 = 375.00 (37500 paise)
        assertEquals(37500L, customItem.total.paise)
        assertEquals(0L, customItem.product.stockMilli)
        assertFalse(customItem.product.trackBatch)
    }

    @Test
    fun testPoint6_SoftStockWarningVsBatchHardBlock() {
        val nonBatchOut = Product(
            id = "non_batch",
            name = "Open Salt",
            barcode = "SALT01",
            price = Money(1500L),
            stockMilli = 0L,
            trackBatch = false
        )
        val batchOut = Product(
            id = "batch_prod",
            name = "Baby Milk Powder",
            barcode = "MILK01",
            price = Money(45000L),
            stockMilli = 0L,
            trackBatch = true
        )

        // Non-batch with zero stock is allowed (soft warning)
        val (successNonBatch, warning) = cartManager.addProduct(nonBatchOut, 1000L)
        assertTrue(successNonBatch)
        assertNotNull(warning)
        assertTrue(warning!!.contains("negative stock", ignoreCase = true))

        // Batch-tracked with zero stock is hard blocked
        val (successBatch, error) = cartManager.addProduct(batchOut, 1000L)
        assertFalse(successBatch)
        assertNotNull(error)
        assertTrue(error!!.contains("out of stock (batch tracked)", ignoreCase = true))
    }

    @Test
    fun testPoint7_ReactivateCustomer() = runBlocking {
        val inactiveCustomer = CustomerEntity(
            id = "cust_inactive",
            name = "Sunil Kumar",
            phone = "9876543210",
            isActive = false,
            openingBalancePaise = 5000L
        )

        viewModel.reactivateCustomer(inactiveCustomer)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeCustomerRepo.updatedCustomers.size)
        val updated = fakeCustomerRepo.updatedCustomers.first()
        assertEquals("cust_inactive", updated.id)
        assertTrue(updated.isActive)
    }

    @Test
    fun testPoint7_ReactivateProduct() = runBlocking {
        val inactiveProduct = Product(
            id = "prod_inactive",
            name = "Discontinued Biscuit",
            barcode = "BISC01",
            price = Money(2000L),
            isActive = false
        )

        viewModel.reactivateProduct(inactiveProduct)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeProductRepo.updatedProducts.size)
        val updated = fakeProductRepo.updatedProducts.first()
        assertEquals("prod_inactive", updated.id)
        assertTrue(updated.isActive)
    }

    @Test
    fun testPoint10_MultiCartTabs() {
        assertEquals(1, cartManager.tabs.value.size)
        val tab1Id = cartManager.activeTabId.value

        val prod = Product(id = "p1", name = "Soap", barcode = "11", price = Money(4000L), stockMilli = 5000L)
        cartManager.addProduct(prod, 1000L)
        assertEquals(1, cartManager.cartItems.value.size)

        // Add Cart 2
        val tab2Id = cartManager.addNewTab()
        assertEquals(2, cartManager.tabs.value.size)
        assertEquals(tab2Id, cartManager.activeTabId.value)
        assertTrue(cartManager.cartItems.value.isEmpty())

        // Switch to Cart 1
        cartManager.switchTab(tab1Id)
        assertEquals(tab1Id, cartManager.activeTabId.value)
        assertEquals(1, cartManager.cartItems.value.size)

        // Close Cart 2
        cartManager.closeTab(tab2Id)
        assertEquals(1, cartManager.tabs.value.size)
    }

    // --- Fakes ---

    private class FakeAuditRepository : AuditRepository {
        val recordedLogs = mutableListOf<AuditLogEntity>()
        private var nextId = 1L

        override suspend fun recordLog(
            userId: String,
            action: String,
            entityType: String,
            entityId: String,
            details: String
        ): Long {
            val id = nextId++
            recordedLogs.add(
                AuditLogEntity(
                    id = id,
                    userId = userId,
                    action = action,
                    entityType = entityType,
                    entityId = entityId,
                    details = details,
                    timestamp = System.currentTimeMillis()
                )
            )
            return id
        }

        override fun getRecentLogs(limit: Int): Flow<List<AuditLogEntity>> =
            flowOf(recordedLogs.take(limit))
    }

    private class FakeUserRepository : UserRepository {
        private val user = UserEntity(
            id = "user_admin",
            username = "admin",
            displayName = "Admin User",
            pinHash = "dummy",
            pinSalt = "salt123",
            role = "ADMIN"
        )
        override val currentUser: MutableStateFlow<UserEntity?> = MutableStateFlow(user)

        override fun getAllActiveUsers(): Flow<List<UserEntity>> = flowOf(listOf(user))
        override fun getAllUsers(): Flow<List<UserEntity>> = flowOf(listOf(user))
        override suspend fun getUserById(userId: String): UserEntity? = user
        override suspend fun login(userId: String, pin: String): Result<UserEntity> = Result.success(user)
        override fun logout() {}
        override suspend fun createDefaultAdminIfNeeded() {}
        override suspend fun verifyManagerPin(pin: String): Boolean = true
        override suspend fun setUserPin(userId: String, newPin: String) {}
        override suspend fun createUser(username: String, displayName: String, pin: String, role: String): Result<UserEntity> = Result.success(user)
        override suspend fun deleteUser(userId: String) {}
        override suspend fun setUserActiveStatus(userId: String, isActive: Boolean) {}
    }

    private class FakeProductRepository : ProductRepository {
        val updatedProducts = mutableListOf<Product>()
        private val products = mutableListOf<Product>()

        override fun getProducts(): Flow<List<Product>> = flowOf(products)
        override fun getProductsByCategory(category: String): Flow<List<Product>> = flowOf(products)
        override fun searchProducts(query: String, category: String?): Flow<List<Product>> = flowOf(products)
        override fun getTotalCount(): Flow<Int> = flowOf(products.size)
        override fun getLowStockCount(): Flow<Int> = flowOf(0)
        override fun getOutOfStockCount(): Flow<Int> = flowOf(0)
        override fun getAllCategories(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getProductByBarcode(barcode: String): Product? = products.firstOrNull { it.barcode == barcode }
        override suspend fun getProductById(id: String): Product? = products.firstOrNull { it.id == id }
        override suspend fun getProductBySku(sku: String): Product? = products.firstOrNull { it.sku == sku }
        override suspend fun getLowAndOutOfStockProducts(): List<Product> = emptyList()
        override suspend fun addProduct(product: Product): Result<Unit> { products.add(product); return Result.success(Unit) }
        override suspend fun addProducts(products: List<Product>): Result<Unit> { this.products.addAll(products); return Result.success(Unit) }
        override suspend fun updateProduct(product: Product): Result<Unit> {
            updatedProducts.add(product)
            return Result.success(Unit)
        }
        override suspend fun adjustStock(id: String, newStock: Int): Result<Unit> = Result.success(Unit)
        override suspend fun deleteProduct(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeCustomerRepository : CustomerRepository {
        val updatedCustomers = mutableListOf<CustomerEntity>()
        private val customers = mutableListOf<CustomerEntity>()

        override fun getAllCustomers(): Flow<List<CustomerEntity>> = flowOf(customers)
        override fun searchCustomers(query: String): Flow<List<CustomerEntity>> = flowOf(customers)
        override fun getCustomerCount(): Flow<Int> = flowOf(customers.size)
        override suspend fun getCustomerById(id: String): CustomerEntity? = customers.firstOrNull { it.id == id }
        override suspend fun getCustomerByPhone(phone: String): CustomerEntity? = customers.firstOrNull { it.phone == phone }
        override suspend fun addCustomer(customer: CustomerEntity): Result<Unit> { customers.add(customer); return Result.success(Unit) }
        override suspend fun addCustomers(customers: List<CustomerEntity>): Result<Unit> { this.customers.addAll(customers); return Result.success(Unit) }
        override suspend fun updateCustomer(customer: CustomerEntity): Result<Unit> {
            updatedCustomers.add(customer)
            return Result.success(Unit)
        }
        override suspend fun deleteCustomer(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeShopRepository : ShopRepository {
        override fun getShopProfile(): Flow<ShopProfile> = flowOf(ShopProfile())
        override suspend fun getShopProfileSync(): ShopProfile = ShopProfile()
        override suspend fun updateShopProfile(profile: ShopProfile): Result<Unit> = Result.success(Unit)
    }

    private class FakePrinterRepository : PrinterRepository {
        override fun getSavedPrinterMac(): Flow<String?> = flowOf(null)
        override fun getSavedPrinterName(): Flow<String?> = flowOf(null)
        override suspend fun getBondedPrinters(): List<com.billing.app.domain.model.BluetoothPrinterDevice> = emptyList()
        override suspend fun connect(macAddress: String): Result<Unit> = Result.success(Unit)
        override fun disconnect() {}
        override val isConnected: Boolean = false
        override suspend fun savePrinterData(mac: String, name: String) {}
        override suspend fun clearPrinterData() {}
        override suspend fun printReceipt(shop: ShopProfile, items: List<CartItem>): Result<Unit> = Result.success(Unit)
        override suspend fun printInvoiceReceipt(receipt: com.billing.app.domain.model.InvoiceReceipt): Result<Unit> = Result.success(Unit)
        override suspend fun printInvoice(
            invoice: InvoiceEntity,
            items: List<InvoiceItemEntity>,
            shop: ShopProfile
        ): Result<Unit> = Result.success(Unit)
        override suspend fun printTestReceipt(shop: ShopProfile): Result<Unit> = Result.success(Unit)
        override suspend fun printZReport(zReport: ShiftZReport): Result<Unit> = Result.success(Unit)
    }

    private class FakeInvoiceRepository : InvoiceRepository {
        override fun getAllInvoices(): Flow<List<InvoiceEntity>> = flowOf(emptyList())
        override fun getInvoicesByType(type: String): Flow<List<InvoiceEntity>> = flowOf(emptyList())
        override fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>> = flowOf(emptyList())
        override fun getInvoicesSince(startTime: Long): Flow<List<InvoiceEntity>> = flowOf(emptyList())
        override fun getInvoicesBetween(startTime: Long, endTime: Long): Flow<List<InvoiceEntity>> = flowOf(emptyList())
        override fun searchInvoices(query: String, status: InvoiceStatus?, type: String?): Flow<List<InvoiceEntity>> = flowOf(emptyList())
        override fun getTotalInvoiceCount(): Flow<Int> = flowOf(0)
        override fun getInvoiceCountBetween(startTime: Long, endTime: Long): Flow<Int> = flowOf(0)
        override fun getTotalSalesBetween(startTime: Long, endTime: Long): Flow<Long> = flowOf(0L)
        override fun getTotalItemsSoldBetween(startTime: Long, endTime: Long): Flow<Int> = flowOf(0)
        override fun getTotalItemsSoldAllTime(): Flow<Int> = flowOf(0)
        override fun getAverageBillBetween(startTime: Long, endTime: Long): Flow<Double> = flowOf(0.0)
        override fun getCreditDuesInvoices(): Flow<List<InvoiceEntity>> = flowOf(emptyList())
        override fun getTotalDuePaise(): Flow<Long> = flowOf(0L)
        override fun getPendingBillBetween(startTime: Long, endTime: Long): Flow<Long> = flowOf(0L)
        override fun getInvoicesByCustomerPhone(phone: String): Flow<List<InvoiceEntity>> = flowOf(emptyList())
        override fun getTopSellingProducts(startTime: Long, endTime: Long, limit: Int): Flow<List<TopProductSummary>> = flowOf(emptyList())
        override fun getSalesByCategory(startTime: Long, endTime: Long): Flow<List<CategorySalesSummary>> = flowOf(emptyList())
        override suspend fun getInvoiceByNumber(invoiceNumber: String): InvoiceEntity? = null
        override suspend fun getInvoiceItems(invoiceNumber: String): List<InvoiceItemEntity> = emptyList()
        override suspend fun getAllInvoiceItems(): List<InvoiceItemEntity> = emptyList()
        override suspend fun generateNextInvoiceNumber(): String = "INV-000001"
        override suspend fun completeSale(invoice: InvoiceEntity, items: List<InvoiceItemEntity>, draftInvoiceNumberToDelete: String?): Result<InvoiceEntity> = Result.success(invoice)
        override suspend fun saveDraft(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): Result<InvoiceEntity> = Result.success(invoice)
        override suspend fun voidInvoice(invoiceNumber: String): Result<Unit> = Result.success(Unit)
        override suspend fun settleInvoiceDue(invoiceNumber: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteInvoice(invoiceNumber: String): Result<Unit> = Result.success(Unit)
        override suspend fun upsertInvoicesAndItems(invoices: List<InvoiceEntity>, items: List<InvoiceItemEntity>): Result<Unit> = Result.success(Unit)
    }
}
