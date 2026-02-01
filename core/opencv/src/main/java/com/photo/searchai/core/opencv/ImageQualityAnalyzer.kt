package com.photo.searchai.core.opencv

import android.graphics.Bitmap

/**
 * Public API for comprehensive image quality analysis.
 * 
 * Analyzes multiple quality metrics in a single pass for efficiency:
 * - Blur detection (Laplacian variance)
 * - Brightness (mean luminance)
 * - Contrast (std deviation)
 * - Overexposure (white pixel ratio)
 * - Resolution check
 * - Perceptual hash for duplicates
 * 
 * Usage:
 * ```
 * class ImageQualityService @Inject constructor(
 *     private val analyzer: ImageQualityAnalyzer
 * ) {
 *     fun checkQuality(bitmap: Bitmap): Boolean {
 *         val result = analyzer.analyze(bitmap)
 *         return !result.hasIssues
 *     }
 * }
 * ```
 */
interface ImageQualityAnalyzer {
    
    /**
     * Analyze image quality metrics.
     * 
     * @param bitmap The image to analyze. Will be downscaled internally
     *               for performance (512px max dimension).
     * @return ImageQualityResult with all computed metrics
     */
    fun analyze(bitmap: Bitmap): ImageQualityResult
}
