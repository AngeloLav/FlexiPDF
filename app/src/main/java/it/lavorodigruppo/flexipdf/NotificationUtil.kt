package it.lavorodigruppo.flexipdf

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat

class NotificationUtil(val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    fun cancelOngoingNotification(id: Int) {
        notificationManager.cancel(id)
    }

    //IS YET TO BE FINISHED

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "1001"
        const val CHANNEL_NAME = "sample_ongoing_noti"
    }

}