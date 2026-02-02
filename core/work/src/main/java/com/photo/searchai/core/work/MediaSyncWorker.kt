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
        private val groupRepository: com.photo.searchai.core.data.repository.GroupRepository,
        private val ocrProcessor: OcrProcessor
) : CoroutineWorker(appContext, workerParams) {

    private val notificationHelper = SyncNotificationHelper(appContext)

    override suspend fun doWork(): Result {
        if (!jobLock.tryLock()) {
            return Result.success()
        }
        return try {
            if (isPermissionFailed()) return Result.failure()

            syncMedia()
            processPendingOcr()
            Result.success()
        } catch (e: Exception) {
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

    companion object {
        private val jobLock = Mutex()
    }
}
