package com.dumbhabit.app.routine

import com.dumbhabit.app.data.AppDao
import com.dumbhabit.app.data.Routine
import com.dumbhabit.app.data.RoutineLog
import com.dumbhabit.app.data.RoutineStep
import com.dumbhabit.app.data.todayIso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StepResult(val name: String, val skipped: Boolean, val seconds: Int)

data class RunState(
    val routineId: Long,
    val routineName: String,
    val steps: List<RoutineStep>,
    val index: Int,
    val stepStartedAt: Long,
    val results: List<StepResult> = emptyList(),
) {
    val finished: Boolean get() = index >= steps.size
    val currentStep: RoutineStep? get() = steps.getOrNull(index)
}

/**
 * Source de vérité de la routine en cours, partagée entre l'écran et le service
 * de notification. Valider et Passer font avancer l'étape courante.
 */
object RoutineEngine {

    private val _state = MutableStateFlow<RunState?>(null)
    val state: StateFlow<RunState?> = _state.asStateFlow()

    private var dao: AppDao? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init(dao: AppDao) {
        this.dao = dao
    }

    fun start(routine: Routine, steps: List<RoutineStep>): Boolean {
        if (steps.isEmpty()) return false
        _state.value = RunState(
            routineId = routine.id,
            routineName = routine.name,
            steps = steps,
            index = 0,
            stepStartedAt = System.currentTimeMillis(),
        )
        return true
    }

    /** Valider : l'étape est faite, on passe à la suivante. */
    fun validate() = advance(skipped = false)

    /** Passer : l'étape est ignorée, on passe à la suivante. */
    fun skip() = advance(skipped = true)

    fun clear() {
        _state.value = null
    }

    private fun advance(skipped: Boolean) {
        val current = _state.value ?: return
        if (current.finished) return

        val now = System.currentTimeMillis()
        val result = StepResult(
            name = current.steps[current.index].name,
            skipped = skipped,
            seconds = ((now - current.stepStartedAt) / 1000).toInt(),
        )
        val next = current.copy(
            index = current.index + 1,
            stepStartedAt = now,
            results = current.results + result,
        )
        _state.value = next

        if (next.finished) {
            val routineId = current.routineId
            scope.launch { dao?.insertRoutineLog(RoutineLog(routineId, todayIso())) }
        }
    }
}
