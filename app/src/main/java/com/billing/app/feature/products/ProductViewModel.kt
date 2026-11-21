package com.billing.app.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.util.Money
import com.billing.app.core.util.UnitRegistry
import com.billing.app.data.local.dao.ProductBatchDao
import com.billing.app.data.local.entity.CategoryEntity
import com.billing.app.data.local.entity.ProductBatchEntity
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.CategoryRepository
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.StockAdjustmentInput
import com.billing.app.domain.repository.StockAdjustmentReason
import com.billing.app.domain.repository.StockAdjustmentRepository
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
    private val categoryRepository: CategoryRepository? = null,
    private val stockAdjustmentRepository: StockAdjustmentRepository? = null,
    private val productBatchDao: ProductBatchDao? = null
) : ViewModel() {

    private val effectiveBatchDao: ProductBatchDao?
        get() = productBatchDao ?: runCatching { com.billing.app.BillingApplication.instance.database.productBatchDao() }.getOrNull()

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
            StockFilter.LOW_STOCK -> result.filter { 
                it.stockMilli > 0L && it.stockMilli <= it.minStockAlertMilli 
            }
            StockFilter.OUT_OF_STOCK -> result.filter { it.stockMilli <= 0L }
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
    val draftUnit = MutableStateFlow("PCS")
    val isUnitLocked = MutableStateFlow(false)
    val draftPurchasePrice = MutableStateFlow("")
    val draftMrp = MutableStateFlow("")
    val draftGstRate = MutableStateFlow("0")
    val draftMinStockAlert = MutableStateFlow("5")
    val draftHsnCode = MutableStateFlow("")
    val draftTrackBatch = MutableStateFlow(false)

    fun clearDraft() {
        draftName.value = ""
        draftBarcode.value = ""
        draftPrice.value = ""
        draftStock.value = ""
        draftCategory.value = "Grocery"
        draftSku.value = ""
        draftImageUri.value = null
        draftUnit.value = "PCS"
        isUnitLocked.value = false
        draftPurchasePrice.value = ""
        draftMrp.value = ""
        draftGstRate.value = "0"
        draftMinStockAlert.value = "5"
        draftHsnCode.value = ""
        draftTrackBatch.value = false
    }

    fun initFromProduct(product: Product) {
        draftName.value = product.name
        draftBarcode.value = product.barcode
        draftPrice.value = product.price.format(includeSymbol = false)
        draftStock.value = product.stock.toString()
        draftCategory.value = product.category
        draftSku.value = product.sku
        draftImageUri.value = product.imageUri
        draftUnit.value = UnitRegistry.resolve(product.unit).code
        viewModelScope.launch {
            val txCount = productRepository.getProductTransactionCount(product.id)
            isUnitLocked.value = product.stockMilli != 0L || txCount > 0
        }
        draftPurchasePrice.value = if (product.purchasePrice.paise > 0) (product.purchasePrice.paise / 100.0).toString() else ""
        draftMrp.value = if (product.mrp.paise > 0) (product.mrp.paise / 100.0).toString() else ""
        draftGstRate.value = (product.gstRateBp / 100).toString()
        draftMinStockAlert.value = (product.minStockAlertMilli / 1000L).toString()
        draftHsnCode.value = product.hsnCode ?: ""
        draftTrackBatch.value = product.trackBatch
    }

    fun setDraftFromProduct(product: Product) {
        initFromProduct(product)
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

    fun saveProduct(
        name: String = draftName.value,
        barcode: String = draftBarcode.value,
        priceStr: String = draftPrice.value,
        stockStr: String = draftStock.value,
        category: String = draftCategory.value,
        sku: String = draftSku.value,
        imageUri: String? = draftImageUri.value,
        unit: String = draftUnit.value,
        purchasePriceStr: String = draftPurchasePrice.value,
        mrpStr: String = draftMrp.value,
        gstRateStr: String = draftGstRate.value,
        minStockAlertStr: String = draftMinStockAlert.value,
        hsnCodeStr: String = draftHsnCode.value,
        trackBatch: Boolean = draftTrackBatch.value
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
        val stockMilli = (stockStr.toDoubleOrNull()?.let { kotlin.math.round(it * 1000.0).toLong() }) ?: (stockStr.toIntOrNull()?.toLong()?.times(1000L) ?: 0L)
        val stock = (stockMilli / 1000L).toInt()
        val purchasePrice = Money.fromRupees(purchasePriceStr)
        val mrp = Money.fromRupees(mrpStr)
        val gstRateBp = (gstRateStr.toIntOrNull() ?: 0) * 100
        val minStockAlertMilli = ((minStockAlertStr.toDoubleOrNull() ?: 5.0) * 1000L).toLong()
        val hsnCode = hsnCodeStr.trim().ifBlank { null }

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
                    price = price,
                    stockMilli = stockMilli,
                    imageUri = imageUri,
                    sku = autoSku,
                    category = category.trim(),
                    unit = unit,
                    purchasePrice = purchasePrice,
                    mrp = mrp,
                    minStockAlertMilli = minStockAlertMilli,
                    gstRateBp = gstRateBp,
                    trackBatch = trackBatch,
                    hsnCode = hsnCode
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

    fun addProduct(
        name: String = draftName.value,
        barcode: String = draftBarcode.value,
        priceStr: String = draftPrice.value,
        stockStr: String = draftStock.value,
        category: String = draftCategory.value,
        sku: String = draftSku.value,
        imageUri: String? = null,
        unit: String = "pc",
        purchasePriceStr: String = draftPurchasePrice.value,
        mrpStr: String = draftMrp.value,
        gstRateStr: String = draftGstRate.value,
        minStockAlertStr: String = draftMinStockAlert.value,
        hsnCodeStr: String = draftHsnCode.value,
        trackBatch: Boolean = draftTrackBatch.value
    ) {
        saveProduct(
            name = name,
            barcode = barcode,
            priceStr = priceStr,
            stockStr = stockStr,
            category = category,
            sku = sku,
            imageUri = imageUri,
            unit = unit,
            purchasePriceStr = purchasePriceStr,
            mrpStr = mrpStr,
            gstRateStr = gstRateStr,
            minStockAlertStr = minStockAlertStr,
            hsnCodeStr = hsnCodeStr,
            trackBatch = trackBatch
        )
    }

    fun updateProduct(
        id: String,
        name: String = draftName.value,
        barcode: String = draftBarcode.value,
        priceStr: String = draftPrice.value,
        stockStr: String = draftStock.value,
        category: String = draftCategory.value,
        sku: String = draftSku.value,
        imageUri: String? = draftImageUri.value,
        unit: String = draftUnit.value,
        purchasePriceStr: String? = null,
        mrpStr: String? = null,
        gstRateStr: String? = null,
        minStockAlertStr: String? = null,
        hsnCodeStr: String? = null,
        trackBatch: Boolean? = null,
        isActive: Boolean? = null
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
        val stockMilli = (stockStr.toDoubleOrNull()?.let { kotlin.math.round(it * 1000.0).toLong() }) ?: (stockStr.toIntOrNull()?.toLong()?.times(1000L) ?: 0L)
        val stock = (stockMilli / 1000L).toInt()

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val trimmedBarcode = barcode.trim()
                val existing = productRepository.getProductByBarcode(trimmedBarcode)
                if (existing != null && existing.id != id) {
                    _errorMessage.value = "Barcode already used by '${existing.name}'"
                    return@launch
                }

                val existingInDb = productRepository.getProductById(id)

                val effPurchasePriceStr = purchasePriceStr ?: draftPurchasePrice.value
                val parsedPurchasePrice = if (effPurchasePriceStr.isNotBlank()) Money.fromRupees(effPurchasePriceStr) else null
                val finalPurchasePrice = when {
                    parsedPurchasePrice != null && parsedPurchasePrice.paise > 0L -> parsedPurchasePrice
                    existingInDb != null && existingInDb.purchasePrice.paise > 0L -> existingInDb.purchasePrice
                    parsedPurchasePrice != null -> parsedPurchasePrice
                    else -> existingInDb?.purchasePrice ?: Money.ZERO
                }

                val effMrpStr = mrpStr ?: draftMrp.value
                val parsedMrp = if (effMrpStr.isNotBlank()) Money.fromRupees(effMrpStr) else null
                val finalMrp = when {
                    parsedMrp != null && parsedMrp.paise > 0L -> parsedMrp
                    existingInDb != null && existingInDb.mrp.paise > 0L -> existingInDb.mrp
                    parsedMrp != null -> parsedMrp
                    else -> existingInDb?.mrp ?: Money.ZERO
                }

                val finalGstRateBp = when {
                    gstRateStr != null -> (gstRateStr.toIntOrNull() ?: 0) * 100
                    existingInDb != null -> existingInDb.gstRateBp
                    draftGstRate.value.isNotBlank() -> (draftGstRate.value.toIntOrNull() ?: 0) * 100
                    else -> 0
                }

                val finalMinStockAlertMilli = when {
                    minStockAlertStr != null && minStockAlertStr.isNotBlank() -> {
                        ((minStockAlertStr.toDoubleOrNull() ?: 5.0) * 1000L).toLong()
                    }
                    existingInDb != null -> existingInDb.minStockAlertMilli
                    draftMinStockAlert.value.isNotBlank() -> {
                        ((draftMinStockAlert.value.toDoubleOrNull() ?: 5.0) * 1000L).toLong()
                    }
                    else -> 5000L
                }

                val finalHsnCode = when {
                    hsnCodeStr != null -> hsnCodeStr.trim().ifBlank { existingInDb?.hsnCode }
                    existingInDb != null -> existingInDb.hsnCode
                    draftHsnCode.value.isNotBlank() -> draftHsnCode.value.trim()
                    else -> null
                }

                val finalTrackBatch = when {
                    trackBatch != null -> trackBatch
                    existingInDb != null -> existingInDb.trackBatch
                    else -> draftTrackBatch.value
                }

                val finalIsActive = isActive ?: existingInDb?.isActive ?: true

                val autoSku = if (sku.isNotBlank()) sku.trim() else existingInDb?.sku?.ifBlank { null } ?: "SKU-${System.currentTimeMillis().toString().takeLast(6)}"

                val product = Product(
                    id = id,
                    name = name.trim(),
                    barcode = trimmedBarcode,
                    sku = autoSku,
                    category = category.trim(),
                    unit = unit,
                    price = price,
                    stockMilli = stockMilli,
                    imageUri = imageUri,
                    purchasePrice = finalPurchasePrice,
                    mrp = finalMrp,
                    minStockAlertMilli = finalMinStockAlertMilli,
                    gstRateBp = finalGstRateBp,
                    description = existingInDb?.description ?: "",
                    trackBatch = finalTrackBatch,
                    hsnCode = finalHsnCode,
                    isActive = finalIsActive,
                    createdAt = existingInDb?.createdAt ?: System.currentTimeMillis()
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

    fun adjustStock(
        productId: String,
        newStock: Double,
        reason: StockAdjustmentReason = StockAdjustmentReason.CORRECTION,
        managerUserId: String = "Admin",
        notes: String = "",
        batchId: Long? = null
    ) {
        viewModelScope.launch {
            val targetStockMilli = kotlin.math.round(newStock * 1000.0).toLong()
            val stockDisplay = if (newStock % 1.0 == 0.0) newStock.toLong().toString() else newStock.toString()

            if (stockAdjustmentRepository != null) {
                val currentProduct = productRepository.getProductById(productId)
                val currentStockMilli = currentProduct?.stockMilli ?: (products.value.find { it.id == productId }?.stockMilli ?: 0L)
                val diffMilli = targetStockMilli - currentStockMilli
                if (diffMilli != 0L) {
                    var resolvedBatchId = batchId
                    if (resolvedBatchId == null && currentProduct?.trackBatch == true) {
                        val activeBatches = effectiveBatchDao?.getActiveBatchesFEFO(productId) ?: emptyList()
                        val matchingBatch = if (diffMilli < 0L) {
                            activeBatches.firstOrNull { it.currentStockMilli >= -diffMilli } ?: activeBatches.firstOrNull()
                        } else {
                            activeBatches.firstOrNull()
                        } ?: effectiveBatchDao?.getBatchListForProduct(productId)?.firstOrNull()

                        resolvedBatchId = matchingBatch?.id ?: run {
                            effectiveBatchDao?.let { dao ->
                                val initialBatch = ProductBatchEntity(
                                    productId = productId,
                                    batchNumber = "BATCH-INIT",
                                    expiryDate = null,
                                    purchasePricePaise = currentProduct.purchasePrice.paise,
                                    mrpPaise = currentProduct.mrp.paise,
                                    initialStockMilli = currentStockMilli,
                                    currentStockMilli = currentStockMilli,
                                    createdAt = System.currentTimeMillis()
                                )
                                dao.insertBatch(initialBatch)
                            }
                        }
                    }

                    val input = StockAdjustmentInput(
                        productId = productId,
                        batchId = resolvedBatchId,
                        adjustedQuantityMilli = diffMilli,
                        reason = reason.code,
                        notes = notes.ifBlank { "Manual stock adjustment: $stockDisplay (${reason.displayNameEn})" },
                        managerUserId = managerUserId
                    )
                    stockAdjustmentRepository.adjustStock(input)
                }
            } else {
                productRepository.adjustStock(productId, newStock.toInt())
            }
        }
    }

    fun adjustStock(
        productId: String,
        newStock: Int,
        reason: StockAdjustmentReason = StockAdjustmentReason.CORRECTION,
        managerUserId: String = "Admin",
        notes: String = "",
        batchId: Long? = null
    ) {
        adjustStock(
            productId = productId,
            newStock = newStock.toDouble(),
            reason = reason,
            managerUserId = managerUserId,
            notes = notes,
            batchId = batchId
        )
    }

    fun adjustStock(
        productId: String,
        newStockStr: String,
        reason: StockAdjustmentReason = StockAdjustmentReason.CORRECTION,
        managerUserId: String = "Admin",
        notes: String = "",
        batchId: Long? = null
    ) {
        val qty = newStockStr.toDoubleOrNull() ?: return
        adjustStock(
            productId = productId,
            newStock = qty,
            reason = reason,
            managerUserId = managerUserId,
            notes = notes,
            batchId = batchId
        )
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
