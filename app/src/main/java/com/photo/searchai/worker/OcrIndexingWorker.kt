package com.photo.searchai.worker

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
import com.photo.searchai.data.datastore.OcrProgressDataStore
import com.photo.searchai.data.local.dao.ImageDao
import com.photo.searchai.data.local.dao.OcrTextDao
import com.photo.searchai.data.local.entity.OcrTextEntity
import com.photo.searchai.datasource.PhotoDataSource
import com.photo.searchai.ocr.OcrProcessor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@HiltWorker
class OcrIndexingWorker
@AssistedInject
constructor(
        @Assisted private val context: Context,
        @Assisted workerParams: WorkerParameters,
        private val imageDao: ImageDao,
        private val ocrTextDao: OcrTextDao,
        private val photoDataSource: PhotoDataSource,
        private val ocrProcessor: OcrProcessor,
        private val progressDataStore: OcrProgressDataStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "ocr_indexing_worker"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ocr_indexing_channel"
        private const val BATCH_SIZE = 25
        private const val MAX_PARALLEL_IMAGES = 2
    }

    private val semaphore = Semaphore(MAX_PARALLEL_IMAGES)
    private val progressMutex = Mutex()

    private val recordedMilestones = mutableSetOf<Int>()

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                try {
                    createNotificationChannel()
                    setForeground(createForegroundInfo(0, 0))

                    val totalImages = imageDao.getTotalCountFlow().first()
                    val initialParsed = imageDao.getParsedCountFlow().first()
                    val parsedCounter = AtomicInteger(initialParsed)

                    val totalToParse = totalImages - initialParsed
                    if (totalToParse > 0) {
                        progressDataStore.startBenchmark()
                        recordedMilestones.clear()
                    }

                    while (true) {
                        val imageIds = imageDao.getUnparsedImageIds(BATCH_SIZE)
                        if (imageIds.isEmpty()) break

                        coroutineScope {
                            imageIds
                                    .map { imageId ->
                                        async {
                                            semaphore.withPermit {
                                                processImageWithProgress(
                                                        imageId,
                                                        totalImages,
                                                        parsedCounter,
                                                        initialParsed,
                                                        totalToParse
                                                )
                                            }
                                        }
                                    }
                                    .awaitAll()
                        }
                    }

                    val finalTotal = imageDao.getTotalCountFlow().first()
                    val finalParsed = imageDao.getParsedCountFlow().first()
                    progressDataStore.updateProgress(finalTotal, finalParsed, 0)

                    if (totalToParse > 0) {
                        progressDataStore.completeBenchmark(totalToParse)
                    }

                    showCompletionNotification(finalTotal)

                    Result.success()
                } catch (e: Exception) {
                    Result.retry()
                }
            }

    private suspend fun processImageWithProgress(
            mediaStoreId: Long,
            total: Int,
            parsedCounter: AtomicInteger,
            initialParsed: Int,
            totalToParse: Int
    ) {
        val success = processImage(mediaStoreId)
        if (success) {
            val newParsed = parsedCounter.incrementAndGet()
            progressMutex.withLock {
                val pending = total - newParsed
                progressDataStore.updateProgress(total, newParsed, pending)
                updateNotification(newParsed, total)

                if (totalToParse > 0) {
                    val processedInSession = newParsed - initialParsed
                    val percentComplete = (processedInSession * 100) / totalToParse

                    checkAndRecordMilestone(percentComplete, processedInSession)
                }
            }
        }
    }

    private suspend fun checkAndRecordMilestone(percentComplete: Int, processedCount: Int) {
        val milestones = listOf(10, 30, 50, 70, 100)
        for (milestone in milestones) {
            if (percentComplete >= milestone && !recordedMilestones.contains(milestone)) {
                recordedMilestones.add(milestone)
                progressDataStore.recordMilestone(milestone, processedCount)
            }
        }
    }

    private suspend fun processImage(mediaStoreId: Long): Boolean {
        return try {
            val path =
                    photoDataSource.getImagePath(mediaStoreId)
                            ?: run {
                                imageDao.markAsParsed(mediaStoreId)
                                return false
                            }

            val bitmap =
                    photoDataSource.getScaledBitmap(path)
                            ?: run {
                                imageDao.markAsParsed(mediaStoreId)
                                return false
                            }

            try {
                val result = ocrProcessor.processImage(bitmap)
                ocrTextDao.insertOcrText(
                        OcrTextEntity(
                                mediaStoreId = mediaStoreId,
                                fullText = result.fullText,
                                indexedTokens = result.indexedTokens
                        )
                )
                imageDao.markAsParsed(mediaStoreId)
                true
            } finally {
                bitmap.recycle()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun updateNotification(parsed: Int, total: Int) {
        val progress = if (total > 0) (parsed * 100 / total) else 0
        val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle("Indexing photos")
                        .setContentText("$parsed of $total photos ($progress%)")
                        .setSmallIcon(android.R.drawable.ic_menu_camera)
                        .setProgress(total, parsed, false)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                        .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(total: Int) {
        val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle("Photo indexing complete!")
                        .setContentText("Successfully indexed $total photos. Ready to search!")
                        .setSmallIcon(android.R.drawable.ic_menu_gallery)
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createForegroundInfo(parsed: Int, total: Int): ForegroundInfo {
        createNotificationChannel()

        val progress = if (total > 0) (parsed * 100 / total) else 0
        val content = if (total > 0) "$parsed of $total ($progress%)" else "Starting..."

        val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle("Indexing photos")
                        .setContentText(content)
                        .setSmallIcon(android.R.drawable.ic_menu_camera)
                        .setProgress(100, progress, total == 0)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                        .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                                    CHANNEL_ID,
                                    "Photo Indexing",
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply {
                                description = "Shows progress of photo OCR indexing"
                                setShowBadge(false)
                            }

            notificationManager.createNotificationChannel(channel)
        }
    }
}
