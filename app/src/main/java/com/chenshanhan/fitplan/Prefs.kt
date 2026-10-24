package com.chenshanhan.fitplan

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Prefs {
    private const val NAME = "fitplan_prefs"
    private const val KEY_NOTIF_ENABLED = "notif_enabled"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isNotificationEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_NOTIF_ENABLED, false)

    fun setNotificationEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
    }

    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun doneKey(date: String, dayOfWeek: Int) = "done_${date}_$dayOfWeek"

    fun isDone(ctx: Context, dayOfWeek: Int): Boolean {
        val today = dateFormat().format(Calendar.getInstance().time)
        return prefs(ctx).getBoolean(doneKey(today, dayOfWeek), false)
    }

    fun setDone(ctx: Context, dayOfWeek: Int, done: Boolean) {
        val today = dateFormat().format(Calendar.getInstance().time)
        prefs(ctx).edit().putBoolean(doneKey(today, dayOfWeek), done).apply()
    }

    /** 本周当前训练计划中，已完成打卡的次数。 */
    fun weekDoneCount(ctx: Context, plans: List<PlanDay>): Int {
        val cal = Calendar.getInstance()
        // 回到本周一：周一->0，周日->6
        val daysFromMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        val fmt = dateFormat()
        val planDays = plans.map { it.dayOfWeek }.toSet()
        var count = 0
        for (i in PlanData.weekOrder.indices) {
            val dow = PlanData.weekOrder[i]
            if (dow in planDays &&
                prefs(ctx).getBoolean(doneKey(fmt.format(cal.time), dow), false)
            ) {
                count++
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return count
    }
}
