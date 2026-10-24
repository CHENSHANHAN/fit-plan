package com.chenshanhan.fitplan

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val dow = intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1)
        val day = PlanRepository(context).getPlans().firstOrNull { it.dayOfWeek == dow } ?: return

        ReminderScheduler.ensureChannel(context)

        val exerciseSummary = day.exercises.joinToString(" / ") {
            "${it.name} ${it.sets}×${it.reps}"
        }
        val content = buildString {
            append(exerciseSummary)
            if (day.cardio.minutes > 0) {
                append(" → 爬坡 ${day.cardio.minutes} 分钟（坡度 ${day.cardio.incline}，速度 ${day.cardio.speed}）")
            }
        }

        val openIntent = PendingIntent.getActivity(
            context, dow, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("该训练了 · ${day.dayName} ${day.focus}")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val granted = Build.VERSION.SDK_INT < 33 ||
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            NotificationManagerCompat.from(context)
                .notify(ReminderScheduler.NOTIF_ID_BASE + dow, notification)
        }

        // 每周提醒：本次触发后安排下一周
        if (Prefs.isNotificationEnabled(context)) {
            ReminderScheduler.scheduleAll(context)
        }
    }

    companion object {
        const val EXTRA_DAY_OF_WEEK = "dow"
    }
}
