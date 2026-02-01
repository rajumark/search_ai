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
import com.photo.searchai.core.database.dao.BarcodeDao
import com.photo.searchai.core.database.entity.BarcodeEntity
import com.photo.searchai.core.ml.BarcodeProcessor
import com.photo.searchai.data.datastore.OcrProgressDataStore
import com.photo.searchai.data.datastore.ProcessingStage
import com.photo.searchai.data.datastore.ScheduledWorkDataStore
import com.photo.searchai.data.local.dao.ImageDao
import com.photo.searchai.data.local.dao.ImageLabelDao
import com.photo.searchai.data.local.dao.OcrTextDao
import com.photo.searchai.data.local.dao.WorkerHistoryDao
import com.photo.searchai.data.local.entity.ImageLabelEntity
import com.photo.searchai.data.local.entity.OcrTextEntity
import com.photo.searchai.data.local.entity.WorkerHistoryEntity
import com.photo.searchai.data.local.entity.WorkerStatus
import com.photo.searchai.datasource.PhotoDataSource
import com.photo.searchai.ocr.ImageLabelProcessor
import com.photo.searchai.ocr.OcrProcessor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Periodic WorkManager worker that runs every 6 hours to process pending images. This worker
 * processes images in the background so when the user opens the app, there are fewer images to
 * process.
 *
 * Features:
 * - Runs every 6 hours reliably
 * - Records run history for tracking
 * - Handles failures gracefully with retry logic
 * - Works offline (no internet required)
 * - Processes all pending images through OCR → Barcode → Label pipeline
 */
