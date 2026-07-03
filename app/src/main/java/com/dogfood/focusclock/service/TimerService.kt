package com.dogfood.focusclock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dogfood.focusclock.R
import com.dogfood.focusclock.state.Phase
import com.dogfood.focusclock.state.TimerState
import com.dogfood.focusclock.state.formatClock

class TimerService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer_channel"
        const val ACTION_UPDATE_STATE = "com.dogfood.focusclock.UPDATE_STATE"
        const val EXTRA_STATE = "com.dogfood.focusclock.EXTRA_STATE"
        const val EXTRA_PHASE = "com.dogfood.focusclock.EXTRA_PHASE"
        const val EXTRA_REMAINING_SEC = "com.dogfood.focusclock.EXTRA_REMAINING_SEC"
        const val EXTRA_COMPLETED_FOCUS = "com.dogfood.focusclock.EXTRA_COMPLETED_FOCUS"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_STATE -> {
                val state = intent.getStringExtra(EXTRA_STATE) ?: "idle"
                val phase = intent.getStringExtra(EXTRA_PHASE) ?: Phase.FOCUS.name
                val remainingSec = intent.getIntExtra(EXTRA_REMAINING_SEC, 0)
                val completedFocus = intent.getIntExtra(EXTRA_COMPLETED_FOCUS, 0)

                // Build and display notification based on state
                val notification = buildNotification(state, phase, remainingSec)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Timer notifications"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        state: String,
        phase: String,
        remainingSec: Int
    ): Notification {
        val title = when (state.lowercase()) {
            "running" -> when (phase) {
                Phase.FOCUS.name -> "Focus"
                Phase.SHORT_BREAK.name -> "Short break"
                Phase.LONG_BREAK.name -> "Long break"
                else -> "Timer"
            }
            "paused" -> when (phase) {
                Phase.FOCUS.name -> "Focus (paused)"
                Phase.SHORT_BREAK.name -> "Short break (paused)"
                Phase.LONG_BREAK.name -> "Long break (paused)"
                else -> "Timer (paused)"
            }
            else -> "Timer Ready"
        }

        val subtitle = formatClock(remainingSec)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }
}
