package com.dumbhabit.app.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dumbhabit.app.data.Habit
import com.dumbhabit.app.data.HabitLog
import com.dumbhabit.app.data.Routine
import com.dumbhabit.app.data.RoutineLog
import com.dumbhabit.app.data.RoutineStep
import com.dumbhabit.app.data.todayIso
import com.dumbhabit.app.dumbApp
import com.dumbhabit.app.reminders.Reminders
import com.dumbhabit.app.routine.RoutineEngine
import com.dumbhabit.app.routine.RoutineService
import com.dumbhabit.app.ui.theme.ColorMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = application.dumbApp.db.dao()
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val ctx: Context get() = getApplication()

    val today: String = todayIso()

    val habits: StateFlow<List<Habit>> =
        dao.observeHabits().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val habitLogs: StateFlow<List<HabitLog>> =
        dao.observeHabitLogs(today).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val routines: StateFlow<List<Routine>> =
        dao.observeRoutines().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val routineLogs: StateFlow<List<RoutineLog>> =
        dao.observeRoutineLogs(today).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var colorMode by mutableStateOf(readColorMode())
        private set

    fun steps(routineId: Long): Flow<List<RoutineStep>> = dao.observeSteps(routineId)

    fun setColorMode(mode: ColorMode) {
        colorMode = mode
        prefs.edit().putString("color", mode.name).apply()
    }

    private fun readColorMode(): ColorMode =
        runCatching { ColorMode.valueOf(prefs.getString("color", ColorMode.LIGHT.name)!!) }
            .getOrDefault(ColorMode.LIGHT)

    // ---- Habitudes -------------------------------------------------------

    fun toggleHabit(habit: Habit, doneToday: Boolean) {
        viewModelScope.launch {
            if (doneToday) {
                dao.deleteHabitLog(habit.id, today)
            } else {
                dao.insertHabitLog(HabitLog(habit.id, today))
            }
        }
    }

    fun saveHabit(habit: Habit) {
        viewModelScope.launch {
            val id = if (habit.id == 0L) dao.upsertHabit(habit) else habit.id.also { dao.upsertHabit(habit) }
            Reminders.scheduleHabit(ctx, habit.copy(id = id))
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            dao.deleteHabit(habit.id)
            Reminders.cancel(ctx, Reminders.KIND_HABIT, habit.id)
        }
    }

    // ---- Routines --------------------------------------------------------

    fun saveRoutine(routine: Routine, steps: List<RoutineStep>) {
        viewModelScope.launch {
            val id = dao.saveRoutine(routine, steps)
            Reminders.scheduleRoutine(ctx, routine.copy(id = id))
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            dao.deleteRoutine(routine.id)
            Reminders.cancel(ctx, Reminders.KIND_ROUTINE, routine.id)
        }
    }

    /** Charge les étapes, démarre la routine et l'affiche en direct dans la barre de notifications. */
    fun startRoutine(routine: Routine, onStarted: () -> Unit) {
        viewModelScope.launch {
            val steps = dao.getSteps(routine.id)
            if (RoutineEngine.start(routine, steps)) {
                RoutineService.send(ctx, RoutineService.ACTION_START)
                onStarted()
            }
        }
    }

    fun validateStep() = RoutineService.send(ctx, RoutineService.ACTION_VALIDATE)

    fun skipStep() = RoutineService.send(ctx, RoutineService.ACTION_SKIP)

    fun endRoutine() = RoutineService.send(ctx, RoutineService.ACTION_STOP)
}
