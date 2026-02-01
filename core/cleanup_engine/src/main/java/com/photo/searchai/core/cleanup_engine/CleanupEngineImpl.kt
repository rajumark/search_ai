package com.photo.searchai.core.cleanup_engine

import com.photo.searchai.core.database.dao.ExifDao
import com.photo.searchai.core.database.entity.CleanupCandidateEntity
import com.photo.searchai.core.media_index.MediaStoreIndexer
import com.photo.searchai.core.media_index.model.MediaItem
import com.photo.searchai.core.rules_engine.RulesEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanupEngineImpl @Inject constructor(
    private val mediaStoreIndexer: MediaStoreIndexer,
    private val rulesEngine: RulesEngine,
    private val exifDao: ExifDao
) : CleanupEngine {

    override suspend fun scanForCleanup(): List<CleanupCandidateEntity> {
        val allMedia = mediaStoreIndexer.getAllMedia()
        val candidates = mutableListOf<CleanupCandidateEntity>()

        for (media in allMedia) {
            val candidate = evaluateForCleanup(media)
            if (candidate != null) {
                candidates.add(candidate)
            }
        }

        return candidates
    }

    override suspend fun evaluateForCleanup(media: MediaItem): CleanupCandidateEntity? {
        val exif = exifDao.getExifForImage(media.id)
        
        // 1. Large files check (threshold 10MB for specific cleanup)
        if (rulesEngine.isLargeFile(media, 10 * 1024 * 1024L)) {
            return CleanupCandidateEntity(
                mediaStoreId = media.id,
                reason = "LARGE",
                reclaimableSize = media.size,
                suggestion = "DELETE"
            )
        }

        // 2. Screenshot check
        if (rulesEngine.isScreenshot(media, exif)) {
            // Check age - e.g., screenshots older than 30 days
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            if (media.dateAdded < thirtyDaysAgo / 1000) {
                return CleanupCandidateEntity(
                    mediaStoreId = media.id,
                    reason = "SCREENSHOT",
                    reclaimableSize = media.size,
                    suggestion = "DELETE"
                )
            }
        }

        // 3. Folder based checks (e.g. WhatsApp Sent)
        if (media.path.contains("WhatsApp/Media/WhatsApp Images/Sent", ignoreCase = true)) {
             return CleanupCandidateEntity(
                mediaStoreId = media.id,
                reason = "OLD_WHATSAPP",
                reclaimableSize = media.size,
                suggestion = "DELETE"
            )
        }

        return null
    }
}
