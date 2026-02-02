package com.photo.searchai.core.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.photo.searchai.core.data.repository.MediaRepository
import com.photo.searchai.core.ocr.LabelingProcessor
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
        private val groupRepository: com.photo.searchai.core.data.repository.GroupRepository,
        private val ocrProcessor: OcrProcessor,
        private val labelingProcessor: LabelingProcessor
) : CoroutineWorker(appContext, workerParams) {

    private val notificationHelper = SyncNotificationHelper(appContext)

    override suspend fun doWork(): Result {
        if (!jobLock.tryLock()) {
            return Result.success()
        }
        return try {
            Log.i(TAG, "MediaSyncWorker started: attempt=$runAttemptCount")
            if (isPermissionFailed()) return Result.failure()

            syncMedia()
            processPendingOcr()
            processPendingLabeling()
            Log.i(TAG, "MediaSyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "MediaSyncWorker failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } finally {
            jobLock.unlock()
        }
    }

    private suspend fun isPermissionFailed(): Boolean {
        if (!PermissionChecker.hasPermission(applicationContext, PermissionType.ALL_FILES)) {
            return true
        }

        // Initial foreground notification
        if (PermissionChecker.hasPermission(applicationContext, PermissionType.NOTIFICATION)) {
            setForeground(notificationHelper.createForegroundInfo(0, 0))
        }
        return false
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

        Log.i(TAG, "OCR pending count=$total")
        if (total == 0) return

        var processedCount = 0
        pendingImages.forEachIndexed { index, image ->
            if (isStopped) return@forEachIndexed

            // Update progress notification
            if (PermissionChecker.hasPermission(applicationContext, PermissionType.NOTIFICATION)) {
                setForeground(notificationHelper.createForegroundInfo(index + 1, total))
            }

            try {
                val text = ocrProcessor.processImage(image.uri)
                repository.updateOcrResult(image.id, text)

                // Extract keywords for grouping
                if (text.isNotBlank()) {
                    groupRepository.extractAndSaveKeywords(image.id, text)
                }

                processedCount++
            } catch (e: Exception) {
                // Individual image processing failure shouldn't stop the whole sync
            }
        }

        // Regenerate groups if we processed any images
        if (processedCount > 0) {
            groupRepository.generateGroups()
        }
    }

    private suspend fun processPendingLabeling() {
        var totalProcessed = 0
        var batchIndex = 0

        while (true) {
            val pendingImages = repository.getPendingLabelingImages()
            if (pendingImages.isEmpty()) break

            pendingImages.forEach { image ->
                if (isStopped) {
                    Log.w(TAG, "Labeling stopped early at imageId=${image.id}")
                    return
                }

                try {
                    val labels = labelingProcessor.processImage(image.uri)
                    val entities =
                            if (labels.isEmpty()) {
                                listOf(
                                        com.photo.searchai.core.database.entity.ImageLabelEntity(
                                                imageId = image.id,
                                                labelId = NO_LABEL_ID,
                                                labelText = NO_LABEL_TEXT,
                                                confidence = 0f,
                                                modelVersion = MODEL_VERSION
                                        )
                                )
                            } else {
                                labels.map { label ->
                                    com.photo.searchai.core.database.entity.ImageLabelEntity(
                                            imageId = image.id,
                                            labelId = label.index.toString(),
                                            labelText = label.text,
                                            confidence = label.confidence,
                                            modelVersion = MODEL_VERSION
                                    )
                                }
                            }
                    repository.saveLabelingResults(entities)
                    totalProcessed++

                } catch (e: Exception) {
                    Log.e(TAG, "Labeling failed for imageId=${image.id} uri=${image.uri}", e)
                }
            }

            batchIndex++
        }

    }

    companion object {
        private val jobLock = Mutex()
        private const val MODEL_VERSION = "mlkit-image-labeling-17.0.7"
        private const val NO_LABEL_ID = "no_label"
        private const val NO_LABEL_TEXT = "NO_LABEL"
        private const val TAG = "MediaSyncWorker"
    }
}
