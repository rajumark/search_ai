package com.photo.searchai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.photo.searchai.core.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SearchAiApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var workScheduler: WorkScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        scheduleBackgroundWork()
    }

    private fun scheduleBackgroundWork() {
        workScheduler.scheduleOneTimeWork(
                "MEDIA_PROCESSING_ONETIME",
                com.photo.searchai.feature.mediaprocessing.domain.MediaProcessingWorker::class.java
        )
        workScheduler.schedulePeriodicWork(
                "MEDIA_PROCESSING_PERIODIC",
                com.photo.searchai.feature.mediaprocessing.domain
                                .PeriodicMediaProcessingWorker::class
                        .java
        )
    }
}
