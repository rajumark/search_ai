package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.core.database.entity.SmartAlbumRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartAlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: SmartAlbumRuleEntity): Long

    @Query("SELECT * FROM smart_album_rules")
    fun getAllRules(): Flow<List<SmartAlbumRuleEntity>>

    @Query("SELECT * FROM smart_album_rules WHERE isEnabled = 1")
    fun getActiveRules(): Flow<List<SmartAlbumRuleEntity>>

    @Query("DELETE FROM smart_album_rules WHERE id = :ruleId")
    suspend fun deleteRule(ruleId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: SmartAlbumRuleEntity)

    @Query("SELECT * FROM smart_album_rules")
    suspend fun getAllRules(): List<SmartAlbumRuleEntity>

    @Query("SELECT * FROM smart_album_rules WHERE isEnabled = 1")
    fun getActiveRulesFlow(): Flow<List<SmartAlbumRuleEntity>>
}
