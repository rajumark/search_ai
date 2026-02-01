package com.photo.searchai.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.media_index.MediaStoreIndexer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker responsible for syncing the local database with MediaStore.
 */
@HiltWorker
class MediaSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaStoreIndexer: MediaStoreIndexer,
    private val imageDao: ImageDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val mediaItems = mediaStoreIndexer.getAllMedia()
            val existingIds = imageDao.getAllMediaStoreIds().toSet()

            val newEntities = mediaItems.filter { it.id !in existingIds }.map {
                ImageEntity(
                    mediaStoreId = it.id,
                    path = it.path,
                    dateAdded = it.dateAdded
                )
            }

            if (newEntities.isNotEmpty()) {
                imageDao.insertImages(newEntities)
            }

            // Optional: Handle deleted items by checking MediaStore vs DB
            // But for now, we'll focus on adding new items.

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
