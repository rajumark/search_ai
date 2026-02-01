package com.photo.searchai.core.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Result from face detection processing. Contains bounding box coordinates and optional cropped
 * face bitmap.
 */
data class FaceDetectionResult(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val width: Int,
        val height: Int,
        val faceIndex: Int
) {
    companion object {
        val EMPTY = emptyList<FaceDetectionResult>()
    }
}

/**
 * Wrapper for ML Kit Face Detection. Uses on-device face detector with performance mode for faster
 * processing.
 */
@Singleton
class FaceDetectionProcessor @Inject constructor() {

    private val detector: FaceDetector by lazy {
        val options =
                FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setMinFaceSize(0.15f) // Minimum face size as a ratio of image width
                        .build()
        FaceDetection.getClient(options)
    }

    /**
     * Process a bitmap and detect all faces. Returns a list of FaceDetectionResult with bounding
     * box coordinates.
     */
    suspend fun processImage(bitmap: Bitmap): List<FaceDetectionResult> =
            suspendCancellableCoroutine { continuation ->
                val inputImage = InputImage.fromBitmap(bitmap, 0)

                detector.process(inputImage)
                        .addOnSuccessListener { faces ->
                            if (continuation.isActive) {
                                val results =
                                        faces.mapIndexed { index, face ->
                                            createFaceResult(face, index)
                                        }
                                continuation.resume(results)
                            }
                        }
                        .addOnFailureListener { exception ->
                            if (continuation.isActive) {
                                // Return empty list on failure instead of throwing
                                continuation.resume(emptyList())
                            }
                        }

                continuation.invokeOnCancellation {
                    // Cancellation handled by coroutine scope
                }
            }

    /** Creates a FaceDetectionResult from ML Kit Face object. */
    private fun createFaceResult(face: Face, index: Int): FaceDetectionResult {
        val boundingBox = face.boundingBox
        return FaceDetectionResult(
                left = boundingBox.left,
                top = boundingBox.top,
                right = boundingBox.right,
                bottom = boundingBox.bottom,
                width = boundingBox.width(),
                height = boundingBox.height(),
                faceIndex = index
        )
    }

    /**
     * Crop a face from the original bitmap with margin.
     * @param bitmap Original image bitmap
     * @param faceResult Face detection result with bounding box
     * @param marginPercent Margin percentage (10-15% recommended)
     * @return Cropped face bitmap or null if cropping fails
     */
    fun cropFaceWithMargin(
            bitmap: Bitmap,
            faceResult: FaceDetectionResult,
            marginPercent: Float = 0.12f
    ): Bitmap? {
        return try {
            val imageWidth = bitmap.width
            val imageHeight = bitmap.height

            // Calculate margin based on face size
            val marginX = (faceResult.width * marginPercent).toInt()
            val marginY = (faceResult.height * marginPercent).toInt()

            // Calculate crop bounds with margin, clamped to image bounds
            val left = max(0, faceResult.left - marginX)
            val top = max(0, faceResult.top - marginY)
            val right = min(imageWidth, faceResult.right + marginX)
            val bottom = min(imageHeight, faceResult.bottom + marginY)

            val cropWidth = right - left
            val cropHeight = bottom - top

            if (cropWidth <= 0 || cropHeight <= 0) {
                return null
            }

            Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
        } catch (e: Exception) {
            null
        }
    }

    /** Close the detector and release resources. */
    fun close() {
        detector.close()
    }
}
