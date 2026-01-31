package com.photo.searchai.repository

import com.photo.searchai.data.datastore.OcrProgress
import com.photo.searchai.data.datastore.OcrProgressDataStore
import com.photo.searchai.data.local.dao.ImageDao
import com.photo.searchai.data.local.dao.OcrTextDao
import com.photo.searchai.data.local.entity.ImageEntity
import com.photo.searchai.data.local.entity.OcrTextEntity
import com.photo.searchai.datasource.PhotoDataSource
import com.photo.searchai.worker.WorkManagerHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for OCR operations.
 * Handles syncing MediaStore images to Room and triggering OCR work.
 */
interface OcrRepository {
    /**
     * Sync images from MediaStore to Room database.
     * Adds new images and removes deleted ones.
     */
    suspend fun syncImagesFromMediaStore()
    
    /**
     * Get parsed count as Flow.
     */
    fun getParsedCountFlow(): Flow<Int>
    
    /**
     * Get total count as Flow.
     */
    fun getTotalCountFlow(): Flow<Int>
    
    /**
     * Get pending count as Flow.
     */
    fun getPendingCountFlow(): Flow<Int>
    
    /**
     * Get progress from DataStore as Flow.
     */
    fun getProgressFlow(): Flow<OcrProgress>
    
    /**
     * Enqueue OCR work if there are pending images.
     */
    suspend fun enqueueOcrWorkIfNeeded()
    
    /**
     * Check if work is running.
     */
    fun isWorkRunning(): Flow<Boolean>
    
    /**
     * Get OCR text for an image.
     */
    suspend fun getOcrText(mediaStoreId: Long): OcrTextEntity?
    
    /**
     * Search OCR text.
     */
    suspend fun searchOcrText(query: String): List<OcrTextEntity>
}

@Singleton
class OcrRepositoryImpl @Inject constructor(
    private val imageDao: ImageDao,
    private val ocrTextDao: OcrTextDao,
    private val photoDataSource: PhotoDataSource,
    private val workManagerHelper: WorkManagerHelper,
    private val progressDataStore: OcrProgressDataStore
) : OcrRepository {
    
    override suspend fun syncImagesFromMediaStore() {
        // Get all images from MediaStore
        val mediaStoreImages = photoDataSource.getAllImageMetadata()
        val mediaStoreIds = mediaStoreImages.map { it.mediaStoreId }.toSet()
        
        // Get existing image IDs from Room
        val existingIds = imageDao.getAllImageIds().toSet()
        
        // Find new images to add
        val newImages = mediaStoreImages.filter { it.mediaStoreId !in existingIds }
        
        // Insert new images
        if (newImages.isNotEmpty()) {
            val entities = newImages.map { metadata ->
                ImageEntity(
                    mediaStoreId = metadata.mediaStoreId,
                    path = metadata.path,
                    dateAdded = metadata.dateAdded,
                    parsed = false
                )
            }
            imageDao.insertImages(entities)
        }
        
        // Remove stale images (deleted from MediaStore)
        if (existingIds.isNotEmpty()) {
            val validIds = mediaStoreIds.toList()
            imageDao.deleteStaleImages(validIds)
        }
        
        // Update progress in DataStore
        val total = imageDao.getTotalCountFlow().first()
        val parsed = imageDao.getParsedCountFlow().first()
        val pending = total - parsed
        progressDataStore.updateProgress(total, parsed, pending)
    }
    
    override fun getParsedCountFlow(): Flow<Int> = imageDao.getParsedCountFlow()
    
    override fun getTotalCountFlow(): Flow<Int> = imageDao.getTotalCountFlow()
    
    override fun getPendingCountFlow(): Flow<Int> = imageDao.getPendingCountFlow()
    
    override fun getProgressFlow(): Flow<OcrProgress> = progressDataStore.progressFlow
    
    override suspend fun enqueueOcrWorkIfNeeded() {
        val pending = imageDao.getPendingCountFlow().first()
        if (pending > 0) {
            workManagerHelper.enqueueOcrWork()
        }
    }
    
    override fun isWorkRunning(): Flow<Boolean> = workManagerHelper.isWorkRunning()
    
    override suspend fun getOcrText(mediaStoreId: Long): OcrTextEntity? {
        return ocrTextDao.getOcrTextByImageId(mediaStoreId)
    }
    
    override suspend fun searchOcrText(query: String): List<OcrTextEntity> {
        return ocrTextDao.searchOcrText(query)
    }
}
