package com.photo.searchai.core.opencv

import android.graphics.Bitmap

/**
 * Public API for blur detection.
 * 
 * Use this interface to check if an image is blurred.
 * The default implementation uses Laplacian variance (industry standard).
 */
interface BlurDetector {
    
    /**
     * Detect if the given bitmap is blurred.
     * 
     * @param bitmap The image to analyze
     * @param threshold The blur threshold. Images with variance below this
     *                  value are considered blurred. Recommended range: 80-120.
     *                  Start with 100.0 and adjust based on your use case.
     * @return BlurResult containing the blur status and score
     */
    fun detect(
        bitmap: Bitmap,
        threshold: Double = 100.0
    ): BlurResult
}
