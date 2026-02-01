package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.core.database.entity.SmartAlbumRuleEntity
import kotlinx.coroutines.flow.Flow

/** DAO for smart album rule database operations. */
@Dao
interface SmartAlbumDao {
    /** Insert a smart album rule, replacing if exists. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: SmartAlbumRuleEntity): Long

    /** Get all rules as a flow. */
    @Query("SELECT * FROM smart_album_rules")
    fun getAllRulesFlow(): Flow<List<SmartAlbumRuleEntity>>

    /** Get all rules once. */
    @Query("SELECT * FROM smart_album_rules") suspend fun getAllRules(): List<SmartAlbumRuleEntity>

    /** Get active rules as a flow. */
    @Query("SELECT * FROM smart_album_rules WHERE isEnabled = 1")
    fun getActiveRulesFlow(): Flow<List<SmartAlbumRuleEntity>>

    /** Get active rules once. */
    @Query("SELECT * FROM smart_album_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<SmartAlbumRuleEntity>

    /** Delete a rule by ID. */
    @Query("DELETE FROM smart_album_rules WHERE id = :ruleId") suspend fun deleteRule(ruleId: Long)
}
