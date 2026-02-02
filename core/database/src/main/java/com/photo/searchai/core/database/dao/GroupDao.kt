package com.photo.searchai.core.database.dao

import androidx.room.*
import com.photo.searchai.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM image_groups ORDER BY imageCount DESC, lastUpdated DESC LIMIT :limit")
    fun getTopGroups(limit: Int): Flow<List<GroupEntity>>

    @Query(
            """
        SELECT images.* FROM images 
        INNER JOIN group_images ON images.id = group_images.imageId 
        WHERE group_images.groupId = :groupId 
        ORDER BY images.dateAdded DESC
        LIMIT 4
    """
    )
    suspend fun getGroupPreviewImages(groupId: Long): List<ImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeywords(keywords: List<KeywordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupImages(groupImages: List<GroupImageEntity>)

    @Transaction @Query("DELETE FROM image_groups") suspend fun clearGroups()

    @Query("SELECT * FROM image_keywords") suspend fun getAllKeywords(): List<KeywordEntity>

    @Query(
            "SELECT word, COUNT(imageId) as count FROM image_keywords GROUP BY word HAVING count >= 5"
    )
    suspend fun getCommonKeywords(): List<KeywordCount>

    @Query("SELECT imageId FROM image_keywords WHERE word = :word")
    suspend fun getImageIdsForKeyword(word: String): List<Long>
}

data class KeywordCount(val word: String, val count: Int)
