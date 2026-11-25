package com.dumbhabit.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

/**
 * Affiche la routine en cours comme « activité en cours » sur la montre
 * (cadran et liste récente). Démarré et arrêté depuis [WearActivity].
 */
class WatchOngoingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, "Routine en cours", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val routine = WatchStore.routine.value
        if (routine == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val label = "${routine.name} · ${routine.index}/${routine.total}"
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, WearActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(label)
            .setContentText(routine.step)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)

        if (routine.durationSec >= 0) {
            builder.setWhen(routine.startedAt + routine.durationSec * 1000L).setChronometerCountDown(true)
        } else {
            builder.setWhen(routine.startedAt).setChronometerCountDown(false)
        }

        // Ongoing Activity : statut affiché sur le cadran, touche = ouvrir l'app.
        OngoingActivity.Builder(this, NOTIF_ID, builder)
            .setStaticIcon(R.drawable.ic_notification)
            .setTouchIntent(open)
            .setStatus(Status.Builder().addTemplate(label).build())
            .build()
            .apply(this)

        startForeground(NOTIF_ID, builder.build())
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "watch_routine"
        private const val NOTIF_ID = 6001

        /** Crée, met à jour ou arrête l'activité en cours selon la routine. */
        fun sync(context: Context, routine: WatchRoutine?) {
            val intent = Intent(context, WatchOngoingService::class.java)
            if (routine == null) {
                context.stopService(intent)
            } else {
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }
}
