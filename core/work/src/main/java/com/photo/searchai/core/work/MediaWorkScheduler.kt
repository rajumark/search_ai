package com.photo.searchai.core.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaWorkScheduler @Inject constructor(@ApplicationContext private val context: Context) {
        fun schedulePeriodicSync() {
                val periodicRequest =
                        PeriodicWorkRequestBuilder<MediaSyncWorker>(6, TimeUnit.HOURS)
                                .setConstraints(
                                        Constraints.Builder().setRequiresBatteryNotLow(true).build()
                                )
                                .build()

                WorkManager.getInstance(context)
                        .enqueueUniquePeriodicWork(
                                "MediaSyncPeriodic",
                                ExistingPeriodicWorkPolicy.KEEP,
                                periodicRequest
                        )
        }

        fun runOneTimeSync() {
                val oneTimeRequest = OneTimeWorkRequestBuilder<MediaSyncWorker>().build()

                WorkManager.getInstance(context)
                        .enqueueUniqueWork(
                                "MediaSyncOneTime",
                                ExistingWorkPolicy.KEEP,
                                oneTimeRequest
                        )
        }
}
