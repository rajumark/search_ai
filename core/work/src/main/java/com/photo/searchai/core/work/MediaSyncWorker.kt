package com.photo.searchai.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.ocr.OcrProcessor
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
        return try {
            repository.syncImages()

            // Process OCR for pending images
            val pendingImages = repository.getPendingOcrImages()
            for (image in pendingImages) {
                if (isStopped) break

                val text = ocrProcessor.processImage(image.uri)
                repository.updateOcrResult(image.id, text)
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
