package com.photo.searchai.core.cleanup_engine

import com.photo.searchai.core.database.entity.CleanupCandidateEntity
import com.photo.searchai.core.media_index.model.MediaItem

/**
 * Interface for identifying and suggesting storage cleanup candidates.
 */
interface CleanupEngine {
    /**
     * Scans for cleanup candidates based on predefined and custom rules.
     */
    suspend fun scanForCleanup(): List<CleanupCandidateEntity>

    /**
     * Evaluates a single media item for potential cleanup.
     */
    suspend fun evaluateForCleanup(media: MediaItem): CleanupCandidateEntity?
}
