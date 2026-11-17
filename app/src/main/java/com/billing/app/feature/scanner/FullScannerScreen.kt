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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
            FeedbackManager.showToast(context, "Camera permission required for scanning")
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
                ProcessCameraProvider.getInstance(context).get()?.unbindAll()
            } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }

    var scanTriggerCount by remember { mutableIntStateOf(0) }

    // Auto-hide success badge after 2 seconds
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
                    // Flashlight Toggle
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

                    // Flip Camera
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

                    // Help Dialog
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
                    // Clear All Button
                    Surface(
                        modifier = Modifier
                            .weight(0.4f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .clickable { cartManager.clearCart() },
                        color = DokandaarTheme.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorColor)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = ErrorColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clear All",
                                color = ErrorColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Done Button
                    Surface(
                        modifier = Modifier
                            .weight(0.6f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .clickable { onDoneScanning() },
                        color = PrimaryPurple,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${strings.done} (${cartItems.sumOf { it.quantity }} ${strings.items})",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
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
            // 1. Live Camera Viewfinder (Top 42% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
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
                                            imageAnalysis.setAnalyzer(
                                                cameraExecutor,
                                                BarcodeAnalyzer(debounceManager) { rawBarcode ->
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
                                            )
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

                    // Laser Viewfinder Overlay
                    ViewfinderLaserOverlay()
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Camera permission needed",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 2. Status Badges Row (Item Added to Cart + Total Items)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cartItems.isNotEmpty()) {
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
                                    "Added: $lastAddedProductName"
                                } else {
                                    strings.itemAddedToCart
                                },
                                color = PastelGreenContent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DokandaarTheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = DokandaarTheme.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.pointAtBarcode,
                                color = DokandaarTheme.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PastelPurpleBg
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
                            text = "Total: ${cartItems.sumOf { it.quantity }} ${strings.items}",
                            color = PrimaryPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Scanned Items List Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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

            // 4. Scanned Items List
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
                            text = "Align barcode within green laser frame above",
                            color = DokandaarTheme.textMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProductThumbnail(
                                    imageUri = item.product.imageUri,
                                    size = 46.dp,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DokandaarTheme.textPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (item.product.sku.isNotBlank()) "SKU: ${item.product.sku}" else "Barcode: ${item.product.barcode}",
                                        fontSize = 11.sp,
                                        color = DokandaarTheme.textMuted,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.product.price.format(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PastelGreenContent
                                    )
                                }

                                // Stepper: - [qty] +
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PastelPurpleBg)
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
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

                                    Text(
                                        text = "${item.quantity}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
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
                                            contentDescription = "Increase",
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = item.total.format(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DokandaarTheme.textPrimary
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = { cartManager.removeItem(item.product.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = ErrorColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Continuous Scanner Guide", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "1. Position any product barcode or QR code in the green laser frame.\n" +
                    "2. The scanner will automatically detect and add items to your cart.\n" +
                    "3. Scanning the same product again increases its quantity.\n" +
                    "4. Tap Done to proceed to POS checkout with your scanned items."
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got It", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            }
        )
    }


}
