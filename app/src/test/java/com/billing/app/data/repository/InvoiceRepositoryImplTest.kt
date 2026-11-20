package com.billing.app.data.repository

import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.CategorySalesSummary
import com.billing.app.data.local.dao.CustomerDao
import com.billing.app.data.local.dao.CustomerLedgerDao
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.dao.TopProductSummary
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import com.billing.app.data.local.entity.InvoiceCounterEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import com.billing.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InvoiceRepositoryImplTest {

    private lateinit var fakeInvoiceDao: FakeInvoiceDao
    private lateinit var fakeProductDao: FakeProductDao
    private lateinit var fakeCustomerDao: FakeCustomerDao
    private lateinit var fakeCustomerLedgerDao: FakeCustomerLedgerDao
    private lateinit var fakeCashMovementDao: FakeCashMovementDao
    private lateinit var invoiceRepository: InvoiceRepositoryImpl

    @Before
    fun setup() {
        fakeInvoiceDao = FakeInvoiceDao()
        fakeProductDao = FakeProductDao()
        fakeCustomerDao = FakeCustomerDao()
        fakeCustomerLedgerDao = FakeCustomerLedgerDao()
        fakeCashMovementDao = FakeCashMovementDao()

        invoiceRepository = InvoiceRepositoryImpl(
            invoiceDao = fakeInvoiceDao,
            productDao = fakeProductDao,
            customerDao = fakeCustomerDao,
            customerLedgerDao = fakeCustomerLedgerDao,
            cashMovementDao = fakeCashMovementDao,
            database = null
        )
    }

    @Test
    fun completeSale_withoutShiftId_failsMandatoryShiftRule() = runBlocking {
        val invoice = InvoiceEntity(
            invoiceNumber = "INV-000001",
            subtotalPaise = 50000L,
            grandTotalPaise = 50000L,
            shiftId = null // Missing shift!
        )
        val items = listOf(
            InvoiceItemEntity(
                productName = "Product A",
                unitPricePaise = 50000L,
                lineTotalPaise = 50000L
            )
        )

        val result = invoiceRepository.completeSale(invoice, items)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertEquals("Active shift required for completing sale", exception?.message)

        // Verify nothing was saved
        assertTrue(fakeInvoiceDao.invoices.isEmpty())
        assertTrue(fakeCashMovementDao.movements.isEmpty())
    }

    @Test
    fun completeSale_withShiftId_decrementsStock_forceForStandard_strictForBatch() = runBlocking {
        // Standard item (trackBatch = false): initial stock 2000 milli (2 items)
        val standardProduct = ProductEntity(
            id = "prod_std_1",
            name = "Biscuits",
            barcode = "11111",
            pricePaise = 2000L,
            stockMilli = 2000L,
            trackBatch = false
        )
        fakeProductDao.insertProduct(standardProduct)

        // Batch-tracked item (trackBatch = true): initial stock 5000 milli (5 items)
        val batchProduct = ProductEntity(
            id = "prod_batch_1",
            name = "Milk Carton",
            barcode = "22222",
            pricePaise = 5000L,
            stockMilli = 5000L,
            trackBatch = true
        )
        fakeProductDao.insertProduct(batchProduct)

        val invoice = InvoiceEntity(
            invoiceNumber = "INV-000002",
            subtotalPaise = 15000L,
            grandTotalPaise = 15000L,
            shiftId = 10L
        )

        // Buy 5 Biscuits (stock 2 -> force decrement into negative: -3 items = -3000 milli)
        // Buy 2 Milk (stock 5 -> 3 items = 3000 milli)
        // Buy 1 Custom loose item (isCustomItem = true -> no stock deduction)
        val items = listOf(
            InvoiceItemEntity(
                productId = "prod_std_1",
                productName = "Biscuits",
                quantityMilli = 5000L,
                unitPricePaise = 2000L,
                lineTotalPaise = 10000L
            ),
            InvoiceItemEntity(
                productId = "prod_batch_1",
                productName = "Milk Carton",
                quantityMilli = 2000L,
                unitPricePaise = 5000L,
                lineTotalPaise = 10000L
            ),
            InvoiceItemEntity(
                productId = null,
                productName = "Custom Packaging",
                isCustomItem = true,
                quantityMilli = 1000L,
                unitPricePaise = 500L,
                lineTotalPaise = 500L
            )
        )

        val result = invoiceRepository.completeSale(invoice, items)
        assertTrue(result.isSuccess)

        // Standard product stock force-decremented to -3000 milli
        assertEquals(-3000L, fakeProductDao.getProductById("prod_std_1")?.stockMilli)
        // Batch product stock decremented to 3000 milli
        assertEquals(3000L, fakeProductDao.getProductById("prod_batch_1")?.stockMilli)
    }

    @Test
    fun completeSale_batchTrackedInsufficientStock_failsWithIllegalStateException() = runBlocking {
        val batchProduct = ProductEntity(
            id = "prod_batch_2",
            name = "Baby Formula",
            barcode = "33333",
            pricePaise = 50000L,
            stockMilli = 1000L, // Only 1 in stock
            trackBatch = true
        )
        fakeProductDao.insertProduct(batchProduct)

        val invoice = InvoiceEntity(
            invoiceNumber = "INV-000003",
            subtotalPaise = 100000L,
            grandTotalPaise = 100000L,
            shiftId = 10L
        )
        val items = listOf(
            InvoiceItemEntity(
                productId = "prod_batch_2",
                productName = "Baby Formula",
                quantityMilli = 2000L, // Requesting 2
                unitPricePaise = 50000L,
                lineTotalPaise = 100000L
            )
        )

        val result = invoiceRepository.completeSale(invoice, items)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is IllegalStateException)
        assertEquals("Insufficient stock for batch tracked product 'Baby Formula'", ex?.message)
    }

    @Test
    fun completeSale_withNetCash_recordsCashMovement() = runBlocking {
        val invoice = InvoiceEntity(
            invoiceNumber = "INV-000004",
            subtotalPaise = 12000L,
            grandTotalPaise = 12000L,
            cashReceivedPaise = 20000L, // Customer gave Rs 200.00
            changeReturnedPaise = 8000L, // Change returned Rs 80.00 (Net cash = Rs 120.00)
            shiftId = 42L
        )
        val items = listOf(
            InvoiceItemEntity(
                productName = "Items",
                isCustomItem = true,
                quantityMilli = 1000L,
                unitPricePaise = 12000L,
                lineTotalPaise = 12000L
            )
        )

        val result = invoiceRepository.completeSale(invoice, items)
        assertTrue(result.isSuccess)

        // Verify cash movement recorded for shift 42
        val movements = fakeCashMovementDao.movements
        assertEquals(1, movements.size)
        val movement = movements.first()
        assertEquals(42L, movement.shiftId)
        assertEquals("SALE_CASH", movement.movementType)
        assertEquals(12000L, movement.amountPaise)
        assertEquals("INVOICE", movement.reason)
        assertEquals("INV-000004", movement.referenceId)
    }

    @Test
    fun completeSale_withCreditDue_recordsDoubleEntryLedgerAndUpdatesBalance() = runBlocking {
        val customer = CustomerEntity(
            id = "cust_credit_1",
            name = "Anita Roy",
            phone = "9998887770",
            openingBalancePaise = 15000L // Previous Khata due Rs 150.00
        )
        fakeCustomerDao.insertCustomer(customer)

        val invoice = InvoiceEntity(
            invoiceNumber = "INV-000005",
            subtotalPaise = 30000L,
            grandTotalPaise = 30000L,
            originalDuePaise = 20000L, // Credit portion Rs 200.00
            duePaise = 20000L,
            customerId = "cust_credit_1",
            customerPhone = "9998887770",
            shiftId = 15L
        )
        val items = listOf(
            InvoiceItemEntity(
                productName = "Bulk Grain",
                isCustomItem = true,
                quantityMilli = 1000L,
                unitPricePaise = 30000L,
                lineTotalPaise = 30000L
            )
        )

        val result = invoiceRepository.completeSale(invoice, items)
        assertTrue(result.isSuccess)

        // Verify Customer Ledger Entry created
        val ledgerEntries = fakeCustomerLedgerDao.entries
        assertEquals(1, ledgerEntries.size)
        val entry = ledgerEntries.first()
        assertEquals("cust_credit_1", entry.customerId)
        assertEquals("CREDIT_SALE", entry.transactionType)
        assertEquals(20000L, entry.debitPaise)
        assertEquals(0L, entry.creditPaise)
        assertEquals(35000L, entry.balanceAfterPaise) // 15000 + 20000 = 35000
        assertEquals("Credit sale: INV-000005", entry.note)

        // Verify customer's openingBalancePaise updated
        val updatedCustomer = fakeCustomerDao.getCustomerById("cust_credit_1")
        assertNotNull(updatedCustomer)
        assertEquals(35000L, updatedCustomer?.openingBalancePaise)
    }

    @Test
    fun completeSale_deletesDraftInvoice_whenDraftNumberProvided() = runBlocking {
        val draft = InvoiceEntity(
            invoiceNumber = "DFT-000099",
            subtotalPaise = 5000L,
            grandTotalPaise = 5000L,
            status = InvoiceStatus.DRAFT,
            shiftId = 1L
        )
        fakeInvoiceDao.insertInvoice(draft)

        val completedInvoice = InvoiceEntity(
            invoiceNumber = "INV-000006",
            subtotalPaise = 5000L,
            grandTotalPaise = 5000L,
            shiftId = 1L
        )
        val items = listOf(
            InvoiceItemEntity(
                productName = "Draft Item",
                isCustomItem = true,
                quantityMilli = 1000L,
                unitPricePaise = 5000L,
                lineTotalPaise = 5000L
            )
        )

        val result = invoiceRepository.completeSale(completedInvoice, items, draftInvoiceNumberToDelete = "DFT-000099")
        assertTrue(result.isSuccess)

        // Draft invoice was deleted
        assertEquals(null, fakeInvoiceDao.getInvoiceByNumber("DFT-000099"))
        // Completed invoice is present
        assertNotNull(fakeInvoiceDao.getInvoiceByNumber("INV-000006"))
    }

    // --- Fake DAOs ---

    class FakeInvoiceDao : InvoiceDao {
        val invoices = mutableListOf<InvoiceEntity>()
        val invoiceItems = mutableListOf<InvoiceItemEntity>()
        private var nextId = 1L

        override suspend fun insertInvoice(invoice: InvoiceEntity): Long {
            val id = if (invoice.id == 0L) nextId++ else invoice.id
            val saved = invoice.copy(id = id)
            invoices.add(saved)
            return id
        }

        override suspend fun insertInvoiceItem(item: InvoiceItemEntity): Long {
            val id = if (item.id == 0L) nextId++ else item.id
            invoiceItems.add(item.copy(id = id))
            return id
        }

        override suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>) {
            invoiceItems.addAll(items)
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
            MutableStateFlow(invoices.filter { it.duePaise > 0L && it.status == InvoiceStatus.COMPLETED }).asStateFlow()

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
            MutableStateFlow(products.count { it.stockMilli in 1..it.minStockAlertMilli }).asStateFlow()

        override fun getOutOfStockCount(): Flow<Int> =
            MutableStateFlow(products.count { it.stockMilli <= 0L }).asStateFlow()

        override suspend fun getLowAndOutOfStockProducts(): List<ProductEntity> =
            products.filter { it.stockMilli <= it.minStockAlertMilli }

        override suspend fun getProductTransactionCount(productId: String): Int = 0

        override fun getAllCategories(): Flow<List<String>> =
            MutableStateFlow(products.map { it.category }.distinct()).asStateFlow()
    }

    class FakeCustomerDao : CustomerDao {
        val customers = mutableListOf<CustomerEntity>()

        override fun getAllCustomers(): Flow<List<CustomerEntity>> =
            MutableStateFlow(customers.toList()).asStateFlow()

        override fun searchCustomers(query: String): Flow<List<CustomerEntity>> =
            MutableStateFlow(customers.filter { it.name.contains(query, true) || it.phone.contains(query) }).asStateFlow()

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
            val saved = entry.copy(id = id)
            entries.add(saved)
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

    class FakeCashMovementDao : CashMovementDao {
        val movements = mutableListOf<CashMovementEntity>()
        private val _movementsFlow = MutableStateFlow<List<CashMovementEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insertCashMovement(movement: CashMovementEntity): Long {
            val id = nextId++
            val saved = movement.copy(id = id)
            movements.add(saved)
            _movementsFlow.value = movements.toList()
            return id
        }

        override suspend fun upsertCashMovement(movement: CashMovementEntity) {
            val idx = movements.indexOfFirst { it.id == movement.id }
            if (idx >= 0) movements[idx] = movement else movements.add(movement)
            _movementsFlow.value = movements.toList()
        }

        override suspend fun upsertAll(movements: List<CashMovementEntity>) {
            movements.forEach { upsertCashMovement(it) }
        }

        override fun getCashMovementsForShift(shiftId: Long): Flow<List<CashMovementEntity>> =
            MutableStateFlow(movements.filter { it.shiftId == shiftId }).asStateFlow()

        override suspend fun getCashMovementListForShift(shiftId: Long): List<CashMovementEntity> =
            movements.filter { it.shiftId == shiftId }

        override fun getAllCashMovements(): Flow<List<CashMovementEntity>> =
            _movementsFlow.asStateFlow()

        override suspend fun getCashMovementById(id: Long): CashMovementEntity? =
            movements.find { it.id == id }

        override fun getTotalByMovementType(shiftId: Long, movementType: String): Flow<Long> =
            MutableStateFlow(
                movements.filter { it.shiftId == shiftId && it.movementType == movementType }
                    .sumOf { it.amountPaise }
            ).asStateFlow()

        override suspend fun getNetCashForShift(shiftId: Long): Long =
            movements.filter { it.shiftId == shiftId && it.reason != "Opening Float" }
                .sumOf { it.amountPaise }

        override fun getNetCashForShiftFlow(shiftId: Long): Flow<Long> =
            MutableStateFlow(
                movements.filter { it.shiftId == shiftId && it.reason != "Opening Float" }
                    .sumOf { it.amountPaise }
            ).asStateFlow()

        override suspend fun deleteCashMovement(id: Long) {
            movements.removeAll { it.id == id }
            _movementsFlow.value = movements.toList()
        }

        override suspend fun deleteCashMovementsForShift(shiftId: Long) {
            movements.removeAll { it.shiftId == shiftId }
            _movementsFlow.value = movements.toList()
        }
    }
}
