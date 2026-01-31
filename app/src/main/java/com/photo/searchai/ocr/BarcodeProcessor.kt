package com.photo.searchai.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Result from barcode processing for a single barcode.
 */
data class BarcodeResult(
    val format: Int,
    val formatName: String,
    val rawValue: String,
    val displayValue: String,
    val valueType: Int
) {
    companion object {
        /**
         * Get human-readable format name from Barcode format constant.
         */
        fun getFormatName(format: Int): String = when (format) {
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_CODE_93 -> "CODE_93"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_PDF417 -> "PDF_417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            else -> "UNKNOWN"
        }
    }
}

/**
 * Wrapper for ML Kit Barcode Scanning.
 * Scans all supported barcode formats on-device.
 */
@Singleton
class BarcodeProcessor @Inject constructor() {
    
    private val scanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }
    
    /**
     * Process a bitmap and detect barcodes.
     * Returns list of BarcodeResult for each detected barcode.
     */
    suspend fun processImage(bitmap: Bitmap): List<BarcodeResult> = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (continuation.isActive) {
                    val results = barcodes.mapNotNull { barcode ->
                        val rawValue = barcode.rawValue ?: return@mapNotNull null
                        BarcodeResult(
                            format = barcode.format,
                            formatName = BarcodeResult.getFormatName(barcode.format),
                            rawValue = rawValue,
                            displayValue = barcode.displayValue ?: rawValue,
                            valueType = barcode.valueType
                        )
                    }
                    continuation.resume(results)
                }
            }
            .addOnFailureListener { _ ->
                if (continuation.isActive) {
                    continuation.resume(emptyList())
                }
            }
        
        continuation.invokeOnCancellation {
            // Cancellation handled by coroutine scope
        }
    }
    
    /**
     * Close the scanner and release resources.
     */
    fun close() {
        scanner.close()
    }
}
