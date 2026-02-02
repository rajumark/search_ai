package com.photo.searchai.feature.mediaprocessing.domain

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photo.searchai.core.permissions.logic.PermissionManager
import com.photo.searchai.core.work.NotificationHelper
import com.photo.searchai.domain.model.FeatureType
import com.photo.searchai.feature.mediaprocessing.logic.MediaProcessingUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class MediaProcessingWorker
@AssistedInject
constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val useCase: MediaProcessingUseCase,
        private val notificationHelper: NotificationHelper,
        private val permissionManager: PermissionManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                // Check storage permissions first
                if (!permissionManager.hasStoragePermission(applicationContext)) {
                    // If we don't have storage permission, we retry later.
                    // This handles the case where permissions are granted after the worker was
                    // started.
                    return@withContext Result.retry()
                }

                val notificationId = 12345

                try {
                    // Check notification permission before setting foreground
                    val hasNotificationPermission =
                            permissionManager.isNotificationGranted(applicationContext)

                    useCase.execute { processed, total ->
                        if (hasNotificationPermission) {
                            setForeground(
                                    notificationHelper.buildForegroundInfo(
                                            FeatureType.MEDIA_PROCESSING,
                                            processed,
                                            total,
                                            notificationId
                                    )
                            )
                        }
                    }
                    Result.success()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Result.retry()
                }
            }
}
