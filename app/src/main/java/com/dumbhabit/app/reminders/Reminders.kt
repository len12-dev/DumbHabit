package com.dumbhabit.app.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dumbhabit.app.R
import com.dumbhabit.app.data.Habit
import com.dumbhabit.app.data.Routine
import com.dumbhabit.app.data.isActiveOn
import com.dumbhabit.app.ui.MainActivity
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Planification des rappels quotidiens (AlarmManager, sans permission d'alarme exacte). */
object Reminders {
    const val KIND_HABIT = 0
    const val KIND_ROUTINE = 1
    const val EXTRA_KIND = "kind"
    const val EXTRA_ID = "id"

    private const val CHANNEL_ID = "rappels"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Rappels", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Rappels d'habitudes et de routines" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun scheduleHabit(context: Context, habit: Habit) =
        schedule(context, KIND_HABIT, habit.id, habit.reminderMinutes, habit.daysMask)

    fun scheduleRoutine(context: Context, routine: Routine) =
        schedule(context, KIND_ROUTINE, routine.id, routine.reminderMinutes, routine.daysMask)

    fun cancel(context: Context, kind: Int, id: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingFor(context, kind, id))
    }

    private fun schedule(context: Context, kind: Int, id: Long, minutes: Int?, daysMask: Int) {
        if (minutes == null || daysMask == 0) {
            cancel(context, kind, id)
            return
        }
        val at = nextTrigger(minutes, daysMask)
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingFor(context, kind, id))
    }

    /** Prochaine occurrence (epoch ms) de `minutes` après minuit, sur un jour actif. */
    fun nextTrigger(minutes: Int, daysMask: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
        var date: LocalDate = now.toLocalDate()
        repeat(8) {
            val candidate = date.atStartOfDay(ZoneId.systemDefault()).plusMinutes(minutes.toLong())
            if (candidate.isAfter(now) && isActiveOn(daysMask, date)) {
                return candidate.toInstant().toEpochMilli()
            }
            date = date.plusDays(1)
        }
        return now.plusDays(1).toInstant().toEpochMilli()
    }

    private fun pendingFor(context: Context, kind: Int, id: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_ID, id)
        // requestCode unique par (type, id) : sert aussi à annuler la bonne alarme.
        val requestCode = (id * 2 + kind).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun canNotify(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, notificationId: Int, title: String, text: String) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
