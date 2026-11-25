package com.dumbhabit.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay

class WearActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WearApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        // Charge l'état déjà présent (le téléphone peut avoir publié avant l'ouverture).
        Wearable.getDataClient(this).dataItems.addOnSuccessListener { buffer ->
            buffer.forEach { item ->
                if (item.uri.path == WearPaths.STATE) WatchStore.apply(DataMapItem.fromDataItem(item).dataMap)
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
            if (event.type == com.google.android.gms.wearable.DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == WearPaths.STATE
            ) {
                WatchStore.apply(DataMapItem.fromDataItem(event.dataItem).dataMap)
            }
        }
        events.release()
    }
}

@Composable
fun WearApp() {
    val habits by WatchStore.habits.collectAsState()
    val routine by WatchStore.routine.collectAsState()
    val context = LocalContext.current

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        routine?.let { r ->
            item { RoutineCard(r) { path -> sendToPhone(context, path) } }
        }

        item {
            Text(
                "Habitudes du jour",
                style = MaterialTheme.typography.caption1,
            )
        }

        if (habits.isEmpty()) {
            item {
                Text("Aucune habitude aujourd'hui", style = MaterialTheme.typography.body2)
            }
        }

        items(habits) { habit ->
            Button(
                onClick = { sendToPhone(context, WearPaths.HABIT_TOGGLE, habit.id.toString()) },
                colors = if (habit.done) ButtonDefaults.primaryButtonColors() else ButtonDefaults.secondaryButtonColors(),
                modifier = Modifier.fillMaxWidth(0.9f),
            ) {
                Text(
                    (if (habit.done) "✓ " else "○ ") + habit.name,
                    maxLines = 2,
                )
            }
        }
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
        modifier = Modifier.fillMaxWidth(0.95f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("${r.name} · ${r.index}/${r.total}", style = MaterialTheme.typography.caption1)
        Text(r.step, style = MaterialTheme.typography.title3, maxLines = 2)
        Text(formatTime(shown), style = MaterialTheme.typography.display3)
        Button(onClick = { send(WearPaths.ROUTINE_VALIDATE) }, modifier = Modifier.fillMaxWidth()) {
            Text("Valider")
        }
        Button(
            onClick = { send(WearPaths.ROUTINE_SKIP) },
            colors = ButtonDefaults.secondaryButtonColors(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Passer")
        }
    }
}

private fun formatTime(totalSec: Long): String {
    val sign = if (totalSec < 0) "-" else ""
    val s = kotlin.math.abs(totalSec)
    return "%s%02d:%02d".format(sign, s / 60, s % 60)
}
