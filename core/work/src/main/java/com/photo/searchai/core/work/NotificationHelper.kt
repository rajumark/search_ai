package com.photo.searchai.core.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.photo.searchai.domain.model.FeatureType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {
    private val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    fun buildForegroundInfo(
            featureType: FeatureType,
            processed: Int,
            total: Int,
            id: Int
    ): ForegroundInfo {
        val percent = if (total > 0) (processed * 100) / total else 0
        val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle("${featureType.name} Processing")
                        .setContentText("$processed / $total ($percent%)")
                        .setSmallIcon(android.R.drawable.ic_menu_rotate)
                        .setOngoing(true)
                        .setProgress(total, processed, false)
                        .build()

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(
                    id,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(id, notification)
        }
    }

    private fun createChannel() {
        val channel =
                NotificationChannel(
                        CHANNEL_ID,
                        "Background Processing",
                        NotificationManager.IMPORTANCE_LOW
                )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "processing_channel"
    }
}
