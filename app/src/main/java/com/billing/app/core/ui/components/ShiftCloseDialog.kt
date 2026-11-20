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
import com.billing.app.domain.model.ShiftZReport
import com.billing.app.domain.repository.ShiftRepository
import kotlinx.coroutines.launch

@Composable
fun ShiftCloseDialog(
    shiftRepository: ShiftRepository = BillingApplication.instance.shiftRepository,
    onDismissRequest: () -> Unit = {},
    onShiftClosed: (ShiftZReport) -> Unit
) {
    val strings = appStrings()
    var cashCountRupees by remember { mutableStateOf("") }
    var closingNote by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isClosing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val submitShiftClose: () -> Unit = {
        val amountDouble = cashCountRupees.trim().toDoubleOrNull()
        if (amountDouble == null || amountDouble < 0.0) {
            errorMessage = "Please enter a valid cash amount (₹0.00 or more)"
        } else if (!isClosing) {
            val actualCashPaise = kotlin.math.round(amountDouble * 100.0).toLong()
            coroutineScope.launch {
                isClosing = true
                errorMessage = null
                val closeResult = shiftRepository.closeShift(
                    actualCashPaise = actualCashPaise,
                    note = closingNote.trim().ifBlank { null }
                )
                closeResult.fold(
                    onSuccess = { closedShift ->
                        val zReportResult = shiftRepository.generateZReport(closedShift.id)
                        isClosing = false
                        zReportResult.fold(
                            onSuccess = { zReport ->
                                onDismissRequest()
                                onShiftClosed(zReport)
                            },
                            onFailure = { error ->
                                errorMessage = error.message ?: "Failed to generate Z-Report"
                            }
                        )
                    },
                    onFailure = { error ->
                        isClosing = false
                        errorMessage = error.message ?: "Failed to close shift"
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
                    text = strings.closeCashDrawerTitle,
                    fontSize = 17.sp,
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
                    text = strings.countCashPrompt,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DokandaarTheme.textMuted
                )

                OutlinedTextField(
                    value = cashCountRupees,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            cashCountRupees = it
                            errorMessage = null
                        }
                    },
                    label = { Text(strings.countedCashInput) },
                    placeholder = { Text("5000") },
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
                    value = closingNote,
                    onValueChange = { closingNote = it },
                    label = { Text(strings.closingNoteOptional) },
                    placeholder = { Text("Shift ended") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitShiftClose() }
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
                onClick = submitShiftClose,
                enabled = !isClosing
            ) {
                if (isClosing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = HeaderGradientStart
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.closingAndGenerating,
                        fontWeight = FontWeight.Bold,
                        color = HeaderGradientStart
                    )
                } else {
                    Text(
                        text = strings.closeShiftAndGenerateZReport,
                        fontWeight = FontWeight.Bold,
                        color = HeaderGradientStart
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isClosing
            ) {
                Text(
                    text = strings.cancel,
                    fontWeight = FontWeight.Medium,
                    color = DokandaarTheme.textMuted
                )
            }
        }
    )
}
