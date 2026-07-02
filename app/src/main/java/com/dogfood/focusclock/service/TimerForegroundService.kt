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
import com.dogfood.focusclock.ui.FocusViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service that manages a persistent notification while the timer is running.
 * Listens to FocusViewModel state changes and starts/stops foreground notification accordingly.
 *
 * Responsibilities:
 * - Show foreground notification when Timer is in Running state
 * - Hide foreground notification when Timer transitions to Paused/Idle
 * - Does NOT manage the ticker Job (that stays in ViewModel)
 * - Does NOT cancel ViewModel's ticker when the Service is destroyed
 */
class TimerForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var observationJob: Job? = null
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startObservingTimerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service is started but foreground notification is managed by state observation
        return START_STICKY
    }

    override fun onDestroy() {
        observationJob?.cancel()
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Observe FocusViewModel's shared state and manage foreground notification lifecycle.
     * - When Running: start foreground with appropriate notification
     * - When Paused/Idle: stop foreground (remove notification)
     */
    private fun startObservingTimerState() {
        observationJob = scope.launch {
            FocusViewModel.currentStateShared.collect { state ->
                when (state) {
                    is TimerState.Running -> {
                        startForeground(NOTIFICATION_ID, createNotification(state))
                    }
                    is TimerState.Paused, is TimerState.Idle -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                }
            }
        }
    }

    /**
     * Create notification that displays current timer state.
     */
    private fun createNotification(state: TimerState.Running): Notification {
        val phaseLabel = when (state.phase) {
            Phase.FOCUS -> "Focus"
            Phase.SHORT_BREAK -> "Short break"
            Phase.LONG_BREAK -> "Long break"
        }
        val timeText = formatClock(state.remainingSec)
        val contentText = "$phaseLabel: $timeText"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Focus Timer Running")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    /**
     * Create notification channel for API 26+.
     * Required for NotificationCompat to show the notification.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification for active focus timer"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
