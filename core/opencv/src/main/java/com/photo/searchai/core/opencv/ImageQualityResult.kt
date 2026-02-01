package com.photo.searchai.core.opencv

/**
 * Comprehensive image quality analysis result.
 * All metrics computed in single-pass for efficiency.
 * 
 * Store raw scores in DB, derive boolean flags at query time
 * to allow dynamic threshold adjustment per device.
 */
data class ImageQualityResult(
    /** Laplacian variance - lower = more blur. Threshold: ~100 */
    val blurScore: Double,
    
    /** Mean luminance (0-255). Below 50 = dark */
    val brightnessScore: Double,
    
    /** Std deviation of grayscale. Below 30 = low contrast */
    val contrastScore: Double,
    
    /** Ratio of near-white pixels (0.0-1.0). Above 0.3 = overexposed */
    val overexposedRatio: Double,
    
    /** Original image width */
    val width: Int,
    
    /** Original image height */
    val height: Int,
    
    /** Perceptual hash for duplicate detection (hex string) */
    val imageHash: String
) {
    companion object {
        // Default thresholds - can be adjusted per device class
        const val BLUR_THRESHOLD = 100.0
        const val DARK_THRESHOLD = 50.0
        const val OVEREXPOSED_THRESHOLD = 0.3
        const val LOW_CONTRAST_THRESHOLD = 30.0
        const val MIN_RESOLUTION = 640 * 480 // ~0.3MP
    }
    
    /** Image is likely blurred (low edge variance) */
    val isBlurred: Boolean 
        get() = blurScore < BLUR_THRESHOLD
    
    /** Image is too dark */
    val isDark: Boolean 
        get() = brightnessScore < DARK_THRESHOLD
    
    /** Image is overexposed (too many white pixels) */
    val isOverexposed: Boolean 
        get() = overexposedRatio > OVEREXPOSED_THRESHOLD
    
    /** Image has low contrast */
    val isLowContrast: Boolean 
        get() = contrastScore < LOW_CONTRAST_THRESHOLD
    
    /** Image resolution is below threshold */
    val isLowRes: Boolean 
        get() = width * height < MIN_RESOLUTION
    
    /** Overall quality score (0-100, higher = better) */
    val qualityScore: Int
        get() {
            var score = 100
            if (isBlurred) score -= 25
            if (isDark) score -= 20
            if (isOverexposed) score -= 20
            if (isLowContrast) score -= 15
            if (isLowRes) score -= 20
            return score.coerceIn(0, 100)
        }
    
    /** True if image has any quality issues */
    val hasIssues: Boolean
        get() = isBlurred || isDark || isOverexposed || isLowContrast || isLowRes
}
