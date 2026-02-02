package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.core.database.entity.ImageLabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageLabelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<ImageLabelEntity>)

    @Query("SELECT COUNT(DISTINCT imageId) FROM image_labels") fun getLabeledImageCount(): Flow<Int>

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
