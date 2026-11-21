package com.billing.app.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.billing.app.core.cart.CartManager
import com.billing.app.core.scanner.BarcodeAnalyzer
import com.billing.app.core.scanner.BarcodeDebounceManager
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.components.ProductThumbnail
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.components.ViewfinderLaserOverlay
import com.billing.app.core.ui.theme.DarkPurple
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelGreenBg
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PastelPurpleContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.ui.theme.SecondaryPurple
import com.billing.app.core.ui.util.FeedbackManager
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun FullScannerScreen(
    cartManager: CartManager,
    onNavigateBack: () -> Unit,
    onDoneScanning: () -> Unit,
    onBarcodeCaptured: ((String) -> Unit)? = null,
    onNavigateToAddProductWithBarcode: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val strings = appStrings()
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val debounceManager = remember { BarcodeDebounceManager(1200L) }

    val cartItems by cartManager.cartItems.collectAsState()

    var isFlashOn by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<Camera?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var lastAddedProductName by remember { mutableStateOf<String?>(null) }
    var showAddedBadge by remember { mutableStateOf(false) }
    var activeAnalyzer by remember { mutableStateOf<com.billing.app.core.scanner.BarcodeAnalyzer?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            FeedbackManager.showError(context, strings.cameraPermissionRequired)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                activeAnalyzer?.close()
            } catch (_: Exception) {}
            try {
                ProcessCameraProvider.getInstance(context).get()?.unbindAll()
            } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }

    var scanTriggerCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(scanTriggerCount) {
        if (showAddedBadge) {
            delay(2000L)
            showAddedBadge = false
        }
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = strings.scanAddItems,
                subtitle = strings.scanSubtitleFull,
                icon = Icons.Default.QrCodeScanner,
                onNavigateBack = onNavigateBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable {
                                isFlashOn = !isFlashOn
                                cameraControl?.cameraControl?.enableTorch(isFlashOn)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = if (isFlashOn) Color.Yellow else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable {
                                useFrontCamera = !useFrontCamera
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Switch Camera",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable { showHelpDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Help",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DokandaarTheme.cardBackground,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(0.9f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { cartManager.clearCart() },
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.clearAll,
                                    color = Color(0xFFEF4444),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { onDoneScanning() },
                            color = PrimaryPurple,
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${strings.done} (${cartItems.sumOf { it.quantity }} ${strings.itemsCount})",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
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
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(230.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
            ) {
                if (hasCameraPermission) {
                    key(useFrontCamera) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val selector = if (useFrontCamera) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    }
                                    val analyzer = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { imageAnalysis ->
                                            val bAnalyzer = BarcodeAnalyzer(debounceManager) { rawBarcode ->
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    if (onBarcodeCaptured != null) {
                                                        onBarcodeCaptured(rawBarcode)
                                                    } else {
                                                        cartManager.scanBarcode(rawBarcode) { success, name, errorMsg ->
                                                            if (success) {
                                                                lastAddedProductName = name
                                                                showAddedBadge = true
                                                                scanTriggerCount++
                                                            } else {
                                                                FeedbackManager.showToast(context, errorMsg ?: "Product not found")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            activeAnalyzer = bAnalyzer
                                            imageAnalysis.setAnalyzer(cameraExecutor, bAnalyzer)
                                        }

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraControl = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            selector,
                                            preview,
                                            analyzer
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    ViewfinderLaserOverlay(showInstruction = false)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.cameraPermissionRequired,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (cartItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PastelGreenBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PastelGreenContent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showAddedBadge && lastAddedProductName != null) {
                                    "${strings.itemAddedToCart}: $lastAddedProductName"
                                } else {
                                    strings.itemAddedToCart
                                },
                                color = PastelGreenContent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (DokandaarTheme.isDark) PastelPurpleBg.copy(alpha = 0.2f) else PastelPurpleBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (DokandaarTheme.isDark) DokandaarTheme.border else Color(0xFFD8B4FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.total}: ${cartItems.sumOf { it.quantity }} ${strings.itemsCount}",
                                color = PrimaryPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.scannedItems,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = DokandaarTheme.border
                    )

                    if (cartItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = DokandaarTheme.textMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = strings.alignBarcodeFrame,
                                    color = DokandaarTheme.textMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            itemsIndexed(cartItems, key = { _, item -> item.product.id }) { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProductThumbnail(
                                        imageUri = item.product.imageUri,
                                        size = 44.dp,
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.product.name,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DokandaarTheme.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (item.product.sku.isNotBlank()) "SKU: ${item.product.sku}" else "Barcode: ${item.product.barcode}",
                                            fontSize = 10.5.sp,
                                            color = DokandaarTheme.textMuted,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = PastelGreenBg
                                        ) {
                                            Text(
                                                text = item.product.price.format(),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PastelGreenContent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(PastelPurpleBg)
                                            .border(1.dp, Color(0xFFE9D5FF), RoundedCornerShape(18.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clickable { cartManager.decrement(item.product.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Decrease",
                                                tint = PrimaryPurple,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DokandaarTheme.surfaceVariant,
                                            modifier = Modifier
                                                .padding(horizontal = 2.dp)
                                                .height(26.dp)
                                                .widthIn(min = 28.dp),
                                            shadowElevation = 0.5.dp
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            ) {
                                                Text(
                                                    text = "${item.quantity}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DokandaarTheme.textPrimary
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clickable {
                                                    val error = cartManager.increment(item.product.id)
                                                    if (error != null) {
                                                        FeedbackManager.showToast(context, error)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = PrimaryPurple,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = item.total.format(),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DokandaarTheme.textPrimary
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { cartManager.removeItem(item.product.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = strings.delete,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (index < cartItems.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp),
                                        thickness = 1.dp,
                                        color = DokandaarTheme.border.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(strings.howToScanBarcodes, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "1. Align barcode within the green frame.\n" +
                    "2. Items are added to the list instantly.\n" +
                    "3. Scan again to increase item quantity.\n" +
                    "4. Tap Done to proceed to billing."
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(strings.done, color = PrimaryPurple, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        )
    }


}
