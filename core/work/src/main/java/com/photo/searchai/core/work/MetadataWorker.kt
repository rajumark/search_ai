package com.photo.searchai.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photo.searchai.core.database.dao.ExifDao
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.metadata_index.MetadataIndexer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker responsible for extracting EXIF metadata from images.
 */
@HiltWorker
class MetadataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val imageDao: ImageDao,
    private val exifDao: ExifDao,
    private val metadataIndexer: MetadataIndexer
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val BATCH_SIZE = 50
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            while (true) {
                val pendingIds = imageDao.getUnparsedMetadataImageIds(BATCH_SIZE)
                if (pendingIds.isEmpty()) break

                for (id in pendingIds) {
                    val image = imageDao.getImageById(id) ?: continue
                    val exif = metadataIndexer.extractMetadata(id, image.path)
                    exifDao.insertExif(exif)
                    imageDao.markAsMetadataParsed(id)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
