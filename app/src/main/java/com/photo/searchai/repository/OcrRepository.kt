package com.photo.searchai.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.photo.searchai.data.datastore.OcrProgress
import com.photo.searchai.data.datastore.OcrProgressDataStore
import com.photo.searchai.data.local.dao.ImageDao
import com.photo.searchai.data.local.dao.OcrTextDao
import com.photo.searchai.data.local.entity.ImageEntity
import com.photo.searchai.data.local.entity.OcrTextEntity
import com.photo.searchai.datasource.PhotoDataSource
import com.photo.searchai.worker.WorkManagerHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for OCR and image processing operations.
 * Handles syncing MediaStore images to Room and triggering processing work.
 */
interface OcrRepository {
    /**
     * Sync images from MediaStore to Room database.
     * Adds new images and removes deleted ones.
     */
    suspend fun syncImagesFromMediaStore()
    
    // OCR Progress Flows
    fun getParsedCountFlow(): Flow<Int>
    fun getTotalCountFlow(): Flow<Int>
    fun getPendingCountFlow(): Flow<Int>
    
    // Barcode Progress Flows
    fun getBarcodeParsedCountFlow(): Flow<Int>
    fun getBarcodePendingCountFlow(): Flow<Int>
    
    // Label Progress Flows
    fun getLabelParsedCountFlow(): Flow<Int>
    fun getLabelPendingCountFlow(): Flow<Int>
    
    /**
     * Get full progress from DataStore as Flow.
     */
    fun getProgressFlow(): Flow<OcrProgress>
    
    /**
     * Enqueue processing work if there are pending images.
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
    
    /**
     * Search OCR text with paging support.
     */
    fun searchOcrTextPaging(query: String): Flow<PagingData<OcrTextEntity>>
    
    /**
     * Get all OCR text with paging (for empty query).
     */
    fun getAllOcrTextPaging(): Flow<PagingData<OcrTextEntity>>
    
    /**
     * Get image entity by mediaStoreId.
     */
    suspend fun getImageById(id: Long): ImageEntity?
    
    /**
     * Get images by list of mediaStoreIds.
     */
    suspend fun getImagesByIds(ids: List<Long>): List<ImageEntity>
    
    /**
     * Delete images by list of mediaStoreIds.
     */
    suspend fun deleteImages(ids: List<Long>)

    /**
     * Get search suggestions based on query.
     */
    suspend fun getSearchSuggestions(query: String): List<String>
}

@Singleton
class OcrRepositoryImpl @Inject constructor(
    private val imageDao: ImageDao,
    private val ocrTextDao: OcrTextDao,
    private val photoDataSource: PhotoDataSource,
    private val workManagerHelper: WorkManagerHelper,
    private val progressDataStore: OcrProgressDataStore
) : OcrRepository {
    
    companion object {
        private const val PAGE_SIZE = 18 // 3x3 grid, load 2 pages worth
    }
    
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
                    parsed = false,
                    barcodeParsed = false,
                    labelParsed = false
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
        val ocrParsed = imageDao.getParsedCountFlow().first()
        val ocrPending = total - ocrParsed
        val barcodeParsed = imageDao.getBarcodeParsedCountFlow().first()
        val barcodePending = total - barcodeParsed
        val labelParsed = imageDao.getLabelParsedCountFlow().first()
        val labelPending = total - labelParsed
        
        progressDataStore.updateAllProgress(
            total = total,
            ocrParsed = ocrParsed,
            ocrPending = ocrPending,
            barcodeParsed = barcodeParsed,
            barcodePending = barcodePending,
            labelParsed = labelParsed,
            labelPending = labelPending,
            currentStage = com.photo.searchai.data.datastore.ProcessingStage.IDLE
        )
    }
    
    // OCR Progress
    override fun getParsedCountFlow(): Flow<Int> = imageDao.getParsedCountFlow()
    override fun getTotalCountFlow(): Flow<Int> = imageDao.getTotalCountFlow()
    override fun getPendingCountFlow(): Flow<Int> = imageDao.getPendingCountFlow()
    
    // Barcode Progress
    override fun getBarcodeParsedCountFlow(): Flow<Int> = imageDao.getBarcodeParsedCountFlow()
    override fun getBarcodePendingCountFlow(): Flow<Int> = imageDao.getBarcodePendingCountFlow()
    
    // Label Progress
    override fun getLabelParsedCountFlow(): Flow<Int> = imageDao.getLabelParsedCountFlow()
    override fun getLabelPendingCountFlow(): Flow<Int> = imageDao.getLabelPendingCountFlow()
    
    override fun getProgressFlow(): Flow<OcrProgress> = progressDataStore.progressFlow
    
    override suspend fun enqueueOcrWorkIfNeeded() {
        val ocrPending = imageDao.getPendingCountFlow().first()
        val barcodePending = imageDao.getBarcodePendingCountFlow().first()
        val labelPending = imageDao.getLabelPendingCountFlow().first()
        
        if (ocrPending > 0 || barcodePending > 0 || labelPending > 0) {
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
    
    override fun searchOcrTextPaging(query: String): Flow<PagingData<OcrTextEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PAGE_SIZE / 2
            ),
            pagingSourceFactory = { ocrTextDao.searchOcrTextPaging(query) }
        ).flow
    }
    
    override fun getAllOcrTextPaging(): Flow<PagingData<OcrTextEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PAGE_SIZE / 2
            ),
            pagingSourceFactory = { ocrTextDao.getAllOcrTextPaging() }
        ).flow
    }
    
    override suspend fun getImageById(id: Long): ImageEntity? {
        return imageDao.getImageById(id)
    }
    
    override suspend fun getImagesByIds(ids: List<Long>): List<ImageEntity> {
        return imageDao.getImagesByIds(ids)
    }
    
    override suspend fun deleteImages(ids: List<Long>) {
        imageDao.deleteImagesByIds(ids)
    }

    override suspend fun getSearchSuggestions(query: String): List<String> {
        // Limit sample size for performance
        val sampleLimit = 100 
        val suggestionLimit = 10
        val ignoredWords = setOf("the", "and", "or", "in", "on", "at", "to", "for", "of", "with")

        val tokenCounts = mutableMapOf<String, Int>()
        val queryTokens = query.lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }.toSet()

        if (query.isBlank()) {
            // Global frequency
            val sample = ocrTextDao.getAllOcrTexts(sampleLimit)
            sample.forEach { entity ->
                // Use indexedTokens if available as it should be pre-processed
                val tokens = entity.indexedTokens.split(" ")
                tokens.forEach { token ->
                    val normalized = token.lowercase().trim()
                    if (normalized.length > 2 && normalized !in ignoredWords) {
                        tokenCounts[normalized] = tokenCounts.getOrDefault(normalized, 0) + 1
                    }
                }
            }
        } else {
            // Co-occurrence frequency
            val sample = ocrTextDao.searchOcrTextsLimited(query, sampleLimit)
            sample.forEach { entity ->
                val tokens = entity.indexedTokens.split(" ")
                tokens.forEach { token ->
                    val normalized = token.lowercase().trim()
                    // Don't suggest words already in the query
                    if (normalized.length > 2 && normalized !in ignoredWords && normalized !in queryTokens) {
                         tokenCounts[normalized] = tokenCounts.getOrDefault(normalized, 0) + 1
                    }
                }
            }
        }

        return tokenCounts.entries
            .sortedByDescending { it.value }
            .take(suggestionLimit)
            .map { it.key }
    }
}


