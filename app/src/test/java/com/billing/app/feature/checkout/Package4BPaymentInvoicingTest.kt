package com.billing.app.feature.checkout

import com.billing.app.core.cart.CartManager
import com.billing.app.core.printer.EscPosFormatter
import com.billing.app.core.qr.UpiQrGenerator
import com.billing.app.core.util.Money
import com.billing.app.core.util.TaxEngine
import com.billing.app.data.local.dao.CategorySalesSummary
import com.billing.app.data.local.dao.TopProductSummary
import com.billing.app.data.local.entity.AuditLogEntity
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.data.local.entity.ShiftEntity
import com.billing.app.data.local.entity.UserEntity
import com.billing.app.domain.model.BluetoothPrinterDevice
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.InvoiceReceipt
import com.billing.app.domain.model.Product
import com.billing.app.domain.model.ReceiptItem
import com.billing.app.domain.model.ShiftZReport
import com.billing.app.domain.model.ShopProfile
import com.billing.app.domain.repository.AuditRepository
import com.billing.app.domain.repository.CustomerRepository
import com.billing.app.domain.repository.InvoiceRepository
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.ShiftRepository
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
import java.net.URLDecoder

@OptIn(ExperimentalCoroutinesApi::class)
class Package4BPaymentInvoicingTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeShopRepo: FakeShopRepository
    private lateinit var fakePrinterRepo: FakePrinterRepository
    private lateinit var fakeInvoiceRepo: FakeInvoiceRepository
    private lateinit var fakeProductRepo: FakeProductRepository
    private lateinit var fakeCustomerRepo: FakeCustomerRepository
    private lateinit var fakeAuditRepo: FakeAuditRepository
    private lateinit var fakeUserRepo: FakeUserRepository
    private lateinit var fakeShiftRepo: FakeShiftRepository
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
        fakeShiftRepo = FakeShiftRepository()

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
            userRepository = fakeUserRepo,
            shiftRepository = fakeShiftRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPoint5_ItemAndBillLevelDiscountProRataUsingTaxEngine() {
        // Setup 2 items with 5% (500 bp) and 18% (1800 bp) GST
        val prod1 = Product(
            id = "p1",
            name = "Sugar 1kg",
            barcode = "111",
            price = Money(10000L), // ₹100.00
            gstRateBp = 500,
            purchasePrice = Money(8000L) // ₹80.00 cost
        )
        val prod2 = Product(
            id = "p2",
            name = "Coffee 500g",
            barcode = "222",
            price = Money(30000L), // ₹300.00
            gstRateBp = 1800,
            purchasePrice = Money(22000L) // ₹220.00 cost
        )

        val items = listOf(
            CartItem(product = prod1, quantity = 1),
            CartItem(product = prod2, quantity = 1)
        )
        // Subtotal = 10000 + 30000 = 40000 paise (₹400)
        // Apply bill discount = ₹40 (4000 paise)
        val billDiscountPaise = 4000L
        val result = viewModel.calculateBillResult(items, billDiscountPaise)

        assertEquals(40000L, result.subtotalPaise)
        assertEquals(4000L, result.billDiscountPaise)
        assertEquals(2, result.lineCalculations.size)

        // Item 1 discount share: 4000 * 10000 / 40000 = 1000 paise (₹10)
        // Item 2 discount share: 4000 - 1000 = 3000 paise (₹30, remainder absorbed)
        val line1 = result.lineCalculations[0]
        val line2 = result.lineCalculations[1]
        assertEquals(1000L, line1.billDiscountSharePaise)
        assertEquals(3000L, line2.billDiscountSharePaise)

        // Net line gross: 10000 - 1000 = 9000 paise, 30000 - 3000 = 27000 paise
        assertEquals(9000L, line1.netLineGrossPaise)
        assertEquals(27000L, line2.netLineGrossPaise)

        // Tax calculation check (with default gstRateBp applied)
        assertTrue(line1.taxPaise >= 0L)
        assertTrue(line2.taxPaise >= 0L)
        assertEquals(line1.cgstPaise + line1.sgstPaise, line1.taxPaise)
        assertEquals(line2.cgstPaise + line2.sgstPaise, line2.taxPaise)

        // Grand total is rounded to nearest Rupee
        assertEquals(0L, result.grandTotalPaise % 100L)
    }

    @Test
    fun testPoint8_UpiQrGeneratorValidAndInvalidVpa() {
        assertTrue(UpiQrGenerator.isValidUpiId("merchant@okaxis"))
        assertTrue(UpiQrGenerator.isValidUpiId("store.shop@upi"))
        assertTrue(UpiQrGenerator.isValidUpiId("9876543210@paytm"))
        assertFalse(UpiQrGenerator.isValidUpiId("invalid-upi-without-at"))
        assertFalse(UpiQrGenerator.isValidUpiId(""))
        assertFalse(UpiQrGenerator.isValidUpiId("@invalid"))
    }

    @Test
    fun testPoint9_SplitPaymentAndNetCashCalculation() = runBlocking {
        val prod = Product(id = "p1", name = "Ghee", barcode = "11", price = Money(50000L), purchasePrice = Money(42000L))
        val items = listOf(CartItem(product = prod, quantity = 1))

        // Split payment: ₹300 cash (30000 paise) + ₹200 UPI (20000 paise) for ₹500 bill
        viewModel.setPaymentMethod(PaymentMethodMode.SPLIT)
        viewModel.setSplitPayment(cashPaise = 30000L, upiPaise = 20000L)

        val (due, change) = viewModel.calculateDueAndChange(50000L)
        assertEquals(0L, due)
        assertEquals(0L, change)

        var completed = false
        viewModel.completeSale(items, Money(50000L)) {
            completed = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(completed)
        val invoice = fakeInvoiceRepo.lastCompletedInvoice
        assertNotNull(invoice)
        assertEquals("SPLIT", invoice!!.paymentMethod)
        assertEquals(30000L, invoice.cashReceivedPaise)
        assertEquals(20000L, invoice.upiPaidPaise)
        assertEquals(0L, invoice.duePaise)
        assertEquals(0L, invoice.changeReturnedPaise)
    }

    @Test
    fun testPoint13_WhatsAppReceiptUrlGeneration() = runBlocking {
        val prod = Product(id = "p1", name = "Atta 5kg", barcode = "11", price = Money(25000L))
        val items = listOf(CartItem(product = prod, quantity = 1))

        viewModel.setCustomer("Rahul Sharma", "9876543210")
        viewModel.setPaymentMethod(PaymentMethodMode.UPI)

        viewModel.completeSale(items, Money(25000L)) {}
        testDispatcher.scheduler.advanceUntilIdle()

        val waUrl = viewModel.getLastInvoiceWhatsAppUrl()
        assertNotNull(waUrl)
        assertTrue(waUrl!!.startsWith("https://wa.me/919876543210?text="))

        val rawEncoded = waUrl.substringAfter("text=")
        val decoded = URLDecoder.decode(rawEncoded, "UTF-8")
        assertTrue(decoded.contains("Atta 5kg"))
        assertTrue(decoded.contains("250.00"))
        assertTrue(decoded.contains("Rahul Sharma"))
        assertTrue(decoded.contains("Payment: UPI"))
    }

    @Test
    fun testPoint16_CostSnapshotRecordedIntoInvoiceItems() = runBlocking {
        val prod1 = Product(id = "p1", name = "Oil", barcode = "1", price = Money(15000L), purchasePrice = Money(12000L))
        val customItem = CartItem(
            product = Product(id = "c1", name = "Loose Masala", barcode = "", price = Money(5000L)),
            isCustomItem = true,
            customPricePaise = 5000L
        )
        val items = listOf(
            CartItem(product = prod1, quantity = 1),
            customItem
        )

        viewModel.completeSale(items, Money(20000L)) {}
        testDispatcher.scheduler.advanceUntilIdle()

        val savedItems = fakeInvoiceRepo.lastCompletedItems
        assertEquals(2, savedItems.size)
        // Product 1 cost snapshot: 12000 paise (₹120)
        assertEquals(12000L, savedItems[0].unitCostPaise)
        // Custom item cost: 0L
        assertEquals(0L, savedItems[1].unitCostPaise)
    }

    @Test
    fun testPoint24_CustomerIdBinding() = runBlocking {
        val customer = CustomerEntity(id = "cust-12345", name = "Vikram Patel", phone = "9123456789")
        fakeCustomerRepo.addCustomer(customer)

        viewModel.setCustomer("Vikram Patel", "9123456789", id = customer.id)
        val prod = Product(id = "p1", name = "Tea", barcode = "1", price = Money(10000L))
        viewModel.completeSale(listOf(CartItem(product = prod, quantity = 1)), Money(10000L)) {}
        testDispatcher.scheduler.advanceUntilIdle()

        val invoice = fakeInvoiceRepo.lastCompletedInvoice
        assertNotNull(invoice)
        assertEquals("cust-12345", invoice!!.customerId)
        assertEquals("Vikram Patel", invoice.customerName)
    }

    @Test
    fun testPoint25_CreditLimitCheckAndManagerAuthorization() = runBlocking {
        // Customer with limit ₹500 (50000 paise), opening balance ₹400 (40000 paise)
        val customer = CustomerEntity(
            id = "cust-credit-1",
            name = "Amit Kumar",
            phone = "9988776655",
            creditLimitPaise = 50000L,
            openingBalancePaise = 40000L
        )
        fakeCustomerRepo.addCustomer(customer)
        viewModel.setCustomer(customer.name, customer.phone, customer.id)

        // Attempt to bill ₹200 (20000 paise) as Udhaar -> Total balance would become ₹600 > ₹500 limit
        viewModel.setPaymentMethod(PaymentMethodMode.OTHER)
        viewModel.setCashReceived(0L) // 0 paid -> 20000 paise due

        val prod = Product(id = "p1", name = "Basmati Rice", barcode = "1", price = Money(20000L))
        val items = listOf(CartItem(product = prod, quantity = 1))

        // 1. Without manager authorization flag -> Should fail
        var completedWithoutAuth = false
        viewModel.completeSale(items, Money(20000L), managerApprovedCreditLimit = false) {
            completedWithoutAuth = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(completedWithoutAuth)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Credit limit exceeded"))

        // 2. With manager authorization flag -> Should succeed
        var completedWithAuth = false
        viewModel.completeSale(items, Money(20000L), managerApprovedCreditLimit = true) {
            completedWithAuth = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(completedWithAuth)
        val invoice = fakeInvoiceRepo.lastCompletedInvoice
        assertNotNull(invoice)
        assertEquals(20000L, invoice!!.duePaise)
        assertEquals("cust-credit-1", invoice.customerId)
    }

    @Test
    fun testPoint31_CompositionBillOfSupplyVsTaxInvoiceEscPosFormatting() {
        val items = listOf(
            ReceiptItem(name = "Dal 1kg", quantityMilli = 1000L, unitCode = "PCS", unitPricePaise = 12000L, lineTotalPaise = 12000L)
        )

        // Case 1: Composition Bill of Supply
        val compReceipt = InvoiceReceipt(
            shopName = "Sharma Kirana",
            invoiceNumber = "INV-001",
            timestamp = 1700000000000L,
            items = items,
            subtotalPaise = 12000L,
            grandTotalPaise = 12000L,
            receiptType = "Bill of Supply",
            cashierName = "Ravi Cashier",
            roundOffPaise = 25L,
            upiPaidPaise = 12000L
        )
        val compBytes = EscPosFormatter.formatInvoiceText(compReceipt, columns = 32)
        val compText = String(compBytes, Charsets.US_ASCII)

        assertTrue(compText.contains("Bill of Supply"))
        assertTrue(compText.contains("Composition Taxable Person, not eligible to collect tax"))
        assertTrue(compText.contains("Cashier: Ravi Cashier"))
        assertTrue(compText.contains("Round Off: + Rs. 0.25"))
        assertTrue(compText.contains("UPI Paid: Rs. 120.00"))

        // Case 2: Standard Tax Invoice
        val taxReceipt = compReceipt.copy(receiptType = "Tax Invoice", taxPaise = 600L)
        val taxBytes = EscPosFormatter.formatInvoiceText(taxReceipt, columns = 32)
        val taxText = String(taxBytes, Charsets.US_ASCII)

        assertTrue(taxText.contains("Tax Invoice"))
        assertFalse(taxText.contains("Composition Taxable Person"))
        assertTrue(taxText.contains("Tax / GST: Rs. 6.00"))
    }

    @Test
    fun testMandatoryActiveShiftAndCashierBinding() = runBlocking {
        val prod = Product(id = "p1", name = "Biscuits", barcode = "1", price = Money(2000L))
        val items = listOf(CartItem(product = prod, quantity = 1))

        // 1. Shift is null -> Should fail
        fakeShiftRepo.currentShift = null
        var completed = false
        viewModel.completeSale(items, Money(2000L)) {
            completed = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(completed)
        assertTrue(viewModel.errorMessage.value!!.contains("Active shift required"))

        // 2. Shift is present -> Should succeed and bind shiftId and cashierId
        fakeShiftRepo.currentShift = ShiftEntity(id = 88L, userId = "user_admin", openingFloatPaise = 50000L)
        viewModel.completeSale(items, Money(2000L)) {
            completed = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(completed)
        val invoice = fakeInvoiceRepo.lastCompletedInvoice
        assertNotNull(invoice)
        assertEquals(88L, invoice!!.shiftId)
        assertEquals("user_admin", invoice.createdByUserId)

        val receipt = viewModel.getLastInvoiceReceipt()
        assertNotNull(receipt)
        assertEquals("Admin User", receipt!!.cashierName)
    }

    // --- Fakes ---

    private class FakeShiftRepository(
        var currentShift: ShiftEntity? = ShiftEntity(id = 1L, userId = "user_admin", openingFloatPaise = 100000L)
    ) : ShiftRepository {
        override fun getActiveShiftFlow(): Flow<ShiftEntity?> = flowOf(currentShift)
        override suspend fun getActiveShift(): ShiftEntity? = currentShift
        override suspend fun openShift(userId: String, openingFloatPaise: Long, note: String?): Result<ShiftEntity> {
            val shift = ShiftEntity(id = 2L, userId = userId, openingFloatPaise = openingFloatPaise, note = note)
            currentShift = shift
            return Result.success(shift)
        }
        override suspend fun closeShift(actualCashPaise: Long, note: String?): Result<ShiftEntity> {
            val shift = currentShift!!.copy(closedAt = System.currentTimeMillis(), actualCashPaise = actualCashPaise)
            currentShift = null
            return Result.success(shift)
        }
        override suspend fun recordCashMovement(movementType: String, amountPaise: Long, referenceId: String?, reason: String?): Result<Long> = Result.success(1L)
        override fun getMovementsForActiveShift(): Flow<List<CashMovementEntity>> = flowOf(emptyList())
        override suspend fun getExpectedCashForActiveShift(): Long = 100000L
        override suspend fun generateZReport(shiftId: Long): Result<ShiftZReport> = Result.failure(NotImplementedError())
    }

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
        override suspend fun updateProduct(product: Product): Result<Unit> = Result.success(Unit)
        override suspend fun adjustStock(id: String, newStock: Int): Result<Unit> = Result.success(Unit)
        override suspend fun deleteProduct(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeCustomerRepository : CustomerRepository {
        private val customers = mutableListOf<CustomerEntity>()

        override fun getAllCustomers(): Flow<List<CustomerEntity>> = flowOf(customers)
        override fun searchCustomers(query: String): Flow<List<CustomerEntity>> = flowOf(customers)
        override fun getCustomerCount(): Flow<Int> = flowOf(customers.size)
        override suspend fun getCustomerById(id: String): CustomerEntity? = customers.firstOrNull { it.id == id }
        override suspend fun getCustomerByPhone(phone: String): CustomerEntity? = customers.firstOrNull { it.phone == phone }
        override suspend fun addCustomer(customer: CustomerEntity): Result<Unit> { customers.add(customer); return Result.success(Unit) }
        override suspend fun addCustomers(customers: List<CustomerEntity>): Result<Unit> { this.customers.addAll(customers); return Result.success(Unit) }
        override suspend fun updateCustomer(customer: CustomerEntity): Result<Unit> {
            val idx = customers.indexOfFirst { it.id == customer.id }
            if (idx >= 0) customers[idx] = customer else customers.add(customer)
            return Result.success(Unit)
        }
        override suspend fun deleteCustomer(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeShopRepository : ShopRepository {
        override fun getShopProfile(): Flow<ShopProfile> = flowOf(ShopProfile(name = "Sharma Kirana", phoneNumber = "9876543210", upiId = "sharma@upi"))
        override suspend fun getShopProfileSync(): ShopProfile = ShopProfile(name = "Sharma Kirana", phoneNumber = "9876543210", upiId = "sharma@upi")
        override suspend fun updateShopProfile(profile: ShopProfile): Result<Unit> = Result.success(Unit)
    }

    private class FakePrinterRepository : PrinterRepository {
        override fun getSavedPrinterMac(): Flow<String?> = flowOf(null)
        override fun getSavedPrinterName(): Flow<String?> = flowOf(null)
        override suspend fun getBondedPrinters(): List<BluetoothPrinterDevice> = emptyList()
        override suspend fun connect(macAddress: String): Result<Unit> = Result.success(Unit)
        override fun disconnect() {}
        override val isConnected: Boolean = false
        override suspend fun savePrinterData(mac: String, name: String) {}
        override suspend fun clearPrinterData() {}
        override suspend fun printReceipt(shop: ShopProfile, items: List<CartItem>): Result<Unit> = Result.success(Unit)
        override suspend fun printInvoiceReceipt(receipt: InvoiceReceipt): Result<Unit> = Result.success(Unit)
        override suspend fun printInvoice(
            invoice: InvoiceEntity,
            items: List<InvoiceItemEntity>,
            shop: ShopProfile
        ): Result<Unit> = Result.success(Unit)
        override suspend fun printTestReceipt(shop: ShopProfile): Result<Unit> = Result.success(Unit)
        override suspend fun printZReport(zReport: ShiftZReport): Result<Unit> = Result.success(Unit)
    }

    private class FakeInvoiceRepository : InvoiceRepository {
        var lastCompletedInvoice: InvoiceEntity? = null
        var lastCompletedItems: List<InvoiceItemEntity> = emptyList()

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
        override suspend fun completeSale(invoice: InvoiceEntity, items: List<InvoiceItemEntity>, draftInvoiceNumberToDelete: String?): Result<InvoiceEntity> {
            lastCompletedInvoice = invoice
            lastCompletedItems = items
            return Result.success(invoice)
        }
        override suspend fun saveDraft(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): Result<InvoiceEntity> = Result.success(invoice)
        override suspend fun voidInvoice(invoiceNumber: String): Result<Unit> = Result.success(Unit)
        override suspend fun settleInvoiceDue(invoiceNumber: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteInvoice(invoiceNumber: String): Result<Unit> = Result.success(Unit)
        override suspend fun upsertInvoicesAndItems(invoices: List<InvoiceEntity>, items: List<InvoiceItemEntity>): Result<Unit> = Result.success(Unit)
    }
}
