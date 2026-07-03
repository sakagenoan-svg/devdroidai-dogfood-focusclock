package com.dogfood.focusclock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dogfood.focusclock.MainActivity
import com.dogfood.focusclock.R
import com.dogfood.focusclock.state.Phase
import com.dogfood.focusclock.state.PomodoroConfig
import com.dogfood.focusclock.state.TimerEvent
import com.dogfood.focusclock.state.TimerMachine
import com.dogfood.focusclock.state.TimerState
import com.dogfood.focusclock.state.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground Service that runs a Pomodoro timer and displays a notification with the
 * current phase, remaining time, and completed focus count.
 */
class TimerService : Service() {

    private val binder = TimerServiceBinder()
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var machine = TimerMachine()
    private var state = TimerState.Idle
    private var ticker: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                state = machine.transition(state, TimerEvent.Start)
                ensureTicker()
                updateNotification()
            }
            ACTION_PAUSE -> {
                state = machine.transition(state, TimerEvent.Pause)
                ticker?.cancel()
                ticker = null
                updateNotification()
            }
            ACTION_RESUME -> {
                state = machine.transition(state, TimerEvent.Resume)
                ensureTicker()
                updateNotification()
            }
            ACTION_RESET -> {
                state = machine.transition(state, TimerEvent.Reset)
                ticker?.cancel()
                ticker = null
                updateNotification()
            }
            else -> {
                // Unknown action or null intent
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        ticker?.cancel()
        scope.launch { stopForeground(STOP_FOREGROUND_REMOVE) }
    }

    private fun ensureTicker() {
        if (ticker != null) return
        ticker = scope.launch {
            while (true) {
                delay(1_000)
                if (state is TimerState.Running) {
                    state = machine.transition(state, TimerEvent.Tick)
                    updateNotification()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val (title, contentText) = when (state) {
            is TimerState.Idle -> {
                getString(R.string.notification_title_idle) to getString(R.string.notification_text_idle)
            }
            is TimerState.Running -> {
                val s = state as TimerState.Running
                val phaseLabel = s.phase.displayName()
                val timeStr = formatClock(s.remainingSec)
                val title = "$phaseLabel $timeStr"
                val contentText = getString(
                    R.string.notification_text_running,
                    s.completedFocus
                )
                title to contentText
            }
            is TimerState.Paused -> {
                val s = state as TimerState.Paused
                val phaseLabel = s.phase.displayName()
                val timeStr = formatClock(s.remainingSec)
                val title = "$phaseLabel $timeStr (paused)"
                val contentText = getString(
                    R.string.notification_text_paused,
                    s.completedFocus
                )
                title to contentText
            }
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(state !is TimerState.Idle)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (state !is TimerState.Idle) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun Phase.displayName(): String = when (this) {
        Phase.FOCUS -> getString(R.string.phase_focus)
        Phase.SHORT_BREAK -> getString(R.string.phase_short_break)
        Phase.LONG_BREAK -> getString(R.string.phase_long_break)
    }

    inner class TimerServiceBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "focus_clock_timer"
        private const val CHANNEL_NAME = "Focus Clock Timer"

        const val ACTION_START = "com.dogfood.focusclock.ACTION_START"
        const val ACTION_PAUSE = "com.dogfood.focusclock.ACTION_PAUSE"
        const val ACTION_RESUME = "com.dogfood.focusclock.ACTION_RESUME"
        const val ACTION_RESET = "com.dogfood.focusclock.ACTION_RESET"
    }
}
