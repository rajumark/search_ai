package com.photo.searchai.ui.home

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val totalImages: Int = 0,
    val parsedImages: Int = 0,
    val pendingImages: Int = 0,
    val progress: Float = 0f,
    val isIndexing: Boolean = false,
    val isLoading: Boolean = true,
    val statusText: String = "",
    // For bottom ribbon progress tracking
    val initialPendingCount: Int = 0,
    val processedInSession: Int = 0,
    val estimatedTimeRemainingSeconds: Long = 0L,
    val showProgressRibbon: Boolean = false
) {
    companion object {
        val Loading = HomeUiState(isLoading = true)
    }
    
    // Progress calculated from pending images processed in this session
    val sessionProgress: Float
        get() = if (initialPendingCount > 0) {
            processedInSession.toFloat() / initialPendingCount
        } else 0f
    
    // Session percentage (0-100)
    val sessionPercentage: Int
        get() = (sessionProgress * 100).toInt()
}
