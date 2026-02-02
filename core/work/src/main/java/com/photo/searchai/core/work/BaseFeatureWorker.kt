package com.photo.searchai.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.photo.searchai.domain.model.FeatureType
import com.photo.searchai.domain.repository.SnapshotRepository

abstract class BaseFeatureWorker(
        context: Context,
        params: WorkerParameters,
        private val snapshotRepository: SnapshotRepository,
        private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    abstract val featureType: FeatureType

    override suspend fun doWork(): Result {
        val snapshot =
                snapshotRepository.getLatestSnapshotSync(featureType) ?: return Result.success()

        // Initial notification
        setForeground(createForegroundInfo(snapshot.processedCount, snapshot.totalPending))

        var currentProcessed = snapshot.processedCount

        try {
            processItems { processedDelta ->
                currentProcessed += processedDelta
                snapshotRepository.updateSnapshotProgress(featureType, currentProcessed)
                setForeground(createForegroundInfo(currentProcessed, snapshot.totalPending))
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    // Lambda returns number of items processed in this batch
    abstract suspend fun processItems(onProgress: suspend (Int) -> Unit)

    private fun createForegroundInfo(processed: Int, total: Int): ForegroundInfo {
        return notificationHelper.buildForegroundInfo(
                featureType = featureType,
                processed = processed,
                total = total,
                id = applicationContext.hashCode() + featureType.ordinal
        )
    }
}
