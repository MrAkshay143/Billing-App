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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.billing.app.core.ui.components.InputField
import com.billing.app.core.ui.components.PrimaryButton
import com.billing.app.core.ui.components.ProductThumbnail
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.BorderColor
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
    onNavigateToScanner: () -> Unit
) {
    val context = LocalContext.current
    val isSaving by viewModel.isSaving.collectAsState()

    val name by viewModel.draftName.collectAsState()
    val barcode by viewModel.draftBarcode.collectAsState()
    val priceStr by viewModel.draftPrice.collectAsState()
    val stockStr by viewModel.draftStock.collectAsState()
    val category by viewModel.draftCategory.collectAsState()
    val sku by viewModel.draftSku.collectAsState()
    val imageUri by viewModel.draftImageUri.collectAsState()
    val unit by viewModel.draftUnit.collectAsState()

    var categoryExpanded by remember { mutableStateOf(false) }
    val categoriesList = listOf(
        "Grocery", "Oil & Ghee", "Pulses & Dal", "Spices & Masala",
        "Snacks & Biscuits", "Beverages", "Household Items", "Dairy Products",
        "Personal Care", "Fruits & Vegetables", "Others"
    )

    LaunchedEffect(existingProduct) {
        if (existingProduct != null) {
            viewModel.setDraftFromProduct(existingProduct)
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
                com.billing.app.core.ui.util.FeedbackManager.showToast(context, "Failed to process image")
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
                com.billing.app.core.ui.util.FeedbackManager.showToast(context, "Failed to capture image")
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
                        text = if (existingProduct == null) "Save Product" else "Update Product",
                        enabled = !isSaving,
                        isLoading = isSaving,
                        onClick = {
                            if (existingProduct == null) {
                                viewModel.addProduct(
                                    name = name,
                                    barcode = barcode,
                                    priceStr = priceStr,
                                    stockStr = stockStr,
                                    category = category,
                                    sku = sku,
                                    imageUri = imageUri,
                                    unit = unit
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
                                    unit = unit
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
                text = "Product Photo (Optional)",
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
                                text = "Photo Attached",
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
                                        text = "Retake",
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
                                            text = "Remove",
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
                                text = "Take Photo",
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
                                text = "From Gallery",
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
                label = "Product Name *",
                value = name,
                onValueChange = { viewModel.draftName.value = it },
                placeholder = "e.g. Aashirvaad Atta (5 kg)"
            )

            InputField(
                label = "Barcode *",
                value = barcode,
                onValueChange = { viewModel.draftBarcode.value = it },
                placeholder = "Scan or type barcode",
                trailingIcon = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan barcode",
                            tint = HeaderGradientStart
                        )
                    }
                }
            )

            InputField(
                label = "SKU Code (Optional)",
                value = sku,
                onValueChange = { viewModel.draftSku.value = it },
                placeholder = "e.g. ATT-5KG"
            )

            // Category Selector
            Text(
                text = "Category",
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
                        unfocusedBorderColor = BorderColor,
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

            InputField(
                label = "Initial Stock Quantity",
                value = stockStr,
                onValueChange = { viewModel.draftStock.value = it },
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
