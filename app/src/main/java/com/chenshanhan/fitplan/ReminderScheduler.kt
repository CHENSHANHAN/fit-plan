package com.chenshanhan.fitplan

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {
    const val CHANNEL_ID = "workout_reminder"
    const val NOTIF_ID_BASE = 100
    private const val REQ_BASE = 1000

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, "训练提醒", NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "按训练计划每天定时提醒" }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleAll(ctx: Context) {
        ensureChannel(ctx)
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (day in PlanRepository(ctx).getPlans()) {
            val triggerAt = nextTriggerMillis(day)
            val pi = pendingIntent(ctx, day.dayOfWeek)
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        }
    }

    fun cancelAll(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // 一周七天都清理，避免已删除或移动的训练日留下旧提醒。
        for (dayOfWeek in PlanData.weekOrder) {
            am.cancel(pendingIntent(ctx, dayOfWeek))
        }
    }

    /** 该训练日下一次提醒时间；若今天的提醒时间已过，则排到下个星期。 */
    fun nextTriggerMillis(day: PlanDay): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, day.dayOfWeek)
            set(Calendar.HOUR_OF_DAY, day.reminderHour)
            set(Calendar.MINUTE, day.reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun pendingIntent(ctx: Context, dayOfWeek: Int): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java).putExtra(
            ReminderReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek
        )
        return PendingIntent.getBroadcast(
            ctx, REQ_BASE + dayOfWeek, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
