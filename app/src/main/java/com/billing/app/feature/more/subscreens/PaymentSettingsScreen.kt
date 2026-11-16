package com.billing.app.feature.more.subscreens

import android.widget.Toast
import com.billing.app.core.ui.util.FeedbackManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.billing.app.core.ui.components.SubPageHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.qr.UpiQrGenerator
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.TextMutedColor
import com.billing.app.core.util.Money
import com.billing.app.feature.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val shopProfile by viewModel.shopProfile.collectAsState()
    val defaultPaymentMethod by viewModel.defaultPaymentMethod.collectAsState()

    var upiInput by remember(shopProfile.upiId) {
        mutableStateOf(shopProfile.upiId.ifBlank { "dokandaar@upi" })
    }
    var selectedDefaultMethod by remember(defaultPaymentMethod) {
        mutableStateOf(defaultPaymentMethod)
    }

    val sampleQr = remember(upiInput, shopProfile.name) {
        UpiQrGenerator.generateUpiQrBitmap(
            upiId = upiInput,
            shopName = shopProfile.name.ifBlank { "Dokandaar" },
            amount = Money.fromRupees(100.0),
            sizePx = 250
        )
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = "Payment Settings",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. UPI Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AddProductBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = HeaderGradientStart, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("UPI QR Code Setup", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DokandaarTheme.textPrimary)
                            Text("Used to generate dynamic Scan & Pay QR on checkout", fontSize = 12.sp, color = TextMutedColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = upiInput,
                        onValueChange = { upiInput = it },
                        label = { Text("Store UPI ID / VPA") },
                        placeholder = { Text("e.g. mobile@upi, merchant@oksbi") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeaderGradientStart,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // QR Preview
                    if (sampleQr != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = sampleQr.asImageBitmap(),
                                contentDescription = "QR Preview",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Live QR Verification", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("VPA: $upiInput", fontSize = 11.sp, color = HeaderGradientStart, fontWeight = FontWeight.SemiBold)
                                Text("Ready to accept payments from GPay, PhonePe, Paytm", fontSize = 11.sp, color = TextMutedColor)
                            }
                        }
                    }
                }
            }

            // 2. Default Checkout Method
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AddProductBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = HeaderGradientStart, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Default Payment Method", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DokandaarTheme.textPrimary)
                            Text("Pre-selected tab when opening checkout", fontSize = 12.sp, color = TextMutedColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            Triple("UPI", Icons.Default.QrCode, "UPI"),
                            Triple("CASH", Icons.Default.Payments, "Cash"),
                            Triple("OTHER", Icons.Default.CreditCard, "Other")
                        ).forEach { (mode, icon, title) ->
                            val isSelected = selectedDefaultMethod == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedDefaultMethod = mode },
                                color = if (isSelected) AddProductBg else DokandaarTheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) HeaderGradientStart else DokandaarTheme.border
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = if (isSelected) HeaderGradientStart else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) HeaderGradientStart else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Save Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        viewModel.updatePaymentSettings(upiInput, selectedDefaultMethod)
                        FeedbackManager.showToast(context, "Payment settings updated successfully!")
                        onNavigateBack()
                    },
                color = HeaderGradientStart
            ) {
                Text(
                    text = "Save Payment Settings",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 14.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
