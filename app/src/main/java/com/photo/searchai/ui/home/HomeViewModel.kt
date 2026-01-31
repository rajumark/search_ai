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
    
    // Session tracking for progress ribbon
    private var initialPendingCount: Int = -1  // -1 means not yet initialized
    private var sessionStartTime: Long = 0L
    private var lastProcessedCount: Int = 0
    
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
                
                // Initialize session tracking on first data
                if (initialPendingCount == -1 && pending > 0) {
                    initialPendingCount = pending
                    sessionStartTime = System.currentTimeMillis()
                    lastProcessedCount = 0
                }
                
                // Calculate session-based progress
                val processedInSession = if (initialPendingCount > 0) {
                    (initialPendingCount - pending).coerceAtLeast(0)
                } else 0
                
                // Calculate estimated time remaining
                val estimatedTimeRemainingSeconds = calculateEstimatedTime(
                    processedInSession = processedInSession,
                    remainingCount = pending
                )
                
                // Show ribbon only when there are pending images to process
                val showRibbon = pending > 0 && initialPendingCount > 0
                
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
                    statusText = statusText,
                    initialPendingCount = if (initialPendingCount > 0) initialPendingCount else 0,
                    processedInSession = processedInSession,
                    estimatedTimeRemainingSeconds = estimatedTimeRemainingSeconds,
                    showProgressRibbon = showRibbon
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    /**
     * Calculate estimated time remaining based on processing speed.
     */
    private fun calculateEstimatedTime(
        processedInSession: Int,
        remainingCount: Int
    ): Long {
        if (processedInSession <= 0 || remainingCount <= 0) return 0L
        
        val elapsedTimeMs = System.currentTimeMillis() - sessionStartTime
        if (elapsedTimeMs <= 0) return 0L
        
        // Calculate average time per image
        val avgTimePerImage = elapsedTimeMs.toFloat() / processedInSession
        
        // Estimate remaining time
        val estimatedRemainingMs = (avgTimePerImage * remainingCount).toLong()
        
        // Return in seconds
        return (estimatedRemainingMs / 1000L).coerceAtLeast(0L)
    }
    
    /**
     * Refresh images and restart OCR if needed.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Reset session tracking on refresh
                initialPendingCount = -1
                sessionStartTime = 0L
                lastProcessedCount = 0
                
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

