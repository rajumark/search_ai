package com.photo.searchai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service for long-running image processing operations.
 *
 * This service is used when:
 * - User manually triggers processing
 * - Large batch processing is needed
 * - Real-time progress updates are required
 *
 * Design principles:
 * - No wake locks unless absolutely necessary
 * - Adaptive processing based on device state
 * - Efficient notification updates (batched)
 * - Clean shutdown on user request
 *
 * The service runs as a foreground service to prevent Android from killing it during active
 * processing. When processing is complete, the service stops itself.
 */
@AndroidEntryPoint
class ImageProcessingService : Service() {

    companion object {
        const val CHANNEL_ID = "image_processing_service_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.photo.searchai.service.START"
        const val ACTION_STOP = "com.photo.searchai.service.STOP"
        const val ACTION_PAUSE = "com.photo.searchai.service.PAUSE"
        const val ACTION_RESUME = "com.photo.searchai.service.RESUME"

        const val EXTRA_STAGE = "extra_stage"

        // Notification update interval to prevent battery drain from frequent updates
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 1000L

        /** Start the foreground service */
        fun start(context: Context, stage: String? = null) {
            val intent =
                    Intent(context, ImageProcessingService::class.java).apply {
                        action = ACTION_START
                        stage?.let { putExtra(EXTRA_STAGE, it) }
                    }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Stop the foreground service */
        fun stop(context: Context) {
            val intent =
                    Intent(context, ImageProcessingService::class.java).apply {
                        action = ACTION_STOP
                    }
            context.startService(intent)
        }
    }

    /** Service state */
    sealed class ServiceState {
        data object Idle : ServiceState()
        data object Starting : ServiceState()
        data class Processing(
                val stage: String,
                val current: Int,
                val total: Int,
                val message: String
        ) : ServiceState()
        data object Paused : ServiceState()
        data object Stopping : ServiceState()
        data object Completed : ServiceState()
        data class Error(val message: String) : ServiceState()
    }

    private val _serviceState = MutableStateFlow<ServiceState>(ServiceState.Idle)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var notificationUpdateJob: Job? = null

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }

    // Binder for local binding
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ImageProcessingService = this@ImageProcessingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val stage = intent.getStringExtra(EXTRA_STAGE) ?: "Processing"
                startForegroundProcessing(stage)
            }
            ACTION_STOP -> {
                stopProcessing()
            }
            ACTION_PAUSE -> {
                pauseProcessing()
            }
            ACTION_RESUME -> {
                resumeProcessing()
            }
        }

        // If the system kills the service, don't restart it automatically
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        notificationUpdateJob?.cancel()
        super.onDestroy()
    }

    /** Start foreground processing with notification */
    private fun startForegroundProcessing(stage: String) {
        _serviceState.value = ServiceState.Starting

        val notification =
                createNotification(
                        title = "Photo Processing",
                        content = "Starting $stage...",
                        progress = 0,
                        indeterminate = true
                )

        // Start as foreground service with proper type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _serviceState.value =
                ServiceState.Processing(
                        stage = stage,
                        current = 0,
                        total = 0,
                        message = "Starting..."
                )

        // Start batched notification updates
        startNotificationUpdates()
    }

    /** Update processing progress Call this from your processing logic */
    fun updateProgress(current: Int, total: Int, stage: String, message: String = "") {
        _serviceState.value =
                ServiceState.Processing(
                        stage = stage,
                        current = current,
                        total = total,
                        message = message
                )
    }

    /** Mark processing as complete */
    fun markComplete() {
        _serviceState.value = ServiceState.Completed

        // Show completion notification
        val notification =
                createNotification(
                        title = "Processing Complete",
                        content = "Your photos have been processed",
                        progress = 100,
                        indeterminate = false,
                        ongoing = false
                )
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Stop the service after a short delay
        serviceScope.launch {
            delay(2000)
            stopSelf()
        }
    }

    /** Mark processing as failed */
    fun markFailed(errorMessage: String) {
        _serviceState.value = ServiceState.Error(errorMessage)

        val notification =
                createNotification(
                        title = "Processing Failed",
                        content = errorMessage,
                        progress = 0,
                        indeterminate = false,
                        ongoing = false
                )
        notificationManager.notify(NOTIFICATION_ID, notification)

        stopSelf()
    }

    private fun pauseProcessing() {
        _serviceState.value = ServiceState.Paused
        notificationUpdateJob?.cancel()

        val notification =
                createNotification(
                        title = "Processing Paused",
                        content = "Tap to resume",
                        progress = 0,
                        indeterminate = false,
                        showActions = true
                )
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun resumeProcessing() {
        val currentState = _serviceState.value
        if (currentState is ServiceState.Paused) {
            startNotificationUpdates()
        }
    }

    private fun stopProcessing() {
        _serviceState.value = ServiceState.Stopping
        notificationUpdateJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Start batched notification updates to minimize battery impact */
    private fun startNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob =
                serviceScope.launch {
                    while (isActive) {
                        val state = _serviceState.value
                        if (state is ServiceState.Processing) {
                            val progress =
                                    if (state.total > 0) {
                                        (state.current * 100 / state.total)
                                    } else {
                                        0
                                    }

                            val content = buildString {
                                append(state.stage)
                                if (state.total > 0) {
                                    append(": ${state.current}/${state.total}")
                                }
                                if (state.message.isNotEmpty()) {
                                    append(" - ${state.message}")
                                }
                            }

                            val notification =
                                    createNotification(
                                            title = "Processing Photos",
                                            content = content,
                                            progress = progress,
                                            indeterminate = state.total == 0,
                                            showActions = true
                                    )
                            notificationManager.notify(NOTIFICATION_ID, notification)
                        }

                        delay(NOTIFICATION_UPDATE_INTERVAL_MS)
                    }
                }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                                    CHANNEL_ID,
                                    "Image Processing",
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply {
                                description = "Progress notifications for photo processing"
                                setShowBadge(false)
                                enableLights(false)
                                enableVibration(false)
                            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(
            title: String,
            content: String,
            progress: Int,
            indeterminate: Boolean,
            ongoing: Boolean = true,
            showActions: Boolean = false
    ): Notification {
        val builder =
                NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                        .setOngoing(ongoing)
                        .setOnlyAlertOnce(true)

        // Progress
        if (indeterminate) {
            builder.setProgress(0, 0, true)
        } else {
            builder.setProgress(100, progress, false)
        }

        // Actions
        if (showActions) {
            val stopIntent =
                    Intent(this, ImageProcessingService::class.java).apply { action = ACTION_STOP }
            val stopPendingIntent =
                    PendingIntent.getService(
                            this,
                            0,
                            stopIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
            builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Stop",
                    stopPendingIntent
            )
        }

        // Click to open app
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.let {
            val pendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            it,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
            builder.setContentIntent(pendingIntent)
        }

        return builder.build()
    }

    /** Check if device is in an optimal state for processing Used for adaptive scheduling */
    fun isOptimalForProcessing(): Boolean {
        // Don't process if in power save mode
        if (powerManager.isPowerSaveMode) {
            return false
        }

        // Prefer processing when device is idle (screen off)
        // but not in Doze mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (powerManager.isDeviceIdleMode) {
                return false
            }
        }

        return true
    }
}
