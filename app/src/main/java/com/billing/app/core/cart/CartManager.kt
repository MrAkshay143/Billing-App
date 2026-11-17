package com.billing.app.core.cart

import com.billing.app.core.audio.SoundHapticManager
import com.billing.app.core.util.Money
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CartManager(
    private val productRepository: ProductRepository,
    private val soundHapticManager: SoundHapticManager
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _discountPaise = MutableStateFlow(0L)
    val discountPaise: StateFlow<Long> = _discountPaise.asStateFlow()

    private val _customerName = MutableStateFlow("Walk-in Customer")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _resumedDraftNumber = MutableStateFlow<String?>(null)
    val resumedDraftNumber: StateFlow<String?> = _resumedDraftNumber.asStateFlow()

    val totalItemsCount: Int
        get() = _cartItems.value.size

    val totalQuantityCount: Int
        get() = _cartItems.value.sumOf { it.quantity }

    val subtotal: Money
        get() = _cartItems.value.fold(Money.ZERO) { acc, item -> acc + item.total }

    val grandTotal: Money
        get() {
            val discount = Money(_discountPaise.value)
            return if (discount >= subtotal) Money.ZERO else subtotal - discount
        }

    fun addProduct(product: Product, quantity: Int = 1): Pair<Boolean, String?> {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            if (product.stock > 0 && existing.quantity + quantity > product.stock) {
                return Pair(false, "Cannot add more. Only ${product.stock} in stock for '${product.name}'")
            }
            current[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            if (product.stock <= 0) {
                return Pair(false, "'${product.name}' is currently out of stock!")
            }
            val initialQty = if (product.stock > 0 && quantity > product.stock) product.stock else quantity
            current.add(CartItem(product = product, quantity = initialQty))
        }
        _cartItems.value = current
        soundHapticManager.triggerScanFeedback()
        return Pair(true, null)
    }

    fun scanBarcode(barcode: String, onResult: (Boolean, String?, String?) -> Unit) {
        scope.launch {
            val cleanBarcode = barcode.trim()
            val product = productRepository.getProductByBarcode(cleanBarcode)
            if (product != null) {
                val (success, errorMsg) = addProduct(product, 1)
                if (success) {
                    onResult(true, product.name, null)
                } else {
                    onResult(false, product.name, errorMsg)
                }
            } else {
                onResult(false, null, "Product not found")
            }
        }
    }

    fun increment(productId: String): String? {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val existing = current[index]
            val stock = existing.product.stock
            if (stock > 0 && existing.quantity >= stock) {
                return "Max stock reached for '${existing.product.name}' ($stock units)"
            }
            current[index] = existing.copy(quantity = existing.quantity + 1)
            _cartItems.value = current
        }
        return null
    }

    fun decrement(productId: String) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val existing = current[index]
            if (existing.quantity > 1) {
                current[index] = existing.copy(quantity = existing.quantity - 1)
            } else {
                current.removeAt(index)
            }
            _cartItems.value = current
        }
    }

    fun setQuantity(productId: String, quantity: Int): String? {
        if (quantity <= 0) {
            removeItem(productId)
            return null
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val existing = current[index]
            val stock = existing.product.stock
            val finalQty = if (stock > 0 && quantity > stock) stock else quantity
            current[index] = existing.copy(quantity = finalQty)
            _cartItems.value = current
            if (stock > 0 && quantity > stock) {
                return "Only $stock in stock for '${existing.product.name}'"
            }
        }
        return null
    }

    fun removeItem(productId: String) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _discountPaise.value = 0L
        _customerName.value = "Walk-in Customer"
        _customerPhone.value = ""
        _resumedDraftNumber.value = null
    }

    fun setDiscount(discountPaise: Long) {
        _discountPaise.value = discountPaise.coerceAtLeast(0L)
    }

    fun setCustomer(name: String, phone: String = "") {
        _customerName.value = name.ifBlank { "Walk-in Customer" }
        _customerPhone.value = phone.trim()
    }

    fun setResumedDraft(
        draftNumber: String?,
        items: List<CartItem>,
        customerName: String? = null,
        customerPhone: String? = null,
        discountPaise: Long = 0L
    ) {
        _resumedDraftNumber.value = draftNumber
        _cartItems.value = items
        _customerName.value = customerName?.ifBlank { "Walk-in Customer" } ?: "Walk-in Customer"
        _customerPhone.value = customerPhone?.trim() ?: ""
        _discountPaise.value = discountPaise.coerceAtLeast(0L)
    }
}
