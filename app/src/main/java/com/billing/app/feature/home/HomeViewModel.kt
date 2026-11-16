package com.billing.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.audio.SoundHapticManager
import com.billing.app.core.util.Money
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val cartItems: List<CartItem> = emptyList(),
    val errorMessage: String? = null,
    val isCameraOn: Boolean = true,
    val isFlashOn: Boolean = false
)

class HomeViewModel(
    private val productRepository: ProductRepository,
    private val soundHapticManager: SoundHapticManager,
    private val shopRepository: ShopRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val storeName: StateFlow<String> = shopRepository?.getShopProfile()
        ?.map { it.name.ifBlank { "Dokandaar" } }
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, "Dokandaar")
        ?: MutableStateFlow("Dokandaar").asStateFlow()

    val totalAmount: StateFlow<Money> = _uiState.map { state ->
        state.cartItems.fold(Money.ZERO) { acc, item -> acc + item.total }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Money.ZERO)

    val totalItems: StateFlow<Int> = _uiState.map { state ->
        state.cartItems.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = productRepository.getProductByBarcode(barcode)
            if (product != null) {
                soundHapticManager.triggerScanFeedback()
                addProductToCart(product)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Product not found")
            }
        }
    }

    fun addProductToCart(product: Product) {
        val currentItems = _uiState.value.cartItems.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = currentItems[index]
            if (product.stock > 0 && existing.quantity >= product.stock) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Cannot add more. Only ${product.stock} in stock for '${product.name}'"
                )
                return
            }
            currentItems[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            if (product.stock <= 0) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "'${product.name}' is currently out of stock!"
                )
                return
            }
            currentItems.add(CartItem(product = product, quantity = 1))
        }
        _uiState.value = _uiState.value.copy(cartItems = currentItems, errorMessage = null)
    }

    fun incrementQuantity(productId: String) {
        val currentItems = _uiState.value.cartItems.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val current = currentItems[index]
            val stock = current.product.stock
            if (stock > 0 && current.quantity >= stock) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Max stock reached for '${current.product.name}' (${stock} units)"
                )
                return
            }
            currentItems[index] = current.copy(quantity = current.quantity + 1)
            _uiState.value = _uiState.value.copy(cartItems = currentItems, errorMessage = null)
        }
    }

    fun decrementQuantity(productId: String) {
        val currentItems = _uiState.value.cartItems.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val current = currentItems[index]
            if (current.quantity <= 1) {
                removeItem(productId)
            } else {
                currentItems[index] = current.copy(quantity = current.quantity - 1)
                _uiState.value = _uiState.value.copy(cartItems = currentItems, errorMessage = null)
            }
        }
    }

    fun updateQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            removeItem(productId)
            return
        }
        val currentItems = _uiState.value.cartItems.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val current = currentItems[index]
            val stock = current.product.stock
            val finalQty = if (stock > 0 && quantity > stock) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Only ${stock} in stock for '${current.product.name}'"
                )
                stock
            } else {
                quantity
            }
            currentItems[index] = current.copy(quantity = finalQty)
            _uiState.value = _uiState.value.copy(cartItems = currentItems)
        }
    }

    fun removeItem(productId: String) {
        val filtered = _uiState.value.cartItems.filter { it.product.id != productId }
        _uiState.value = _uiState.value.copy(cartItems = filtered)
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(cartItems = emptyList())
    }

    fun loadCartItems(items: List<CartItem>) {
        _uiState.value = _uiState.value.copy(cartItems = items)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun toggleCamera() {
        _uiState.value = _uiState.value.copy(isCameraOn = !_uiState.value.isCameraOn)
    }

    fun toggleFlash() {
        _uiState.value = _uiState.value.copy(isFlashOn = !_uiState.value.isFlashOn)
    }
}
