package com.photo.searchai.ui.fullscreen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.repository.OcrRepository
import com.photo.searchai.ui.search.ImageWithText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for the FullScreen Image viewer.
 * Handles loading images, OCR text display, sharing, and deletion.
 */
@HiltViewModel
class FullScreenViewModel @Inject constructor(
    private val ocrRepository: OcrRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mediaStoreId: Long = savedStateHandle.get<String>("mediaStoreId")?.toLongOrNull() ?: 0L
    private val initialIndex: Int = savedStateHandle.get<String>("initialIndex")?.toIntOrNull() ?: 0

    private val _uiState = MutableStateFlow(FullScreenUiState())
    val uiState: StateFlow<FullScreenUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FullScreenEvent>()
    val events = _events.asSharedFlow()

    init {
        loadImagesAndNavigate()
    }

    /**
     * Loads all images with OCR text and navigates to the specified image.
     */
    private fun loadImagesAndNavigate() {
        viewModelScope.launch {
            try {
                // Get all OCR text entries
                val ocrTexts = ocrRepository.searchOcrText("")
                val images = ocrTexts.mapNotNull { ocrText ->
                    val image = ocrRepository.getImageById(ocrText.mediaStoreId)
                    image?.let {
                        ImageWithText(
                            mediaStoreId = it.mediaStoreId,
                            imagePath = it.path,
                            ocrText = ocrText.fullText,
                            dateAdded = it.dateAdded
                        )
                    }
                }

                // Find the index of the requested image
                val targetIndex = images.indexOfFirst { it.mediaStoreId == mediaStoreId }
                    .takeIf { it >= 0 } ?: initialIndex

                _uiState.update {
                    it.copy(
                        images = images,
                        currentIndex = targetIndex,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load images"
                    )
                }
            }
        }
    }

    /**
     * Updates the current index when user swipes.
     */
    fun onPageChanged(index: Int) {
        _uiState.update { it.copy(currentIndex = index) }
    }

    /**
     * Shows the OCR text bottom sheet for the current image.
     */
    fun showOcrText() {
        viewModelScope.launch {
            val currentImage = _uiState.value.images.getOrNull(_uiState.value.currentIndex)
            if (currentImage != null) {
                val ocrText = currentImage.ocrText
                if (ocrText.isNullOrBlank()) {
                    _events.emit(FullScreenEvent.ShowToast("No text found in this image"))
                } else {
                    _uiState.update {
                        it.copy(
                            showOcrBottomSheet = true,
                            currentOcrText = ocrText
                        )
                    }
                }
            }
        }
    }

    /**
     * Hides the OCR text bottom sheet.
     */
    fun hideOcrBottomSheet() {
        _uiState.update {
            it.copy(showOcrBottomSheet = false)
        }
    }

    /**
     * Copies the OCR text to clipboard.
     */
    fun copyOcrText() {
        viewModelScope.launch {
            val text = _uiState.value.currentOcrText
            if (!text.isNullOrBlank()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("OCR Text", text)
                clipboard.setPrimaryClip(clip)
                _events.emit(FullScreenEvent.ShowToast("Text copied to clipboard"))
            }
        }
    }

    /**
     * Shares the OCR text.
     */
    fun shareOcrText() {
        viewModelScope.launch {
            val text = _uiState.value.currentOcrText
            if (!text.isNullOrBlank()) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share text").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    /**
     * Shares the current image.
     */
    fun shareCurrentImage() {
        viewModelScope.launch {
            val currentImage = _uiState.value.images.getOrNull(_uiState.value.currentIndex)
            if (currentImage != null) {
                try {
                    val file = File(currentImage.imagePath)
                    if (file.exists()) {
                        val uri = try {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } catch (e: Exception) {
                            Uri.fromFile(file)
                        }

                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, uri)
                            type = "image/*"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share image").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } else {
                        _events.emit(FullScreenEvent.ShowToast("Image file not found"))
                    }
                } catch (e: Exception) {
                    _events.emit(FullScreenEvent.ShowToast("Failed to share image"))
                }
            }
        }
    }

    /**
     * Deletes the current image.
     */
    fun deleteCurrentImage() {
        viewModelScope.launch {
            val currentImage = _uiState.value.images.getOrNull(_uiState.value.currentIndex)
            if (currentImage != null) {
                try {
                    ocrRepository.deleteImages(listOf(currentImage.mediaStoreId))
                    
                    val updatedImages = _uiState.value.images.filterNot { 
                        it.mediaStoreId == currentImage.mediaStoreId 
                    }
                    
                    if (updatedImages.isEmpty()) {
                        _events.emit(FullScreenEvent.ShowToast("Image deleted"))
                        _events.emit(FullScreenEvent.NavigateBack)
                    } else {
                        val newIndex = (_uiState.value.currentIndex).coerceAtMost(updatedImages.size - 1)
                        _uiState.update { 
                            it.copy(
                                images = updatedImages,
                                currentIndex = newIndex
                            ) 
                        }
                        _events.emit(FullScreenEvent.ShowToast("Image deleted"))
                    }
                } catch (e: Exception) {
                    _events.emit(FullScreenEvent.ShowToast("Failed to delete image"))
                }
            }
        }
    }

    /**
     * Adds the current image to favorites.
     */
    fun addToFavorites() {
        viewModelScope.launch {
            // TODO: Implement favorites functionality with a favorites table
            _events.emit(FullScreenEvent.ShowToast("Added to favorites"))
        }
    }
}
