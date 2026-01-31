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
import com.photo.searchai.data.datastore.ProcessingStage
import com.photo.searchai.data.local.dao.BarcodeDao
import com.photo.searchai.data.local.dao.ImageDao
import com.photo.searchai.data.local.dao.ImageLabelDao
import com.photo.searchai.data.local.dao.OcrTextDao
import com.photo.searchai.data.local.entity.BarcodeEntity
import com.photo.searchai.data.local.entity.ImageLabelEntity
import com.photo.searchai.data.local.entity.OcrTextEntity
import com.photo.searchai.datasource.PhotoDataSource
import com.photo.searchai.ocr.BarcodeProcessor
import com.photo.searchai.ocr.ImageLabelProcessor
import com.photo.searchai.ocr.OcrProcessor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for sequential image processing.
 * Processes images in order: OCR → Barcode → Image Labeling.
 * Each stage completes before the next begins.
 */
@HiltWorker
class ImageProcessingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val imageDao: ImageDao,
    private val ocrTextDao: OcrTextDao,
    private val barcodeDao: BarcodeDao,
    private val imageLabelDao: ImageLabelDao,
    private val photoDataSource: PhotoDataSource,
    private val ocrProcessor: OcrProcessor,
    private val barcodeProcessor: BarcodeProcessor,
    private val imageLabelProcessor: ImageLabelProcessor,
    private val progressDataStore: OcrProgressDataStore
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "image_processing_worker"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "image_processing_channel"
        private const val BATCH_SIZE = 25
    }
    
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            createNotificationChannel()
            setForeground(createForegroundInfo("Starting...", 0, 0, ProcessingStage.IDLE))
            
            // STAGE 1: OCR Processing
            processOcrStage()
            
            // STAGE 2: Barcode Processing  
            processBarcodeStage()
            
            // STAGE 3: Image Labeling
            processLabelStage()
            
            // Final completion
            progressDataStore.updateCurrentStage(ProcessingStage.COMPLETE)
            showCompletionNotification()
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
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
                if (success) parsed++
                
                val pending = total - parsed
                progressDataStore.updateOcrProgress(total, parsed, pending)
                updateNotification("Text Recognition", parsed, total, ProcessingStage.OCR)
            }
        }
    }
    
    private suspend fun processOcrForImage(mediaStoreId: Long): Boolean {
        try {
            val path = photoDataSource.getImagePath(mediaStoreId) ?: run {
                imageDao.markAsParsed(mediaStoreId)
                return false
            }
            
            val bitmap = photoDataSource.getScaledBitmap(path) ?: run {
                imageDao.markAsParsed(mediaStoreId)
                return false
            }
            
            try {
                val ocrResult = ocrProcessor.processImage(bitmap)
                val ocrTextEntity = OcrTextEntity(
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
                if (success) parsed++
                
                val pending = total - parsed
                progressDataStore.updateBarcodeProgress(total, parsed, pending)
                updateNotification("Barcode Scanning", parsed, total, ProcessingStage.BARCODE)
            }
        }
    }
    
    private suspend fun processBarcodeForImage(mediaStoreId: Long): Boolean {
        try {
            val path = photoDataSource.getImagePath(mediaStoreId) ?: run {
                imageDao.markAsBarcodeParsed(mediaStoreId)
                return false
            }
            
            val bitmap = photoDataSource.getScaledBitmap(path) ?: run {
                imageDao.markAsBarcodeParsed(mediaStoreId)
                return false
            }
            
            try {
                val barcodeResults = barcodeProcessor.processImage(bitmap)
                
                // Store each detected barcode
                if (barcodeResults.isNotEmpty()) {
                    val entities = barcodeResults.map { result ->
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
                if (success) parsed++
                
                val pending = total - parsed
                progressDataStore.updateLabelProgress(total, parsed, pending)
                updateNotification("Image Labeling", parsed, total, ProcessingStage.LABELING)
            }
        }
    }
    
    private suspend fun processLabelForImage(mediaStoreId: Long): Boolean {
        try {
            val path = photoDataSource.getImagePath(mediaStoreId) ?: run {
                imageDao.markAsLabelParsed(mediaStoreId)
                return false
            }
            
            val bitmap = photoDataSource.getScaledBitmap(path) ?: run {
                imageDao.markAsLabelParsed(mediaStoreId)
                return false
            }
            
            try {
                val labelResults = imageLabelProcessor.processImage(bitmap)
                
                // Store each detected label
                if (labelResults.isNotEmpty()) {
                    val entities = labelResults.map { result ->
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
    private fun updateNotification(stageName: String, parsed: Int, total: Int, stage: ProcessingStage) {
        val progress = if (total > 0) (parsed * 100 / total) else 0
        val stageNumber = when (stage) {
            ProcessingStage.OCR -> "1/3"
            ProcessingStage.BARCODE -> "2/3"
            ProcessingStage.LABELING -> "3/3"
            else -> ""
        }
        
        val title = "Processing photos ($stageNumber)"
        val content = "$stageName: $parsed of $total ($progress%)"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setProgress(total, parsed, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showCompletionNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Photo processing complete!")
            .setContentText("All photos indexed. Text, barcodes, and labels ready to search!")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createForegroundInfo(
        status: String,
        parsed: Int,
        total: Int,
        stage: ProcessingStage
    ): ForegroundInfo {
        createNotificationChannel()
        
        val stageNumber = when (stage) {
            ProcessingStage.OCR -> "1/3"
            ProcessingStage.BARCODE -> "2/3"
            ProcessingStage.LABELING -> "3/3"
            else -> ""
        }
        
        val progress = if (total > 0) (parsed * 100 / total) else 0
        val title = if (stageNumber.isNotEmpty()) "Processing photos ($stageNumber)" else "Processing photos"
        val content = if (total > 0) "$status: $parsed of $total ($progress%)" else status
        
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
                "Photo Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of photo indexing (OCR, barcodes, labels)"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
}
