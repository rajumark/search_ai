package com.photo.searchai.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.data.datastore.ScheduledWorkDataStore
import com.photo.searchai.data.local.dao.WorkerHistoryDao
import com.photo.searchai.data.local.entity.WorkerHistoryEntity
import com.photo.searchai.data.local.entity.WorkerStatus
import com.photo.searchai.worker.WorkManagerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class RefreshHistoryUiState(
        val historyItems: List<WorkerHistoryItem> = emptyList(),
        val isLoading: Boolean = true,
        val isPeriodicScheduled: Boolean = false,
        val nextScheduledTime: Long = 0L,
        val lastRunStatus: String = "",
        val totalRuns: Int = 0,
        val successfulRuns: Int = 0
)

data class WorkerHistoryItem(
        val id: Long,
        val startTime: Long,
        val endTime: Long?,
        val durationMs: Long?,
        val status: WorkerStatus,
        val imagesProcessedOcr: Int,
        val imagesProcessedBarcode: Int,
        val imagesProcessedLabel: Int,
        val errorMessage: String?,
        val isScheduledRun: Boolean
) {
    val formattedStartTime: String
        get() = formatDateTime(startTime)

    val formattedEndTime: String
        get() = endTime?.let { formatDateTime(it) } ?: "—"

    val formattedDuration: String
        get() = durationMs?.let { formatDuration(it) } ?: "—"

    val formattedDate: String
        get() = formatDate(startTime)

    val formattedTimeRange: String
        get() {
            val start = formatTime(startTime)
            val end = endTime?.let { formatTime(it) } ?: "..."
            return "$start → $end"
        }

    val totalImagesProcessed: Int
        get() = imagesProcessedOcr + imagesProcessedBarcode + imagesProcessedLabel

    val statusText: String
        get() =
                when (status) {
                    WorkerStatus.RUNNING -> "Running..."
                    WorkerStatus.COMPLETED -> "Completed"
                    WorkerStatus.FAILED -> "Failed"
                    WorkerStatus.CANCELLED -> "Cancelled"
                }

    private fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDuration(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}

@HiltViewModel
class RefreshHistoryViewModel
@Inject
constructor(
        private val workerHistoryDao: WorkerHistoryDao,
        private val scheduledWorkDataStore: ScheduledWorkDataStore,
        private val workManagerHelper: WorkManagerHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(RefreshHistoryUiState())
    val uiState: StateFlow<RefreshHistoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                            workerHistoryDao.getRecentHistory(50),
                            scheduledWorkDataStore.stateFlow,
                            workManagerHelper.isPeriodicWorkScheduled()
                    ) { history, scheduledState, isScheduled ->
                RefreshHistoryUiState(
                        historyItems = history.map { it.toUiItem() },
                        isLoading = false,
                        isPeriodicScheduled = isScheduled,
                        nextScheduledTime = scheduledState.nextScheduledTime,
                        lastRunStatus = if (scheduledState.lastRunSuccess) "Success" else "Failed",
                        totalRuns = scheduledState.totalRuns,
                        successfulRuns = scheduledState.totalSuccessfulRuns
                )
            }
                    .collect { state -> _uiState.value = state }
        }
    }

    fun schedulePeriodicProcessing() {
        workManagerHelper.schedulePeriodicProcessing()
        viewModelScope.launch { scheduledWorkDataStore.recordWorkScheduled() }
    }

    fun cancelPeriodicProcessing() {
        workManagerHelper.cancelPeriodicProcessing()
    }

    fun triggerManualRefresh() {
        workManagerHelper.triggerImmediatePeriodicProcessing()
    }

    fun rescheduleWork() {
        workManagerHelper.reschedulePeriodicProcessing()
        viewModelScope.launch {
            scheduledWorkDataStore.recordWorkScheduled()
            scheduledWorkDataStore.resetFailureCount()
        }
    }

    private fun WorkerHistoryEntity.toUiItem(): WorkerHistoryItem {
        return WorkerHistoryItem(
                id = id,
                startTime = startTime,
                endTime = endTime,
                durationMs = durationMs,
                status = status,
                imagesProcessedOcr = imagesProcessedOcr,
                imagesProcessedBarcode = imagesProcessedBarcode,
                imagesProcessedLabel = imagesProcessedLabel,
                errorMessage = errorMessage,
                isScheduledRun = isScheduledRun
        )
    }
}
