package com.dumbhabit.app

import android.app.Application
import android.content.Context
import com.dumbhabit.app.data.AppDatabase
import com.dumbhabit.app.reminders.Reminders
import com.dumbhabit.app.routine.RoutineEngine

class DumbHabitApp : Application() {

    val db: AppDatabase by lazy { AppDatabase.build(this) }

    override fun onCreate() {
        super.onCreate()
        Reminders.createChannels(this)
        RoutineEngine.init(db.dao())
    }
}

/** Accès au conteneur d'application depuis n'importe quel Context (receivers, service, VM). */
val Context.dumbApp: DumbHabitApp
    get() = applicationContext as DumbHabitApp
