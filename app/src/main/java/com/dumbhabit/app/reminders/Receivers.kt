package com.dumbhabit.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dumbhabit.app.dumbApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Se déclenche à l'heure d'un rappel : affiche la notification puis reprogramme le lendemain. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getIntExtra(Reminders.EXTRA_KIND, Reminders.KIND_HABIT)
        val id = intent.getLongExtra(Reminders.EXTRA_ID, 0L)
        val pending = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = context.dumbApp.db.dao()
                if (kind == Reminders.KIND_HABIT) {
                    dao.getHabit(id)?.let { habit ->
                        Reminders.show(context, (id * 2).toInt(), "Habitude", habit.name)
                        Reminders.scheduleHabit(context, habit)
                    }
                } else {
                    dao.getRoutine(id)?.let { routine ->
                        Reminders.show(context, (id * 2 + 1).toInt(), "Routine", routine.name)
                        Reminders.scheduleRoutine(context, routine)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Après un redémarrage, les alarmes sont perdues : on les reprogramme toutes. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = context.dumbApp.db.dao()
                dao.getHabits().forEach { Reminders.scheduleHabit(context, it) }
                dao.getRoutines().forEach { Reminders.scheduleRoutine(context, it) }
            } finally {
                pending.finish()
            }
        }
    }
}
