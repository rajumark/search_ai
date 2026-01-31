package com.photo.searchai.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing image processing WorkManager operations.
 * Handles both legacy OCR worker and new unified ImageProcessingWorker.
 */
@Singleton
class WorkManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Enqueue the new unified image processing work.
     * Uses KEEP policy to avoid duplicate work.
     */
    fun enqueueImageProcessingWork() {
        val workRequest = OneTimeWorkRequestBuilder<ImageProcessingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        
        workManager.enqueueUniqueWork(
            ImageProcessingWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Legacy method - now delegates to enqueueImageProcessingWork.
     */
    fun enqueueOcrWork() {
        enqueueImageProcessingWork()
    }
    
    /**
     * Cancel all image processing work.
     */
    fun cancelImageProcessingWork() {
        workManager.cancelUniqueWork(ImageProcessingWorker.WORK_NAME)
        // Also cancel legacy work if running
        workManager.cancelUniqueWork(OcrIndexingWorker.WORK_NAME)
    }
    
    /**
     * Legacy method - now delegates to cancelImageProcessingWork.
     */
    fun cancelOcrWork() {
        cancelImageProcessingWork()
    }
    
    /**
     * Get current work state as Flow.
     */
    fun getWorkStateFlow(): Flow<WorkInfo.State?> {
        return workManager.getWorkInfosForUniqueWorkFlow(ImageProcessingWorker.WORK_NAME)
            .map { workInfos ->
                workInfos.firstOrNull()?.state
            }
    }
    
    /**
     * Check if any processing work is currently running.
     * Checks both new and legacy workers.
     */
    fun isWorkRunning(): Flow<Boolean> {
        val newWorkerState = workManager.getWorkInfosForUniqueWorkFlow(ImageProcessingWorker.WORK_NAME)
            .map { workInfos ->
                val state = workInfos.firstOrNull()?.state
                state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
            }
        
        val legacyWorkerState = workManager.getWorkInfosForUniqueWorkFlow(OcrIndexingWorker.WORK_NAME)
            .map { workInfos ->
                val state = workInfos.firstOrNull()?.state
                state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
            }
        
        return combine(newWorkerState, legacyWorkerState) { newRunning, legacyRunning ->
            newRunning || legacyRunning
        }
    }
}

