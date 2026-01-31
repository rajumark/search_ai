package com.photo.searchai.ui.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.photo.searchai.data.local.entity.OcrTextEntity
import com.photo.searchai.repository.OcrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for the SearchByText screen.
 * Handles search, selection, and gallery-like actions.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val ocrRepository: OcrRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SearchEvent>()
    val events: Flow<SearchEvent> = _events.asSharedFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadSuggestions("")
    }

    // Paginated search results
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: Flow<PagingData<ImageWithText>> = searchQueryFlow
        .debounce(300) // Wait 300ms after user stops typing
        .distinctUntilChanged()
        .flatMapLatest { query ->
            loadSuggestions(query) // Update suggestions when query changes (debounced)
            if (query.isBlank()) {
                ocrRepository.getAllOcrTextPaging()
            } else {
                ocrRepository.searchOcrTextPaging(query)
            }
        }
        .map { pagingData ->
            pagingData.map { ocrTextEntity ->
                mapToImageWithText(ocrTextEntity)
            }
        }
        .cachedIn(viewModelScope)

    private suspend fun mapToImageWithText(ocrTextEntity: OcrTextEntity): ImageWithText {
        val image = ocrRepository.getImageById(ocrTextEntity.mediaStoreId)
        return ImageWithText(
            mediaStoreId = ocrTextEntity.mediaStoreId,
            imagePath = image?.path ?: "",
            ocrText = ocrTextEntity.fullText,
            dateAdded = image?.dateAdded ?: 0L
        )
    }

    /**
     * Updates the search query and triggers a new search.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    /**
     * Loads search suggestions based on the current query.
     */
    private fun loadSuggestions(query: String) {
        viewModelScope.launch {
            try {
                val suggestions = ocrRepository.getSearchSuggestions(query)
                _uiState.update { it.copy(suggestions = suggestions) }
            } catch (e: Exception) {
                // Ignore errors for suggestions
            }
        }
    }

    /**
     * Called when a suggestion chip is clicked.
     * Appends the suggestion to the current query.
     */
    fun onSuggestionClicked(suggestion: String) {
        val currentQuery = _uiState.value.searchQuery
        val newQuery = if (currentQuery.isBlank()) {
            suggestion
        } else {
            "$currentQuery $suggestion"
        }
        onSearchQueryChanged(newQuery)
    }

    /**
     * Toggles search active state.
     */
    fun onSearchActiveChanged(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active) }
    }

    /**
     * Clears the search query.
     */
    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", isSearchActive = false) }
        searchQueryFlow.value = ""
        loadSuggestions("")
    }

    /**
     * Toggles selection of an image.
     */
    fun toggleImageSelection(mediaStoreId: Long) {
        _uiState.update { currentState ->
            val newSelection = currentState.selectedImages.toMutableSet()
            if (mediaStoreId in newSelection) {
                newSelection.remove(mediaStoreId)
            } else {
                newSelection.add(mediaStoreId)
            }
            currentState.copy(
                selectedImages = newSelection,
                isInSelectionMode = newSelection.isNotEmpty()
            )
        }
    }

    /**
     * Called when an image is clicked.
     * If in selection mode, toggles selection.
     * Otherwise, opens full-screen view.
     */
    fun onImageClicked(mediaStoreId: Long, index: Int) {
        if (_uiState.value.isInSelectionMode) {
            toggleImageSelection(mediaStoreId)
        } else {
            viewModelScope.launch {
                _events.emit(SearchEvent.NavigateToFullScreen(mediaStoreId, index))
            }
        }
    }

    /**
     * Called when an image is long-pressed.
     * Enters selection mode and selects the image.
     */
    fun onImageLongPressed(mediaStoreId: Long) {
        _uiState.update { currentState ->
            val newSelection = currentState.selectedImages + mediaStoreId
            currentState.copy(
                selectedImages = newSelection,
                isInSelectionMode = true
            )
        }
    }

    /**
     * Clears all selections and exits selection mode.
     */
    fun clearSelection() {
        _uiState.update { 
            it.copy(
                selectedImages = emptySet(),
                isInSelectionMode = false
            ) 
        }
    }

    /**
     * Selects all items.
     */
    fun selectAll(allIds: List<Long>) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedImages = allIds.toSet(),
                isInSelectionMode = allIds.isNotEmpty()
            )
        }
    }

    /**
     * Deletes selected images.
     */
    fun deleteSelectedImages() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedImages.toList()
            if (selectedIds.isEmpty()) return@launch
 
            try {
                ocrRepository.deleteImages(selectedIds)
                _events.emit(SearchEvent.ShowToast("${selectedIds.size} image(s) deleted"))
                clearSelection()
            } catch (e: Exception) {
                _events.emit(SearchEvent.ShowToast("Failed to delete images"))
            }
        }
    }

    /**
     * Shares selected images.
     */
    fun shareSelectedImages() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedImages.toList()
            if (selectedIds.isEmpty()) return@launch
 
            try {
                val images = ocrRepository.getImagesByIds(selectedIds)
                val uris = images.mapNotNull { image ->
                    val file = File(image.path)
                    if (file.exists()) {
                        try {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } catch (e: Exception) {
                            Uri.fromFile(file)
                        }
                    } else null
                }
 
                if (uris.isNotEmpty()) {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                        type = "image/*"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share images").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } else {
                    _events.emit(SearchEvent.ShowToast("No valid images to share"))
                }
            } catch (e: Exception) {
                _events.emit(SearchEvent.ShowToast("Failed to share images"))
            }
        }
    }

    /**
     * Adds selected images to favorites (placeholder - would need favorites table).
     */
    fun addToFavorites() {
        viewModelScope.launch {
            val count = _uiState.value.selectedImages.size
            // TODO: Implement favorites functionality with a favorites table
            _events.emit(SearchEvent.ShowToast("$count image(s) added to favorites"))
            clearSelection()
        }
    }

    /**
     * Gets OCR text for a specific image.
     */
    suspend fun getOcrTextForImage(mediaStoreId: Long): String? {
        return ocrRepository.getOcrText(mediaStoreId)?.fullText
    }
}
