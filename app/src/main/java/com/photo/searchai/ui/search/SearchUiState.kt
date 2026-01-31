package com.photo.searchai.ui.search

import androidx.paging.PagingData
import com.photo.searchai.data.local.entity.ImageEntity
import com.photo.searchai.data.local.entity.OcrTextEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * UI state for the SearchByText screen.
 */
data class SearchUiState(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedImages: Set<Long> = emptySet(),
    val isInSelectionMode: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val imageWithTextList: List<ImageWithText> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Represents an image with its associated OCR text for UI display.
 */
data class ImageWithText(
    val mediaStoreId: Long,
    val imagePath: String,
    val ocrText: String?,
    val dateAdded: Long
)

/**
 * Events from the SearchByText screen that require navigation or other handling.
 */
sealed class SearchEvent {
    data class NavigateToFullScreen(val mediaStoreId: Long, val index: Int) : SearchEvent()
    data class ShowToast(val message: String) : SearchEvent()
    data object ClearSelection : SearchEvent()
}
