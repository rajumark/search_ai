package com.photo.searchai.core.opencv

/**
 * Result from blur detection analysis.
 * 
 * @param isBlurred Whether the image is considered blurred based on the threshold
 * @param blurScore The computed blur score (Laplacian variance). 
 *                  Lower values indicate more blur.
 */
data class BlurResult(
    val isBlurred: Boolean,
    val blurScore: Double
)
