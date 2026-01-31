package com.photo.searchai.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing OCR WorkManager operations.
 */
@Singleton
class WorkManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Enqueue OCR indexing work.
     * Uses KEEP policy to avoid duplicate work.
     */
    fun enqueueOcrWork() {
        val workRequest = OneTimeWorkRequestBuilder<OcrIndexingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        
        workManager.enqueueUniqueWork(
            OcrIndexingWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Cancel OCR indexing work.
     */
    fun cancelOcrWork() {
        workManager.cancelUniqueWork(OcrIndexingWorker.WORK_NAME)
    }
    
    /**
     * Get current work state as Flow.
     */
    fun getWorkStateFlow(): Flow<WorkInfo.State?> {
        return workManager.getWorkInfosForUniqueWorkFlow(OcrIndexingWorker.WORK_NAME)
            .map { workInfos ->
                workInfos.firstOrNull()?.state
            }
    }
    
    /**
     * Check if work is currently running.
     */
    fun isWorkRunning(): Flow<Boolean> {
        return getWorkStateFlow().map { state ->
            state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
        }
    }
}
