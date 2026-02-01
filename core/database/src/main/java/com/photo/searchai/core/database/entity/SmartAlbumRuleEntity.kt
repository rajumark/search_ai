package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a rule for a smart album.
 */
@Entity(tableName = "smart_album_rules")
data class SmartAlbumRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val ruleType: String, // "SCREENSHOT", "LARGE_FILE", "DATE_RANGE", "CAMERA_MODEL", etc.
    val configurationJson: String, // JSON payload for specific rule parameters
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
