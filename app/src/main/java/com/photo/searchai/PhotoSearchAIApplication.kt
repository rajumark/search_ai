package com.photo.searchai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.photo.searchai.worker.WorkManagerHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PhotoSearchAIApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var workManagerHelper: WorkManagerHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Schedule periodic background processing every 6 hours
        // This runs once on app start and will persist even after app closes
        workManagerHelper.schedulePeriodicProcessing()
        
        // Start metadata processing chain and schedule organization tasks
        workManagerHelper.enqueueMetadataProcessing()
        workManagerHelper.scheduleOrganizationTasks()
    }
}
