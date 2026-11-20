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
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
    val strings = com.billing.app.core.localization.LocalAppStrings.current
    val shopProfile by viewModel.shopProfile.collectAsState()
    val defaultPaymentMethod by viewModel.defaultPaymentMethod.collectAsState()
    val isGstEnabled by viewModel.isGstEnabled.collectAsState()
    val gstin by viewModel.gstin.collectAsState()
    val taxRate by viewModel.taxRate.collectAsState()

    var upiInput by remember(shopProfile.upiId) {
        mutableStateOf(shopProfile.upiId.ifBlank { "dokandaar@upi" })
    }
    var selectedDefaultMethod by remember(defaultPaymentMethod) {
        mutableStateOf(defaultPaymentMethod)
    }
    var gstEnabledState by remember(isGstEnabled) {
        mutableStateOf(isGstEnabled)
    }
    var gstinInput by remember(gstin) {
        mutableStateOf(gstin)
    }
    var selectedTaxRate by remember(taxRate) {
        mutableStateOf(taxRate.ifBlank { "0" })
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
                title = strings.paymentSettings,
                subtitle = "UPI QR, cash & GST settings",
                icon = Icons.Default.QrCode,
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
                            Text(strings.upiQrSetup, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DokandaarTheme.textPrimary)
                            Text("Used to generate dynamic Scan & Pay QR on checkout", fontSize = 12.sp, color = DokandaarTheme.textMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = upiInput,
                        onValueChange = { upiInput = it },
                        label = { Text(strings.storeUpiId) },
                        placeholder = { Text(strings.placeholderUpiId) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeaderGradientStart,
                            unfocusedBorderColor = DokandaarTheme.border
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
                                .background(DokandaarTheme.surfaceVariant)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = sampleQr.asImageBitmap(),
                                contentDescription = "QR Preview",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, DokandaarTheme.border, RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Live QR Verification", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DokandaarTheme.textPrimary)
                                Text("VPA: $upiInput", fontSize = 11.sp, color = HeaderGradientStart, fontWeight = FontWeight.SemiBold)
                                Text("Ready to accept payments from GPay, PhonePe, Paytm", fontSize = 11.sp, color = DokandaarTheme.textMuted)
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
                            Text(strings.defaultPaymentMethod, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DokandaarTheme.textPrimary)
                            Text("Pre-selected tab when opening checkout", fontSize = 12.sp, color = DokandaarTheme.textMuted)
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
                                        tint = if (isSelected) HeaderGradientStart else DokandaarTheme.textMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) HeaderGradientStart else DokandaarTheme.textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. GST & Tax Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AddProductBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = HeaderGradientStart,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(strings.gstTaxConfig, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DokandaarTheme.textPrimary)
                                Text(strings.enableGstBilling, fontSize = 12.sp, color = DokandaarTheme.textMuted)
                            }
                        }
                        Switch(
                            checked = gstEnabledState,
                            onCheckedChange = { gstEnabledState = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HeaderGradientStart
                            )
                        )
                    }

                    if (gstEnabledState) {
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = gstinInput,
                            onValueChange = { gstinInput = it.uppercase() },
                            label = { Text(strings.storeGstin) },
                            placeholder = { Text(strings.placeholderGstin) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HeaderGradientStart,
                                unfocusedBorderColor = DokandaarTheme.border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = strings.defaultTaxRate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DokandaarTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("0", "5", "12", "18", "28").forEach { rate ->
                                val isSelected = selectedTaxRate == rate
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedTaxRate = rate },
                                    color = if (isSelected) HeaderGradientStart else DokandaarTheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) HeaderGradientStart else DokandaarTheme.border
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$rate%",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else DokandaarTheme.textPrimary
                                        )
                                    }
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
                        viewModel.saveGstConfiguration(gstEnabledState, gstinInput, selectedTaxRate)
                        FeedbackManager.showToast(context, "Payment & GST settings updated successfully!")
                        onNavigateBack()
                    },
                color = HeaderGradientStart
            ) {
                Text(
                    text = strings.saveSettings,
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
