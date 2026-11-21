package com.billing.app.core.cart

import com.billing.app.core.audio.SoundHapticManager
import com.billing.app.core.util.Money
import com.billing.app.core.util.UnitRegistry
import com.billing.app.domain.model.AppError
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CartTab(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val items: List<CartItem> = emptyList(),
    val discountPaise: Long = 0L,
    val customerName: String = "Walk-in Customer",
    val customerPhone: String = "",
    val customerId: String? = null,
    val resumedDraftNumber: String? = null
)

class CartManager(
    private val productRepository: ProductRepository,
    private val soundHapticManager: SoundHapticManager? = null
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val initialTab = CartTab(name = "Cart 1")
    private val _tabs = MutableStateFlow<List<CartTab>>(listOf(initialTab))
    val tabs: StateFlow<List<CartTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String>(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _discountPaise = MutableStateFlow(0L)
    val discountPaise: StateFlow<Long> = _discountPaise.asStateFlow()

    private val _customerName = MutableStateFlow("Walk-in Customer")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _customerId = MutableStateFlow<String?>(null)
    val customerId: StateFlow<String?> = _customerId.asStateFlow()

    private val _resumedDraftNumber = MutableStateFlow<String?>(null)
    val resumedDraftNumber: StateFlow<String?> = _resumedDraftNumber.asStateFlow()

    init {
        syncStateWithActiveTab()
    }

    val totalItemsCount: Int
        get() = _cartItems.value.size

    val totalQuantityCount: Int
        get() = _cartItems.value.sumOf { it.quantity }

    val totalQuantityMilli: Long
        get() = _cartItems.value.sumOf { it.quantityMilli }

    val subtotal: Money
        get() = _cartItems.value.fold(Money.ZERO) { acc, item -> acc + item.total }

    val grandTotal: Money
        get() {
            val discount = Money(_discountPaise.value)
            return if (discount >= subtotal) Money.ZERO else subtotal - discount
        }

    val activeTab: CartTab
        get() = _tabs.value.firstOrNull { it.id == _activeTabId.value } ?: _tabs.value.first()

    private fun syncStateWithActiveTab() {
        val currentTab = _tabs.value.firstOrNull { it.id == _activeTabId.value } ?: _tabs.value.first()
        _cartItems.value = currentTab.items
        _discountPaise.value = currentTab.discountPaise
        _customerName.value = currentTab.customerName
        _customerPhone.value = currentTab.customerPhone
        _customerId.value = currentTab.customerId
        _resumedDraftNumber.value = currentTab.resumedDraftNumber
    }

    private fun updateActiveTab(update: (CartTab) -> CartTab) {
        val activeId = _activeTabId.value
        _tabs.update { tabList ->
            tabList.map { tab ->
                if (tab.id == activeId) update(tab) else tab
            }
        }
        syncStateWithActiveTab()
    }

    fun switchTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
            syncStateWithActiveTab()
        }
    }

    fun addNewTab(): String {
        val currentTabs = _tabs.value
        if (currentTabs.size >= 5) {
            return _activeTabId.value
        }
        val existingNumbers = currentTabs.mapNotNull {
            it.name.removePrefix("Cart ").trim().toIntOrNull()
        }.toSet()
        val nextNum = (1..5).firstOrNull { it !in existingNumbers } ?: (currentTabs.size + 1)
        val newTab = CartTab(name = "Cart $nextNum")
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        syncStateWithActiveTab()
        return newTab.id
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        if (currentTabs.size <= 1) {
            updateActiveTab { tab ->
                tab.copy(
                    items = emptyList(),
                    discountPaise = 0L,
                    customerName = "Walk-in Customer",
                    customerPhone = "",
                    customerId = null,
                    resumedDraftNumber = null
                )
            }
            return
        }
        val indexToRemove = currentTabs.indexOfFirst { it.id == tabId }
        if (indexToRemove >= 0) {
            val remaining = currentTabs.toMutableList()
            remaining.removeAt(indexToRemove)
            _tabs.value = remaining
            if (_activeTabId.value == tabId) {
                val newIndex = (indexToRemove - 1).coerceAtLeast(0)
                _activeTabId.value = remaining[newIndex].id
            }
            syncStateWithActiveTab()
        }
    }

    fun addProduct(product: Product, quantityMilli: Long = 1000L): Pair<Boolean, String?> {
        val unitDef = UnitRegistry.resolve(product.unit)
        if (!unitDef.allowsFractional && quantityMilli % 1000L != 0L) {
            return Pair(false, "Fractional quantity is not allowed for discrete unit '${unitDef.code}'")
        }

        var errorMsg: String? = null
        var success = false

        // Batch tracking hard block rule:
        if (product.trackBatch && product.stockMilli <= 0L) {
            return Pair(false, "${product.name} is out of stock (batch tracked)")
        }

        updateActiveTab { tab ->
            val current = tab.items.toMutableList()
            val index = current.indexOfFirst { it.product.id == product.id }

            if (index >= 0) {
                val existing = current[index]
                val newQtyMilli = existing.quantityMilli + quantityMilli

                if (product.trackBatch) {
                    if (product.stockMilli > 0L && newQtyMilli > product.stockMilli) {
                        errorMsg = "Only ${product.stock} left in stock for ${product.name}"
                        tab
                    } else {
                        current[index] = existing.copy(quantityMilli = newQtyMilli)
                        success = true
                        tab.copy(items = current)
                    }
                } else {
                    current[index] = existing.copy(quantityMilli = newQtyMilli)
                    success = true
                    if (product.stockMilli <= 0L || newQtyMilli > product.stockMilli) {
                        errorMsg = "Warning: ${product.name} has negative stock"
                    }
                    tab.copy(items = current)
                }
            } else {
                if (product.trackBatch) {
                    if (product.stockMilli > 0L && quantityMilli > product.stockMilli) {
                        errorMsg = "Only ${product.stock} left in stock for ${product.name}"
                        tab
                    } else {
                        current.add(CartItem(product = product, quantityMilli = quantityMilli))
                        success = true
                        tab.copy(items = current)
                    }
                } else {
                    current.add(CartItem(product = product, quantityMilli = quantityMilli))
                    success = true
                    if (product.stockMilli <= 0L || quantityMilli > product.stockMilli) {
                        errorMsg = "Warning: ${product.name} has negative stock"
                    }
                    tab.copy(items = current)
                }
            }
        }

        if (success) {
            soundHapticManager?.triggerScanFeedback()
        }
        return Pair(success, errorMsg)
    }

    fun addProduct(product: Product, quantity: Int): Pair<Boolean, String?> =
        addProduct(product, quantity.toLong() * 1000L)

    fun addCustomItem(name: String, pricePaise: Long, quantityMilli: Long = 1000L, unit: String = "PCS"): CartItem {
        val customId = "custom_" + UUID.randomUUID().toString()
        val dummyProduct = Product(
            id = customId,
            name = name,
            barcode = "",
            price = Money(pricePaise),
            stockMilli = 0L,
            unit = unit,
            trackBatch = false
        )
        val item = CartItem(
            product = dummyProduct,
            quantityMilli = quantityMilli,
            customPricePaise = pricePaise,
            isCustomItem = true,
            customName = name,
            isCostUnknown = true
        )
        updateActiveTab { tab ->
            tab.copy(items = tab.items + item)
        }
        soundHapticManager?.triggerScanFeedback()
        return item
    }

    fun addCustomItem(name: String, pricePaise: Long, quantity: Int): CartItem =
        addCustomItem(name, pricePaise, quantity.toLong() * 1000L, "PCS")

    fun overrideItemPrice(productId: String, newPricePaise: Long) {
        updateActiveTab { tab ->
            tab.copy(
                items = tab.items.map { item ->
                    if (item.product.id == productId) {
                        item.copy(customPricePaise = newPricePaise.coerceAtLeast(0L))
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun setItemDiscount(productId: String, discountPaise: Long) {
        updateActiveTab { tab ->
            tab.copy(
                items = tab.items.map { item ->
                    if (item.product.id == productId) {
                        item.copy(discountPaise = discountPaise.coerceAtLeast(0L))
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun scanBarcode(
        barcode: String,
        onInactiveProduct: ((Product) -> Unit)? = null,
        onResult: (Boolean, String?, String?) -> Unit
    ) {
        scope.launch {
            val cleanBarcode = barcode.trim()
            val product = productRepository.getProductByBarcode(cleanBarcode)
            if (product != null) {
                if (!product.isActive) {
                    onInactiveProduct?.invoke(product)
                    onResult(false, product.name, "This product is inactive/deleted")
                    return@launch
                }
                val (success, errorMsg) = addProduct(product, 1000L)
                if (success) {
                    onResult(true, product.name, errorMsg)
                } else {
                    onResult(false, product.name, errorMsg)
                }
            } else {
                onResult(false, null, "Barcode not registered. Add product first.")
            }
        }
    }

    fun scanBarcode(barcode: String, onResult: (Boolean, String?, String?) -> Unit) =
        scanBarcode(barcode, null, onResult)

    fun increment(productId: String, stepMilli: Long = 1000L): String? {
        var warningMsg: String? = null
        updateActiveTab { tab ->
            val index = tab.items.indexOfFirst { it.product.id == productId }
            if (index >= 0) {
                val existing = tab.items[index]
                val product = existing.product
                val unitDef = UnitRegistry.resolve(product.unit)
                if (!unitDef.allowsFractional && stepMilli % 1000L != 0L) {
                    warningMsg = "Fractional increment not allowed for discrete unit '${unitDef.code}'"
                    return@updateActiveTab tab
                }
                val newQtyMilli = existing.quantityMilli + stepMilli
                if (product.trackBatch) {
                    if (product.stockMilli > 0L && newQtyMilli > product.stockMilli) {
                        warningMsg = "Max stock reached (${product.stock} units)"
                        tab
                    } else if (product.stockMilli <= 0L) {
                        warningMsg = "${product.name} is out of stock (batch tracked)"
                        tab
                    } else {
                        val current = tab.items.toMutableList()
                        current[index] = existing.copy(quantityMilli = newQtyMilli)
                        tab.copy(items = current)
                    }
                } else {
                    val current = tab.items.toMutableList()
                    current[index] = existing.copy(quantityMilli = newQtyMilli)
                    if (product.stockMilli <= 0L || newQtyMilli > product.stockMilli) {
                        warningMsg = "Warning: ${product.name} has negative stock"
                    }
                    tab.copy(items = current)
                }
            } else {
                tab
            }
        }
        return warningMsg
    }

    fun decrement(productId: String, stepMilli: Long = 1000L) {
        updateActiveTab { tab ->
            val index = tab.items.indexOfFirst { it.product.id == productId }
            if (index >= 0) {
                val existing = tab.items[index]
                val current = tab.items.toMutableList()
                val unitDef = UnitRegistry.resolve(existing.product.unit)
                val effectiveStep = if (unitDef.allowsFractional && existing.quantityMilli <= stepMilli) {
                    minOf(stepMilli, 100L)
                } else {
                    stepMilli
                }
                if (existing.quantityMilli > effectiveStep) {
                    current[index] = existing.copy(quantityMilli = existing.quantityMilli - effectiveStep)
                } else {
                    current.removeAt(index)
                }
                tab.copy(items = current)
            } else {
                tab
            }
        }
    }

    fun setQuantityMilli(productId: String, quantityMilli: Long): String? {
        if (quantityMilli <= 0L) {
            removeItem(productId)
            return null
        }
        var warningMsg: String? = null
        updateActiveTab { tab ->
            val index = tab.items.indexOfFirst { it.product.id == productId }
            if (index >= 0) {
                val existing = tab.items[index]
                val product = existing.product
                val unitDef = UnitRegistry.resolve(product.unit)
                if (!unitDef.allowsFractional && quantityMilli % 1000L != 0L) {
                    warningMsg = "Fractional quantity not allowed for discrete unit '${unitDef.code}'"
                    return@updateActiveTab tab
                }
                val finalQtyMilli: Long
                if (product.trackBatch) {
                    if (product.stockMilli > 0L && quantityMilli > product.stockMilli) {
                        finalQtyMilli = product.stockMilli
                        warningMsg = "Only ${product.stock} in stock for '${product.name}'"
                    } else if (product.stockMilli <= 0L) {
                        finalQtyMilli = existing.quantityMilli
                        warningMsg = "${product.name} is out of stock (batch tracked)"
                    } else {
                        finalQtyMilli = quantityMilli
                    }
                } else {
                    finalQtyMilli = quantityMilli
                    if (product.stockMilli <= 0L || quantityMilli > product.stockMilli) {
                        warningMsg = "Warning: ${product.name} has negative stock"
                    }
                }
                val current = tab.items.toMutableList()
                current[index] = existing.copy(quantityMilli = finalQtyMilli)
                tab.copy(items = current)
            } else {
                tab
            }
        }
        return warningMsg
    }

    fun setQuantity(productId: String, quantity: Int): String? =
        setQuantityMilli(productId, quantity.toLong() * 1000L)

    fun removeItem(productId: String) {
        updateActiveTab { tab ->
            tab.copy(items = tab.items.filter { it.product.id != productId })
        }
    }

    fun clearCart() {
        updateActiveTab { tab ->
            tab.copy(
                items = emptyList(),
                discountPaise = 0L,
                customerName = "Walk-in Customer",
                customerPhone = "",
                customerId = null,
                resumedDraftNumber = null
            )
        }
    }

    fun setDiscount(discountPaise: Long) {
        updateActiveTab { it.copy(discountPaise = discountPaise.coerceAtLeast(0L)) }
    }

    fun setCustomer(name: String, phone: String = "", id: String? = null) {
        updateActiveTab {
            it.copy(
                customerName = name.ifBlank { "Walk-in Customer" },
                customerPhone = phone.trim(),
                customerId = id
            )
        }
    }

    fun setResumedDraft(
        draftNumber: String?,
        items: List<CartItem>,
        customerName: String? = null,
        customerPhone: String? = null,
        discountPaise: Long = 0L,
        customerId: String? = null
    ) {
        updateActiveTab {
            it.copy(
                resumedDraftNumber = draftNumber,
                items = items,
                customerName = customerName?.ifBlank { "Walk-in Customer" } ?: "Walk-in Customer",
                customerPhone = customerPhone?.trim() ?: "",
                customerId = customerId,
                discountPaise = discountPaise.coerceAtLeast(0L)
            )
        }
    }
}
