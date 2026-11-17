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
    val selectedCategory = MutableStateFlow("All")
    val currentPage = MutableStateFlow(1)
    val pageSize = 20

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
        activeFilter,
        selectedCategory
    ) { list, query, filter, category ->
        var result = list

        // Category filter
        if (category != "All" && category.isNotBlank()) {
            result = result.filter { it.category.equals(category, ignoreCase = true) }
        }

        // Query filter
        if (query.isNotBlank()) {
            val q = query.lowercase().trim()
            result = result.filter {
                it.name.lowercase().contains(q) ||
                it.barcode.lowercase().contains(q) ||
                it.sku.lowercase().contains(q)
            }
        }

        // Stock filter
        when (filter) {
            StockFilter.ALL -> result
            StockFilter.LOW_STOCK -> result.filter { it.stock in 1..5 }
            StockFilter.OUT_OF_STOCK -> result.filter { it.stock <= 0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Form draft fields for Add/Edit to avoid losing data during scanner roundtrip
    val draftName = MutableStateFlow("")
    val draftBarcode = MutableStateFlow("")
    val draftPrice = MutableStateFlow("")
    val draftStock = MutableStateFlow("")
    val draftCategory = MutableStateFlow("Grocery")
    val draftSku = MutableStateFlow("")
    val draftImageUri = MutableStateFlow<String?>(null)
    val draftUnit = MutableStateFlow("pc")

    fun clearDraft() {
        draftName.value = ""
        draftBarcode.value = ""
        draftPrice.value = ""
        draftStock.value = ""
        draftCategory.value = "Grocery"
        draftSku.value = ""
        draftImageUri.value = null
        draftUnit.value = "pc"
    }

    fun setDraftFromProduct(product: Product) {
        draftName.value = product.name
        draftBarcode.value = product.barcode
        draftPrice.value = product.price.format(includeSymbol = false)
        draftStock.value = product.stock.toString()
        draftCategory.value = product.category
        draftSku.value = product.sku
        draftImageUri.value = product.imageUri
        draftUnit.value = product.unit
    }

    fun setScannedBarcode(barcode: String) {
        draftBarcode.value = barcode.trim()
    }

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setFilter(filter: StockFilter) {
        activeFilter.value = filter
        currentPage.value = 1
    }

    fun setCategory(category: String) {
        selectedCategory.value = category
        currentPage.value = 1
    }

    fun addProduct(
        name: String,
        barcode: String,
        priceStr: String,
        stockStr: String,
        category: String = "Grocery",
        sku: String = "",
        imageUri: String? = null,
        unit: String = "pc"
    ) {
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
                val existing = productRepository.getProductByBarcode(barcode.trim())
                if (existing != null) {
                    _errorMessage.value = "A product with this barcode already exists!"
                    _isSaving.value = false
                    return@launch
                }

                val autoSku = if (sku.isNotBlank()) sku.trim() else "SKU-${System.currentTimeMillis().toString().takeLast(6)}"
                val product = Product(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    barcode = barcode.trim(),
                    sku = autoSku,
                    category = category.trim(),
                    unit = unit,
                    price = price,
                    stock = stock,
                    imageUri = imageUri
                )
                val result = productRepository.addProduct(product)
                if (result.isSuccess) {
                    clearDraft()
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to save product"
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateProduct(
        id: String,
        name: String,
        barcode: String,
        priceStr: String,
        stockStr: String,
        category: String = "Grocery",
        sku: String = "",
        imageUri: String? = null,
        unit: String = "pc"
    ) {
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
                val trimmedBarcode = barcode.trim()
                val existing = productRepository.getProductByBarcode(trimmedBarcode)
                if (existing != null && existing.id != id) {
                    _errorMessage.value = "A product with barcode '$trimmedBarcode' already exists (${existing.name})"
                    return@launch
                }

                val autoSku = if (sku.isNotBlank()) sku.trim() else "SKU-${System.currentTimeMillis().toString().takeLast(6)}"
                val product = Product(
                    id = id,
                    name = name.trim(),
                    barcode = trimmedBarcode,
                    sku = autoSku,
                    category = category.trim(),
                    unit = unit,
                    price = price,
                    stock = stock,
                    imageUri = imageUri
                )
                val result = productRepository.updateProduct(product)
                if (result.isSuccess) {
                    clearDraft()
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

    fun adjustStock(productId: String, newStock: Int) {
        viewModelScope.launch {
            productRepository.adjustStock(productId, newStock)
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
