package com.photo.searchai.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.ocr.OcrProcessor
import com.photo.searchai.core.permission.PermissionChecker
import com.photo.searchai.core.permission.PermissionType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.sync.Mutex

@HiltWorker
class MediaSyncWorker
@AssistedInject
constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val repository: MediaRepository,
        private val ocrProcessor: OcrProcessor
) : CoroutineWorker(appContext, workerParams) {

    private val notificationHelper = SyncNotificationHelper(appContext)

    override suspend fun doWork(): Result {
        if (!jobLock.tryLock()) {
            return Result.success()
        }
        return try {
            if (!PermissionChecker.hasPermission(applicationContext, PermissionType.ALL_FILES)) {
                return Result.failure()
            }

            // Initial foreground notification
            if (PermissionChecker.hasPermission(applicationContext, PermissionType.NOTIFICATION)) {
                setForeground(notificationHelper.createForegroundInfo(0, 0))
            }

            syncMedia()
            processPendingOcr()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } finally {
            jobLock.unlock()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return notificationHelper.createForegroundInfo(0, 0)
    }

    private suspend fun syncMedia() {
        repository.syncImages()
    }

    private suspend fun processPendingOcr() {
        val pendingImages = repository.getPendingOcrImages()
        val total = pendingImages.size

        pendingImages.forEachIndexed { index, image ->
            if (isStopped) return@forEachIndexed

            // Update progress notification
            if (PermissionChecker.hasPermission(applicationContext, PermissionType.NOTIFICATION)) {
                setForeground(notificationHelper.createForegroundInfo(index + 1, total))
            }

            try {
                val text = ocrProcessor.processImage(image.uri)
                repository.updateOcrResult(image.id, text)
            } catch (e: Exception) {
                // Individual image processing failure shouldn't stop the whole sync
            }
        }
    }

    companion object {
        private val jobLock = Mutex()
    }
}
