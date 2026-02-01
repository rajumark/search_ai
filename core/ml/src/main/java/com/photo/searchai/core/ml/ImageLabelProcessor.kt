package com.photo.searchai.core.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Result from image labeling for a single label.
 */
data class ImageLabelResult(
    val label: String,
    val confidence: Float,
    val index: Int
)

/**
 * Wrapper for ML Kit Image Labeling.
 * Uses on-device model for fast labeling.
 */
@Singleton
class ImageLabelProcessor @Inject constructor() {
    
    companion object {
        // Minimum confidence threshold for labels (0.5 = 50%)
        private const val CONFIDENCE_THRESHOLD = 0.5f
        // Maximum number of labels to return per image
        private const val MAX_LABELS = 10
    }
    
    private val labeler: ImageLabeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
            .build()
        ImageLabeling.getClient(options)
    }
    
    /**
     * Process a bitmap and detect labels.
     * Returns list of ImageLabelResult sorted by confidence.
     */
    suspend fun processImage(bitmap: Bitmap): List<ImageLabelResult> = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        labeler.process(inputImage)
            .addOnSuccessListener { labels ->
                if (continuation.isActive) {
                    val results = labels
                        .take(MAX_LABELS)
                        .map { label ->
                            ImageLabelResult(
                                label = label.text,
                                confidence = label.confidence,
                                index = label.index
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
     * Close the labeler and release resources.
     */
    fun close() {
        labeler.close()
    }
}
