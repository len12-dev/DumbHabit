package com.dumbhabit.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dumbhabit.app.data.EVERY_DAY
import com.dumbhabit.app.data.Routine
import com.dumbhabit.app.data.RoutineStep
import com.dumbhabit.app.data.describeDays
import com.dumbhabit.app.data.formatSeconds
import com.dumbhabit.app.data.minutesToLabel
import com.dumbhabit.app.ui.AppViewModel
import com.dumbhabit.app.ui.components.DaysPicker
import com.dumbhabit.app.ui.components.PixelBox
import com.dumbhabit.app.ui.components.PixelButton
import com.dumbhabit.app.ui.components.PixelDivider
import com.dumbhabit.app.ui.components.PixelStepper
import com.dumbhabit.app.ui.components.PixelTextField
import com.dumbhabit.app.ui.components.ReminderField
import com.dumbhabit.app.ui.components.ScreenHeader
import com.dumbhabit.app.ui.theme.pixel
import kotlin.math.min

@Composable
fun RoutinesScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onRunStarted: () -> Unit,
) {
    val p = pixel()
    val routines by vm.routines.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = "Routines",
            onBack = onBack,
            action = "Ajouter",
            onAction = { onEdit(0L) },
        )

        PixelBox(label = "${routines.size} routines") {
            if (routines.isEmpty()) {
                Text(
                    "Crée ta première routine : une suite d'étapes, chacune avec son chrono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.muted,
                )
            }
            routines.forEachIndexed { i, routine ->
                if (i > 0) PixelDivider(Modifier.padding(vertical = 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onEdit(routine.id) }) {
                        Text(routine.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOfNotNull(
                                describeDays(routine.daysMask),
                                routine.reminderMinutes?.let { minutesToLabel(it) },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = p.muted,
                        )
                    }
                    PixelButton(
                        text = "Lancer",
                        onClick = { vm.startRoutine(routine, onRunStarted) },
                        filled = true,
                    )
                }
            }
        }
    }
}

@Composable
fun RoutineEditScreen(vm: AppViewModel, routineId: Long, onBack: () -> Unit) {
    val p = pixel()
    val routines by vm.routines.collectAsState()
    // Flow créé une seule fois : sinon collectAsState se réabonne à chaque recomposition.
    val stepsFlow = remember(routineId) { vm.steps(routineId) }
    val savedSteps by stepsFlow.collectAsState(initial = null)

    val existing = routines.find { it.id == routineId }
    if (routineId != 0L && existing == null) return
    val loaded: List<RoutineStep> = savedSteps ?: return

    var name by remember(routineId) { mutableStateOf(existing?.name ?: "") }
    var reminder by remember(routineId) { mutableStateOf(existing?.reminderMinutes) }
    var days by remember(routineId) { mutableIntStateOf(existing?.daysMask ?: EVERY_DAY) }
    val draft = remember(routineId) { mutableStateListOf<RoutineStep>().apply { addAll(loaded) } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = if (routineId == 0L) "Nouvelle routine" else "Modifier",
            onBack = onBack,
        )

        PixelBox(label = "Routine") {
            PixelTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Nom (ex. Lever)",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            DaysPicker(mask = days, onChange = { days = it })
            Spacer(Modifier.height(14.dp))
            ReminderField(minutes = reminder, onChange = { reminder = it })
        }

        PixelBox(
            label = "Étapes dans l'ordre",
            trailing = {
                Text("${draft.size}", style = MaterialTheme.typography.labelSmall, color = p.muted)
            },
        ) {
            if (draft.isEmpty()) {
                Text(
                    "Ajoute une première étape (ex. Se brosser les dents, 2:00).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.muted,
                )
            }
            draft.forEachIndexed { i, step ->
                if (i > 0) PixelDivider(Modifier.padding(vertical = 10.dp))
                StepEditor(
                    index = i + 1,
                    step = step,
                    isFirst = i == 0,
                    isLast = i == draft.lastIndex,
                    onChange = { draft[i] = it },
                    onUp = { draft.swap(i, i - 1) },
                    onDown = { draft.swap(i, i + 1) },
                    onDelete = { draft.removeAt(i) },
                )
            }
            Spacer(Modifier.height(14.dp))
            PixelButton(
                text = "+ Étape",
                onClick = {
                    draft.add(RoutineStep(routineId = routineId, position = draft.size, name = ""))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PixelButton(
                text = "Enregistrer",
                onClick = {
                    vm.saveRoutine(
                        Routine(
                            id = routineId,
                            name = name.trim(),
                            reminderMinutes = reminder,
                            daysMask = days,
                            position = existing?.position ?: 0,
                        ),
                        draft.filter { it.name.isNotBlank() }.toList(),
                    )
                    onBack()
                },
                filled = true,
                enabled = name.isNotBlank() && days != 0 && draft.any { it.name.isNotBlank() },
                modifier = Modifier.weight(1f),
            )
            PixelButton(text = "Annuler", onClick = onBack, modifier = Modifier.weight(1f))
        }

        existing?.let { routine ->
            PixelButton(
                text = "Supprimer la routine",
                onClick = {
                    vm.deleteRoutine(routine)
                    onBack()
                },
            )
        }
    }
}

@Composable
private fun StepEditor(
    index: Int,
    step: RoutineStep,
    isFirst: Boolean,
    isLast: Boolean,
    onChange: (RoutineStep) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val p = pixel()
    val duration = step.durationSeconds

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "%02d".format(index),
                style = MaterialTheme.typography.labelMedium,
                color = p.muted,
            )
            Spacer(Modifier.width(10.dp))
            PixelTextField(
                value = step.name,
                onValueChange = { onChange(step.copy(name = it)) },
                placeholder = "Étape",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PixelButton(text = "×", onClick = onDelete)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelStepper(
                value = duration?.let { formatSeconds(it.toLong()) } ?: "Libre",
                onMinus = {
                    onChange(step.copy(durationSeconds = if (duration == null || duration <= 15) null else duration - 15))
                },
                onPlus = {
                    onChange(step.copy(durationSeconds = min((duration ?: 0) + 15, 3600)))
                },
            )
            Spacer(Modifier.weight(1f))
            PixelButton(text = "↑", onClick = onUp, enabled = !isFirst)
            PixelButton(text = "↓", onClick = onDown, enabled = !isLast)
        }
    }
}

private fun MutableList<RoutineStep>.swap(a: Int, b: Int) {
    val tmp = this[a]
    this[a] = this[b]
    this[b] = tmp
}
