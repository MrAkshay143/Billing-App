package com.billing.app.core.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

class BarcodeAnalyzer(
    private val debounceManager: BarcodeDebounceManager,
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer, Closeable {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_QR_CODE
            )
            .build()
    )

    private var isPaused = false

    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isPaused) {
            imageProxy.close()
            return
        }

        val isClosed = AtomicBoolean(false)
        fun safeClose() {
            if (isClosed.compareAndSet(false, true)) {
                imageProxy.close()
            }
        }

        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                val imgWidth = imageProxy.width
                val imgHeight = imageProxy.height

                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue ?: continue
                            // ROI filtering: verify barcode is within central viewfinder area
                            val box = barcode.boundingBox
                            if (box != null && imgWidth > 0 && imgHeight > 0) {
                                val centerX = box.centerX()
                                val centerY = box.centerY()
                                val isInFocus = (centerX >= imgWidth * 0.08 && centerX <= imgWidth * 0.92) &&
                                               (centerY >= imgHeight * 0.08 && centerY <= imgHeight * 0.92)
                                if (!isInFocus) continue
                            }
                            if (debounceManager.shouldProcess(rawValue)) {
                                onBarcodeDetected(rawValue)
                                break
                            }
                        }
                    }
                    .addOnCompleteListener {
                        safeClose()
                    }
            } else {
                safeClose()
            }
        } catch (_: Exception) {
            safeClose()
        }
    }

    override fun close() {
        try {
            scanner.close()
        } catch (_: Exception) {}
    }
}
