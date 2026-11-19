package com.example.opencodeandroidclient.ui

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/** Analyses camera frames and reports the first QR code produced by `opencode pair`. */
class QrCodeAnalyzer(
    private val onResult: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )
    private val emitted = AtomicBoolean(false)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (emitted.get()) return@addOnSuccessListener
                val value = barcodes.firstOrNull()?.rawValue
                if (!value.isNullOrBlank() && emitted.compareAndSet(false, true)) {
                    onResult(value)
                }
            }
            .addOnFailureListener {
                // Ignore failed frames; keep scanning.
            }
            .addOnCompleteListener {
                image.close()
            }
    }
}
