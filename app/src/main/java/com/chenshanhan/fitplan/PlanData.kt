package com.chenshanhan.fitplan

import java.util.Calendar

data class Exercise(val name: String, val sets: Int, val reps: String)

data class Cardio(val minutes: Int, val incline: String, val speed: String)

data class PlanDay(
    val dayOfWeek: Int,   // Calendar.SUNDAY=1 ... Calendar.SATURDAY=7
    val focus: String,
    val exercises: List<Exercise>,
    val cardio: Cardio,
    val reminderHour: Int,
    val reminderMinute: Int
) {
    val dayName: String
        get() = weekdayName(dayOfWeek)

    val reminderTimeText: String
        get() = String.format("%02d:%02d", reminderHour, reminderMinute)
}

fun weekdayName(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.MONDAY -> "周一"
    Calendar.TUESDAY -> "周二"
    Calendar.WEDNESDAY -> "周三"
    Calendar.THURSDAY -> "周四"
    Calendar.FRIDAY -> "周五"
    Calendar.SATURDAY -> "周六"
    Calendar.SUNDAY -> "周日"
    else -> "训练日"
}

object PlanData {
    /** 周一至周日，首页也按此顺序展示。 */
    val weekOrder: List<Int> = listOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    )

    /** 用户最初提供的五套计划，作为可一键添加的内置模板保留。 */
    val templates: List<PlanDay> = listOf(
        PlanDay(
            Calendar.MONDAY, "胸部",
            listOf(
                Exercise("坐姿推胸", 4, "8-12"),
                Exercise("夹胸", 4, "10-15")
            ),
            Cardio(20, "10", "5"), 18, 30
        ),
        PlanDay(
            Calendar.TUESDAY, "背部",
            listOf(
                Exercise("高位下拉", 4, "8-12"),
                Exercise("划船", 4, "8-12"),
                Exercise("反向飞鸟", 3, "12-15")
            ),
            Cardio(20, "10", "5"), 12, 45
        ),
        PlanDay(
            Calendar.WEDNESDAY, "肩部",
            listOf(
                Exercise("向上推肩", 4, "8-12"),
                Exercise("侧平举", 4, "12-15")
            ),
            Cardio(25, "12", "4.5-5"), 18, 30
        ),
        PlanDay(
            Calendar.THURSDAY, "臀腿",
            listOf(
                Exercise("夹腿机内收", 4, "12-15"),
                Exercise("夹腿机外展", 4, "12-15")
            ),
            Cardio(15, "8", "4.5"), 18, 30
        ),
        PlanDay(
            Calendar.FRIDAY, "手臂",
            listOf(
                Exercise("三头下压", 4, "10-12"),
                Exercise("二头弯举", 4, "10-12")
            ),
            Cardio(25, "12", "4.5-5"), 18, 30
        )
    )
}
