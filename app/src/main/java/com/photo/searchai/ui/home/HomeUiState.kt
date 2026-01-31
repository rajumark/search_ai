package com.photo.searchai.ui.home

import com.photo.searchai.data.datastore.ProcessingStage

/**
 * Progress data for a single processing stage.
 */
data class StageProgress(
    val name: String,
    val parsed: Int = 0,
    val pending: Int = 0,
    val total: Int = 0,
    val isActive: Boolean = false,
    val isComplete: Boolean = false
) {
    val progress: Float
        get() = if (total > 0) parsed.toFloat() / total else 0f
    
    val percentage: Int
        get() = (progress * 100).toInt()
}

/**
 * UI state for the Home screen.
 * Tracks progress for OCR, barcode scanning, and image labeling.
 */
data class HomeUiState(
    val totalImages: Int = 0,
    val parsedImages: Int = 0,  // Legacy: OCR parsed count
    val pendingImages: Int = 0, // Legacy: OCR pending count
    val progress: Float = 0f,   // Legacy: OCR progress
    val isIndexing: Boolean = false,
    val isLoading: Boolean = true,
    val statusText: String = "",
    
    // Multi-stage progress tracking
    val currentStage: ProcessingStage = ProcessingStage.IDLE,
    val ocrProgress: StageProgress = StageProgress("Text Recognition"),
    val barcodeProgress: StageProgress = StageProgress("Barcode Scanning"),
    val labelProgress: StageProgress = StageProgress("Image Labeling"),
    
    // Overall progress
    val overallProgress: Float = 0f,
    val overallPercentage: Int = 0,
    
    // Session tracking for progress ribbon
    val initialPendingCount: Int = 0,
    val processedInSession: Int = 0,
    val estimatedTimeRemainingSeconds: Long = 0L,
    val showProgressRibbon: Boolean = false
) {
    companion object {
        val Loading = HomeUiState(isLoading = true)
    }
    
    // Check if any stage is in progress
    val isProcessing: Boolean
        get() = currentStage != ProcessingStage.IDLE && currentStage != ProcessingStage.COMPLETE
    
    // Calculate total pending across all stages
    val totalPending: Int
        get() = ocrProgress.pending + barcodeProgress.pending + labelProgress.pending
    
    // Check if all stages are complete
    val isFullyComplete: Boolean
        get() = totalImages > 0 && ocrProgress.isComplete && barcodeProgress.isComplete && labelProgress.isComplete
    
    // Current stage name for display
    val currentStageName: String
        get() = when (currentStage) {
            ProcessingStage.OCR -> "Text Recognition"
            ProcessingStage.BARCODE -> "Barcode Scanning"
            ProcessingStage.LABELING -> "Image Labeling"
            ProcessingStage.COMPLETE -> "Complete"
            ProcessingStage.IDLE -> "Ready"
        }
    
    // Stage number for display (1/3, 2/3, 3/3)
    val stageNumberText: String
        get() = when (currentStage) {
            ProcessingStage.OCR -> "1/3"
            ProcessingStage.BARCODE -> "2/3"
            ProcessingStage.LABELING -> "3/3"
            else -> ""
        }
    
    // Legacy compatibility
    val sessionProgress: Float
        get() = if (initialPendingCount > 0) {
            processedInSession.toFloat() / initialPendingCount
        } else 0f
    
    val sessionPercentage: Int
        get() = (sessionProgress * 100).toInt()
}

