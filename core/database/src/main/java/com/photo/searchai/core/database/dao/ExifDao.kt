package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.core.database.entity.ExifEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExifDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExif(exif: ExifEntity)

    /** Get EXIF for a specific image. */
    @Query("SELECT * FROM exif_metadata WHERE mediaStoreId = :imageId")
    suspend fun getExifForImage(imageId: Long): ExifEntity?

    @Query("SELECT * FROM exif_metadata WHERE mediaStoreId = :mediaStoreId")
    fun getExifForImageFlow(mediaStoreId: Long): Flow<ExifEntity?>

    /** Get all EXIF records. */
    @Query("SELECT * FROM exif_metadata")
    suspend fun getAllExif(): List<ExifEntity>

    @Query("SELECT * FROM exif_metadata")
    fun getAllExif(): Flow<List<ExifEntity>>
    
    @Query("SELECT COUNT(*) FROM exif_metadata")
    suspend fun getExifCount(): Int
}
