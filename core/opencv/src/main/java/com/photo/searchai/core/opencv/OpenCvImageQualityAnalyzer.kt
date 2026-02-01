package com.photo.searchai.core.opencv

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Optimized OpenCV implementation of [ImageQualityAnalyzer].
 * 
 * Performs all Tier 1 checks in a single pass to minimize bitmap loads
 * and native memory overhead.
 */
@Singleton
internal class OpenCvImageQualityAnalyzer @Inject constructor() : ImageQualityAnalyzer {

    override fun analyze(bitmap: Bitmap): ImageQualityResult {
        // 1. Pre-process: Downscale for performance (max 512px)
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val scaledBitmap = downscaleIfNeeded(bitmap, 512)
        
        val mat = Mat()
        Utils.bitmapToMat(scaledBitmap, mat)
        
        // 2. Convert to Grayscale (base for most analyzers)
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)
        
        // 3. Analyzers
        val brightnessScore = calcBrightness(grayMat)
        val contrastScore = calcContrast(grayMat)
        val blurScore = calcBlur(grayMat)
        val overexposedRatio = calcOverexposedRatio(grayMat)
        val imageHash = calcPerceptualHash(grayMat)
        
        // Cleanup
        mat.release()
        grayMat.release()
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        
        return ImageQualityResult(
            blurScore = blurScore,
            brightnessScore = brightnessScore,
            contrastScore = contrastScore,
            overexposedRatio = overexposedRatio,
            width = originalWidth,
            height = originalHeight,
            imageHash = imageHash
        )
    }

    /**
     * Laplacian variance method for blur detection.
     */
    private fun calcBlur(grayMat: Mat): Double {
        val laplacian = Mat()
        Imgproc.Laplacian(grayMat, laplacian, CvType.CV_64F)
        
        val stdDev = MatOfDouble()
        Core.meanStdDev(laplacian, MatOfDouble(), stdDev)
        
        val variance = stdDev.toArray()[0].pow(2.0)
        laplacian.release()
        stdDev.release()
        return variance
    }

    /**
     * Mean luminance for brightness.
     */
    private fun calcBrightness(grayMat: Mat): Double {
        val mean = Core.mean(grayMat)
        return mean.`val`[0]
    }

    /**
     * Std deviation for contrast.
     */
    private fun calcContrast(grayMat: Mat): Double {
        val stdDev = MatOfDouble()
        Core.meanStdDev(grayMat, MatOfDouble(), stdDev)
        val value = stdDev.toArray()[0]
        stdDev.release()
        return value
    }

    /**
     * Ratio of near-white pixels (> 240) for overexposure.
     */
    private fun calcOverexposedRatio(grayMat: Mat): Double {
        val thresholded = Mat()
        Imgproc.threshold(grayMat, thresholded, 240.0, 255.0, Imgproc.THRESH_BINARY)
        val nonZero = Core.countNonZero(thresholded)
        val total = grayMat.width() * grayMat.height()
        thresholded.release()
        return nonZero.toDouble() / total.toDouble()
    }

    /**
     * Simple Average Hash (aHash) for duplicates.
     * 8x8 grayscale -> mean threshold -> 64-bit fingerprint.
     */
    private fun calcPerceptualHash(grayMat: Mat): String {
        val small = Mat()
        Imgproc.resize(grayMat, small, Size(8.0, 8.0))
        
        val mean = Core.mean(small).`val`[0]
        val hash = StringBuilder()
        
        for (y in 0 until 8) {
            var rowHash = 0L
            for (x in 0 until 8) {
                val pixel = small.get(y, x)[0]
                if (pixel >= mean) {
                    rowHash = rowHash or (1L shl x)
                }
            }
            hash.append(rowHash.toString(16).padStart(2, '0'))
        }
        
        small.release()
        return hash.toString()
    }

    private fun downscaleIfNeeded(bitmap: Bitmap, maxSide: Int): Bitmap {
        if (bitmap.width <= maxSide && bitmap.height <= maxSide) return bitmap
        
        val ratio = maxSide.toFloat() / Math.max(bitmap.width, bitmap.height)
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
