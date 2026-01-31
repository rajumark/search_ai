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
    val statusText: String = ""
) {
    companion object {
        val Loading = HomeUiState(isLoading = true)
    }
}
