package com.billing.app.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.util.Money
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class StockFilter {
    ALL, LOW_STOCK, OUT_OF_STOCK
}

class ProductViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow(StockFilter.ALL)

    val products: StateFlow<List<Product>> = productRepository.getProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCount: StateFlow<Int> = productRepository.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lowStockCount: StateFlow<Int> = productRepository.getLowStockCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val outOfStockCount: StateFlow<Int> = productRepository.getOutOfStockCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val filteredProducts: StateFlow<List<Product>> = combine(
        products,
        searchQuery,
        activeFilter
    ) { list, query, filter ->
        val filteredByQuery = if (query.isBlank()) {
            list
        } else {
            val q = query.lowercase().trim()
            list.filter { it.name.lowercase().contains(q) || it.barcode.lowercase().contains(q) }
        }
        when (filter) {
            StockFilter.ALL -> filteredByQuery
            StockFilter.LOW_STOCK -> filteredByQuery.filter { it.stock in 1..5 }
            StockFilter.OUT_OF_STOCK -> filteredByQuery.filter { it.stock <= 0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setFilter(filter: StockFilter) {
        activeFilter.value = filter
    }

    fun addProduct(name: String, barcode: String, priceStr: String, stockStr: String, imageUri: String? = null) {
        if (_isSaving.value) return
        if (name.isBlank()) {
            _errorMessage.value = "Product name cannot be empty"
            return
        }
        if (barcode.isBlank()) {
            _errorMessage.value = "Barcode cannot be empty"
            return
        }
        val price = Money.fromRupees(priceStr)
        if (price.paise <= 0L) {
            _errorMessage.value = "Price must be greater than 0"
            return
        }
        val stock = stockStr.toIntOrNull() ?: 0

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val existing = productRepository.getProductByBarcode(barcode)
                if (existing != null) {
                    _errorMessage.value = "A product with this barcode already exists!"
                    _isSaving.value = false
                    return@launch
                }

                val product = Product(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    barcode = barcode.trim(),
                    price = price,
                    stock = stock,
                    imageUri = imageUri
                )
                val result = productRepository.addProduct(product)
                if (result.isSuccess) {
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to save product"
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateProduct(id: String, name: String, barcode: String, priceStr: String, stockStr: String, imageUri: String? = null) {
        if (_isSaving.value) return
        if (name.isBlank() || barcode.isBlank()) {
            _errorMessage.value = "Name and Barcode cannot be empty"
            return
        }
        val price = Money.fromRupees(priceStr)
        val stock = stockStr.toIntOrNull() ?: 0

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val product = Product(
                    id = id,
                    name = name.trim(),
                    barcode = barcode.trim(),
                    price = price,
                    stock = stock,
                    imageUri = imageUri
                )
                val result = productRepository.updateProduct(product)
                if (result.isSuccess) {
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to update product"
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(id)
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
