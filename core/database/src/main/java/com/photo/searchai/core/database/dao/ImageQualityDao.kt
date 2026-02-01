package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.core.database.entity.ImageQualityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageQualityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuality(quality: ImageQualityEntity)

    @Query("SELECT * FROM image_quality WHERE mediaStoreId = :mediaStoreId")
    suspend fun getQualityForImage(mediaStoreId: Long): ImageQualityEntity?

    @Query("SELECT * FROM image_quality WHERE mediaStoreId = :mediaStoreId")
    fun getQualityFlow(mediaStoreId: Long): Flow<ImageQualityEntity?>

    @Query("SELECT COUNT(*) FROM image_quality")
    fun getAnalyzedCountFlow(): Flow<Int>

    // Example quality queries
    @Query("SELECT * FROM image_quality WHERE blurScore < 100.0")
    fun getBlurredImages(): Flow<List<ImageQualityEntity>>

    @Query("SELECT * FROM image_quality WHERE brightnessScore < 50.0")
    fun getDarkImages(): Flow<List<ImageQualityEntity>>
}
