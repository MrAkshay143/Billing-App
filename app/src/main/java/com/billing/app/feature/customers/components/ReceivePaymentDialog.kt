package com.billing.app.feature.customers.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.components.ShiftOpenDialog
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelGreenBg
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelOrangeBg
import com.billing.app.core.ui.theme.PastelOrangeContent
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PastelPurpleContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.domain.repository.CustomerRepository
import com.billing.app.domain.repository.ShiftRepository
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ReceivePaymentDialog(
    customer: CustomerEntity,
    currentDuePaise: Long,
    customerRepository: CustomerRepository,
    shiftRepository: ShiftRepository,
    onDismissRequest: () -> Unit,
    onPaymentSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = appStrings()

    val initialAmount = if (currentDuePaise > 0L) {
        String.format(Locale.US, "%.2f", currentDuePaise / 100.0)
    } else {
        ""
    }

    var amountText by remember { mutableStateOf(initialAmount) }
    var paymentMode by remember { mutableStateOf("CASH") } // "CASH" or "UPI"
    var noteText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showShiftOpenDialog by remember { mutableStateOf(false) }

    val activeShift by shiftRepository.getActiveShiftFlow().collectAsState(initial = null)

    if (showShiftOpenDialog) {
        ShiftOpenDialog(
            onDismissRequest = { showShiftOpenDialog = false },
            onShiftOpened = {
                showShiftOpenDialog = false
                FeedbackManager.showToast(context, "Shift opened successfully.")
            }
        )
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) onDismissRequest()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PastelPurpleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = strings.receivePaymentTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer & Balance Info Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DokandaarTheme.background,
                    border = BorderStroke(1.dp, DokandaarTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = customer.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                        if (customer.phone.isNotBlank()) {
                            Text(
                                text = customer.phone,
                                fontSize = 12.sp,
                                color = DokandaarTheme.textMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${strings.customerDueBalance}:",
                                fontSize = 12.sp,
                                color = DokandaarTheme.textMuted
                            )
                            Text(
                                text = Money(currentDuePaise).format(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentDuePaise > 0L) PastelOrangeContent else PastelGreenContent
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("${strings.payableAmountLabel} (₹)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = DokandaarTheme.border,
                        focusedLabelColor = PrimaryPurple
                    )
                )

                // Payment Mode Selection Row
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${strings.paymentMode}:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DokandaarTheme.textPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cash Option
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (paymentMode == "CASH") PastelPurpleBg else DokandaarTheme.cardBackground,
                            border = BorderStroke(
                                width = if (paymentMode == "CASH") 2.dp else 1.dp,
                                color = if (paymentMode == "CASH") PrimaryPurple else DokandaarTheme.border
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMode = "CASH" }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = strings.cash,
                                    tint = if (paymentMode == "CASH") PrimaryPurple else DokandaarTheme.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.cash,
                                    fontSize = 13.sp,
                                    fontWeight = if (paymentMode == "CASH") FontWeight.Bold else FontWeight.Medium,
                                    color = if (paymentMode == "CASH") PrimaryPurple else DokandaarTheme.textPrimary
                                )
                            }
                        }

                        // UPI Option
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (paymentMode == "UPI") PastelPurpleBg else DokandaarTheme.cardBackground,
                            border = BorderStroke(
                                width = if (paymentMode == "UPI") 2.dp else 1.dp,
                                color = if (paymentMode == "UPI") PrimaryPurple else DokandaarTheme.border
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMode = "UPI" }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = strings.upiOnline,
                                    tint = if (paymentMode == "UPI") PrimaryPurple else DokandaarTheme.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.upiOnline,
                                    fontSize = 13.sp,
                                    fontWeight = if (paymentMode == "UPI") FontWeight.Bold else FontWeight.Medium,
                                    color = if (paymentMode == "UPI") PrimaryPurple else DokandaarTheme.textPrimary
                                )
                            }
                        }
                    }
                }

                // Cash Shift Warning Banner
                if (paymentMode == "CASH" && activeShift == null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PastelOrangeBg,
                        border = BorderStroke(1.dp, PastelOrangeContent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = PastelOrangeContent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No active cash shift",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelOrangeContent
                                )
                                Text(
                                    text = "Cash sync requires opening a shift drawer.",
                                    fontSize = 10.sp,
                                    color = PastelOrangeContent
                                )
                            }
                            TextButton(
                                onClick = { showShiftOpenDialog = true }
                            ) {
                                Text(
                                    text = "Open Shift",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple
                                )
                            }
                        }
                    }
                }

                // Note Field (Optional)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(strings.paymentNotePlaceholder) },
                    placeholder = { Text(strings.paymentNotePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = DokandaarTheme.border,
                        focusedLabelColor = PrimaryPurple
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountPaise = Money.fromRupees(amountText).paise
                    if (amountPaise <= 0L) {
                        FeedbackManager.showError(context, strings.enterValidQuantity)
                        return@Button
                    }

                    if (paymentMode == "CASH" && activeShift == null) {
                        showShiftOpenDialog = true
                        FeedbackManager.showError(context, "Please open a shift before recording cash payment")
                        return@Button
                    }

                    scope.launch {
                        isSubmitting = true
                        val note = noteText.trim().ifBlank { null }
                        val result = customerRepository.recordKhataPayment(
                            customerId = customer.id,
                            amountPaise = amountPaise,
                            paymentMode = paymentMode,
                            shiftId = activeShift?.id,
                            note = note
                        )
                        isSubmitting = false

                        if (result.isSuccess) {
                            FeedbackManager.showSuccess(
                                context,
                                "Payment of ${Money(amountPaise).format()} recorded successfully."
                            )
                            onPaymentSuccess()
                            onDismissRequest()
                        } else {
                            FeedbackManager.showError(
                                context,
                                "Failed to record payment: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                            )
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(strings.confirmReceivePayment, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isSubmitting
            ) {
                Text(strings.cancel, color = DokandaarTheme.textMuted)
            }
        },
        containerColor = DokandaarTheme.cardBackground,
        shape = RoundedCornerShape(16.dp)
    )
}
