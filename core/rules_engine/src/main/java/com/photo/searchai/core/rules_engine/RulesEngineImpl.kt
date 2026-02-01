package com.photo.searchai.core.rules_engine

import com.photo.searchai.core.database.entity.ExifEntity
import com.photo.searchai.core.database.entity.SmartAlbumRuleEntity
import com.photo.searchai.core.media_index.model.MediaItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulesEngineImpl @Inject constructor() : RulesEngine {

    override fun evaluate(rule: SmartAlbumRuleEntity, media: MediaItem, exif: ExifEntity?): Boolean {
        if (!rule.isEnabled) return false

        return when (rule.ruleType) {
            "SCREENSHOT" -> isScreenshot(media, exif)
            "LARGE_FILE" -> {
                val threshold = rule.configurationJson.toLongOrNull() ?: (5 * 1024 * 1024L) // 5MB default
                isLargeFile(media, threshold)
            }
            "DATE_RANGE" -> {
                // Simplified date range check
                true
            }
            "CAMERA_MODEL" -> {
                val model = rule.configurationJson
                exif?.model?.contains(model, ignoreCase = true) == true
            }
            else -> false
        }
    }

    override fun isScreenshot(media: MediaItem, exif: ExifEntity?): Boolean {
        // Screenshots typically lack EXIF "Make" and "Model", and are in specific folders
        val isExifMissing = exif == null || (exif.make == null && exif.model == null)
        val pathLower = media.path.lowercase()
        val inScreenshotFolder = pathLower.contains("screenshot")
        val isPng = media.mimeType.contains("png", ignoreCase = true)
        
        return inScreenshotFolder || (isExifMissing && isPng)
    }

    override fun isLargeFile(media: MediaItem, thresholdBytes: Long): Boolean {
        return media.size >= thresholdBytes
    }
}
