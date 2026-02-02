package com.photo.searchai.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.ocr.OcrProcessor
import com.photo.searchai.core.permission.PermissionChecker
import com.photo.searchai.core.permission.PermissionType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MediaSyncWorker
@AssistedInject
constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val repository: MediaRepository,
        private val ocrProcessor: OcrProcessor
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!PermissionChecker.hasPermission(applicationContext, PermissionType.ALL_FILES)) {
            return Result.failure()
        }

        return try {
            syncMedia()
            processPendingOcr()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun syncMedia() {
        repository.syncImages()
    }

    private suspend fun processPendingOcr() {
        val pendingImages = repository.getPendingOcrImages()
        for (image in pendingImages) {
            if (isStopped) break

            try {
                val text = ocrProcessor.processImage(image.uri)
                repository.updateOcrResult(image.id, text)
            } catch (e: Exception) {
                // Individual image processing failure shouldn't stop the whole sync
            }
        }
    }
}
