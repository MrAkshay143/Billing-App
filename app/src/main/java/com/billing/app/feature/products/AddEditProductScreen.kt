package com.billing.app.feature.products

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.billing.app.core.localization.appStrings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.billing.app.core.ui.components.BarcodeScannerDialog
import com.billing.app.core.ui.components.InputField
import com.billing.app.core.ui.components.PrimaryButton
import com.billing.app.core.ui.components.ProductThumbnail
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.domain.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    viewModel: ProductViewModel,
    existingProduct: Product? = null,
    scannedBarcode: String? = null,
    onBarcodeConsumed: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit = {}
) {
    val context = LocalContext.current
    val strings = appStrings()
    val isSaving by viewModel.isSaving.collectAsState()

    val name by viewModel.draftName.collectAsState()
    val barcode by viewModel.draftBarcode.collectAsState()
    val priceStr by viewModel.draftPrice.collectAsState()
    val stockStr by viewModel.draftStock.collectAsState()
    val category by viewModel.draftCategory.collectAsState()
    val sku by viewModel.draftSku.collectAsState()
    val imageUri by viewModel.draftImageUri.collectAsState()
    val unit by viewModel.draftUnit.collectAsState()
    val isUnitLocked by viewModel.isUnitLocked.collectAsState()
    val purchasePrice by viewModel.draftPurchasePrice.collectAsState()
    val mrp by viewModel.draftMrp.collectAsState()
    val gstRate by viewModel.draftGstRate.collectAsState()
    val minStockAlert by viewModel.draftMinStockAlert.collectAsState()
    val hsnCode by viewModel.draftHsnCode.collectAsState()
    val trackBatch by viewModel.draftTrackBatch.collectAsState()

    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var gstExpanded by remember { mutableStateOf(false) }
    val dbCategories by viewModel.categories.collectAsState()
    val categoriesList = remember(dbCategories) {
        if (dbCategories.isNotEmpty()) {
            dbCategories.map { it.name }
        } else {
            listOf(
                "Grocery", "Oil & Ghee", "Pulses & Dal", "Spices & Masala",
                "Snacks & Biscuits", "Beverages", "Household Items", "Dairy Products",
                "Personal Care", "Fruits & Vegetables", "Others"
            )
        }
    }

    LaunchedEffect(existingProduct) {
        if (existingProduct != null) {
            viewModel.initFromProduct(existingProduct)
        }
    }

    LaunchedEffect(scannedBarcode) {
        if (!scannedBarcode.isNullOrBlank()) {
            viewModel.setScannedBarcode(scannedBarcode)
            onBarcodeConsumed()
        }
    }

    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val oldPath = imageUri
            val newPath = com.billing.app.core.util.ImageUtil.saveAndDownscaleImage(context, it)
            if (newPath != null) {
                if (oldPath != null && oldPath != existingProduct?.imageUri) {
                    com.billing.app.core.util.ImageUtil.deleteImageFile(oldPath)
                }
                viewModel.draftImageUri.value = newPath
            } else {
                com.billing.app.core.ui.util.FeedbackManager.showError(context, strings.galleryErrorToast)
            }
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val oldPath = imageUri
            val newPath = com.billing.app.core.util.ImageUtil.saveBitmapImage(context, it)
            if (newPath != null) {
                if (oldPath != null && oldPath != existingProduct?.imageUri) {
                    com.billing.app.core.util.ImageUtil.deleteImageFile(oldPath)
                }
                viewModel.draftImageUri.value = newPath
            } else {
                com.billing.app.core.ui.util.FeedbackManager.showError(context, strings.cameraErrorToast)
            }
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = if (existingProduct == null) "Add Product" else "Edit Product",
                subtitle = if (existingProduct == null) "Barcode & catalog details" else "Update price & stock",
                icon = Icons.Default.Inventory2,
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
                Box(modifier = Modifier.padding(16.dp)) {
                    PrimaryButton(
                        text = if (existingProduct == null) strings.save else strings.editProductTitle,
                        enabled = !isSaving,
                        isLoading = isSaving,
                        onClick = {
                            if (name.isBlank()) {
                                com.billing.app.core.ui.util.FeedbackManager.showError(context, strings.errorProductNameRequired)
                                return@PrimaryButton
                            }
                            if (barcode.isBlank()) {
                                com.billing.app.core.ui.util.FeedbackManager.showError(context, strings.errorBarcodeRequired)
                                return@PrimaryButton
                            }
                            if (existingProduct == null) {
                                viewModel.saveProduct(
                                    name = name,
                                    barcode = barcode,
                                    priceStr = priceStr,
                                    stockStr = stockStr,
                                    category = category,
                                    sku = sku,
                                    imageUri = imageUri,
                                    unit = unit,
                                    purchasePriceStr = purchasePrice,
                                    mrpStr = mrp,
                                    gstRateStr = gstRate,
                                    minStockAlertStr = minStockAlert,
                                    hsnCodeStr = hsnCode,
                                    trackBatch = trackBatch
                                )
                            } else {
                                viewModel.updateProduct(
                                    id = existingProduct.id,
                                    name = name,
                                    barcode = barcode,
                                    priceStr = priceStr,
                                    stockStr = stockStr,
                                    category = category,
                                    sku = sku,
                                    imageUri = imageUri,
                                    unit = unit,
                                    purchasePriceStr = purchasePrice,
                                    mrpStr = mrp,
                                    gstRateStr = gstRate,
                                    minStockAlertStr = minStockAlert,
                                    hsnCodeStr = hsnCode,
                                    trackBatch = trackBatch
                                )
                            }
                        }
                    )
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
            // Optional Image Picker Card
            Text(
                text = strings.productPhotoOptional,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DokandaarTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DokandaarTheme.cardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
            ) {
                if (imageUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductThumbnail(
                            imageUri = imageUri,
                            size = 72.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.photoAttached,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { cameraLauncher.launch(null) },
                                    color = AddProductBg
                                ) {
                                    Text(
                                        text = strings.retake,
                                        color = HeaderGradientStart,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.draftImageUri.value = null },
                                    color = Color(0xFFFEE2E2)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = strings.remove,
                                            color = Color(0xFFEF4444),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Take Photo Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { cameraLauncher.launch(null) }
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AddProductBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = HeaderGradientStart,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.takePhoto,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HeaderGradientStart
                            )
                        }

                        // Pick Gallery Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { galleryLauncher.launch("image/*") }
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AddProductBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Gallery",
                                    tint = HeaderGradientStart,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.fromGallery,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HeaderGradientStart
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            InputField(
                label = "${strings.productsTitle} *",
                value = name,
                onValueChange = { viewModel.draftName.value = it },
                placeholder = strings.placeholderProductName
            )

            InputField(
                label = "${strings.barcodeLabel} *",
                value = barcode,
                onValueChange = { viewModel.draftBarcode.value = it },
                placeholder = strings.placeholderBarcode,
                trailingIcon = {
                    IconButton(onClick = { showBarcodeScannerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = strings.scanQrBarcode,
                            tint = HeaderGradientStart
                        )
                    }
                }
            )

            InputField(
                label = "SKU (${strings.optional})",
                value = sku,
                onValueChange = { viewModel.draftSku.value = it },
                placeholder = strings.placeholderSku
            )

            // Category Selector
            Text(
                text = strings.categoryLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DokandaarTheme.textMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HeaderGradientStart,
                        unfocusedBorderColor = DokandaarTheme.border,
                        focusedContainerColor = DokandaarTheme.cardBackground,
                        unfocusedContainerColor = DokandaarTheme.cardBackground
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categoriesList.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.draftCategory.value = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            InputField(
                label = "Selling Price (₹) *",
                value = priceStr,
                onValueChange = { viewModel.draftPrice.value = it },
                placeholder = "0.00",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stock Quantity
                Box(modifier = Modifier.weight(1.2f)) {
                    InputField(
                        label = "Initial Stock Quantity",
                        value = stockStr,
                        onValueChange = { viewModel.draftStock.value = it },
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Unit Dropdown
                Box(modifier = Modifier.weight(0.8f)) {
                    Column {
                        Text(
                            text = strings.unitLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = DokandaarTheme.textMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = unitExpanded && !isUnitLocked,
                            onExpandedChange = { if (!isUnitLocked) unitExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = unit,
                                onValueChange = {},
                                readOnly = true,
                                enabled = !isUnitLocked,
                                trailingIcon = {
                                    if (!isUnitLocked) {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HeaderGradientStart,
                                    unfocusedBorderColor = DokandaarTheme.border,
                                    focusedContainerColor = DokandaarTheme.cardBackground,
                                    unfocusedContainerColor = DokandaarTheme.cardBackground
                                )
                            )
                            if (!isUnitLocked) {
                                ExposedDropdownMenu(
                                    expanded = unitExpanded,
                                    onDismissRequest = { unitExpanded = false }
                                ) {
                                    listOf("PCS", "KG", "L", "PACK", "BOX", "MTR").forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u) },
                                            onClick = {
                                                viewModel.draftUnit.value = u
                                                unitExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (isUnitLocked) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.unitLockedInUse,
                                fontSize = 10.sp,
                                color = DokandaarTheme.textMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Pricing, GST & Batch Details Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DokandaarTheme.cardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.pricingGstBatchDetails,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Purchase Price and MRP Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            InputField(
                                label = "Purchase Price (₹)",
                                value = purchasePrice,
                                onValueChange = { viewModel.draftPurchasePrice.value = it },
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            InputField(
                                label = "MRP (₹)",
                                value = mrp,
                                onValueChange = { viewModel.draftMrp.value = it },
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // GST Slab Dropdown & HSN Code Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Text(
                                    text = strings.gstRate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DokandaarTheme.textMuted
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ExposedDropdownMenuBox(
                                    expanded = gstExpanded,
                                    onExpandedChange = { gstExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = if (gstRate.isNotBlank()) "${gstRate}%" else "0%",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gstExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = HeaderGradientStart,
                                            unfocusedBorderColor = DokandaarTheme.border,
                                            focusedContainerColor = DokandaarTheme.cardBackground,
                                            unfocusedContainerColor = DokandaarTheme.cardBackground
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = gstExpanded,
                                        onDismissRequest = { gstExpanded = false }
                                    ) {
                                        listOf("0", "5", "18", "40").forEach { rate ->
                                            DropdownMenuItem(
                                                text = { Text("$rate%") },
                                                onClick = {
                                                    viewModel.draftGstRate.value = rate
                                                    gstExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            InputField(
                                label = "HSN Code",
                                value = hsnCode,
                                onValueChange = { viewModel.draftHsnCode.value = it },
                                placeholder = strings.placeholderHsn,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Low Stock Alert
                    InputField(
                        label = "Low Stock Alert Threshold",
                        value = minStockAlert,
                        onValueChange = { viewModel.draftMinStockAlert.value = it },
                        placeholder = strings.placeholderLowStockThreshold,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Batch Tracking Switch / Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = strings.enableBatchFefo,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DokandaarTheme.textPrimary
                            )
                            Text(
                                text = strings.batchTrackingDesc,
                                fontSize = 12.sp,
                                color = DokandaarTheme.textMuted
                            )
                        }
                        Switch(
                            checked = trackBatch,
                            onCheckedChange = { viewModel.draftTrackBatch.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HeaderGradientStart
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showBarcodeScannerDialog) {
        BarcodeScannerDialog(
            onBarcodeScanned = { scanned ->
                viewModel.setScannedBarcode(scanned)
                showBarcodeScannerDialog = false
            },
            onDismiss = {
                showBarcodeScannerDialog = false
            }
        )
    }
}
