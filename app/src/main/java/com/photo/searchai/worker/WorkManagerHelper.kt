package com.photo.searchai.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.photo.searchai.core.work.MediaSyncWorker
import com.photo.searchai.core.work.MetadataWorker
import com.photo.searchai.core.work.OrganizationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Helper class for managing image processing WorkManager operations. Handles both one-time
 * processing and periodic 6-hour background processing.
 */
@Singleton
class WorkManagerHelper @Inject constructor(@ApplicationContext private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        // Periodic work runs every 6 hours
        const val PERIODIC_INTERVAL_HOURS = 6L
        // Flex interval - work can run within this window before the interval
        const val PERIODIC_FLEX_HOURS = 1L
    }

    /**
     * Enqueue the new unified image processing work (one-time). Uses KEEP policy to avoid duplicate
     * work.
     */
    fun enqueueImageProcessingWork() {
        val workRequest =
                OneTimeWorkRequestBuilder<ImageProcessingWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()

        workManager.enqueueUniqueWork(
                ImageProcessingWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
        )
    }

    /**
     * Schedule periodic background processing every 6 hours. This ensures images are processed even
     * when the app is not open.
     *
     * Features:
     * - Runs every 6 hours reliably
     * - Uses KEEP policy to not reset if already scheduled
     * - Backoff policy for retry on failure
     * - No network required (works offline)
     */
    fun schedulePeriodicProcessing() {
        // Constraints: No network required, battery not critical
        val constraints =
                Constraints.Builder()
                        .setRequiresBatteryNotLow(true) // Don't run when battery is critically low
                        .build()

        val periodicWorkRequest =
                PeriodicWorkRequestBuilder<PeriodicImageProcessingWorker>(
                                repeatInterval = PERIODIC_INTERVAL_HOURS,
                                repeatIntervalTimeUnit = TimeUnit.HOURS,
                                flexTimeInterval = PERIODIC_FLEX_HOURS,
                                flexTimeIntervalUnit = TimeUnit.HOURS
                        )
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                30, // Initial backoff: 30 minutes
                                TimeUnit.MINUTES
                        )
                        .addTag("periodic_processing")
                        .build()

        workManager.enqueueUniquePeriodicWork(
                PeriodicImageProcessingWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing schedule if already active
                periodicWorkRequest
        )
    }

    /**
     * Force reschedule periodic work (useful after failure recovery). Uses REPLACE policy to
     * restart the schedule.
     */
    fun reschedulePeriodicProcessing() {
        val constraints = Constraints.Builder().setRequiresBatteryNotLow(true).build()

        val periodicWorkRequest =
                PeriodicWorkRequestBuilder<PeriodicImageProcessingWorker>(
                                repeatInterval = PERIODIC_INTERVAL_HOURS,
                                repeatIntervalTimeUnit = TimeUnit.HOURS,
                                flexTimeInterval = PERIODIC_FLEX_HOURS,
                                flexTimeIntervalUnit = TimeUnit.HOURS
                        )
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                        .addTag("periodic_processing")
                        .build()

        workManager.enqueueUniquePeriodicWork(
                PeriodicImageProcessingWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, // Replace/update existing schedule
                periodicWorkRequest
        )
    }

    /** Cancel periodic processing. */
    fun cancelPeriodicProcessing() {
        workManager.cancelUniqueWork(PeriodicImageProcessingWorker.WORK_NAME)
    }

    /**
     * Run periodic processing immediately (one-time trigger). Useful for testing or manual refresh.
     */
    fun triggerImmediatePeriodicProcessing() {
        val workRequest =
                OneTimeWorkRequestBuilder<PeriodicImageProcessingWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .addTag("manual_periodic_trigger")
                        .build()

        workManager.enqueueUniqueWork(
                "manual_periodic_processing",
                ExistingWorkPolicy.KEEP,
                workRequest
        )
    }

    /**
     * Enqueue metadata extraction and organization tasks.
     * Usually called after a MediaSyncWorker completes or on app startup.
     */
    fun enqueueMetadataProcessing() {
        val syncRequest = OneTimeWorkRequestBuilder<MediaSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
            
        val metadataRequest = OneTimeWorkRequestBuilder<MetadataWorker>()
            .build()
            
        val organizationRequest = OneTimeWorkRequestBuilder<OrganizationWorker>()
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        workManager.beginUniqueWork("metadata_processing_chain", ExistingWorkPolicy.KEEP, syncRequest)
            .then(metadataRequest)
            .then(organizationRequest)
            .enqueue()
    }

    /**
     * Schedule periodic organization tasks (duplicates, cleanup).
     */
    fun scheduleOrganizationTasks() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<OrganizationWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("periodic_organization")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "periodic_organization_task",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    /** Legacy method - now delegates to enqueueImageProcessingWork. */
    fun enqueueOcrWork() {
        enqueueImageProcessingWork()
    }

    /** Cancel all image processing work. */
    fun cancelImageProcessingWork() {
        workManager.cancelUniqueWork(ImageProcessingWorker.WORK_NAME)
        // Also cancel legacy work if running
        workManager.cancelUniqueWork(OcrIndexingWorker.WORK_NAME)
    }

    /** Legacy method - now delegates to cancelImageProcessingWork. */
    fun cancelOcrWork() {
        cancelImageProcessingWork()
    }

    /** Get current work state as Flow. */
    fun getWorkStateFlow(): Flow<WorkInfo.State?> {
        return workManager.getWorkInfosForUniqueWorkFlow(ImageProcessingWorker.WORK_NAME).map {
                workInfos ->
            workInfos.firstOrNull()?.state
        }
    }

    /** Get periodic work state as Flow. */
    fun getPeriodicWorkStateFlow(): Flow<WorkInfo.State?> {
        return workManager.getWorkInfosForUniqueWorkFlow(PeriodicImageProcessingWorker.WORK_NAME)
                .map { workInfos -> workInfos.firstOrNull()?.state }
    }

    /** Check if periodic work is scheduled/active. */
    fun isPeriodicWorkScheduled(): Flow<Boolean> {
        return workManager.getWorkInfosForUniqueWorkFlow(PeriodicImageProcessingWorker.WORK_NAME)
                .map { workInfos ->
                    val state = workInfos.firstOrNull()?.state
                    state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING
                }
    }

    /**
     * Check if any processing work is currently running. Checks one-time, periodic, and legacy
     * workers.
     */
    fun isWorkRunning(): Flow<Boolean> {
        val newWorkerState =
                workManager.getWorkInfosForUniqueWorkFlow(ImageProcessingWorker.WORK_NAME).map {
                        workInfos ->
                    val state = workInfos.firstOrNull()?.state
                    state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
                }

        val periodicWorkerState =
                workManager.getWorkInfosForUniqueWorkFlow(PeriodicImageProcessingWorker.WORK_NAME)
                        .map { workInfos ->
                            val state = workInfos.firstOrNull()?.state
                            state == WorkInfo.State.RUNNING
                        }

        val legacyWorkerState =
                workManager.getWorkInfosForUniqueWorkFlow(OcrIndexingWorker.WORK_NAME).map {
                        workInfos ->
                    val state = workInfos.firstOrNull()?.state
                    state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
                }

        return combine(newWorkerState, periodicWorkerState, legacyWorkerState) {
                newRunning,
                periodicRunning,
                legacyRunning ->
            newRunning || periodicRunning || legacyRunning
        }
    }
}
