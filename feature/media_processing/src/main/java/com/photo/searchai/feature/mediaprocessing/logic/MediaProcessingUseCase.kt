package com.photo.searchai.feature.mediaprocessing.logic

import android.content.ContentUris
import android.provider.MediaStore
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.entity.ProcessingStatus
import com.photo.searchai.core.work.NotificationHelper
import com.photo.searchai.feature.labeling.logic.LabelingProcessor
import com.photo.searchai.feature.ocr.logic.OcrProcessor
import javax.inject.Inject

class MediaProcessingUseCase
@Inject
constructor(
        private val imageRepository: ImageRepository,
        private val imageDao: ImageDao,
        private val ocrProcessor: OcrProcessor,
        private val labelingProcessor: LabelingProcessor,
        private val notificationHelper: NotificationHelper
) {

    suspend fun execute(onProgress: suspend (Int, Int) -> Unit) {
        // 1. Fetch and Sync
        val allImages = imageRepository.getAllImages()
        if (allImages.isNotEmpty()) {
            imageDao.insertOrIgnore(allImages)
        }

        // 2. Process
        // Counting is expensive if we count *everything*, but let's try to get a total for proper
        // progress.
        // For simplicity, we can just use size of allImages as total if we assume we process
        // everything eventually.
        // Or query depending on status.
        var totalImages = allImages.size
        // We can't easily know how many *remain* without querying DB, but `allImages` is from
        // MediaStore.
        // DB might have some that are deleted from MediaStore? (We didn't handle deletion sync yet,
        // but user didn't ask).

        var processedCount = 0

        while (true) {
            val pending = imageDao.getPendingImages(limit = 10)
            if (pending.isEmpty()) break

            for (image in pending) {
                var updatedImage = image

                val contentUri =
                        ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                image.imageId.toLong()
                        )

                // OCR
                if (image.ocrStatus == ProcessingStatus.PENDING) {
                    val text = ocrProcessor.process(contentUri)
                    updatedImage =
                            updatedImage.copy(ocrStatus = ProcessingStatus.DONE, ocrText = text)
                }

                // Labeling
                if (image.labelingStatus == ProcessingStatus.PENDING) {
                    val labels = labelingProcessor.process(contentUri)
                    updatedImage =
                            updatedImage.copy(
                                    labelingStatus = ProcessingStatus.DONE,
                                    labels = labels.joinToString(",")
                            )
                }

                updatedImage = updatedImage.copy(lastProcessedAt = System.currentTimeMillis())
                imageDao.updateImage(updatedImage)
                processedCount++
            }

            onProgress(processedCount, totalImages)
        }
    }
}
