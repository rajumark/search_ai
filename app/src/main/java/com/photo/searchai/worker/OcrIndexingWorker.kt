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
import com.photo.searchai.R
import com.photo.searchai.data.datastore.OcrProgressDataStore
import com.photo.searchai.data.local.dao.ImageDao
import com.photo.searchai.data.local.dao.OcrTextDao
import com.photo.searchai.data.local.entity.OcrTextEntity
import com.photo.searchai.datasource.PhotoDataSource
import com.photo.searchai.ocr.OcrProcessor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for OCR indexing.
 * Runs as foreground service to survive app kill.
 * Processes images in batches of 25.
 */
@HiltWorker
class OcrIndexingWorker @AssistedInject constructor(
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
    }
    
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Create notification channel first
            createNotificationChannel()
            
            // Show foreground notification
            setForeground(createForegroundInfo(0, 0))
            
            // Process until no more pending images
            var processedInSession = 0
            
            while (true) {
                // Get next batch of unparsed images
                val unparsedIds = imageDao.getUnparsedImageIds(BATCH_SIZE)
                
                if (unparsedIds.isEmpty()) {
                    // All done
                    break
                }
                
                // Get current counts for progress calculation
                val total = imageDao.getTotalCountFlow().first()
                var parsed = imageDao.getParsedCountFlow().first()
                
                // Process each image in batch
                for ((index, imageId) in unparsedIds.withIndex()) {
                    val success = processImage(imageId)
                    if (success) {
                        processedInSession++
                        parsed++
                    }
                    
                    // Update notification after EACH image for live progress
                    val pending = total - parsed
                    progressDataStore.updateProgress(total, parsed, pending)
                    updateNotification(parsed, total)
                }
            }
            
            // Final progress update
            val total = imageDao.getTotalCountFlow().first()
            val parsed = imageDao.getParsedCountFlow().first()
            progressDataStore.updateProgress(total, parsed, 0)
            
            // Show completion notification
            showCompletionNotification(total)
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private suspend fun processImage(mediaStoreId: Long): Boolean {
        try {
            // Get image path
            val path = photoDataSource.getImagePath(mediaStoreId) ?: run {
                // Image no longer exists, mark as parsed to skip
                imageDao.markAsParsed(mediaStoreId)
                return false
            }
            
            // Get scaled bitmap
            val bitmap = photoDataSource.getScaledBitmap(path) ?: run {
                // Cannot decode, mark as parsed to skip
                imageDao.markAsParsed(mediaStoreId)
                return false
            }
            
            try {
                // Run OCR
                val ocrResult = ocrProcessor.processImage(bitmap)
                
                // Store OCR text
                val ocrTextEntity = OcrTextEntity(
                    mediaStoreId = mediaStoreId,
                    fullText = ocrResult.fullText,
                    indexedTokens = ocrResult.indexedTokens
                )
                ocrTextDao.insertOcrText(ocrTextEntity)
                
                // Mark as parsed AFTER successful DB insert
                imageDao.markAsParsed(mediaStoreId)
                
                return true
            } finally {
                // Recycle bitmap to free memory
                bitmap.recycle()
            }
        } catch (e: Exception) {
            // Log error but continue with next image
            return false
        }
    }
    
    /**
     * Updates the notification with current progress.
     * This is called frequently for live progress updates.
     */
    private fun updateNotification(parsed: Int, total: Int) {
        val progress = if (total > 0) (parsed * 100 / total) else 0
        val title = "Indexing photos"
        val content = "$parsed of $total photos ($progress%)"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setProgress(total, parsed, false) // Use actual values for smoother progress
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Prevent sound/vibration on each update
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Shows a completion notification when all photos are indexed.
     */
    private fun showCompletionNotification(total: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Photo indexing complete!")
            .setContentText("Successfully indexed $total photos. Ready to search!")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setProgress(0, 0, false) // Remove progress bar
            .setOngoing(false) // Allow user to dismiss
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createForegroundInfo(parsed: Int, total: Int): ForegroundInfo {
        createNotificationChannel()
        
        val progress = if (total > 0) (parsed * 100 / total) else 0
        val title = "Indexing photos"
        val content = if (total > 0) "$parsed of $total ($progress%)" else "Starting..."
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setProgress(100, progress, total == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        
        // Android 10+ (API 29+) requires foreground service type
        // Android 14+ (API 34+, targetSDK 35) strictly enforces this
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Photo Indexing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of photo OCR indexing"
                setShowBadge(false) // Don't show badge for progress notifications
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
}
