package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.photo.searchai.core.database.entity.DuplicateGroupEntity
import com.photo.searchai.core.database.entity.DuplicateMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DuplicateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: DuplicateGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: DuplicateMappingEntity)

    @Transaction
    suspend fun addImageToGroup(mediaStoreId: Long, groupHash: String, type: String, isOriginal: Boolean) {
        val group = getGroupByHash(groupHash)
        val groupId = group?.id ?: insertGroup(DuplicateGroupEntity(groupHash = groupHash, type = type))
        insertMapping(DuplicateMappingEntity(mediaStoreId = mediaStoreId, groupId = groupId, isOriginal = isOriginal))
    }

    @Query("SELECT * FROM duplicate_groups WHERE groupHash = :hash")
    suspend fun getGroupByHash(hash: String): DuplicateGroupEntity?

    @Query("SELECT * FROM duplicate_groups")
    fun getAllGroups(): Flow<List<DuplicateGroupEntity>>

    @Query("""
        SELECT images.* FROM images 
        JOIN duplicate_mappings ON images.mediaStoreId = duplicate_mappings.mediaStoreId 
        WHERE duplicate_mappings.groupId = :groupId
    """)
    fun getImagesInGroup(groupId: Long): Flow<List<com.photo.searchai.core.database.entity.ImageEntity>>

    @Query("DELETE FROM duplicate_groups")
    suspend fun clearAllDuplicates()
}
