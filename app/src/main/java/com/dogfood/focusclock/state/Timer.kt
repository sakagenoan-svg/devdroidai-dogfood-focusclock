package com.dogfood.focusclock.state

/** Phases of a pomodoro cycle. */
enum class Phase { FOCUS, SHORT_BREAK, LONG_BREAK }

/** Immutable timer state. */
sealed interface TimerState {
    data object Idle : TimerState
    data class Running(val phase: Phase, val remainingSec: Int, val completedFocus: Int) : TimerState
    data class Paused(val phase: Phase, val remainingSec: Int, val completedFocus: Int) : TimerState
}

/** Configuration for cycle lengths. */
data class PomodoroConfig(
    val focusSec: Int = 25 * 60,
    val shortBreakSec: Int = 5 * 60,
    val longBreakSec: Int = 15 * 60,
    val longBreakEvery: Int = 4,
)

/** Events fed into the machine. */
sealed interface TimerEvent {
    data object Start : TimerEvent
    data object Pause : TimerEvent
    data object Resume : TimerEvent
    data object Reset : TimerEvent
    data object Tick : TimerEvent
}

/**
 * Pure transition function: (state, event) -> state. No Android dependency, so the entire
 * cycle logic (including the long-break-every-N rule) is unit-testable in isolation.
 */
class TimerMachine(private val config: PomodoroConfig = PomodoroConfig()) {

    fun transition(state: TimerState, event: TimerEvent): TimerState = when (event) {
        TimerEvent.Start -> when (state) {
            is TimerState.Idle -> TimerState.Running(Phase.FOCUS, config.focusSec, 0)
            else -> state
        }

        TimerEvent.Pause -> when (state) {
            is TimerState.Running -> TimerState.Paused(state.phase, state.remainingSec, state.completedFocus)
            else -> state
        }

        TimerEvent.Resume -> when (state) {
            is TimerState.Paused -> TimerState.Running(state.phase, state.remainingSec, state.completedFocus)
            else -> state
        }

        TimerEvent.Reset -> TimerState.Idle

        TimerEvent.Tick -> when (state) {
            is TimerState.Running ->
                if (state.remainingSec > 1) state.copy(remainingSec = state.remainingSec - 1) else advance(state)
            else -> state
        }
    }

    /** Move to the next phase when the current one elapses. */
    private fun advance(running: TimerState.Running): TimerState = when (running.phase) {
        Phase.FOCUS -> {
            val done = running.completedFocus + 1
            val nextPhase = if (done % config.longBreakEvery == 0) Phase.LONG_BREAK else Phase.SHORT_BREAK
            val duration = if (nextPhase == Phase.LONG_BREAK) config.longBreakSec else config.shortBreakSec
            TimerState.Running(nextPhase, duration, done)
        }

        Phase.SHORT_BREAK, Phase.LONG_BREAK ->
            TimerState.Running(Phase.FOCUS, config.focusSec, running.completedFocus)
    }
}

/** Format helper kept pure for testability. */
fun formatClock(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return m.toString().padStart(2, '0') + ":" + s.toString().padStart(2, '0')
}
