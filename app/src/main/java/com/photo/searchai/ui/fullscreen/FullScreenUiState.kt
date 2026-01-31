package com.photo.searchai.ui.fullscreen

import com.photo.searchai.ui.search.ImageWithText

/**
 * UI state for the FullScreen Image viewer.
 */
data class FullScreenUiState(
    val currentIndex: Int = 0,
    val images: List<ImageWithText> = emptyList(),
    val isLoading: Boolean = true,
    val showOcrBottomSheet: Boolean = false,
    val currentOcrText: String? = null,
    val errorMessage: String? = null
)

/**
 * Events from the FullScreen viewer.
 */
sealed class FullScreenEvent {
    data class ShowToast(val message: String) : FullScreenEvent()
    data object NavigateBack : FullScreenEvent()
}
