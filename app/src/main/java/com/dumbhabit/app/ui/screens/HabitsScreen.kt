package com.dumbhabit.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dumbhabit.app.data.EVERY_DAY
import com.dumbhabit.app.data.Habit
import com.dumbhabit.app.data.describeDays
import com.dumbhabit.app.data.minutesToLabel
import com.dumbhabit.app.ui.AppViewModel
import com.dumbhabit.app.ui.components.DaysPicker
import com.dumbhabit.app.ui.components.PixelBox
import com.dumbhabit.app.ui.components.PixelButton
import com.dumbhabit.app.ui.components.PixelDivider
import com.dumbhabit.app.ui.components.PixelTextField
import com.dumbhabit.app.ui.components.ReminderField
import com.dumbhabit.app.ui.components.ScreenHeader
import com.dumbhabit.app.ui.theme.pixel

@Composable
fun HabitsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val p = pixel()
    val habits by vm.habits.collectAsState()
    var editing by remember { mutableStateOf<Habit?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = "Habitudes",
            onBack = onBack,
            action = "Ajouter",
            onAction = {
                editing = null
                showEditor = true
            },
        )

        PixelBox(label = "${habits.size} habitudes") {
            if (habits.isEmpty()) {
                Text(
                    "Ajoute une habitude à suivre chaque jour.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.muted,
                )
            }
            habits.forEachIndexed { i, habit ->
                if (i > 0) PixelDivider(Modifier.padding(vertical = 10.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            editing = habit
                            showEditor = true
                        },
                ) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(
                            describeDays(habit.daysMask),
                            habit.reminderMinutes?.let { minutesToLabel(it) },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = p.muted,
                    )
                }
            }
        }
    }

    if (showEditor) {
        HabitEditor(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = {
                vm.saveHabit(it)
                showEditor = false
            },
            onDelete = {
                vm.deleteHabit(it)
                showEditor = false
            },
        )
    }
}

@Composable
private fun HabitEditor(
    initial: Habit?,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onDelete: (Habit) -> Unit,
) {
    val p = pixel()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var reminder by remember { mutableStateOf(initial?.reminderMinutes) }
    var days by remember { mutableIntStateOf(initial?.daysMask ?: EVERY_DAY) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(p.background)
                .border(2.dp, p.ink)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (initial == null) "NOUVELLE HABITUDE" else "MODIFIER L'HABITUDE",
                style = MaterialTheme.typography.labelSmall,
                color = p.muted,
            )
            PixelTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Nom (ex. Lire 10 pages)",
                modifier = Modifier.fillMaxWidth(),
            )
            DaysPicker(mask = days, onChange = { days = it })
            ReminderField(minutes = reminder, onChange = { reminder = it })

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PixelButton(
                    text = "Enregistrer",
                    onClick = {
                        onSave(
                            Habit(
                                id = initial?.id ?: 0L,
                                name = name.trim(),
                                reminderMinutes = reminder,
                                daysMask = days,
                                position = initial?.position ?: 0,
                            ),
                        )
                    },
                    filled = true,
                    enabled = name.isNotBlank() && days != 0,
                    modifier = Modifier.weight(1f),
                )
                PixelButton(text = "Annuler", onClick = onDismiss, modifier = Modifier.weight(1f))
            }
            if (initial != null) {
                PixelButton(text = "Supprimer", onClick = { onDelete(initial) })
            }
        }
    }
}
