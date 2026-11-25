package com.dumbhabit.app.pomodoro

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dumbhabit.app.reminders.Reminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Phase { WORK, BREAK }

data class PomodoroState(
    val phase: Phase = Phase.WORK,
    val running: Boolean = false,
    val remainingMs: Long = 25 * 60_000L,
    val endAt: Long = 0L,
    val workSec: Int = 25 * 60,
    val breakSec: Int = 5 * 60,
    val completedWork: Int = 0,
) {
    fun durationOf(p: Phase): Int = if (p == Phase.WORK) workSec else breakSec
    val phaseSec: Int get() = durationOf(phase)
}

/**
 * Minuteur Pomodoro. L'heure de fin est mémorisée, donc le temps continue de
 * couler si l'app est en arrière-plan : une alarme prévient à la fin de la phase.
 */
object PomodoroEngine {
    private const val REQUEST_CODE = 5000
    private const val NOTIF_ID = 5001

    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun selectPhase(phase: Phase) = update { s ->
        if (s.running) s else s.copy(phase = phase, remainingMs = s.durationOf(phase) * 1000L)
    }

    /** Règle la durée de la phase courante (en secondes), uniquement à l'arrêt. */
    fun setDuration(seconds: Int) = update { s ->
        if (s.running) return@update s
        val clamped = seconds.coerceIn(0, 99 * 60 + 59)
        if (s.phase == Phase.WORK) {
            s.copy(workSec = clamped, remainingMs = clamped * 1000L)
        } else {
            s.copy(breakSec = clamped, remainingMs = clamped * 1000L)
        }
    }

    fun start() {
        val ctx = appContext ?: return
        update { s ->
            if (s.running || s.remainingMs <= 0L) return@update s
            val end = System.currentTimeMillis() + s.remainingMs
            alarmManager(ctx).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, endIntent(ctx))
            s.copy(running = true, endAt = end)
        }
    }

    fun pause() {
        val ctx = appContext ?: return
        update { s ->
            if (!s.running) return@update s
            alarmManager(ctx).cancel(endIntent(ctx))
            s.copy(
                running = false,
                endAt = 0L,
                remainingMs = (s.endAt - System.currentTimeMillis()).coerceAtLeast(0L),
            )
        }
    }

    fun reset() {
        appContext?.let { alarmManager(it).cancel(endIntent(it)) }
        update { s -> s.copy(running = false, endAt = 0L, remainingMs = s.phaseSec * 1000L) }
    }

    /** Termine la phase si son heure de fin est passée. Appelé par l'écran et par l'alarme. */
    fun tick() {
        update { s ->
            if (s.running && System.currentTimeMillis() >= s.endAt) finish(s) else s
        }
    }

    private fun finish(s: PomodoroState): PomodoroState {
        val workDone = s.phase == Phase.WORK
        val next = if (workDone) Phase.BREAK else Phase.WORK
        appContext?.let { ctx ->
            alarmManager(ctx).cancel(endIntent(ctx))
            Reminders.show(
                ctx,
                NOTIF_ID,
                "Pomodoro",
                if (workDone) "Travail terminé : place à la pause" else "Pause terminée : on reprend",
            )
        }
        return s.copy(
            running = false,
            endAt = 0L,
            phase = next,
            remainingMs = s.durationOf(next) * 1000L,
            completedWork = if (workDone) s.completedWork + 1 else s.completedWork,
        )
    }

    private fun update(block: (PomodoroState) -> PomodoroState) {
        _state.value = block(_state.value)
    }

    private fun alarmManager(ctx: Context): AlarmManager =
        ctx.getSystemService(AlarmManager::class.java)

    private fun endIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            REQUEST_CODE,
            Intent(ctx, PomodoroReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

/** Se déclenche à la fin prévue d'une phase. */
class PomodoroReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PomodoroEngine.tick()
    }
}
