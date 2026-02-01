package com.photo.searchai.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.core.database.entity.FaceEntity
import kotlinx.coroutines.flow.Flow

/** DAO for face-related database operations. */
@Dao
interface FaceDao {

    /** Insert faces, replacing on conflict. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaces(faces: List<FaceEntity>): List<Long>

    /** Insert a single face. */
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFace(face: FaceEntity): Long

    /** Get all faces for a specific image. */
    @Query("SELECT * FROM faces WHERE mediaStoreId = :mediaStoreId ORDER BY faceIndex")
    suspend fun getFacesForImage(mediaStoreId: Long): List<FaceEntity>

    /** Get all faces for a specific image as Flow. */
    @Query("SELECT * FROM faces WHERE mediaStoreId = :mediaStoreId ORDER BY faceIndex")
    fun getFacesForImageFlow(mediaStoreId: Long): Flow<List<FaceEntity>>

    /** Get a specific face by ID. */
    @Query("SELECT * FROM faces WHERE id = :faceId")
    suspend fun getFaceById(faceId: Long): FaceEntity?

    /** Get count of faces for a specific image. */
    @Query("SELECT COUNT(*) FROM faces WHERE mediaStoreId = :mediaStoreId")
    suspend fun getFaceCountForImage(mediaStoreId: Long): Int

    /** Get total face count across all images. */
    @Query("SELECT COUNT(*) FROM faces") fun getTotalFaceCountFlow(): Flow<Int>

    /** Get total face count (suspend). */
    @Query("SELECT COUNT(*) FROM faces") suspend fun getTotalFaceCount(): Int

    /** Get count of images that have at least one face. */
    @Query("SELECT COUNT(DISTINCT mediaStoreId) FROM faces")
    fun getImagesWithFacesCountFlow(): Flow<Int>

    /** Get count of images that have at least one face (suspend). */
    @Query("SELECT COUNT(DISTINCT mediaStoreId) FROM faces")
    suspend fun getImagesWithFacesCount(): Int

    /** Delete all faces for a specific image. */
    @Query("DELETE FROM faces WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteFacesForImage(mediaStoreId: Long): Int

    /** Delete all faces by image IDs. */
    @Query("DELETE FROM faces WHERE mediaStoreId IN (:mediaStoreIds)")
    suspend fun deleteFacesByImageIds(mediaStoreIds: List<Long>): Int

    /** Delete a specific face by ID. */
    @Query("DELETE FROM faces WHERE id = :faceId") suspend fun deleteFaceById(faceId: Long): Int

    /** Get all unique image IDs that have faces (for displaying images with faces). */
    @Query("SELECT DISTINCT mediaStoreId FROM faces ORDER BY detectedAt DESC")
    suspend fun getImageIdsWithFaces(): List<Long>

    /** Paging source for all faces ordered by detection time. */
    @Query("SELECT * FROM faces ORDER BY detectedAt DESC")
    fun getAllFacesPaging(): PagingSource<Int, FaceEntity>

    /**
     * Paging source for distinct images that have faces. Returns one face per image, useful for
     * showing images with faces.
     */
    @Query(
            """
        SELECT f.* FROM faces f
        INNER JOIN (
            SELECT mediaStoreId, MIN(id) as minId
            FROM faces
            GROUP BY mediaStoreId
        ) grouped ON f.id = grouped.minId
        ORDER BY f.detectedAt DESC
    """
    )
    fun getDistinctImageFacesPaging(): PagingSource<Int, FaceEntity>

    /** Get all faces as a paging source for grid display. */
    @Query("SELECT * FROM faces ORDER BY mediaStoreId, faceIndex")
    fun getAllFacesForGridPaging(): PagingSource<Int, FaceEntity>
}
