package com.dumbhabit.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScreenScaffold
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay

// Palette pixel de l'app téléphone, en sombre.
private val Bg = Color(0xFF1C2119)
private val Sage = Color(0xFFA8BA93)
private val Muted = Color(0xFF7E8F71)
private val Ink = Color(0xFF1F2A1D)

class WearActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent {
            MaterialTheme(colors = Colors(primary = Sage, onPrimary = Ink, background = Bg, onBackground = Sage, surface = Bg, onSurface = Sage)) {
                WearApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        // Charge l'état déjà publié par le téléphone avant l'ouverture.
        Wearable.getDataClient(this).dataItems.addOnSuccessListener { buffer ->
            buffer.forEach { item ->
                if (item.uri.path == WearPaths.STATE) applyMap(DataMapItem.fromDataItem(item).dataMap)
            }
            buffer.release()
        }
    }

    override fun onPause() {
        Wearable.getDataClient(this).removeListener(this)
        super.onPause()
    }

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearPaths.STATE) {
                applyMap(DataMapItem.fromDataItem(event.dataItem).dataMap)
            }
        }
        events.release()
    }

    private fun applyMap(map: DataMap) {
        WatchStore.apply(map)
        // La routine en cours est aussi affichée comme activité sur le cadran.
        WatchOngoingService.sync(this, WatchStore.routine.value)
    }
}

@Composable
fun WearApp() {
    val habits by WatchStore.habits.collectAsState()
    val routine by WatchStore.routine.collectAsState()
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                Header()
            }

            routine?.let { r ->
                item { RoutineCard(r) { path -> sendToPhone(context, path) } }
            }

            item {
                PixelLabel("Habitudes du jour")
            }

            if (habits.isEmpty()) {
                item { PixelLabel("Aucune habitude") }
            }

            items(habits) { habit ->
                Button(
                    onClick = { sendToPhone(context, WearPaths.HABIT_TOGGLE, habit.id.toString()) },
                    colors = if (habit.done) {
                        ButtonDefaults.buttonColors(backgroundColor = Sage, contentColor = Ink)
                    } else {
                        ButtonDefaults.buttonColors(backgroundColor = Bg, contentColor = Sage)
                    },
                    modifier = Modifier.fillMaxWidth(0.85f),
                ) {
                    PixelText(
                        (if (habit.done) "✓ " else "○ ") + habit.name,
                        color = if (habit.done) Ink else Sage,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_notification),
            contentDescription = "DumbHabit",
            tint = Sage,
            modifier = Modifier.padding(4.dp),
        )
        PixelText("DumbHabit", color = Sage, size = 14)
    }
}

@Composable
private fun RoutineCard(r: WatchRoutine, send: (String) -> Unit) {
    // Rafraîchit le chrono deux fois par seconde.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(r.startedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }

    val elapsedSec = (now - r.startedAt) / 1000
    val shown = if (r.durationSec >= 0) r.durationSec - elapsedSec else elapsedSec

    Column(
        modifier = Modifier.fillMaxWidth(0.9f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PixelLabel("${r.name} · ${r.index}/${r.total}")
        PixelText(r.step, color = Sage, size = 15, maxLines = 2)
        PixelText(formatTime(shown), color = Sage, size = 28)
        Button(
            onClick = { send(WearPaths.ROUTINE_VALIDATE) },
            colors = ButtonDefaults.buttonColors(backgroundColor = Sage, contentColor = Ink),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PixelText("Valider", color = Ink)
        }
        Button(
            onClick = { send(WearPaths.ROUTINE_SKIP) },
            colors = ButtonDefaults.buttonColors(backgroundColor = Bg, contentColor = Sage),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PixelText("Passer", color = Sage)
        }
    }
}

@Composable
private fun PixelLabel(text: String) {
    PixelText(text.uppercase(), color = Muted, size = 10)
}

@Composable
private fun PixelText(
    text: String,
    color: Color,
    size: Int = 12,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = size.sp,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
    )
}

private fun formatTime(totalSec: Long): String {
    val sign = if (totalSec < 0) "-" else ""
    val s = kotlin.math.abs(totalSec)
    return "%s%02d:%02d".format(sign, s / 60, s % 60)
}
