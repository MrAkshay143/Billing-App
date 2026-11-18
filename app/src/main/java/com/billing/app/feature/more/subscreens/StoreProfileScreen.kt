package com.billing.app.feature.more.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.billing.app.core.ui.components.SubPageHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.TextMutedColor
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.feature.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreProfileScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.shopProfile.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by rememberSaveable(key = "store_name") { mutableStateOf(profile.name) }
    var address1 by rememberSaveable(key = "store_addr1") { mutableStateOf(profile.addressLine1) }
    var address2 by rememberSaveable(key = "store_addr2") { mutableStateOf(profile.addressLine2) }
    var phone by rememberSaveable(key = "store_phone") { mutableStateOf(profile.phoneNumber) }
    var upiId by rememberSaveable(key = "store_upi") { mutableStateOf(profile.upiId) }
    var footer by rememberSaveable(key = "store_footer") { mutableStateOf(profile.footerText) }

    val isGstEnabledPref by viewModel.isGstEnabled.collectAsState()
    val gstinPref by viewModel.gstin.collectAsState()
    val taxRatePref by viewModel.taxRate.collectAsState()

    var isGstEnabled by rememberSaveable(key = "store_gst_enabled") { mutableStateOf(false) }
    var gstinInput by rememberSaveable(key = "store_gstin") { mutableStateOf("") }
    var taxRateInput by rememberSaveable(key = "store_tax_rate") { mutableStateOf("0") }

    LaunchedEffect(isGstEnabledPref, gstinPref, taxRatePref) {
        isGstEnabled = isGstEnabledPref
        gstinInput = gstinPref
        taxRateInput = taxRatePref
    }

    LaunchedEffect(profile) {
        if (name.isBlank() && address1.isBlank() && phone.isBlank()) {
            name = profile.name
            address1 = profile.addressLine1
            address2 = profile.addressLine2
            phone = profile.phoneNumber
            upiId = profile.upiId
            footer = profile.footerText
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = "Store Profile",
                subtitle = "Shop details, address & contact",
                icon = Icons.Default.Storefront,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = DokandaarTheme.cardBackground,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                FeedbackManager.showToast(context, "Please enter store name")
                                return@Button
                            }
                            viewModel.updateShopProfile(
                                name = name,
                                address1 = address1,
                                address2 = address2,
                                phone = phone,
                                upiId = profile.upiId, // Preserve UPI ID from Payment Settings
                                footer = footer
                            )
                            viewModel.saveGstConfiguration(
                                enabled = isGstEnabled,
                                gstin = gstinInput,
                                taxRate = taxRateInput
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HeaderGradientStart)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Store Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AddProductBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = HeaderGradientStart,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = name.ifBlank { "My Dokandaar Store" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                        Text(
                            text = if (phone.isNotBlank()) phone else "Add store contact & address",
                            fontSize = 13.sp,
                            color = TextMutedColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 1: Basic Information
            Text(
                text = "Store Information",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DokandaarTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Store Name *") },
                        placeholder = { Text("e.g. Dokandaar Grocery Store") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = HeaderGradientStart)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeaderGradientStart,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Store Phone Number") },
                        placeholder = { Text("+91 9876543210") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = HeaderGradientStart)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeaderGradientStart,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 2: Address Details
            Text(
                text = "Store Address (Printed on Receipts)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DokandaarTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = address1,
                        onValueChange = { address1 = it },
                        label = { Text("Address Line 1") },
                        placeholder = { Text("Shop No, Building, Street") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = HeaderGradientStart)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeaderGradientStart,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = address2,
                        onValueChange = { address2 = it },
                        label = { Text("Address Line 2 (City, Pincode)") },
                        placeholder = { Text("Kolkata, WB - 700001") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = HeaderGradientStart)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeaderGradientStart,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section: GST & Taxation Details (H7)
            Text(
                text = "GST & Tax Details",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DokandaarTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable GST Billing", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DokandaarTheme.textPrimary)
                            Text("Display GSTIN and tax split on invoices", fontSize = 12.sp, color = TextMutedColor)
                        }
                        Switch(
                            checked = isGstEnabled,
                            onCheckedChange = { isGstEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HeaderGradientStart
                            )
                        )
                    }

                    if (isGstEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = gstinInput,
                            onValueChange = { gstinInput = it.uppercase() },
                            label = { Text("Store GSTIN") },
                            placeholder = { Text("e.g. 27AAPFU0939F1ZV") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HeaderGradientStart,
                                unfocusedBorderColor = DokandaarTheme.border
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Default Tax Rate:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DokandaarTheme.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("0", "5", "12", "18", "28").forEach { rate ->
                                val selected = taxRateInput == rate
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { taxRateInput = rate },
                                    color = if (selected) HeaderGradientStart else if (DokandaarTheme.isDark) DokandaarTheme.surfaceVariant else AddProductBg
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$rate%",
                                            color = if (selected) Color.White else if (DokandaarTheme.isDark) DokandaarTheme.textPrimary else HeaderGradientStart,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 3: Receipt Footer
            Text(
                text = "Receipt Customization",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DokandaarTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = footer,
                        onValueChange = { footer = it },
                        label = { Text("Receipt Footer Message") },
                        placeholder = { Text("Thank you! Visit again.") },
                        leadingIcon = {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = HeaderGradientStart)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeaderGradientStart,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tip: Store UPI and payment modes can be configured in Payment Settings.",
                        fontSize = 11.sp,
                        color = TextMutedColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
