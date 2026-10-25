package com.notesapp.tasks

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val CHANNEL_ID = "reminders"
const val ACTION_DONE = "com.notesapp.tasks.DONE"

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val ch = NotificationChannel(CHANNEL_ID, "التذكيرات", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
}

object Reminder {
    private fun pending(ctx: Context, id: Long): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java).putExtra("id", id)
        return PendingIntent.getBroadcast(
            ctx, id.toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(ctx: Context, item: Item) {
        val at = item.reminderAt ?: return
        if (item.done || at <= System.currentTimeMillis()) return
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, item.id)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(ctx: Context, id: Long) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, id))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val id = i.getLongExtra("id", -1)
        if (id < 0) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDb.get(ctx).dao()
                val item = dao.get(id)
                if (item != null) {
                    if (i.action == ACTION_DONE) {
                        dao.upsert(item.copy(done = true))
                        NotificationManagerCompat.from(ctx).cancel(id.toInt())
                    } else if (!item.done) {
                        show(ctx, item)
                        if (item.repeat > 0 && item.reminderAt != null) {
                            val step = if (item.repeat == 1) 86_400_000L else 7 * 86_400_000L
                            var next = item.reminderAt
                            while (next <= System.currentTimeMillis()) next += step
                            val updated = item.copy(reminderAt = next)
                            dao.upsert(updated)
                            Reminder.schedule(ctx, updated)
                        }
                    }
                }
            } finally {
                result.finish()
            }
        }
    }

    private fun show(ctx: Context, item: Item) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            ctx, item.id.toInt(),
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val done = PendingIntent.getBroadcast(
            ctx, item.id.toInt() + 1_000_000,
            Intent(ctx, ReminderReceiver::class.java).setAction(ACTION_DONE).putExtra("id", item.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = item.content.ifBlank { "حان الوقت" }
        val b = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(item.title.ifBlank { "تذكير" })
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (item.isTask) b.addAction(0, "تمّت", done)
        try {
            nm.notify(item.id.toInt(), b.build())
        } catch (_: SecurityException) {
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDb.get(ctx).dao().withReminders().forEach { Reminder.schedule(ctx, it) }
            } finally {
                result.finish()
            }
        }
    }
}
