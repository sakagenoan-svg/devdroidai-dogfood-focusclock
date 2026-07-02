package com.dogfood.focusclock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dogfood.focusclock.MainActivity
import com.dogfood.focusclock.R

/** Foreground service to display a persistent timer notification on the lock screen. */
class TimerForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1
        private const val EXTRA_TIMER_TEXT = "timer_text"
        private const val EXTRA_PHASE = "phase"

        /**
         * Start the foreground service with the current timer state.
         */
        fun start(context: Context, timerText: String, phase: String) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                putExtra(EXTRA_TIMER_TEXT, timerText)
                putExtra(EXTRA_PHASE, phase)
            }
            context.startForegroundService(intent)
        }

        /**
         * Stop the foreground service.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, TimerForegroundService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timerText = intent?.getStringExtra(EXTRA_TIMER_TEXT) ?: "00:00"
        val phase = intent?.getStringExtra(EXTRA_PHASE) ?: "Ready"

        val notification = buildNotification(timerText, phase)
        // MUST call startForeground() within 5 seconds of onStartCommand()
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(timerText: String, phase: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(phase)
            .setContentText(timerText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for the focus timer"
                setShowBadge(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
