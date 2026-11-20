package com.billing.app.feature.checkout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.util.Money
import com.billing.app.domain.model.CartItem
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CartItemEditDialog(
    item: CartItem,
    onDismissRequest: () -> Unit,
    onSave: (newQuantityMilli: Long, newPricePaise: Long, discountPaise: Long) -> Unit,
    onRemove: () -> Unit
) {
    val strings = appStrings()
    val initialQtyStr = remember(item) {
        val qty = item.quantityMilli / 1000.0
        if (item.quantityMilli % 1000L == 0L) {
            (item.quantityMilli / 1000L).toString()
        } else {
            String.format(Locale.US, "%.3f", qty).trimEnd('0').trimEnd('.')
        }
    }
    val initialPriceStr = remember(item) {
        String.format(Locale.US, "%.2f", item.effectiveUnitPricePaise / 100.0).trimEnd('0').trimEnd('.')
    }
    val initialDiscountStr = remember(item) {
        if (item.discountPaise > 0) {
            String.format(Locale.US, "%.2f", item.discountPaise / 100.0).trimEnd('0').trimEnd('.')
        } else ""
    }

    var quantityInput by remember { mutableStateOf(initialQtyStr) }
    var priceInput by remember { mutableStateOf(initialPriceStr) }
    var discountInput by remember { mutableStateOf(initialDiscountStr) }

    fun addQuantity(delta: Double) {
        val current = quantityInput.toDoubleOrNull() ?: 0.0
        val updated = (current + delta).coerceAtLeast(0.001)
        quantityInput = String.format(Locale.US, "%.3f", updated).trimEnd('0').trimEnd('.')
    }

    val currentQty = quantityInput.toDoubleOrNull() ?: 0.0
    val currentQtyMilli = (currentQty * 1000.0).toLong().coerceAtLeast(1L)
    val currentPricePaise = Money.fromRupees(priceInput).paise
    val currentDiscountPaise = Money.fromRupees(discountInput).paise
    val grossLinePaise = (currentPricePaise * currentQtyMilli + 500L) / 1000L
    val calculatedTotalPaise = (grossLinePaise - currentDiscountPaise).coerceAtLeast(0L)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Column {
                Text(
                    text = item.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = DokandaarTheme.textPrimary
                )
                Text(
                    text = "Standard Price: ${item.product.price.format()} / ${item.product.unit.lowercase()}",
                    fontSize = 11.sp,
                    color = DokandaarTheme.textMuted
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Quantity input with quick addition chips
                Text(
                    text = "Quantity (${item.product.unit})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DokandaarTheme.textPrimary
                )
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Chips: +100g, +250g, +500g, +1kg, +1 pc
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val chips = listOf(
                        Pair("+100g", 0.100),
                        Pair("+250g", 0.250),
                        Pair("+500g", 0.500),
                        Pair("+1kg", 1.000),
                        Pair("+1 pc", 1.000)
                    )
                    chips.forEach { (label, delta) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PastelPurpleBg,
                            modifier = Modifier.clickable { addQuantity(delta) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Unit Price Override
                Text(
                    text = strings.unitPriceOverride,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DokandaarTheme.textPrimary
                )
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (currentPricePaise != item.product.price.paise) {
                            Text(
                                text = "Overriding ${item.product.price.format()}",
                                color = PrimaryPurple,
                                fontSize = 11.sp
                            )
                        }
                    }
                )

                // 3. Line Discount
                Text(
                    text = strings.lineDiscountLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DokandaarTheme.textPrimary
                )
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. Live Calculation Summary
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PastelPurpleBg.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.grandTotalLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = DokandaarTheme.textPrimary
                        )
                        Text(
                            text = Money(calculatedTotalPaise).format(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = PrimaryPurple
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qtyDouble = quantityInput.toDoubleOrNull() ?: 1.0
                    val qtyMilli = (qtyDouble * 1000.0).toLong().coerceAtLeast(1L)
                    val pricePaise = Money.fromRupees(priceInput).paise
                    val discPaise = Money.fromRupees(discountInput).paise
                    onSave(qtyMilli, pricePaise, discPaise)
                }
            ) {
                Text(strings.apply, color = PrimaryPurple, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onRemove
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = ErrorColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.delete, color = ErrorColor, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismissRequest) {
                    Text(strings.cancel)
                }
            }
        }
    )
}
