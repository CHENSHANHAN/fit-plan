package com.chenshanhan.fitplan

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 训练历史与实际重量的本地存储。 */
class TrainingLogRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun today(): String = dateFormat().format(Calendar.getInstance().time)

    fun getLog(date: String, dayOfWeek: Int): TrainingLog? {
        val raw = prefs.getString(logKey(date, dayOfWeek), null) ?: return null
        return try {
            parseLog(JSONObject(raw))
        } catch (e: Exception) {
            null
        }
    }

    fun updateWeight(
        date: String,
        plan: PlanDay,
        exerciseName: String,
        weight: String
    ) {
        val normalized = weight.trim()
        // 输入过程中的“.”等中间状态不写入，保留上一次有效值。
        if (normalized.isNotEmpty() && normalized.toDoubleOrNull() == null) return
        val base = editableLog(date, plan)
        val updated = base.copy(
            exercises = base.exercises.map {
                if (it.name == exerciseName) it.copy(weight = normalized) else it
            },
            updatedAt = System.currentTimeMillis()
        )
        saveOrDelete(updated)
    }

    fun setCompleted(date: String, plan: PlanDay, completed: Boolean) {
        val base = editableLog(date, plan)
        saveOrDelete(base.copy(completed = completed, updatedAt = System.currentTimeMillis()))
    }

    fun getAllLogs(): List<TrainingLog> {
        val logs = mutableListOf<TrainingLog>()
        for ((key, value) in prefs.all) {
            if (!key.startsWith(LOG_PREFIX) || value !is String) continue
            try {
                logs.add(parseLog(JSONObject(value)))
            } catch (e: Exception) {
                // 单条损坏记录不影响其他历史展示。
            }
        }
        return logs.sortedWith(
            compareByDescending<TrainingLog> { it.date }.thenByDescending { it.updatedAt }
        )
    }

    /** 将 v1.1 及更早版本的打卡标记迁移为历史快照，原数据保持不动。 */
    fun migrateLegacyDone(plans: List<PlanDay>) {
        if (prefs.getBoolean(KEY_LEGACY_MIGRATED, false)) return
        val legacy = appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        for ((key, value) in legacy.all) {
            if (!key.startsWith("done_") || value != true) continue
            val parts = key.removePrefix("done_").split("_")
            if (parts.size != 2) continue
            val date = parts[0]
            val dayOfWeek = parts[1].toIntOrNull() ?: continue
            if (getLog(date, dayOfWeek) != null) continue
            val plan = plans.firstOrNull { it.dayOfWeek == dayOfWeek }
            val log = if (plan != null) {
                snapshot(date, plan).copy(completed = true, updatedAt = System.currentTimeMillis())
            } else {
                TrainingLog(
                    date = date,
                    dayOfWeek = dayOfWeek,
                    focus = weekdayName(dayOfWeek),
                    exercises = emptyList(),
                    cardio = Cardio(0, "", ""),
                    completed = true,
                    updatedAt = System.currentTimeMillis()
                )
            }
            saveOrDelete(log)
        }
        prefs.edit().putBoolean(KEY_LEGACY_MIGRATED, true).apply()
    }

    private fun editableLog(date: String, plan: PlanDay): TrainingLog {
        val existing = getLog(date, plan.dayOfWeek)
        val base = snapshot(date, plan)
        if (existing == null) return base
        return base.copy(
            completed = existing.completed,
            updatedAt = existing.updatedAt,
            exercises = base.exercises.map { current ->
                current.copy(
                    weight = existing.exercises.firstOrNull { it.name == current.name }
                        ?.weight.orEmpty()
                )
            }
        )
    }

    private fun snapshot(date: String, plan: PlanDay): TrainingLog = TrainingLog(
        date = date,
        dayOfWeek = plan.dayOfWeek,
        focus = plan.focus,
        exercises = plan.exercises.map {
            ExerciseLog(it.name, it.sets, it.reps)
        },
        cardio = plan.cardio,
        completed = false,
        updatedAt = System.currentTimeMillis()
    )

    private fun saveOrDelete(log: TrainingLog) {
        val key = logKey(log.date, log.dayOfWeek)
        if (!log.completed && log.exercises.all { it.weight.isBlank() }) {
            prefs.edit().remove(key).apply()
            return
        }
        val exerciseArray = org.json.JSONArray()
        log.exercises.forEach { exercise ->
            exerciseArray.put(
                JSONObject()
                    .put("name", exercise.name)
                    .put("sets", exercise.sets)
                    .put("reps", exercise.reps)
                    .put("weight", exercise.weight)
            )
        }
        val obj = JSONObject()
            .put("date", log.date)
            .put("dayOfWeek", log.dayOfWeek)
            .put("focus", log.focus)
            .put("exercises", exerciseArray)
            .put(
                "cardio", JSONObject()
                    .put("minutes", log.cardio.minutes)
                    .put("incline", log.cardio.incline)
                    .put("speed", log.cardio.speed)
            )
            .put("completed", log.completed)
            .put("updatedAt", log.updatedAt)
        prefs.edit().putString(key, obj.toString()).apply()
    }

    private fun parseLog(obj: JSONObject): TrainingLog {
        val exercises = mutableListOf<ExerciseLog>()
        val exerciseArray = obj.optJSONArray("exercises") ?: org.json.JSONArray()
        for (i in 0 until exerciseArray.length()) {
            val item = exerciseArray.getJSONObject(i)
            exercises.add(
                ExerciseLog(
                    name = item.optString("name", ""),
                    sets = item.optInt("sets", 0),
                    reps = item.optString("reps", ""),
                    weight = item.optString("weight", "")
                )
            )
        }
        val cardio = obj.optJSONObject("cardio") ?: JSONObject()
        return TrainingLog(
            date = obj.getString("date"),
            dayOfWeek = obj.getInt("dayOfWeek"),
            focus = obj.optString("focus", "训练"),
            exercises = exercises,
            cardio = Cardio(
                cardio.optInt("minutes", 0),
                cardio.optString("incline", ""),
                cardio.optString("speed", "")
            ),
            completed = obj.optBoolean("completed", false),
            updatedAt = obj.optLong("updatedAt", 0L)
        )
    }

    private fun logKey(date: String, dayOfWeek: Int) = "$LOG_PREFIX${date}_$dayOfWeek"

    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private companion object {
        const val NAME = "fitplan_history"
        const val LEGACY_PREFS = "fitplan_prefs"
        const val LOG_PREFIX = "log_"
        const val KEY_LEGACY_MIGRATED = "legacy_done_migrated"
    }
}
