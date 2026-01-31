package com.photo.searchai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.repository.OcrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 * Observes OCR progress and manages indexing state.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ocrRepository: OcrRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        initializeOcr()
        observeProgress()
    }
    
    private fun initializeOcr() {
        viewModelScope.launch {
            try {
                // Sync images from MediaStore to Room
                ocrRepository.syncImagesFromMediaStore()
                
                // Enqueue OCR work if there are pending images
                ocrRepository.enqueueOcrWorkIfNeeded()
            } catch (e: Exception) {
                // Handle error - continue with observation
            }
        }
    }
    
    private fun observeProgress() {
        viewModelScope.launch {
            // Combine all progress flows
            combine(
                ocrRepository.getTotalCountFlow(),
                ocrRepository.getParsedCountFlow(),
                ocrRepository.getPendingCountFlow(),
                ocrRepository.isWorkRunning()
            ) { total, parsed, pending, isRunning ->
                val progress = if (total > 0) parsed.toFloat() / total else 0f
                val statusText = when {
                    isRunning -> "Indexing images…"
                    pending > 0 -> "Indexing paused"
                    total > 0 -> "Indexing complete"
                    else -> "No images found"
                }
                
                HomeUiState(
                    totalImages = total,
                    parsedImages = parsed,
                    pendingImages = pending,
                    progress = progress,
                    isIndexing = isRunning,
                    isLoading = false,
                    statusText = statusText
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    /**
     * Refresh images and restart OCR if needed.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                ocrRepository.syncImagesFromMediaStore()
                ocrRepository.enqueueOcrWorkIfNeeded()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

