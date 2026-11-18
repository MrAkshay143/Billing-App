package com.billing.app.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.CategoryEntity
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.CategoryRepository
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

enum class StockFilter {
    ALL, LOW_STOCK, OUT_OF_STOCK
}

class ProductViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository? = null
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow(StockFilter.ALL)
    val selectedCategory = MutableStateFlow("All")
    val currentPage = MutableStateFlow(1)
    val pageSize = 20

    private val _visibleItemCount = MutableStateFlow(20)
    val visibleItemCount: StateFlow<Int> = _visibleItemCount.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    init {
        viewModelScope.launch {
            searchQuery.collect {
                resetPagination()
            }
        }
    }

    fun loadMoreProducts() {
        if (_isLoadingMore.value) return
        val current = _visibleItemCount.value
        val total = filteredProducts.value.size
        if (current >= total) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            delay(150)
            _visibleItemCount.value = minOf(current + 20, total)
            _isLoadingMore.value = false
        }
    }

    fun resetPagination() {
        _visibleItemCount.value = 20
        _isLoadingMore.value = false
    }

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository?.getAllCategories()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList<CategoryEntity>()).asStateFlow()

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
        resetPagination()
    }

    fun setCategory(category: String) {
        selectedCategory.value = category
        currentPage.value = 1
        resetPagination()
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
            _errorMessage.value = "Please enter product name"
            return
        }
        if (barcode.isBlank()) {
            _errorMessage.value = "Please enter or scan barcode"
            return
        }
        val price = Money.fromRupees(priceStr)
        if (price.paise <= 0L) {
            _errorMessage.value = "Selling price must be greater than ₹0"
            return
        }
        val stock = stockStr.toIntOrNull() ?: 0

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val existing = productRepository.getProductByBarcode(barcode.trim())
                if (existing != null) {
                    _errorMessage.value = "Barcode already registered to another product"
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
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Could not save product. Please try again."
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
        if (name.isBlank()) {
            _errorMessage.value = "Please enter product name"
            return
        }
        if (barcode.isBlank()) {
            _errorMessage.value = "Please enter or scan barcode"
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
                    _errorMessage.value = "Barcode already used by '${existing.name}'"
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
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Could not save product. Please try again."
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
