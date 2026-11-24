package com.dumbhabit.app.ui.screens

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
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dumbhabit.app.data.isActiveOn
import com.dumbhabit.app.data.minutesToLabel
import com.dumbhabit.app.data.Routine
import com.dumbhabit.app.ui.AppViewModel
import com.dumbhabit.app.ui.components.PixelBox
import com.dumbhabit.app.ui.components.PixelButton
import com.dumbhabit.app.ui.components.PixelCheck
import com.dumbhabit.app.ui.components.PixelDivider
import com.dumbhabit.app.ui.components.PixelNavItem
import com.dumbhabit.app.ui.theme.pixel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onHabits: () -> Unit,
    onRoutines: () -> Unit,
    onSettings: () -> Unit,
    onRunStarted: () -> Unit,
) {
    val p = pixel()
    val habits by vm.habits.collectAsState()
    val habitLogs by vm.habitLogs.collectAsState()
    val routines by vm.routines.collectAsState()
    val routineLogs by vm.routineLogs.collectAsState()

    val todayDate = LocalDate.now()
    val todayHabits = habits.filter { isActiveOn(it.daysMask, todayDate) }
    val doneHabitIds = habitLogs.map { it.habitId }.toSet()
    val todayRoutines = routines.filter { isActiveOn(it.daysMask, todayDate) }
    val doneRoutineIds = routineLogs.map { it.routineId }.toSet()

    val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
    val nextRoutine: Pair<Routine, Int>? = todayRoutines
        .mapNotNull { r -> r.reminderMinutes?.let { r to it } }
        .filter { it.second > nowMinutes }
        .minByOrNull { it.second }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Date, façon onglet d'écran LCD
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                todayDate.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = p.background,
                modifier = Modifier
                    .background(p.ink)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        PixelDivider()

        // Indicateurs du jour
        PixelBox {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCell(
                    label = "Habitudes",
                    value = "${todayHabits.count { it.id in doneHabitIds }}/${todayHabits.size}",
                )
                StatCell(
                    label = "Routines",
                    value = "${todayRoutines.count { it.id in doneRoutineIds }}/${todayRoutines.size}",
                )
                StatCell(
                    label = "Prochaine",
                    value = nextRoutine?.let { minutesToLabel(it.second) } ?: "—",
                )
            }
        }

        // Routines du jour
        PixelBox(label = "Aujourd'hui") {
            if (todayRoutines.isEmpty()) {
                Text("Aucune routine prévue aujourd'hui.", style = MaterialTheme.typography.bodyMedium, color = p.muted)
            }
            todayRoutines.forEachIndexed { i, routine ->
                if (i > 0) PixelDivider(Modifier.padding(vertical = 10.dp))
                val done = routine.id in doneRoutineIds
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(routine.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            routine.reminderMinutes?.let { "Rappel ${minutesToLabel(it)}" } ?: "Sans rappel",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.muted,
                        )
                    }
                    if (done) {
                        Text("FAIT", style = MaterialTheme.typography.labelLarge, color = p.ink)
                    } else {
                        PixelButton(
                            text = "Lancer",
                            onClick = { vm.startRoutine(routine, onRunStarted) },
                            filled = true,
                        )
                    }
                }
            }
        }

        // Habitudes du jour
        PixelBox(
            label = "Habitudes",
            trailing = {
                Text(
                    "${todayHabits.count { it.id in doneHabitIds }} of ${todayHabits.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.muted,
                )
            },
        ) {
            if (todayHabits.isEmpty()) {
                Text("Aucune habitude aujourd'hui.", style = MaterialTheme.typography.bodyMedium, color = p.muted)
            }
            todayHabits.forEachIndexed { i, habit ->
                if (i > 0) PixelDivider(Modifier.padding(vertical = 10.dp))
                val done = habit.id in doneHabitIds
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelCheck(checked = done, onToggle = { vm.toggleHabit(habit, done) })
                    Spacer(Modifier.width(12.dp))
                    Text(
                        habit.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (done) p.muted else p.ink,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f),
                    )
                    habit.reminderMinutes?.let {
                        Text(minutesToLabel(it), style = MaterialTheme.typography.labelSmall, color = p.muted)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Navigation du bas, comme sur l'écran d'accueil d'origine
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PixelNavItem(label = "Réglages", onClick = onSettings)
            PixelNavItem(label = "Habitudes", onClick = onHabits)
            PixelNavItem(label = "Routines", onClick = onRoutines)
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    val p = pixel()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            color = p.muted,
        )
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
