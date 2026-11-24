package com.dumbhabit.app.routine

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dumbhabit.app.reminders.Reminders
import com.dumbhabit.app.ui.MainActivity

/**
 * Affiche la routine en direct dans la barre de notifications : étape courante,
 * chronomètre (compte à rebours si l'étape a un minuteur) et boutons Valider / Passer.
 */
class RoutineService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, "Routine en cours", NotificationManager.IMPORTANCE_LOW)
            .apply {
                description = "Routine chronométrée affichée en direct"
                setShowBadge(false)
            }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Obligatoire : passer au premier plan dans les 5 s suivant startForegroundService().
        startForeground(NOTIF_ID, buildNotification(RoutineEngine.state.value))

        when (intent?.action) {
            ACTION_VALIDATE -> RoutineEngine.validate()
            ACTION_SKIP -> RoutineEngine.skip()
            ACTION_STOP -> RoutineEngine.clear()
            else -> Unit
        }

        val state = RoutineEngine.state.value
        when {
            state == null -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            state.finished -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                postDone(state)
                stopSelf()
            }
            else -> startForeground(NOTIF_ID, buildNotification(state))
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(state: RunState?): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openApp())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val step = state?.currentStep
        if (state == null || step == null) {
            return builder
                .setContentTitle("Routine")
                .setContentText("Aucune étape en cours")
                .build()
        }

        val total = state.steps.size
        builder
            .setContentTitle("${state.routineName} · ${state.index + 1}/$total")
            .setContentText(step.name)
            .setProgress(total, state.index, false)
            .setUsesChronometer(true)

        val duration = step.durationSeconds
        if (duration != null) {
            builder.setWhen(state.stepStartedAt + duration * 1000L).setChronometerCountDown(true)
        } else {
            builder.setWhen(state.stepStartedAt).setChronometerCountDown(false)
        }

        builder
            .addAction(0, "Valider", servicePending(ACTION_VALIDATE, 1))
            .addAction(0, "Passer", servicePending(ACTION_SKIP, 2))
        return builder.build()
    }

    @SuppressLint("MissingPermission")
    private fun postDone(state: RunState) {
        if (!Reminders.canNotify(this)) return
        val done = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Routine terminée")
            .setContentText("${state.routineName} · ${state.results.size} étapes")
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(DONE_ID, done)
    }

    private fun servicePending(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, RoutineService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openApp(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_START = "com.dumbhabit.app.routine.START"
        const val ACTION_VALIDATE = "com.dumbhabit.app.routine.VALIDATE"
        const val ACTION_SKIP = "com.dumbhabit.app.routine.SKIP"
        const val ACTION_STOP = "com.dumbhabit.app.routine.STOP"

        private const val CHANNEL_ID = "routine_live"
        private const val NOTIF_ID = 4001
        private const val DONE_ID = 4002

        fun send(context: Context, action: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, RoutineService::class.java).setAction(action),
            )
        }
    }
}
