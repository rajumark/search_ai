package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.database.entity.ImageLabelEntity
import com.photo.searchai.core.database.entity.LabelCount
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageLabelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<ImageLabelEntity>)

    @Query("SELECT COUNT(DISTINCT imageId) FROM image_labels") fun getLabeledImageCount(): Flow<Int>

    @Query(
            """
        SELECT labelText AS label, COUNT(DISTINCT imageId) AS count
        FROM image_labels
        WHERE labelText != :noLabelText
        GROUP BY labelText
        ORDER BY count DESC
    """
    )
    fun getLabelCounts(noLabelText: String = "NO_LABEL"): Flow<List<LabelCount>>

    @Query(
            """
        SELECT images.* FROM images
        INNER JOIN image_labels ON images.id = image_labels.imageId
        WHERE image_labels.labelText = :labelText
        ORDER BY images.dateAdded DESC
        LIMIT 3
    """
    )
    suspend fun getLabelPreviewImages(labelText: String): List<ImageEntity>

    @Query(
            """
        SELECT images.* FROM images
        INNER JOIN image_labels ON images.id = image_labels.imageId
        WHERE image_labels.labelText IN (:labels)
        GROUP BY images.id
        HAVING COUNT(DISTINCT image_labels.labelText) = :labelCount
        ORDER BY images.dateAdded DESC
    """
    )
    suspend fun getImagesForLabels(labels: List<String>, labelCount: Int): List<ImageEntity>

    @Query(
            """
        SELECT labelText AS label, COUNT(DISTINCT imageId) AS count
        FROM image_labels
        WHERE imageId IN (
            SELECT imageId FROM image_labels WHERE labelText IN (:labels)
        )
        AND labelText NOT IN (:labels)
        GROUP BY labelText
        ORDER BY count DESC
        LIMIT :limit
    """
    )
    suspend fun getRelatedLabels(labels: List<String>, limit: Int = 20): List<LabelCount>

    // Find images that are not in the image_labels table
    // Limiting to a batch size to avoid processing everything at once
    @Query(
            """
        SELECT * FROM images 
        WHERE id NOT IN (SELECT DISTINCT imageId FROM image_labels)
        LIMIT :limit
    """
    )
    suspend fun getPendingLabelingImages(
            limit: Int = 50
    ): List<com.photo.searchai.core.database.entity.ImageEntity>
}
