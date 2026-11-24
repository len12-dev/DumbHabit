package com.dumbhabit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dumbhabit.app.data.formatSeconds
import com.dumbhabit.app.routine.RoutineEngine
import com.dumbhabit.app.routine.RunState
import com.dumbhabit.app.ui.AppViewModel
import com.dumbhabit.app.ui.components.PixelBox
import com.dumbhabit.app.ui.components.PixelButton
import com.dumbhabit.app.ui.components.PixelDivider
import com.dumbhabit.app.ui.components.ScreenHeader
import com.dumbhabit.app.ui.theme.pixel
import kotlinx.coroutines.delay

/** Écran de la routine en direct. Le même état pilote la notification (Valider / Passer). */
@Composable
fun RoutineRunScreen(vm: AppViewModel, onFinish: () -> Unit) {
    val run by RoutineEngine.state.collectAsState()

    // Rafraîchit l'affichage du chrono 5 fois par seconde.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(200)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val current = run
        when {
            current == null -> EmptyRun(onFinish)
            current.finished -> FinishedRun(
                run = current,
                onClose = {
                    vm.endRoutine()
                    onFinish()
                },
            )
            else -> ActiveRun(
                run = current,
                now = now,
                onValidate = vm::validateStep,
                onSkip = vm::skipStep,
                onQuit = {
                    vm.endRoutine()
                    onFinish()
                },
            )
        }
    }
}

@Composable
private fun EmptyRun(onBack: () -> Unit) {
    val p = pixel()
    ScreenHeader(title = "Routine", onBack = onBack)
    Text(
        "Aucune routine en cours.",
        style = MaterialTheme.typography.bodyLarge,
        color = p.muted,
    )
    PixelButton(text = "Retour", onClick = onBack)
}

@Composable
private fun ActiveRun(
    run: RunState,
    now: Long,
    onValidate: () -> Unit,
    onSkip: () -> Unit,
    onQuit: () -> Unit,
) {
    val p = pixel()
    val step = run.currentStep ?: return

    val elapsedSec = (now - run.stepStartedAt) / 1000
    val target = step.durationSeconds
    val overtime = target != null && elapsedSec > target
    val shown = if (target != null) target - elapsedSec else elapsedSec

    ScreenHeader(title = run.routineName, onBack = onQuit)

    Text(
        "ÉTAPE ${run.index + 1} / ${run.steps.size}",
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
        color = p.muted,
    )

    // Progression : une case par étape (pleine = faite, grise = en cours, vide = à venir)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        run.steps.forEachIndexed { i, _ ->
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .border(1.dp, p.ink)
                    .background(
                        when {
                            i < run.index -> p.ink
                            i == run.index -> p.muted
                            else -> Color.Transparent
                        },
                    ),
            )
        }
    }

    Text(
        step.name,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxWidth(),
    )

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            formatSeconds(shown),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            when {
                overtime -> "Temps dépassé"
                target != null -> "Minuteur"
                else -> "Chronomètre"
            },
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = p.muted,
        )
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PixelButton(
            text = "Valider",
            onClick = onValidate,
            filled = true,
            modifier = Modifier.weight(1f),
        )
        PixelButton(
            text = "Passer",
            onClick = onSkip,
            modifier = Modifier.weight(1f),
        )
    }

    val upcoming = run.steps.drop(run.index + 1)
    if (upcoming.isNotEmpty()) {
        PixelDivider()
        PixelBox(label = "À suivre") {
            upcoming.forEachIndexed { i, s ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                Text(
                    buildString {
                        append("· ").append(s.name)
                        s.durationSeconds?.let { append("  ").append(formatSeconds(it.toLong())) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun FinishedRun(run: RunState, onClose: () -> Unit) {
    val p = pixel()
    ScreenHeader(title = "Bilan", onBack = onClose)
    Text(
        "${run.routineName} terminée",
        style = MaterialTheme.typography.titleLarge,
    )
    PixelBox(label = "Étapes") {
        run.results.forEachIndexed { i, r ->
            if (i > 0) PixelDivider(Modifier.padding(vertical = 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    if (r.skipped) "PASSÉE" else "FAIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.muted,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    formatSeconds(r.seconds.toLong()),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
    PixelButton(text = "Terminer", onClick = onClose, filled = true)
}
