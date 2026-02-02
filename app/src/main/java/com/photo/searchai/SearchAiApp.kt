package com.photo.searchai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.photo.searchai.core.work.WorkScheduler
import com.photo.searchai.feature.ocr.OcrWorker
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
        workScheduler.schedulePeriodicWork("OCR_WORK", OcrWorker::class.java)
        // workScheduler.schedulePeriodicWork("LABEL_WORK", LabelWorker::class.java)
    }
}
