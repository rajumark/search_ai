package com.photo.searchai.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Result from OCR processing.
 */
data class OcrResult(
    val fullText: String,
    val indexedTokens: String
) {
    companion object {
        val EMPTY = OcrResult("", "")
    }
}

/**
 * Wrapper for ML Kit Text Recognition.
 * Uses on-device recognizer only - no cloud calls.
 */
@Singleton
class OcrProcessor @Inject constructor() {
    
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    
    /**
     * Process a bitmap and extract text.
     * Returns OcrResult with full text and indexed tokens.
     */
    suspend fun processImage(bitmap: Bitmap): OcrResult = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                if (continuation.isActive) {
                    val fullText = visionText.text
                    // Create indexed tokens by normalizing and splitting text
                    val indexedTokens = createIndexedTokens(fullText)
                    continuation.resume(OcrResult(fullText, indexedTokens))
                }
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) {
                    // Return empty result on failure instead of throwing
                    continuation.resume(OcrResult.EMPTY)
                }
            }
        
        continuation.invokeOnCancellation {
            // Cancellation handled by coroutine scope
        }
    }
    
    /**
     * Create indexed tokens from full text for better search.
     * Normalizes, lowercases, and extracts unique words.
     */
    private fun createIndexedTokens(fullText: String): String {
        if (fullText.isBlank()) return ""
        
        return fullText
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
            .distinct()
            .joinToString(" ")
    }
    
    /**
     * Close the recognizer and release resources.
     */
    fun close() {
        recognizer.close()
    }
}
