package com.photo.searchai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to track when the periodic WorkManager job runs. Records start time, end time, duration,
 * status, and images processed.
 */
@Entity(tableName = "worker_history")
data class WorkerHistoryEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val startTime: Long,
        val endTime: Long? = null,
        val durationMs: Long? = null,
        val status: WorkerStatus = WorkerStatus.RUNNING,
        val imagesProcessedOcr: Int = 0,
        val imagesProcessedBarcode: Int = 0,
        val imagesProcessedLabel: Int = 0,
        val errorMessage: String? = null,
        val isScheduledRun: Boolean = true // true = periodic job, false = manual trigger
)

enum class WorkerStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
