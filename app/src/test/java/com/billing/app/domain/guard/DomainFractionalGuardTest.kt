package com.billing.app.domain.guard

import com.billing.app.core.cart.CartManager
import com.billing.app.core.util.Money
import com.billing.app.core.util.UnitRegistry
import com.billing.app.domain.model.AppError
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DomainFractionalGuardTest {

    private class TestProductRepository : ProductRepository {
        val products = mutableListOf<Product>()
        var transactionCounts = mutableMapOf<String, Int>()

        override fun getProducts(): Flow<List<Product>> = flowOf(products)
        override fun getProductsByCategory(category: String): Flow<List<Product>> = flowOf(products)
        override fun searchProducts(query: String, category: String?): Flow<List<Product>> = flowOf(products)
        override fun getTotalCount(): Flow<Int> = flowOf(products.size)
        override fun getLowStockCount(): Flow<Int> = flowOf(0)
        override fun getOutOfStockCount(): Flow<Int> = flowOf(0)
        override fun getAllCategories(): Flow<List<String>> = flowOf(listOf("Grocery"))
        override suspend fun getProductByBarcode(barcode: String): Product? = products.find { it.barcode == barcode }
        override suspend fun getProductById(id: String): Product? = products.find { it.id == id }
        override suspend fun getProductBySku(sku: String): Product? = products.find { it.sku == sku }
        override suspend fun getLowAndOutOfStockProducts(): List<Product> = emptyList()

        override suspend fun addProduct(product: Product): Result<Unit> {
            val unitDef = UnitRegistry.resolve(product.unit)
            if (!unitDef.allowsFractional && product.stockMilli % 1000L != 0L) {
                return Result.failure(AppError.FractionalNotAllowed(unitDef.code))
            }
            products.add(product.copy(unit = unitDef.code))
            return Result.success(Unit)
        }

        override suspend fun addProducts(products: List<Product>): Result<Unit> {
            for (p in products) {
                val res = addProduct(p)
                if (res.isFailure) return res
            }
            return Result.success(Unit)
        }

        override suspend fun updateProduct(product: Product): Result<Unit> {
            val existing = products.find { it.id == product.id }
            if (existing != null) {
                val existingUnit = UnitRegistry.resolve(existing.unit).code
                val newUnit = UnitRegistry.resolve(product.unit).code
                if (existingUnit != newUnit) {
                    val txCount = transactionCounts[product.id] ?: 0
                    if (existing.stockMilli != 0L || txCount > 0) {
                        return Result.failure(
                            AppError.UnitChangeNotAllowed(
                                product.name,
                                "Product has existing stock or transaction history"
                            )
                        )
                    }
                }
            }
            val unitDef = UnitRegistry.resolve(product.unit)
            if (!unitDef.allowsFractional && product.stockMilli % 1000L != 0L) {
                return Result.failure(AppError.FractionalNotAllowed(unitDef.code))
            }
            val idx = products.indexOfFirst { it.id == product.id }
            if (idx >= 0) {
                products[idx] = product.copy(unit = unitDef.code)
            } else {
                products.add(product.copy(unit = unitDef.code))
            }
            return Result.success(Unit)
        }

        override suspend fun adjustStock(id: String, newStock: Int): Result<Unit> = Result.success(Unit)
        override suspend fun deleteProduct(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun getProductTransactionCount(productId: String): Int = transactionCounts[productId] ?: 0
    }

    private lateinit var repo: TestProductRepository
    private lateinit var cartManager: CartManager

    @Before
    fun setUp() {
        repo = TestProductRepository()
        cartManager = CartManager(repo)
    }

    @Test
    fun testCartManagerRejectsFractionalForDiscreteUnits() {
        val discreteProd = Product(
            id = "p-disc-1",
            name = "Soap Bar",
            barcode = "111",
            price = Money(2000L),
            stockMilli = 10000L,
            unit = "PCS"
        )

        // Try adding 1.5 units (1500 milli)
        val (success, errorMsg) = cartManager.addProduct(discreteProd, 1500L)
        assertFalse("Adding fractional quantity for PCS must fail", success)
        assertTrue(errorMsg?.contains("Fractional") == true)
        assertEquals(0, cartManager.cartItems.value.size)

        // Whole unit (1000 milli) must succeed
        val (okSuccess, okError) = cartManager.addProduct(discreteProd, 1000L)
        assertTrue("Adding whole quantity for PCS must succeed", okSuccess)
        assertEquals(1, cartManager.cartItems.value.size)

        // Try setting fractional quantity via setQuantityMilli
        val setWarning = cartManager.setQuantityMilli("p-disc-1", 2500L)
        assertTrue(setWarning?.contains("Fractional") == true)
        // Quantity should remain unchanged (1000 milli)
        assertEquals(1000L, cartManager.cartItems.value.first().quantityMilli)
    }

    @Test
    fun testCartManagerAllowsFractionalForKG_L_and_MTR() {
        val kgProd = Product(
            id = "p-kg-1",
            name = "Sugar",
            barcode = "222",
            price = Money(4000L),
            stockMilli = 50000L,
            unit = "KG"
        )
        val mtrProd = Product(
            id = "p-mtr-1",
            name = "Electric Wire",
            barcode = "333",
            price = Money(1500L),
            stockMilli = 100000L,
            unit = "MTR"
        )

        // 250g sugar (250 milli)
        val (kgSuccess, _) = cartManager.addProduct(kgProd, 250L)
        assertTrue("Fractional quantity for KG must succeed", kgSuccess)

        // 2.5m wire (2500 milli)
        val (mtrSuccess, _) = cartManager.addProduct(mtrProd, 2500L)
        assertTrue("Fractional quantity for MTR must succeed (MTR is fractional)", mtrSuccess)

        assertEquals(2, cartManager.cartItems.value.size)
        assertEquals(250L, cartManager.cartItems.value.find { it.product.id == "p-kg-1" }?.quantityMilli)
        assertEquals(2500L, cartManager.cartItems.value.find { it.product.id == "p-mtr-1" }?.quantityMilli)
    }

    @Test
    fun testUnitMutationBlockedWhenProductHasStockOrTransactions() = runBlocking {
        val prodWithStock = Product(
            id = "p-stock",
            name = "Rice 1kg",
            barcode = "444",
            price = Money(6000L),
            stockMilli = 5000L, // 5 kg in stock
            unit = "KG"
        )
        repo.addProduct(prodWithStock)

        // Attempting to change unit to PCS while stock > 0 must fail
        val updateResult = repo.updateProduct(prodWithStock.copy(unit = "PCS"))
        assertTrue("Changing unit with existing stock must fail", updateResult.isFailure)
        assertTrue(updateResult.exceptionOrNull() is AppError.UnitChangeNotAllowed)

        // Product with 0 stock but with transaction history
        val prodWithTx = Product(
            id = "p-tx",
            name = "Oil 1L",
            barcode = "555",
            price = Money(15000L),
            stockMilli = 0L,
            unit = "L"
        )
        repo.addProduct(prodWithTx)
        repo.transactionCounts["p-tx"] = 3 // 3 past sales/purchases

        val updateTxResult = repo.updateProduct(prodWithTx.copy(unit = "BOX"))
        assertTrue("Changing unit with transaction history must fail", updateTxResult.isFailure)
        assertTrue(updateTxResult.exceptionOrNull() is AppError.UnitChangeNotAllowed)

        // Fresh product with 0 stock and 0 transactions CAN change unit
        val freshProd = Product(
            id = "p-fresh",
            name = "Fresh Biscuit",
            barcode = "666",
            price = Money(1000L),
            stockMilli = 0L,
            unit = "PACK"
        )
        repo.addProduct(freshProd)
        val okChangeResult = repo.updateProduct(freshProd.copy(unit = "BOX"))
        assertTrue("Fresh product with 0 stock and 0 transactions can change unit", okChangeResult.isSuccess)
    }
}
