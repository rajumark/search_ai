package com.photo.searchai.core.rules_engine

import com.photo.searchai.core.database.entity.ExifEntity
import com.photo.searchai.core.database.entity.SmartAlbumRuleEntity
import com.photo.searchai.core.media_index.model.MediaItem

/**
 * Interface for evaluating rules against photo metadata.
 */
interface RulesEngine {
    /**
     * Evaluates a rule against a media item and its EXIF metadata.
     */
    fun evaluate(rule: SmartAlbumRuleEntity, media: MediaItem, exif: ExifEntity?): Boolean
    
    /**
     * Determines if a photo is a screenshot based on technical rules.
     */
    fun isScreenshot(media: MediaItem, exif: ExifEntity?): Boolean
    
    /**
     * Determines if a photo is "large" based on a threshold.
     */
    fun isLargeFile(media: MediaItem, thresholdBytes: Long): Boolean
}