@HiltWorker
class PeriodicImageProcessingWorker
@AssistedInject
constructor(
        @Assisted private val context: Context,
        @Assisted workerParams: WorkerParameters,
        private val imageDao: ImageDao,
        private val ocrTextDao: OcrTextDao,
        private val barcodeDao: BarcodeDao,
        private val imageLabelDao: ImageLabelDao,
        private val workerHistoryDao: WorkerHistoryDao,
        private val photoDataSource: PhotoDataSource,
        private val ocrProcessor: OcrProcessor,
        private val barcodeProcessor: BarcodeProcessor,
        private val imageLabelProcessor: ImageLabelProcessor,
        private val progressDataStore: OcrProgressDataStore,
        private val scheduledWorkDataStore: ScheduledWorkDataStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "periodic_image_processing_worker"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "periodic_processing_channel"
        private const val BATCH_SIZE = 25
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var historyId: Long = 0
    private var ocrProcessed = 0
    private var barcodeProcessed = 0
    private var labelProcessed = 0

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo("Starting background processing...", 0)
    }

    override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                try {
                    createNotificationChannel()
                    setForeground(getForegroundInfo())

                    // Record work started
                    scheduledWorkDataStore.recordRunStarted()

                    // Create history entry
                    val startTime = System.currentTimeMillis()
                    historyId =
                            workerHistoryDao.insert(
                                    WorkerHistoryEntity(
                                            startTime = startTime,
                                            status = WorkerStatus.RUNNING,
                                            isScheduledRun = true
                                    )
                            )

                    // Check if there's any pending work
                    val ocrPending = imageDao.getPendingCountFlow().first()
                    val barcodePending = imageDao.getBarcodePendingCountFlow().first()
                    val labelPending = imageDao.getLabelPendingCountFlow().first()

                    if (ocrPending == 0 && barcodePending == 0 && labelPending == 0) {
                        // No pending work, mark as complete
                        completeWork(
                                startTime,
                                success = true,
                                message = "No pending images to process"
                        )
                        return@withContext Result.success()
                    }

                    // Process all stages
                    var hasMoreWork = true
                    while (hasMoreWork) {
                        // STAGE 1: OCR Processing
                        processOcrStage()

                        // STAGE 2: Barcode Processing
                        processBarcodeStage()

                        // STAGE 3: Image Labeling
                        processLabelStage()

                        // Check if any new images were added during processing
                        val ocrPendingAfter = imageDao.getPendingCountFlow().first()
                        val barcodePendingAfter = imageDao.getBarcodePendingCountFlow().first()
                        val labelPendingAfter = imageDao.getLabelPendingCountFlow().first()

                        hasMoreWork =
                                ocrPendingAfter > 0 ||
                                        barcodePendingAfter > 0 ||
                                        labelPendingAfter > 0
                    }

                    // Mark completion
                    progressDataStore.updateCurrentStage(ProcessingStage.COMPLETE)
                    completeWork(startTime, success = true, message = null)
                    showCompletionNotification()

                    Result.success()
                } catch (e: Exception) {
                    val startTime =
                            workerHistoryDao.getById(historyId)?.startTime
                                    ?: System.currentTimeMillis()
                    completeWork(startTime, success = false, message = e.message ?: "Unknown error")

                    // Check consecutive failures
                    val state = scheduledWorkDataStore.getState()
                    if (state.consecutiveFailures >= ScheduledWorkDataStore.MAX_CONSECUTIVE_FAILURES
                    ) {
                        // Too many failures, don't retry endlessly
                        return@withContext Result.failure()
                    }

                    Result.retry()
                }
            }

    private suspend fun completeWork(startTime: Long, success: Boolean, message: String?) {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Update history entry
        workerHistoryDao.updateStatus(
                id = historyId,
                status = if (success) WorkerStatus.COMPLETED else WorkerStatus.FAILED,
                endTime = endTime,
                durationMs = duration,
                errorMessage = message
        )
        workerHistoryDao.updateProcessedCounts(
                id = historyId,
                ocr = ocrProcessed,
                barcode = barcodeProcessed,
                label = labelProcessed
        )

        // Record in DataStore
        scheduledWorkDataStore.recordRunCompleted(success, message)
    }

    // ============================================================================
    // STAGE 1: OCR Processing
    // ============================================================================
    private suspend fun processOcrStage() {
        progressDataStore.updateCurrentStage(ProcessingStage.OCR)

        while (true) {
            val unparsedIds = imageDao.getUnparsedImageIds(BATCH_SIZE)
            if (unparsedIds.isEmpty()) break

            val total = imageDao.getTotalCountFlow().first()
            var parsed = imageDao.getParsedCountFlow().first()

            for (imageId in unparsedIds) {
                val success = processOcrForImage(imageId)
                if (success) {
                    parsed++
                    ocrProcessed++
                }

                val pending = total - parsed
                progressDataStore.updateOcrProgress(total, parsed, pending)
                updateNotification("Text Recognition", parsed, total, "1/3")
            }
        }
    }

    private suspend fun processOcrForImage(mediaStoreId: Long): Boolean {
        try {
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
                val ocrResult = ocrProcessor.processImage(bitmap)
                val ocrTextEntity =
                        OcrTextEntity(
                                mediaStoreId = mediaStoreId,
                                fullText = ocrResult.fullText,
                                indexedTokens = ocrResult.indexedTokens
                        )
                ocrTextDao.insertOcrText(ocrTextEntity)
                imageDao.markAsParsed(mediaStoreId)
                return true
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            return false
        }
    }

    // ============================================================================
    // STAGE 2: Barcode Processing
    // ============================================================================
    private suspend fun processBarcodeStage() {
        progressDataStore.updateCurrentStage(ProcessingStage.BARCODE)

        while (true) {
            val unparsedIds = imageDao.getUnparsedBarcodeImageIds(BATCH_SIZE)
            if (unparsedIds.isEmpty()) break

            val total = imageDao.getTotalCountFlow().first()
            var parsed = imageDao.getBarcodeParsedCountFlow().first()

            for (imageId in unparsedIds) {
                val success = processBarcodeForImage(imageId)
                if (success) {
                    parsed++
                    barcodeProcessed++
                }

                val pending = total - parsed
                progressDataStore.updateBarcodeProgress(total, parsed, pending)
                updateNotification("Barcode Scanning", parsed, total, "2/3")
            }
        }
    }

    private suspend fun processBarcodeForImage(mediaStoreId: Long): Boolean {
        try {
            val path =
                    photoDataSource.getImagePath(mediaStoreId)
                            ?: run {
                                imageDao.markAsBarcodeParsed(mediaStoreId)
                                return false
                            }

            val bitmap =
                    photoDataSource.getScaledBitmap(path)
                            ?: run {
                                imageDao.markAsBarcodeParsed(mediaStoreId)
                                return false
                            }

            try {
                val barcodeResults = barcodeProcessor.processImage(bitmap)

                if (barcodeResults.isNotEmpty()) {
                    val entities =
                            barcodeResults.map { result ->
                                BarcodeEntity(
                                        mediaStoreId = mediaStoreId,
                                        format = result.format,
                                        formatName = result.formatName,
                                        rawValue = result.rawValue,
                                        displayValue = result.displayValue,
                                        valueType = result.valueType
                                )
                            }
                    barcodeDao.insertBarcodes(entities)
                }

                imageDao.markAsBarcodeParsed(mediaStoreId)
                return true
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            return false
        }
    }

    // ============================================================================
    // STAGE 3: Image Labeling
    // ============================================================================
    private suspend fun processLabelStage() {
        progressDataStore.updateCurrentStage(ProcessingStage.LABELING)

        while (true) {
            val unparsedIds = imageDao.getUnparsedLabelImageIds(BATCH_SIZE)
            if (unparsedIds.isEmpty()) break

            val total = imageDao.getTotalCountFlow().first()
            var parsed = imageDao.getLabelParsedCountFlow().first()

            for (imageId in unparsedIds) {
                val success = processLabelForImage(imageId)
                if (success) {
                    parsed++
                    labelProcessed++
                }

                val pending = total - parsed
                progressDataStore.updateLabelProgress(total, parsed, pending)
                updateNotification("Image Labeling", parsed, total, "3/3")
            }
        }
    }

    private suspend fun processLabelForImage(mediaStoreId: Long): Boolean {
        try {
            val path =
                    photoDataSource.getImagePath(mediaStoreId)
                            ?: run {
                                imageDao.markAsLabelParsed(mediaStoreId)
                                return false
                            }

            val bitmap =
                    photoDataSource.getScaledBitmap(path)
                            ?: run {
                                imageDao.markAsLabelParsed(mediaStoreId)
                                return false
                            }

            try {
                val labelResults = imageLabelProcessor.processImage(bitmap)

                if (labelResults.isNotEmpty()) {
                    val entities =
                            labelResults.map { result ->
                                ImageLabelEntity(
                                        mediaStoreId = mediaStoreId,
                                        label = result.label,
                                        confidence = result.confidence,
                                        index = result.index
                                )
                            }
                    imageLabelDao.insertLabels(entities)
                }

                imageDao.markAsLabelParsed(mediaStoreId)
                return true
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            return false
        }
    }

    // ============================================================================
    // Notification Helpers
    // ============================================================================
    private fun updateNotification(
            stageName: String,
            parsed: Int,
            total: Int,
            stageNumber: String
    ) {
        val progress = if (total > 0) (parsed * 100 / total) else 0
        val title = "Background Processing ($stageNumber)"
        val content = "$stageName: $parsed of $total ($progress%)"

        val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setProgress(total, parsed, false)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                        .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification() {
        val totalProcessed = ocrProcessed + barcodeProcessed + labelProcessed
        val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle("Background processing complete!")
                        .setContentText("Processed $totalProcessed operations in background")
                        .setSmallIcon(android.R.drawable.ic_menu_gallery)
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createForegroundInfo(status: String, progress: Int): ForegroundInfo {
        createNotificationChannel()

        val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle("Background Photo Processing")
                        .setContentText(status)
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setProgress(100, progress, progress == 0)
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
                                    "Background Processing",
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply {
                                description = "Background photo processing every 6 hours"
                                setShowBadge(false)
                            }

            notificationManager.createNotificationChannel(channel)
        }
    }
}
