package com.billing.app.feature.products

import com.billing.app.core.util.Money
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeProductRepository
    private lateinit var viewModel: ProductViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeProductRepository()
        viewModel = ProductViewModel(productRepository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initFromProduct_populatesAllDraftFieldsCorrectly() {
        val product = Product(
            id = "prod_1",
            name = "Aashirvaad Atta",
            barcode = "8901030000001",
            price = Money.fromRupees(250.0),
            stockMilli = 20000L,
            imageUri = "content://media/1",
            sku = "SKU-ATTA",
            category = "Flour",
            unit = "kg",
            purchasePrice = Money.fromRupees(210.50),
            mrp = Money.fromRupees(260.0),
            minStockAlertMilli = 8000L,
            gstRateBp = 500,
            trackBatch = true,
            hsnCode = "1101"
        )

        viewModel.initFromProduct(product)

        assertEquals("Aashirvaad Atta", viewModel.draftName.value)
        assertEquals("8901030000001", viewModel.draftBarcode.value)
        assertEquals(product.price.format(includeSymbol = false), viewModel.draftPrice.value)
        assertEquals("20", viewModel.draftStock.value)
        assertEquals("Flour", viewModel.draftCategory.value)
        assertEquals("SKU-ATTA", viewModel.draftSku.value)
        assertEquals("content://media/1", viewModel.draftImageUri.value)
        assertEquals("KG", viewModel.draftUnit.value)
        assertEquals("210.5", viewModel.draftPurchasePrice.value)
        assertEquals("260.0", viewModel.draftMrp.value)
        assertEquals("5", viewModel.draftGstRate.value)
        assertEquals("8", viewModel.draftMinStockAlert.value)
        assertEquals("1101", viewModel.draftHsnCode.value)
        assertTrue(viewModel.draftTrackBatch.value)
    }

    @Test
    fun clearDraft_resetsAllDraftFieldsToDefaults() {
        viewModel.draftName.value = "Test Product"
        viewModel.draftBarcode.value = "123456"
        viewModel.draftPrice.value = "100"
        viewModel.draftStock.value = "10"
        viewModel.draftPurchasePrice.value = "80"
        viewModel.draftMrp.value = "120"
        viewModel.draftGstRate.value = "18"
        viewModel.draftMinStockAlert.value = "15"
        viewModel.draftHsnCode.value = "1901"
        viewModel.draftTrackBatch.value = true

        viewModel.clearDraft()

        assertEquals("", viewModel.draftName.value)
        assertEquals("", viewModel.draftBarcode.value)
        assertEquals("", viewModel.draftPrice.value)
        assertEquals("", viewModel.draftStock.value)
        assertEquals("Grocery", viewModel.draftCategory.value)
        assertEquals("", viewModel.draftPurchasePrice.value)
        assertEquals("", viewModel.draftMrp.value)
        assertEquals("0", viewModel.draftGstRate.value)
        assertEquals("5", viewModel.draftMinStockAlert.value)
        assertEquals("", viewModel.draftHsnCode.value)
        assertFalse(viewModel.draftTrackBatch.value)
    }

    @Test
    fun saveProduct_savesAllFieldsToDatabase() = runTest(testDispatcher) {
        viewModel.draftName.value = "Fortune Mustard Oil"
        viewModel.draftBarcode.value = "8901234567890"
        viewModel.draftPrice.value = "180.00"
        viewModel.draftStock.value = "15"
        viewModel.draftCategory.value = "Oil & Ghee"
        viewModel.draftSku.value = "SKU-OIL-1L"
        viewModel.draftUnit.value = "l"
        viewModel.draftPurchasePrice.value = "150.00"
        viewModel.draftMrp.value = "190.00"
        viewModel.draftGstRate.value = "5"
        viewModel.draftMinStockAlert.value = "6"
        viewModel.draftHsnCode.value = "1514"
        viewModel.draftTrackBatch.value = true

        viewModel.saveProduct()
        advanceUntilIdle()

        assertTrue(viewModel.saveSuccess.value)
        val saved = fakeRepository.productsList.firstOrNull { it.barcode == "8901234567890" }
        assertNotNull(saved)
        assertEquals("Fortune Mustard Oil", saved!!.name)
        assertEquals(Money.fromRupees(180.0), saved.price)
        assertEquals(15, saved.stock)
        assertEquals(Money.fromRupees(150.0), saved.purchasePrice)
        assertEquals(Money.fromRupees(190.0), saved.mrp)
        assertEquals(500, saved.gstRateBp)
        assertEquals(6000L, saved.minStockAlertMilli)
        assertEquals("1514", saved.hsnCode)
        assertTrue(saved.trackBatch)
    }

    @Test
    fun updateProduct_doesNotClobberProtectedFields_whenEditingOtherFields() = runTest(testDispatcher) {
        val originalProduct = Product(
            id = "prod_protected",
            name = "Tata Salt",
            barcode = "8901058852271",
            price = Money.fromRupees(28.0),
            stockMilli = 50000L,
            imageUri = null,
            sku = "SKU-SALT-1KG",
            category = "Grocery",
            unit = "pack",
            purchasePrice = Money.fromRupees(22.50),
            mrp = Money.fromRupees(30.0),
            minStockAlertMilli = 12000L,
            gstRateBp = 1800,
            trackBatch = true,
            hsnCode = "2501",
            isActive = true
        )
        fakeRepository.addProduct(originalProduct)
        advanceUntilIdle()

        // Clear drafts to simulate a caller that doesn't populate draft fields or a partial update
        viewModel.clearDraft()

        // Caller updates only name, barcode, price and stock without passing purchasePrice or MRP
        viewModel.updateProduct(
            id = originalProduct.id,
            name = "Tata Salt (1 kg Refill)",
            barcode = originalProduct.barcode,
            priceStr = "30.00",
            stockStr = "45",
            category = originalProduct.category,
            sku = originalProduct.sku,
            unit = originalProduct.unit
        )
        advanceUntilIdle()

        assertTrue(viewModel.saveSuccess.value)
        val updated = fakeRepository.getProductById(originalProduct.id)
        assertNotNull(updated)
        // Verify modified fields changed
        assertEquals("Tata Salt (1 kg Refill)", updated!!.name)
        assertEquals(Money.fromRupees(30.0), updated.price)
        assertEquals(45, updated.stock)

        // Verify protected fields were NOT clobbered!
        assertEquals(Money.fromRupees(22.50), updated.purchasePrice)
        assertEquals(Money.fromRupees(30.0), updated.mrp)
        assertEquals(1800, updated.gstRateBp)
        assertEquals(12000L, updated.minStockAlertMilli)
        assertEquals("2501", updated.hsnCode)
        assertTrue(updated.trackBatch)
        assertTrue(updated.isActive)
    }

    @Test
    fun updateProduct_updatesFieldsWhenUserProvidesNewValues() = runTest(testDispatcher) {
        val originalProduct = Product(
            id = "prod_updatable",
            name = "Maggi Noodles",
            barcode = "8901058850000",
            price = Money.fromRupees(14.0),
            stockMilli = 10000L,
            purchasePrice = Money.fromRupees(10.0),
            mrp = Money.fromRupees(15.0),
            minStockAlertMilli = 5000L,
            gstRateBp = 1200,
            trackBatch = false,
            hsnCode = "1902"
        )
        fakeRepository.addProduct(originalProduct)
        advanceUntilIdle()

        viewModel.initFromProduct(originalProduct)

        // User edits purchase price, MRP, GST rate, alert threshold and batch tracking
        viewModel.draftPurchasePrice.value = "11.50"
        viewModel.draftMrp.value = "16.00"
        viewModel.draftGstRate.value = "18"
        viewModel.draftMinStockAlert.value = "15"
        viewModel.draftTrackBatch.value = true
        viewModel.draftHsnCode.value = "190299"

        viewModel.updateProduct(
            id = originalProduct.id,
            name = viewModel.draftName.value,
            barcode = viewModel.draftBarcode.value,
            priceStr = "15.00",
            stockStr = "20",
            purchasePriceStr = viewModel.draftPurchasePrice.value,
            mrpStr = viewModel.draftMrp.value,
            gstRateStr = viewModel.draftGstRate.value,
            minStockAlertStr = viewModel.draftMinStockAlert.value,
            hsnCodeStr = viewModel.draftHsnCode.value,
            trackBatch = viewModel.draftTrackBatch.value
        )
        advanceUntilIdle()

        assertTrue(viewModel.saveSuccess.value)
        val updated = fakeRepository.getProductById(originalProduct.id)
        assertNotNull(updated)
        assertEquals(Money.fromRupees(15.0), updated!!.price)
        assertEquals(20, updated.stock)
        assertEquals(Money.fromRupees(11.50), updated.purchasePrice)
        assertEquals(Money.fromRupees(16.00), updated.mrp)
        assertEquals(1800, updated.gstRateBp)
        assertEquals(15000L, updated.minStockAlertMilli)
        assertEquals("190299", updated.hsnCode)
        assertTrue(updated.trackBatch)
    }

    // Helper fake repository
    private class FakeProductRepository : ProductRepository {
        val productsList = mutableListOf<Product>()
        private val _productsFlow = MutableStateFlow<List<Product>>(emptyList())

        private fun updateFlow() {
            _productsFlow.value = productsList.toList()
        }

        override fun getProducts(): Flow<List<Product>> = _productsFlow
        override fun getProductsByCategory(category: String): Flow<List<Product>> =
            _productsFlow.map { list -> list.filter { it.category == category } }
        override fun searchProducts(query: String, category: String?): Flow<List<Product>> =
            _productsFlow.map { list -> list.filter { it.name.contains(query, ignoreCase = true) } }
        override fun getTotalCount(): Flow<Int> = _productsFlow.map { it.size }
        override fun getLowStockCount(): Flow<Int> = _productsFlow.map { it.count { p -> p.stock in 1..5 } }
        override fun getOutOfStockCount(): Flow<Int> = _productsFlow.map { it.count { p -> p.stock <= 0 } }
        override fun getAllCategories(): Flow<List<String>> =
            _productsFlow.map { list -> list.map { it.category }.distinct() }
        override suspend fun getProductByBarcode(barcode: String): Product? =
            productsList.find { it.barcode == barcode }
        override suspend fun getProductById(id: String): Product? =
            productsList.find { it.id == id }
        override suspend fun getProductBySku(sku: String): Product? =
            productsList.find { it.sku == sku }
        override suspend fun getLowAndOutOfStockProducts(): List<Product> =
            productsList.filter { it.stock <= 5 }
        override suspend fun addProduct(product: Product): Result<Unit> {
            productsList.add(product)
            updateFlow()
            return Result.success(Unit)
        }
        override suspend fun addProducts(products: List<Product>): Result<Unit> {
            productsList.addAll(products)
            updateFlow()
            return Result.success(Unit)
        }
        override suspend fun updateProduct(product: Product): Result<Unit> {
            val idx = productsList.indexOfFirst { it.id == product.id }
            if (idx >= 0) {
                productsList[idx] = product
                updateFlow()
                return Result.success(Unit)
            }
            return Result.failure(NoSuchElementException("Product not found"))
        }
        override suspend fun adjustStock(id: String, newStock: Int): Result<Unit> {
            val idx = productsList.indexOfFirst { it.id == id }
            if (idx >= 0) {
                productsList[idx] = productsList[idx].copy(stockMilli = newStock.toLong() * 1000L)
                updateFlow()
                return Result.success(Unit)
            }
            return Result.failure(NoSuchElementException("Product not found"))
        }
        override suspend fun deleteProduct(id: String): Result<Unit> {
            productsList.removeAll { it.id == id }
            updateFlow()
            return Result.success(Unit)
        }
    }
}
