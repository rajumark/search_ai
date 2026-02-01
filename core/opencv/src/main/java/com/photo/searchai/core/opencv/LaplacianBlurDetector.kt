package com.photo.searchai.core.opencv

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Blur detector implementation using Laplacian variance method.
 * 
 * This is the industry-standard approach for blur detection:
 * 1. Convert image to grayscale
 * 2. Apply Laplacian operator (edge detection)
 * 3. Calculate variance of the Laplacian
 * 4. Low variance = blurred image (fewer edges)
 * 
 * Performance tips:
 * - Downscale images to ~512px max side before detection
 * - Never run on UI thread (use WorkManager or coroutines)
 */
@Singleton
internal class LaplacianBlurDetector @Inject constructor() : BlurDetector {

    override fun detect(bitmap: Bitmap, threshold: Double): BlurResult {
        // Convert bitmap to OpenCV Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)

        // Apply Laplacian operator
        val laplacian = Mat()
        Imgproc.Laplacian(mat, laplacian, CvType.CV_64F)

        // Calculate mean and standard deviation
        val mean = MatOfDouble()
        val stdDev = MatOfDouble()
        Core.meanStdDev(laplacian, mean, stdDev)

        // Variance = stdDev^2
        val variance = stdDev.toArray()[0].pow(2.0)

        // Clean up native memory
        mat.release()
        laplacian.release()
        mean.release()
        stdDev.release()

        return BlurResult(
            isBlurred = variance < threshold,
            blurScore = variance
        )
    }
}
