package com.billing.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.BillingApplication
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.data.local.entity.ShiftEntity
import kotlinx.coroutines.launch

@Composable
fun ShiftOpenDialog(
    userId: String? = null,
    onDismissRequest: () -> Unit = {},
    onShiftOpened: (ShiftEntity) -> Unit
) {
    val strings = appStrings()
    val effectiveUserId = userId ?: BillingApplication.instance.userRepository.currentUser.value?.id ?: ""
    var floatAmountRupees by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isOpening by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val submitShiftOpen: () -> Unit = {
        val amountDouble = floatAmountRupees.trim().toDoubleOrNull()
        if (amountDouble == null || amountDouble < 0.0) {
            errorMessage = "Please enter a valid cash float amount"
        } else if (effectiveUserId.isBlank()) {
            errorMessage = "No active user logged in to open shift"
        } else if (!isOpening) {
            val floatPaise = (amountDouble * 100).toLong()
            coroutineScope.launch {
                isOpening = true
                errorMessage = null
                val result = BillingApplication.instance.shiftRepository.openShift(
                    userId = effectiveUserId,
                    openingFloatPaise = floatPaise,
                    note = noteInput.trim().ifBlank { null }
                )
                isOpening = false
                result.fold(
                    onSuccess = { shift ->
                        onShiftOpened(shift)
                    },
                    onFailure = { error ->
                        errorMessage = error.message ?: "Failed to open shift"
                    }
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        containerColor = DokandaarTheme.cardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = HeaderGradientStart,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.openRegisterShift,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DokandaarTheme.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.enterOpeningFloatPrompt,
                    fontSize = 13.sp,
                    color = DokandaarTheme.textMuted
                )

                OutlinedTextField(
                    value = floatAmountRupees,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            floatAmountRupees = it
                            errorMessage = null
                        }
                    },
                    label = { Text(strings.openingCashFloatInput) },
                    placeholder = { Text("1000") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text(strings.closingNoteOptional) },
                    placeholder = { Text("Morning shift") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitShiftOpen() }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = submitShiftOpen,
                enabled = !isOpening
            ) {
                if (isOpening) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = HeaderGradientStart
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.opening,
                        fontWeight = FontWeight.Bold,
                        color = HeaderGradientStart
                    )
                } else {
                    Text(
                        text = strings.openShift,
                        fontWeight = FontWeight.Bold,
                        color = HeaderGradientStart
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = strings.cancel,
                    fontWeight = FontWeight.Medium,
                    color = DokandaarTheme.textMuted
                )
            }
        }
    )
}
