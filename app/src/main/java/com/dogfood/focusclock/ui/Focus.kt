package com.dogfood.focusclock.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dogfood.focusclock.service.TimerService
import com.dogfood.focusclock.state.Phase
import com.dogfood.focusclock.state.PomodoroConfig
import com.dogfood.focusclock.state.TimerEvent
import com.dogfood.focusclock.state.TimerMachine
import com.dogfood.focusclock.state.TimerState
import com.dogfood.focusclock.state.formatClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** DataStore-backed persistence of the user's focus length (minutes). */
val Context.focusDataStore: DataStore<Preferences> by preferencesDataStore(name = "focusclock")

class SettingsStore(private val store: DataStore<Preferences>) {
    private val focusMinutesKey = intPreferencesKey("focus_minutes")

    suspend fun focusMinutes(default: Int = 25): Int =
        store.data.map { it[focusMinutesKey] ?: default }.first()

    suspend fun setFocusMinutes(value: Int) {
        store.edit { it[focusMinutesKey] = value }
    }
}

class FocusViewModel(
    private val settings: SettingsStore,
    private val appContext: Context,
) : ViewModel() {

    private var machine = TimerMachine()
    private var ticker: Job? = null

    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val minutes = settings.focusMinutes()
            machine = TimerMachine(PomodoroConfig(focusSec = minutes * 60))
        }
    }

    fun start() {
        send(TimerEvent.Start)
        ensureTicker()
        startTimerService()
    }

    fun pause() = send(TimerEvent.Pause)

    fun resume() {
        send(TimerEvent.Resume)
        ensureTicker()
    }

    fun reset() {
        send(TimerEvent.Reset)
        ticker?.cancel()
        ticker = null
        stopTimerService()
    }

    private fun ensureTicker() {
        if (ticker != null) return
        ticker = viewModelScope.launch {
            while (true) {
                delay(1_000)
                if (_state.value is TimerState.Running) {
                    send(TimerEvent.Tick)
                }
            }
        }
    }

    private fun send(event: TimerEvent) {
        _state.value = machine.transition(_state.value, event)
    }

    private fun startTimerService() {
        val intent = Intent(appContext, TimerService::class.java)
        appContext.startService(intent)
    }

    private fun stopTimerService() {
        val intent = Intent(appContext, TimerService::class.java)
        appContext.stopService(intent)
    }
}

@Composable
fun FocusScreen(vm: FocusViewModel) {
    val state by vm.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val (label, seconds) = when (val s = state) {
            is TimerState.Idle -> "Ready" to 0
            is TimerState.Running -> s.phase.display() to s.remainingSec
            is TimerState.Paused -> (s.phase.display() + " (paused)") to s.remainingSec
        }
        Text(label, style = MaterialTheme.typography.titleLarge)
        Text(formatClock(seconds), style = MaterialTheme.typography.displayLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (state) {
                is TimerState.Idle -> Button(onClick = vm::start) { Text("Start") }
                is TimerState.Running -> Button(onClick = vm::pause) { Text("Pause") }
                is TimerState.Paused -> Button(onClick = vm::resume) { Text("Resume") }
            }
            Button(onClick = vm::reset) { Text("Reset") }
        }
    }
}

private fun Phase.display(): String = when (this) {
    Phase.FOCUS -> "Focus"
    Phase.SHORT_BREAK -> "Short break"
    Phase.LONG_BREAK -> "Long break"
}
