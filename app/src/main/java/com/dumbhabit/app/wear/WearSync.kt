package com.dumbhabit.app.wear

import android.content.Context
import com.dumbhabit.app.data.Habit
import com.dumbhabit.app.routine.RunState
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject

/** Chemins partagés avec le module montre (wear). */
object WearPaths {
    const val STATE = "/dumbhabit/state"
    const val HABIT_TOGGLE = "/dumbhabit/habit/toggle"
    const val ROUTINE_VALIDATE = "/dumbhabit/routine/validate"
    const val ROUTINE_SKIP = "/dumbhabit/routine/skip"
}

/** Publie sur la montre les habitudes du jour et la routine en cours. */
object WearSync {

    fun publish(context: Context, habits: List<Habit>, doneIds: Set<Long>, run: RunState?) {
        val habitsJson = JSONArray().apply {
            habits.forEach { h ->
                put(JSONObject().put("id", h.id).put("name", h.name).put("done", h.id in doneIds))
            }
        }.toString()

        val routineJson = run?.takeIf { !it.finished }?.let { r ->
            val step = r.currentStep ?: return@let null
            JSONObject()
                .put("name", r.routineName)
                .put("index", r.index + 1)
                .put("total", r.steps.size)
                .put("step", step.name)
                .put("startedAt", r.stepStartedAt)
                .put("durationSec", step.durationSeconds ?: -1)
                .toString()
        } ?: ""

        val request = PutDataMapRequest.create(WearPaths.STATE).run {
            dataMap.putString("habits", habitsJson)
            dataMap.putString("routine", routineJson)
            // Horodatage : force la mise à jour même si le contenu est identique.
            dataMap.putLong("ts", System.currentTimeMillis())
            asPutDataRequest().setUrgent()
        }
        Wearable.getDataClient(context).putDataItem(request)
    }
}
