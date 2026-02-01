package com.photo.searchai.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photo.searchai.core.cleanup_engine.CleanupEngine
import com.photo.searchai.core.database.dao.CleanupDao
import com.photo.searchai.core.database.dao.DuplicateDao
import com.photo.searchai.core.database.dao.ExifDao
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.duplicate_engine.DuplicateEngine
import com.photo.searchai.core.media_index.MediaStoreIndexer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker responsible for periodic organization tasks: duplicate detection and cleanup scanning.
 */
@HiltWorker
class OrganizationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaStoreIndexer: MediaStoreIndexer,
    private val duplicateEngine: DuplicateEngine,
    private val cleanupEngine: CleanupEngine,
    private val duplicateDao: DuplicateDao,
    private val cleanupDao: CleanupDao,
    private val imageDao: ImageDao,
    private val exifDao: ExifDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Duplicate Detection
            processDuplicates()

            // 2. Cleanup Scanning
            processCleanup()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun processDuplicates() {
        val allMedia = mediaStoreIndexer.getAllMedia()
        val processedHashes = mutableMapOf<String, MutableList<Long>>()

        for (media in allMedia) {
            val hash = duplicateEngine.computeHash(media.path)
            if (hash.isNotEmpty()) {
                processedHashes.getOrPut(hash) { mutableListOf() }.add(media.id)
            }
        }

        duplicateDao.clearAllDuplicates()
        for ((hash, ids) in processedHashes) {
            if (ids.size > 1) {
                // We found a group of exact duplicates
                val mediaList = ids.mapNotNull { id -> allMedia.find { it.id == id } }
                if (mediaList.size > 1) {
                    val (original, duplicates) = duplicateEngine.rankDuplicates(mediaList)
                    
                    val groupId = duplicateDao.insertGroup(
                        com.photo.searchai.core.database.entity.DuplicateGroupEntity(
                            groupHash = hash,
                            type = "EXACT"
                        )
                    )
                    
                    duplicateDao.insertMapping(
                        com.photo.searchai.core.database.entity.DuplicateMappingEntity(
                            mediaStoreId = original.id,
                            groupId = groupId,
                            isOriginal = true,
                            score = 1.0f
                        )
                    )
                    
                    for (dup in duplicates) {
                        duplicateDao.insertMapping(
                            com.photo.searchai.core.database.entity.DuplicateMappingEntity(
                                mediaStoreId = dup.id,
                                groupId = groupId,
                                isOriginal = false,
                                score = 0.5f
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun processCleanup() {
        cleanupDao.clearAllCandidates()
        val candidates = cleanupEngine.scanForCleanup()
        for (candidate in candidates) {
            cleanupDao.insertCandidate(candidate)
        }
    }
}
