package com.dumbhabit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dumbhabit.app.data.formatSeconds
import com.dumbhabit.app.pomodoro.Phase
import com.dumbhabit.app.pomodoro.PomodoroEngine
import com.dumbhabit.app.ui.components.PixelButton
import com.dumbhabit.app.ui.components.PixelSegmented
import com.dumbhabit.app.ui.components.ScreenHeader
import com.dumbhabit.app.ui.theme.pixel
import kotlinx.coroutines.delay

@Composable
fun PomodoroScreen(onBack: () -> Unit) {
    val p = pixel()
    val s by PomodoroEngine.state.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Vérifie la fin de phase 5 fois par seconde tant que le minuteur tourne.
    LaunchedEffect(s.running) {
        while (PomodoroEngine.state.value.running) {
            PomodoroEngine.tick()
            now = System.currentTimeMillis()
            delay(200)
        }
    }

    // Chiffres saisis au clavier (MMSS). Se remet à zéro quand on change de phase.
    var typed by remember(s.phase) { mutableStateOf(digitsOf(s.phaseSec)) }

    val shownMs = if (s.running) (s.endAt - now).coerceAtLeast(0L) else s.remainingMs
    val paused = !s.running && s.remainingMs < s.phaseSec * 1000L

    fun applyTyped(digits: String) {
        typed = digits
        PomodoroEngine.setDuration(secondsOf(digits))
    }

    val onDigit: (Char) -> Unit = { d ->
        val candidate = (typed + d).takeLast(4)
        // Les secondes ne peuvent pas dépasser 59.
        if (candidate.takeLast(2).toInt() <= 59) applyTyped(candidate)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = "Pomodoro", onBack = onBack)

        if (!s.running) {
            PixelSegmented(
                options = listOf(Phase.WORK to "Travail", Phase.BREAK to "Pause"),
                selected = s.phase,
                onSelect = { PomodoroEngine.selectPhase(it) },
            )
        }

        // Affichage du temps, en négatif comme la boîte du code de la capture
        Box(
            Modifier
                .fillMaxWidth()
                .background(p.ink)
                .padding(vertical = 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                formatSeconds((shownMs + 999L) / 1000L),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = p.background,
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    s.running && s.phase == Phase.WORK -> "CONCENTRATION"
                    s.running -> "PAUSE"
                    paused -> "EN PAUSE"
                    else -> "PRÊT"
                },
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = p.muted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Cycles : ${s.completedWork}",
                style = MaterialTheme.typography.labelSmall,
                color = p.muted,
            )
        }

        if (!s.running) {
            Keypad(
                onDigit = onDigit,
                onClear = { applyTyped("") },
                onDelete = { applyTyped(typed.dropLast(1)) },
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (s.running) {
                PixelButton(
                    text = "Pause",
                    onClick = { PomodoroEngine.pause() },
                    filled = true,
                    modifier = Modifier.weight(1f),
                )
            } else {
                PixelButton(
                    text = if (paused) "Reprendre" else "Démarrer",
                    onClick = { PomodoroEngine.start() },
                    filled = true,
                    enabled = shownMs > 0L,
                    modifier = Modifier.weight(1f),
                )
            }
            PixelButton(
                text = "Réinitialiser",
                onClick = {
                    PomodoroEngine.reset()
                    typed = digitsOf(PomodoroEngine.state.value.phaseSec)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (Char) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("CLR", "0", "DEL"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    KeyCell(
                        label = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                "CLR" -> onClear()
                                "DEL" -> onDelete()
                                else -> onDigit(key[0])
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyCell(label: String, modifier: Modifier, onClick: () -> Unit) {
    val p = pixel()
    Box(
        modifier
            .height(64.dp)
            .border(2.dp, p.ink)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = if (label.length == 1) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp)
            },
        )
    }
}

/** 1500 s -> "2500" (MMSS). */
private fun digitsOf(seconds: Int): String =
    "%02d%02d".format((seconds / 60) % 100, seconds % 60)

/** "2500" -> 25 * 60 secondes. */
private fun secondsOf(digits: String): Int {
    val d = digits.padStart(4, '0')
    return d.take(2).toInt() * 60 + d.takeLast(2).toInt()
}
