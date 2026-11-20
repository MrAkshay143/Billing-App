package com.billing.app.core.cart

import com.billing.app.core.util.Money
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeProductRepository : ProductRepository {
    private val products = mutableListOf<Product>()

    fun setProducts(list: List<Product>) {
        products.clear()
        products.addAll(list)
    }

    override fun getProducts(): Flow<List<Product>> = flowOf(products)
    override fun getProductsByCategory(category: String): Flow<List<Product>> = flowOf(products.filter { it.category == category })
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

class CartManagerTest {

    private lateinit var fakeRepository: FakeProductRepository
    private lateinit var cartManager: CartManager

    @Before
    fun setUp() {
        fakeRepository = FakeProductRepository()
        cartManager = CartManager(productRepository = fakeRepository)
    }

    @Test
    fun testHalfUpRoundingCalculation() {
        // 33.00/kg x 1.250kg = 41.25 (4125 paise)
        val product = Product(
            id = "rice_kg",
            name = "Rice Loose",
            barcode = "1001",
            price = Money(3300L), // Rs 33.00
            unit = "KG",
            stockMilli = 50000L
        )
        val item = CartItem(
            product = product,
            quantityMilli = 1250L // 1.250 kg
        )
        assertEquals(4125L, item.grossLinePaise)
        assertEquals(4125L, item.netLinePaise)
        assertEquals(4125L, item.total.paise)
        assertEquals(1, item.quantity)
    }

    @Test
    fun testLivePriceOverrideAndLineDiscount() {
        val product = Product(
            id = "p1",
            name = "Soap",
            barcode = "1002",
            price = Money(5000L), // Rs 50.00
            stockMilli = 10000L
        )
        val (success, _) = cartManager.addProduct(product, 1000L)
        assertTrue(success)
        assertEquals(5000L, cartManager.subtotal.paise)

        // Live price override: change unit price from 50.00 (5000 paise) to 45.00 (4500 paise)
        cartManager.overrideItemPrice("p1", 4500L)
        val itemAfterOverride = cartManager.cartItems.value.first()
        assertEquals(4500L, itemAfterOverride.effectiveUnitPricePaise)
        assertEquals(4500L, itemAfterOverride.total.paise)
        assertEquals(4500L, cartManager.subtotal.paise)

        // Line discount: Rs 5.00 (500 paise) discount on this line
        cartManager.setItemDiscount("p1", 500L)
        val itemAfterDiscount = cartManager.cartItems.value.first()
        assertEquals(500L, itemAfterDiscount.discountPaise)
        assertEquals(4000L, itemAfterDiscount.netLinePaise)
        assertEquals(4000L, itemAfterDiscount.total.paise)
        assertEquals(4000L, cartManager.subtotal.paise)
    }

    @Test
    fun testCustomItemAddition() {
        val customItem = cartManager.addCustomItem(name = "Loose Sugar", pricePaise = 2500L, quantityMilli = 500L)
        assertTrue(customItem.isCustomItem)
        assertEquals("Loose Sugar", customItem.customName)
        assertEquals("Loose Sugar", customItem.displayName)
        assertEquals(2500L, customItem.effectiveUnitPricePaise)
        assertEquals(500L, customItem.quantityMilli)
        // 25.00 x 0.500kg = 12.50 (1250 paise)
        assertEquals(1250L, customItem.total.paise)
        assertEquals(1, cartManager.totalItemsCount)
        assertEquals(1250L, cartManager.subtotal.paise)
    }

    @Test
    fun testMultiCartTabCreationSwitchingAndIsolation() {
        val p1 = Product(id = "p1", name = "Atta", barcode = "1001", price = Money(3000L), stockMilli = 10000L)
        val p2 = Product(id = "p2", name = "Dal", barcode = "1002", price = Money(8000L), stockMilli = 10000L)

        // Cart 1: initial active tab
        assertEquals(1, cartManager.tabs.value.size)
        val tab1Id = cartManager.activeTabId.value
        assertEquals("Cart 1", cartManager.activeTab.name)

        cartManager.addProduct(p1, 1000L)
        cartManager.setCustomer("Alice", "9876543210")
        cartManager.setDiscount(500L)

        assertEquals(1, cartManager.cartItems.value.size)
        assertEquals("Alice", cartManager.customerName.value)
        assertEquals(500L, cartManager.discountPaise.value)

        // Add new tab: should be Cart 2 and switched to it
        val tab2Id = cartManager.addNewTab()
        assertEquals(2, cartManager.tabs.value.size)
        assertEquals(tab2Id, cartManager.activeTabId.value)
        assertEquals("Cart 2", cartManager.activeTab.name)

        // Cart 2 should be clean/isolated
        assertTrue(cartManager.cartItems.value.isEmpty())
        assertEquals("Walk-in Customer", cartManager.customerName.value)
        assertEquals(0L, cartManager.discountPaise.value)

        // Add item to Cart 2
        cartManager.addProduct(p2, 2000L)
        cartManager.setCustomer("Bob", "9123456789")
        assertEquals(1, cartManager.cartItems.value.size)
        assertEquals("p2", cartManager.cartItems.value.first().product.id)
        assertEquals("Bob", cartManager.customerName.value)

        // Switch back to Cart 1
        cartManager.switchTab(tab1Id)
        assertEquals(tab1Id, cartManager.activeTabId.value)
        assertEquals(1, cartManager.cartItems.value.size)
        assertEquals("p1", cartManager.cartItems.value.first().product.id)
        assertEquals("Alice", cartManager.customerName.value)
        assertEquals(500L, cartManager.discountPaise.value)

        // Switch back to Cart 2
        cartManager.switchTab(tab2Id)
        assertEquals(tab2Id, cartManager.activeTabId.value)
        assertEquals(1, cartManager.cartItems.value.size)
        assertEquals("p2", cartManager.cartItems.value.first().product.id)
        assertEquals("Bob", cartManager.customerName.value)

        // Close Cart 2
        cartManager.closeTab(tab2Id)
        assertEquals(1, cartManager.tabs.value.size)
        assertEquals(tab1Id, cartManager.activeTabId.value)
        assertEquals("Alice", cartManager.customerName.value)

        // Add tabs up to maximum 5 tabs
        val tabIds = mutableListOf<String>()
        for (i in 2..5) {
            tabIds.add(cartManager.addNewTab())
        }
        assertEquals(5, cartManager.tabs.value.size)
        // Attempting 6th tab should return current active tab and keep size at 5
        val sixthId = cartManager.addNewTab()
        assertEquals(5, cartManager.tabs.value.size)
        assertEquals(cartManager.activeTabId.value, sixthId)
    }

    @Test
    fun testNegativeStockSoftWarningVsBatchHardBlock() {
        val nonBatchProductOutOfStock = Product(
            id = "loose_sugar",
            name = "Loose Sugar",
            barcode = "9901",
            price = Money(4000L),
            stockMilli = 0L, // 0 stock
            trackBatch = false
        )
        val batchTrackedProductOutOfStock = Product(
            id = "paracetamol_strip",
            name = "Paracetamol 500mg",
            barcode = "9902",
            price = Money(2000L),
            stockMilli = 0L, // 0 stock
            trackBatch = true
        )

        // Non-batch item with 0 stock -> allowed with warning
        val (successNonBatch, warningMsg) = cartManager.addProduct(nonBatchProductOutOfStock, 1000L)
        assertTrue(successNonBatch)
        assertNotNull(warningMsg)
        assertTrue(warningMsg!!.contains("negative stock", ignoreCase = true))
        assertEquals(1, cartManager.cartItems.value.size)

        // Batch-tracked item with 0 stock -> hard blocked
        val (successBatch, errorMsg) = cartManager.addProduct(batchTrackedProductOutOfStock, 1000L)
        assertFalse(successBatch)
        assertNotNull(errorMsg)
        assertTrue(errorMsg!!.contains("out of stock (batch tracked)", ignoreCase = true))
        // Cart item count should still be 1 (only the non-batch was added)
        assertEquals(1, cartManager.cartItems.value.size)
    }

    @Test
    fun testCloseSingleTabClearsIt() {
        val product = Product(id = "p1", name = "Atta", barcode = "1001", price = Money(3000L), stockMilli = 10000L)
        cartManager.addProduct(product, 1000L)
        cartManager.setCustomer("Alice", "9876543210")
        assertEquals(1, cartManager.cartItems.value.size)

        val tabId = cartManager.activeTabId.value
        cartManager.closeTab(tabId)

        assertEquals(1, cartManager.tabs.value.size)
        assertTrue(cartManager.cartItems.value.isEmpty())
        assertEquals("Walk-in Customer", cartManager.customerName.value)
    }

    @Test
    fun testIncrementDecrementAndSetQuantityMilli() {
        val product = Product(id = "p1", name = "Atta", barcode = "1001", price = Money(3000L), stockMilli = 5000L, unit = "KG")
        cartManager.addProduct(product, 1000L)

        // Increment by default 1000L
        cartManager.increment("p1")
        assertEquals(2000L, cartManager.cartItems.value.first().quantityMilli)

        // Set quantity to 3500L
        cartManager.setQuantityMilli("p1", 3500L)
        assertEquals(3500L, cartManager.cartItems.value.first().quantityMilli)

        // Decrement by 1000L
        cartManager.decrement("p1")
        assertEquals(2500L, cartManager.cartItems.value.first().quantityMilli)

        // Set quantity to 0 removes item
        cartManager.setQuantityMilli("p1", 0L)
        assertTrue(cartManager.cartItems.value.isEmpty())
    }
}
