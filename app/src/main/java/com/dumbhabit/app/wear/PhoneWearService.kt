package com.dumbhabit.app.wear

import com.dumbhabit.app.data.HabitLog
import com.dumbhabit.app.data.todayIso
import com.dumbhabit.app.dumbApp
import com.dumbhabit.app.routine.RoutineEngine
import com.dumbhabit.app.routine.RoutineService
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Reçoit les actions envoyées par la montre. */
class PhoneWearService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        val payload = String(event.data)
        when (event.path) {
            WearPaths.HABIT_TOGGLE -> scope.launch {
                val id = payload.toLongOrNull() ?: return@launch
                val dao = dumbApp.db.dao()
                val today = todayIso()
                if (dao.getHabitLog(id, today) != null) {
                    dao.deleteHabitLog(id, today)
                } else {
                    dao.insertHabitLog(HabitLog(id, today))
                }
            }
            WearPaths.ROUTINE_VALIDATE -> {
                RoutineEngine.validate()
                RoutineService.send(this, RoutineService.ACTION_VALIDATE)
            }
            WearPaths.ROUTINE_SKIP -> {
                RoutineEngine.skip()
                RoutineService.send(this, RoutineService.ACTION_SKIP)
            }
        }
    }
}
