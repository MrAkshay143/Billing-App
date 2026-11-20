package com.billing.app.feature.checkout.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.util.Money
import com.billing.app.core.util.UnitRegistry
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCustomItemDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, pricePaise: Long, quantityMilli: Long, unit: String) -> Unit
) {
    val strings = appStrings()
    var name by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("1.0") }
    var selectedUnit by remember { mutableStateOf("PCS") }
    var isNameError by remember { mutableStateOf(false) }

    val units = UnitRegistry.STANDARD_UNITS.map { it.code }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = strings.addCustomItemTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = DokandaarTheme.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (isNameError && it.isNotBlank()) isNameError = false
                    },
                    label = { Text(strings.itemName) },
                    placeholder = { Text("Open item") },
                    isError = isNameError,
                    supportingText = if (isNameError) {
                        { Text(strings.itemNameRequired, color = Color(0xFFEF4444)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Price
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text(strings.unitPriceOverride) },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quantity & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text(strings.unitLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Unit selection chips
                Text(
                    text = strings.unitLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DokandaarTheme.textPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    units.forEach { unit ->
                        val isSelected = selectedUnit.equals(unit, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PrimaryPurple else PastelPurpleBg,
                            modifier = Modifier.clickable { selectedUnit = unit }
                        ) {
                            Text(
                                text = strings.unitSymbol(unit),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else PrimaryPurple,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isBlank()) {
                        isNameError = true
                        return@TextButton
                    }
                    val pricePaise = Money.fromRupees(priceInput).paise
                    val qtyDouble = quantityInput.toDoubleOrNull() ?: 1.0
                    val qtyMilli = (qtyDouble * 1000.0).toLong().coerceAtLeast(1L)
                    onConfirm(trimmedName, pricePaise, qtyMilli, selectedUnit)
                }
            ) {
                Text(strings.quickAddProduct, color = PrimaryPurple, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(strings.cancel)
            }
        }
    )
}
