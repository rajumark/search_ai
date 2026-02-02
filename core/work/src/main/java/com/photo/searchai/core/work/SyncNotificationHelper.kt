package com.photo.searchai.core.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo

class SyncNotificationHelper(private val context: Context) {

    fun createForegroundInfo(progress: Int, total: Int): ForegroundInfo {
        val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                            CHANNEL_ID,
                            "Media Sync",
                            NotificationManager.IMPORTANCE_LOW
                    )
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Processing Images"
        val content =
                if (total > 0) "Processing image $progress of $total" else "Starting process..."

        val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(android.R.drawable.stat_notify_sync)
                        .setOngoing(true)
                        .setProgress(total, progress, total == 0)
                        .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "media_sync_channel"
    }
}
