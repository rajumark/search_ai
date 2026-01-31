package com.photo.searchai.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.data.local.entity.ImageLabelEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for image label database operations.
 */
@Dao
interface ImageLabelDao {
    
    /**
     * Insert image labels, replacing if exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<ImageLabelEntity>): List<Long>
    
    /**
     * Insert single label.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: ImageLabelEntity): Long
    
    /**
     * Get all labels for an image.
     */
    @Query("SELECT * FROM image_labels WHERE mediaStoreId = :mediaStoreId ORDER BY confidence DESC")
    suspend fun getLabelsForImage(mediaStoreId: Long): List<ImageLabelEntity>
    
    /**
     * Search images by label text.
     */
    @Query("SELECT * FROM image_labels WHERE label LIKE '%' || :query || '%' ORDER BY confidence DESC")
    suspend fun searchLabels(query: String): List<ImageLabelEntity>
    
    /**
     * Search labels with paging.
     */
    @Query("SELECT * FROM image_labels WHERE label LIKE '%' || :query || '%' ORDER BY confidence DESC, mediaStoreId DESC")
    fun searchLabelsPaging(query: String): PagingSource<Int, ImageLabelEntity>
    
    /**
     * Get all unique image IDs that have labels.
     */
    @Query("SELECT DISTINCT mediaStoreId FROM image_labels")
    suspend fun getImageIdsWithLabels(): List<Long>
    
    /**
     * Get distinct labels with their count (for tag cloud/filtering).
     */
    @Query("SELECT label, COUNT(*) as count FROM image_labels GROUP BY label ORDER BY count DESC LIMIT :limit")
    suspend fun getTopLabels(limit: Int = 50): List<LabelCount>
    
    /**
     * Get all labels with paging.
     */
    @Query("SELECT * FROM image_labels ORDER BY confidence DESC, mediaStoreId DESC")
    fun getAllLabelsPaging(): PagingSource<Int, ImageLabelEntity>
    
    /**
     * Get count of labels.
     */
    @Query("SELECT COUNT(*) FROM image_labels")
    fun getLabelCountFlow(): Flow<Int>
    
    /**
     * Delete labels for an image.
     */
    @Query("DELETE FROM image_labels WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteLabelsForImage(mediaStoreId: Long): Int
    
    /**
     * Get count of images with labels.
     */
    @Query("SELECT COUNT(DISTINCT mediaStoreId) FROM image_labels")
    fun getImagesWithLabelsCountFlow(): Flow<Int>
}

/**
 * Helper class for label count query.
 */
data class LabelCount(
    val label: String,
    val count: Int
)
