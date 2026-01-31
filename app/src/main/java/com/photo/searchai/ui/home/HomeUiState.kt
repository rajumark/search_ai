package com.photo.searchai.ui.home

import com.photo.searchai.data.datastore.BenchmarkData
import com.photo.searchai.data.datastore.ProcessingStage

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

data class HomeUiState(
        val totalImages: Int = 0,
        val parsedImages: Int = 0,
        val pendingImages: Int = 0,
        val progress: Float = 0f,
        val isIndexing: Boolean = false,
        val isLoading: Boolean = true,
        val statusText: String = "",
        val currentStage: ProcessingStage = ProcessingStage.IDLE,
        val ocrProgress: StageProgress = StageProgress("Text Recognition"),
        val barcodeProgress: StageProgress = StageProgress("Barcode Scanning"),
        val labelProgress: StageProgress = StageProgress("Image Labeling"),
        val overallProgress: Float = 0f,
        val overallPercentage: Int = 0,
        val initialPendingCount: Int = 0,
        val processedInSession: Int = 0,
        val estimatedTimeRemainingSeconds: Long = 0L,
        val showProgressRibbon: Boolean = false,
        val benchmarkData: BenchmarkData = BenchmarkData(),
        val showBenchmarkCard: Boolean = false,
        val averageTimePerImageMs: Long = 0L
) {
    companion object {
        val Loading = HomeUiState(isLoading = true)
    }

    val isProcessing: Boolean
        get() = currentStage != ProcessingStage.IDLE && currentStage != ProcessingStage.COMPLETE

    val totalPending: Int
        get() = ocrProgress.pending + barcodeProgress.pending + labelProgress.pending

    val isFullyComplete: Boolean
        get() =
                totalImages > 0 &&
                        ocrProgress.isComplete &&
                        barcodeProgress.isComplete &&
                        labelProgress.isComplete

    val currentStageName: String
        get() =
                when (currentStage) {
                    ProcessingStage.OCR -> "Text Recognition"
                    ProcessingStage.BARCODE -> "Barcode Scanning"
                    ProcessingStage.LABELING -> "Image Labeling"
                    ProcessingStage.COMPLETE -> "Complete"
                    ProcessingStage.IDLE -> "Ready"
                }

    val stageNumberText: String
        get() =
                when (currentStage) {
                    ProcessingStage.OCR -> "1/3"
                    ProcessingStage.BARCODE -> "2/3"
                    ProcessingStage.LABELING -> "3/3"
                    else -> ""
                }

    val sessionProgress: Float
        get() =
                if (initialPendingCount > 0) {
                    processedInSession.toFloat() / initialPendingCount
                } else 0f

    val sessionPercentage: Int
        get() = (sessionProgress * 100).toInt()

    val formattedAverageTime: String
        get() {
            val avgMs = averageTimePerImageMs
            return when {
                avgMs <= 0 -> "—"
                avgMs < 1000 -> "${avgMs}ms"
                avgMs < 60000 -> String.format("%.1fs", avgMs / 1000.0)
                else -> String.format("%.1fmin", avgMs / 60000.0)
            }
        }

    val formattedEstimatedTime: String
        get() {
            val seconds = estimatedTimeRemainingSeconds
            return when {
                seconds <= 0 -> "—"
                seconds < 60 -> "~${seconds}s"
                seconds < 3600 -> "~${seconds / 60}min"
                else -> "~${seconds / 3600}h ${(seconds % 3600) / 60}min"
            }
        }
}
