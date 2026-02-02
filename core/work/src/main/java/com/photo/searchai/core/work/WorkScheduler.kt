package com.photo.searchai.core.work

import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(private val workManager: WorkManager) {
    fun schedulePeriodicWork(uniqueName: String, workerClass: Class<out ListenableWorker>) {
        val request =
                PeriodicWorkRequest.Builder(workerClass, 6, TimeUnit.HOURS)
                        .setConstraints(
                                Constraints.Builder()
                                        .setRequiresBatteryNotLow(true)
                                        .setRequiresStorageNotLow(true)
                                        .build()
                        )
                        .build()

        workManager.enqueueUniquePeriodicWork(uniqueName, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
