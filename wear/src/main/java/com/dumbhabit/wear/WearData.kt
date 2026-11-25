package com.dumbhabit.wear

import android.content.Context
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Chemins partagés avec le téléphone (app). */
object WearPaths {
    const val STATE = "/dumbhabit/state"
    const val HABIT_TOGGLE = "/dumbhabit/habit/toggle"
    const val ROUTINE_VALIDATE = "/dumbhabit/routine/validate"
    const val ROUTINE_SKIP = "/dumbhabit/routine/skip"
}

data class WatchHabit(val id: Long, val name: String, val done: Boolean)

/** Étape courante d'une routine. durationSec = -1 : simple chronomètre. */
data class WatchRoutine(
    val name: String,
    val index: Int,
    val total: Int,
    val step: String,
    val startedAt: Long,
    val durationSec: Int,
)

object WatchStore {
    val habits = MutableStateFlow<List<WatchHabit>>(emptyList())
    val routine = MutableStateFlow<WatchRoutine?>(null)

    fun apply(map: DataMap) {
        habits.value = parseHabits(map.getString("habits", "[]"))
        routine.value = parseRoutine(map.getString("routine", ""))
    }

    private fun parseHabits(json: String): List<WatchHabit> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WatchHabit(o.getLong("id"), o.getString("name"), o.getBoolean("done"))
        }
    }

    private fun parseRoutine(json: String): WatchRoutine? {
        if (json.isBlank()) return null
        val o = JSONObject(json)
        return WatchRoutine(
            name = o.getString("name"),
            index = o.getInt("index"),
            total = o.getInt("total"),
            step = o.getString("step"),
            startedAt = o.getLong("startedAt"),
            durationSec = o.getInt("durationSec"),
        )
    }
}

/** Envoie une action à tous les téléphones connectés. */
fun sendToPhone(context: Context, path: String, payload: String = "") {
    val messages = Wearable.getMessageClient(context)
    Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
        nodes.forEach { node -> messages.sendMessage(node.id, path, payload.toByteArray()) }
    }
}
