package com.chenshanhan.fitplan

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 训练计划的本地存储。没有保存过自定义计划时，使用内置五套模板。 */
class PlanRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getPlans(): List<PlanDay> {
        val raw = prefs.getString(KEY_PLANS, null) ?: return PlanData.templates
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<PlanDay>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val exercises = mutableListOf<Exercise>()
                val exerciseArray = obj.getJSONArray("exercises")
                for (j in 0 until exerciseArray.length()) {
                    val item = exerciseArray.getJSONObject(j)
                    exercises.add(
                        Exercise(
                            item.getString("name"),
                            item.getInt("sets"),
                            item.getString("reps")
                        )
                    )
                }
                val cardio = obj.getJSONObject("cardio")
                result.add(
                    PlanDay(
                        dayOfWeek = obj.getInt("dayOfWeek"),
                        focus = obj.getString("focus"),
                        exercises = exercises,
                        cardio = Cardio(
                            cardio.optInt("minutes", 0),
                            cardio.optString("incline", ""),
                            cardio.optString("speed", ""),
                            CardioType.normalize(cardio.optString("type", CardioType.INCLINE))
                        ),
                        reminderHour = obj.getInt("reminderHour"),
                        reminderMinute = obj.getInt("reminderMinute")
                    )
                )
            }
            result.sortedBy { (it.dayOfWeek + 5) % 7 }
        } catch (e: Exception) {
            PlanData.templates
        }
    }

    fun hasPlan(dayOfWeek: Int): Boolean =
        getPlans().any { it.dayOfWeek == dayOfWeek }

    fun upsert(plan: PlanDay, originalDayOfWeek: Int? = null) {
        val plans = getPlans().toMutableList()
        if (originalDayOfWeek != null && originalDayOfWeek != plan.dayOfWeek) {
            plans.removeAll { it.dayOfWeek == originalDayOfWeek }
        }
        val index = plans.indexOfFirst { it.dayOfWeek == plan.dayOfWeek }
        if (index >= 0) plans[index] = plan else plans.add(plan)
        savePlans(plans)
    }

    fun delete(dayOfWeek: Int) {
        savePlans(getPlans().filterNot { it.dayOfWeek == dayOfWeek })
    }

    private fun savePlans(plans: List<PlanDay>) {
        val array = JSONArray()
        plans.sortedBy { (it.dayOfWeek + 5) % 7 }.forEach { plan ->
            val exerciseArray = JSONArray()
            plan.exercises.forEach { exercise ->
                exerciseArray.put(
                    JSONObject()
                        .put("name", exercise.name)
                        .put("sets", exercise.sets)
                        .put("reps", exercise.reps)
                )
            }
            array.put(
                JSONObject()
                    .put("dayOfWeek", plan.dayOfWeek)
                    .put("focus", plan.focus)
                    .put("exercises", exerciseArray)
                    .put(
                        "cardio", JSONObject()
                            .put("minutes", plan.cardio.minutes)
                            .put("incline", plan.cardio.incline)
                            .put("speed", plan.cardio.speed)
                            .put("type", CardioType.normalize(plan.cardio.type))
                    )
                    .put("reminderHour", plan.reminderHour)
                    .put("reminderMinute", plan.reminderMinute)
            )
        }
        prefs.edit().putString(KEY_PLANS, array.toString()).apply()
    }

    private companion object {
        const val NAME = "fitplan_plans"
        const val KEY_PLANS = "plans_json"
    }
}
