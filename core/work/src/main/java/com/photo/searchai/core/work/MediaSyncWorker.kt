package com.photo.searchai.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photo.searchai.core.data.repository.MediaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MediaSyncWorker
@AssistedInject
constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val repository: MediaRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            repository.syncImages()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
