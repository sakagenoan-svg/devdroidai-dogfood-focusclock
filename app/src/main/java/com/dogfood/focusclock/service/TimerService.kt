package com.dogfood.focusclock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    companion object {
        const val ACTION_START_TIMER = "com.dogfood.focusclock.START_TIMER"
        const val ACTION_STOP_TIMER = "com.dogfood.focusclock.STOP_TIMER"
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Timer")
            .setContentText("Timer is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
}
