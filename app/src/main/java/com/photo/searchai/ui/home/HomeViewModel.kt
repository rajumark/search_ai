package com.photo.searchai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.data.datastore.ProcessingStage
import com.photo.searchai.repository.OcrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(private val ocrRepository: OcrRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var initialPendingCount: Int = -1
    private var sessionStartTime: Long = 0L
    private var lastProcessedCount: Int = 0

    init {
        initializeProcessing()
        observeProgress()
    }

    private fun initializeProcessing() {
        viewModelScope.launch {
            try {
                ocrRepository.syncImagesFromMediaStore()
                ocrRepository.enqueueOcrWorkIfNeeded()
            } catch (e: Exception) {}
        }
    }

    private fun observeProgress() {
        viewModelScope.launch {
            combine(
                            ocrRepository.getTotalCountFlow(),
                            ocrRepository.getParsedCountFlow(),
                            ocrRepository.getPendingCountFlow(),
                            ocrRepository.getBarcodeParsedCountFlow(),
                            ocrRepository.getBarcodePendingCountFlow(),
                            ocrRepository.getLabelParsedCountFlow(),
                            ocrRepository.getLabelPendingCountFlow(),
                            ocrRepository.getProgressFlow(),
                            ocrRepository.isWorkRunning()
                    ) { values ->
                val total = values[0] as Int
                val ocrParsed = values[1] as Int
                val ocrPending = values[2] as Int
                val barcodeParsed = values[3] as Int
                val barcodePending = values[4] as Int
                val labelParsed = values[5] as Int
                val labelPending = values[6] as Int
                val progress = values[7] as com.photo.searchai.data.datastore.ProcessingProgress
                val isRunning = values[8] as Boolean

                val totalPending = ocrPending + barcodePending + labelPending
                if (initialPendingCount == -1 && totalPending > 0) {
                    initialPendingCount = totalPending
                    sessionStartTime = System.currentTimeMillis()
                    lastProcessedCount = 0
                }

                val processedInSession =
                        if (initialPendingCount > 0) {
                            (initialPendingCount - totalPending).coerceAtLeast(0)
                        } else 0

                val estimatedTimeRemainingSeconds =
                        calculateEstimatedTime(
                                processedInSession = processedInSession,
                                remainingCount = totalPending
                        )

                val avgTimePerImage = calculateAverageTimePerImage(processedInSession)

                val showRibbon = totalPending > 0 && initialPendingCount > 0

                val totalWork = total * 3
                val completedWork = ocrParsed + barcodeParsed + labelParsed
                val overallProgress = if (totalWork > 0) completedWork.toFloat() / totalWork else 0f
                val overallPercentage = (overallProgress * 100).toInt()

                // Use actual stage from DataStore when work is running
                // This prevents UI showing wrong stage when new images are added mid-processing
                val currentStage =
                        when {
                            !isRunning && totalPending == 0 && total > 0 -> ProcessingStage.COMPLETE
                            !isRunning -> ProcessingStage.IDLE
                            // When running, trust the actual stage from the worker
                            progress.currentStage == ProcessingStage.LABELING ->
                                    ProcessingStage.LABELING
                            progress.currentStage == ProcessingStage.BARCODE ->
                                    ProcessingStage.BARCODE
                            progress.currentStage == ProcessingStage.OCR -> ProcessingStage.OCR
                            // Fallback to checking pending counts if DataStore stage is stale
                            ocrPending > 0 -> ProcessingStage.OCR
                            barcodePending > 0 -> ProcessingStage.BARCODE
                            labelPending > 0 -> ProcessingStage.LABELING
                            else -> ProcessingStage.COMPLETE
                        }

                val statusText =
                        when (currentStage) {
                            ProcessingStage.OCR -> "Finding text in photos…"
                            ProcessingStage.BARCODE -> "Scanning for barcodes…"
                            ProcessingStage.LABELING -> "Labeling images…"
                            ProcessingStage.COMPLETE -> "All processing complete"
                            ProcessingStage.IDLE ->
                                    if (total > 0) "Ready to search" else "No images found"
                        }

                val benchmarkData = progress.benchmarkData
                val showBenchmarkCard = true

                HomeUiState(
                        totalImages = total,
                        parsedImages = ocrParsed,
                        pendingImages = ocrPending,
                        progress = if (total > 0) ocrParsed.toFloat() / total else 0f,
                        isIndexing = isRunning,
                        isLoading = false,
                        statusText = statusText,
                        currentStage = currentStage,
                        ocrProgress =
                                StageProgress(
                                        name = "Text Recognition",
                                        parsed = ocrParsed,
                                        pending = ocrPending,
                                        total = total,
                                        isActive = currentStage == ProcessingStage.OCR,
                                        isComplete = ocrPending == 0 && total > 0
                                ),
                        barcodeProgress =
                                StageProgress(
                                        name = "Barcode Scanning",
                                        parsed = barcodeParsed,
                                        pending = barcodePending,
                                        total = total,
                                        isActive = currentStage == ProcessingStage.BARCODE,
                                        isComplete = barcodePending == 0 && total > 0
                                ),
                        labelProgress =
                                StageProgress(
                                        name = "Image Labeling",
                                        parsed = labelParsed,
                                        pending = labelPending,
                                        total = total,
                                        isActive = currentStage == ProcessingStage.LABELING,
                                        isComplete = labelPending == 0 && total > 0
                                ),
                        overallProgress = overallProgress,
                        overallPercentage = overallPercentage,
                        initialPendingCount =
                                if (initialPendingCount > 0) initialPendingCount else 0,
                        processedInSession = processedInSession,
                        estimatedTimeRemainingSeconds = estimatedTimeRemainingSeconds,
                        showProgressRibbon = showRibbon,
                        benchmarkData = benchmarkData,
                        showBenchmarkCard = showBenchmarkCard,
                        averageTimePerImageMs = avgTimePerImage
                )
            }
                    .collect { state -> _uiState.value = state }
        }
    }

    private fun calculateEstimatedTime(processedInSession: Int, remainingCount: Int): Long {
        if (processedInSession <= 0 || remainingCount <= 0) return 0L

        val elapsedTimeMs = System.currentTimeMillis() - sessionStartTime
        if (elapsedTimeMs <= 0) return 0L

        val avgTimePerImage = elapsedTimeMs.toFloat() / processedInSession
        val estimatedRemainingMs = (avgTimePerImage * remainingCount).toLong()

        return (estimatedRemainingMs / 1000L).coerceAtLeast(0L)
    }

    private fun calculateAverageTimePerImage(processedInSession: Int): Long {
        if (processedInSession <= 0 || sessionStartTime <= 0) return 0L

        val elapsedTimeMs = System.currentTimeMillis() - sessionStartTime
        if (elapsedTimeMs <= 0) return 0L

        return elapsedTimeMs / processedInSession
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                initialPendingCount = -1
                sessionStartTime = 0L
                lastProcessedCount = 0

                ocrRepository.syncImagesFromMediaStore()
                ocrRepository.enqueueOcrWorkIfNeeded()
            } catch (e: Exception) {} finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
