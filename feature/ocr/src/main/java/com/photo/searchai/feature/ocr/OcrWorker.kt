package com.photo.searchai.feature.ocr

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.entity.ProcessingStatus
import com.photo.searchai.core.work.BaseFeatureWorker
import com.photo.searchai.core.work.NotificationHelper
import com.photo.searchai.domain.model.FeatureType
import com.photo.searchai.domain.repository.SnapshotRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class OcrWorker
@AssistedInject
constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val imageDao: ImageDao,
        snapshotRepository: SnapshotRepository,
        notificationHelper: NotificationHelper
) : BaseFeatureWorker(context, params, snapshotRepository, notificationHelper) {

    override val featureType = FeatureType.OCR

    override suspend fun processItems(onProgress: suspend (Int) -> Unit) {
        // Process in batches
        while (true) {
            val pending = imageDao.getPendingOcrImages(limit = 10)
            if (pending.isEmpty()) break

            var batchProcessed = 0
            for (image in pending) {
                // Simulate ML Kit OCR processing here
                // val result = mlKit.process(image.path)
                delay(100) // Simulating work

                val updated =
                        image.copy(
                                ocrStatus = ProcessingStatus.DONE,
                                lastProcessedAt = System.currentTimeMillis()
                        )
                imageDao.updateImage(updated)
                batchProcessed++
            }
            onProgress(batchProcessed)
        }
    }
}
