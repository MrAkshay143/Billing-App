package com.billing.app.feature.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billing.app.core.util.UnitRegistry
import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.PurchaseItemEntity
import com.billing.app.data.local.entity.SupplierEntity
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import com.billing.app.domain.repository.PurchaseRepository
import com.billing.app.domain.repository.ShiftRepository
import com.billing.app.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PurchaseItemDraft(
    val product: Product,
    val quantityMilli: Long,
    val unitPricePaise: Long,
    val gstRateBp: Int = 0,
    val mrpPaise: Long = 0L,
    val batchNumber: String = "",
    val expiryDate: Long? = null
)

class PurchasesViewModel(
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val shiftRepository: ShiftRepository
) : ViewModel() {

    val purchases: StateFlow<List<PurchaseEntity>> = purchaseRepository.getAllPurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = supplierRepository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = productRepository.getProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedSupplier = MutableStateFlow<SupplierEntity?>(null)
    val invoiceNumber = MutableStateFlow("")
    val isTaxInclusive = MutableStateFlow(true)
    val paymentMode = MutableStateFlow("CASH") // "CASH", "CREDIT", "UPI"
    val paidAmountPaise = MutableStateFlow(0L)
    val items = MutableStateFlow<List<PurchaseItemDraft>>(emptyList())
    val isSaving = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)
    val successMessage = MutableStateFlow<String?>(null)

    fun setSelectedSupplier(supplier: SupplierEntity?) {
        selectedSupplier.value = supplier
    }

    fun setInvoiceNumber(number: String) {
        invoiceNumber.value = number
    }

    fun setTaxInclusive(inclusive: Boolean) {
        isTaxInclusive.value = inclusive
        val (_, _, grandTotal) = calculateTotals()
        if (paymentMode.value != "CREDIT") {
            paidAmountPaise.value = grandTotal
        }
    }

    fun setPaymentMode(mode: String) {
        paymentMode.value = mode
        val (_, _, grandTotal) = calculateTotals()
        if (mode == "CREDIT") {
            paidAmountPaise.value = 0L
        } else {
            if (paidAmountPaise.value == 0L || paidAmountPaise.value < grandTotal) {
                paidAmountPaise.value = grandTotal
            }
        }
    }

    fun setPaidAmountPaise(amount: Long) {
        paidAmountPaise.value = amount
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun clearSuccess() {
        successMessage.value = null
    }

    fun addItem(item: PurchaseItemDraft): Boolean {
        if (item.quantityMilli <= 0L) {
            errorMessage.value = "Quantity must be greater than zero"
            return false
        }
        val unitDef = UnitRegistry.resolve(item.product.unit)
        if (!unitDef.allowsFractional && item.quantityMilli % 1000L != 0L) {
            errorMessage.value = "Fractional quantity is not allowed for discrete unit '${unitDef.code}'"
            return false
        }
        if (item.unitPricePaise < 0L) {
            errorMessage.value = "Unit price cannot be negative"
            return false
        }
        if (item.product.trackBatch) {
            if (item.batchNumber.isBlank()) {
                errorMessage.value = "Batch number is mandatory for ${item.product.name}"
                return false
            }
            if (item.expiryDate == null) {
                errorMessage.value = "Expiry date is mandatory for ${item.product.name}"
                return false
            }
            if (item.mrpPaise <= 0L) {
                errorMessage.value = "MRP is mandatory for ${item.product.name}"
                return false
            }
        }

        items.value = items.value + item
        val (_, _, newGrandTotal) = calculateTotals()
        if (paymentMode.value != "CREDIT") {
            paidAmountPaise.value = newGrandTotal
        }
        errorMessage.value = null
        return true
    }

    fun removeItem(index: Int) {
        if (index in items.value.indices) {
            val updated = items.value.toMutableList()
            updated.removeAt(index)
            items.value = updated
            val (_, _, newGrandTotal) = calculateTotals()
            if (paymentMode.value != "CREDIT") {
                paidAmountPaise.value = newGrandTotal
            }
        }
    }

    fun clearDraft() {
        selectedSupplier.value = null
        invoiceNumber.value = ""
        isTaxInclusive.value = true
        paymentMode.value = "CASH"
        paidAmountPaise.value = 0L
        items.value = emptyList()
        errorMessage.value = null
        successMessage.value = null
        isSaving.value = false
    }

    fun calculateTotals(): Triple<Long, Long, Long> {
        var subtotal = 0L
        var tax = 0L
        var grandTotal = 0L
        val inclusive = isTaxInclusive.value

        for (item in items.value) {
            if (inclusive) {
                val lineTotal = (item.unitPricePaise * item.quantityMilli + 500L) / 1000L
                val baseAmount = if (item.gstRateBp > 0) {
                    (lineTotal * 10000L + (10000L + item.gstRateBp) / 2) / (10000L + item.gstRateBp)
                } else {
                    lineTotal
                }
                val taxAmount = lineTotal - baseAmount
                subtotal += baseAmount
                tax += taxAmount
                grandTotal += lineTotal
            } else {
                val baseAmount = (item.unitPricePaise * item.quantityMilli + 500L) / 1000L
                val taxAmount = if (item.gstRateBp > 0) {
                    (baseAmount * item.gstRateBp + 5000L) / 10000L
                } else {
                    0L
                }
                val lineTotal = baseAmount + taxAmount
                subtotal += baseAmount
                tax += taxAmount
                grandTotal += lineTotal
            }
        }
        return Triple(subtotal, tax, grandTotal)
    }

    fun savePurchase(onSuccess: () -> Unit) {
        val supplier = selectedSupplier.value
        if (supplier == null) {
            errorMessage.value = "Please select a supplier"
            return
        }
        val invNum = invoiceNumber.value.trim()
        if (invNum.isBlank()) {
            errorMessage.value = "Invoice number is required"
            return
        }
        if (items.value.isEmpty()) {
            errorMessage.value = "Please add at least one item"
            return
        }

        for (draft in items.value) {
            val unitDef = UnitRegistry.resolve(draft.product.unit)
            if (!unitDef.allowsFractional && draft.quantityMilli % 1000L != 0L) {
                errorMessage.value = "Fractional quantity is not allowed for discrete unit '${unitDef.code}' (${draft.product.name})"
                return
            }
            if (draft.product.trackBatch) {
                if (draft.batchNumber.isBlank() || draft.expiryDate == null || draft.mrpPaise <= 0L) {
                    errorMessage.value = "Complete batch details (Batch, Expiry, MRP) required for ${draft.product.name}"
                    return
                }
            }
        }

        viewModelScope.launch {
            isSaving.value = true
            errorMessage.value = null
            try {
                var activeShiftId: Long? = null
                if (paymentMode.value.equals("CASH", ignoreCase = true) && paidAmountPaise.value > 0L) {
                    val activeShift = shiftRepository.getActiveShift()
                    if (activeShift == null) {
                        errorMessage.value = "Active shift is required for cash payment"
                        isSaving.value = false
                        return@launch
                    }
                    activeShiftId = activeShift.id
                }

                val (_, _, grandTotal) = calculateTotals()
                val inclusive = isTaxInclusive.value

                val purchaseItems = items.value.map { draft ->
                    val lineTotalPaise = if (inclusive) {
                        (draft.unitPricePaise * draft.quantityMilli + 500L) / 1000L
                    } else {
                        val base = (draft.unitPricePaise * draft.quantityMilli + 500L) / 1000L
                        val gst = if (draft.gstRateBp > 0) (base * draft.gstRateBp + 5000L) / 10000L else 0L
                        base + gst
                    }
                    PurchaseItemEntity(
                        purchaseId = 0L,
                        productId = draft.product.id,
                        productName = draft.product.name,
                        quantityMilli = draft.quantityMilli,
                        unitPricePaise = draft.unitPricePaise,
                        lineTotalPaise = lineTotalPaise,
                        gstRateBp = draft.gstRateBp,
                        batchNumber = draft.batchNumber.trim().ifBlank { null },
                        expiryDate = draft.expiryDate,
                        mrpPaise = draft.mrpPaise,
                        unit = UnitRegistry.resolve(draft.product.unit).code
                    )
                }

                val paid = when (paymentMode.value) {
                    "CREDIT" -> 0L
                    else -> paidAmountPaise.value
                }.coerceAtMost(grandTotal)
                val due = (grandTotal - paid).coerceAtLeast(0L)
                val payStatus = if (due == 0L) "PAID" else if (paid == 0L) "DUE" else "PARTIAL"

                val purchase = PurchaseEntity(
                    purchaseNumber = invNum,
                    supplierId = supplier.id,
                    supplierName = supplier.name,
                    timestamp = System.currentTimeMillis(),
                    totalPaise = grandTotal,
                    paymentStatus = payStatus,
                    shiftId = activeShiftId,
                    paidPaise = paid,
                    duePaise = due,
                    status = "RECEIVED"
                )

                val result = purchaseRepository.addPurchase(purchase, purchaseItems, paymentMode.value)
                if (result.isSuccess) {
                    clearDraft()
                    successMessage.value = "GRN saved successfully"
                    onSuccess()
                } else {
                    errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to save purchase"
                }
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                isSaving.value = false
            }
        }
    }
}
