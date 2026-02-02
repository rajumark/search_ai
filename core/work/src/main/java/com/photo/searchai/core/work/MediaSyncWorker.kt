package com.photo.searchai.core.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
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
            // Initial foreground notification
            if (PermissionChecker.hasPermission(applicationContext, PermissionType.NOTIFICATION)) {
                setForeground(createForegroundInfo(0, 0))
            }

            syncMedia()
            processPendingOcr()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0, 0)
    }

    private fun createForegroundInfo(progress: Int, total: Int): ForegroundInfo {
        val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                        NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                            CHANNEL_ID,
                            "Media Sync",
                            NotificationManager.IMPORTANCE_LOW
                    )
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Processing Images"
        val content =
                if (total > 0) "Processing image $progress of $total" else "Starting process..."

        val notification =
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(android.R.drawable.stat_notify_sync)
                        .setOngoing(true)
                        .setProgress(total, progress, total == 0)
                        .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
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
                setForeground(createForegroundInfo(index + 1, total))
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
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "media_sync_channel"
    }
}
