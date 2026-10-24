package com.chenshanhan.fitplan

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 训练历史、实际重量、组数进度与有氧计时的本地存储。 */
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

    /** 记录一组；达到计划组数后，该动作自动标记为已完成。 */
    fun recordSet(date: String, plan: PlanDay, exerciseName: String): TrainingLog {
        val base = editableLog(date, plan)
        val updated = base.copy(
            exercises = base.exercises.map {
                if (it.name == exerciseName) {
                    val nextSets = (it.completedSets + 1).coerceAtMost(it.sets)
                    it.copy(
                        completedSets = nextSets,
                        completed = nextSets >= it.sets
                    )
                } else {
                    it
                }
            },
            updatedAt = System.currentTimeMillis()
        )
        saveOrDelete(updated)
        return updated
    }

    /** 防止训练中漏记组数，可一次性将整个动作标记为完成。 */
    fun completeExercise(date: String, plan: PlanDay, exerciseName: String): TrainingLog {
        val base = editableLog(date, plan)
        val updated = base.copy(
            exercises = base.exercises.map {
                if (it.name == exerciseName) {
                    it.copy(completedSets = it.sets, completed = true)
                } else {
                    it
                }
            },
            updatedAt = System.currentTimeMillis()
        )
        saveOrDelete(updated)
        return updated
    }

    /** 保存有氧实际秒数、完成状态和计时起点；旧历史不受新字段影响。 */
    fun updateCardioSession(
        date: String,
        plan: PlanDay,
        elapsedSeconds: Long,
        completed: Boolean,
        timerStartedAt: Long,
        timerRunning: Boolean
    ): TrainingLog {
        val base = editableLog(date, plan)
        val updated = base.copy(
            cardioElapsedSeconds = elapsedSeconds.coerceAtLeast(0L),
            cardioCompleted = completed,
            cardioTimerStartedAt = timerStartedAt,
            cardioTimerRunning = timerRunning,
            updatedAt = System.currentTimeMillis()
        )
        saveOrDelete(updated)
        return updated
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
                val old = existing.exercises.firstOrNull { it.name == current.name }
                val oldCompletedSets = old?.let {
                    if (it.completed) it.sets else it.completedSets
                } ?: 0
                val completedSets = oldCompletedSets.coerceIn(0, current.sets)
                current.copy(
                    weight = old?.weight.orEmpty(),
                    completedSets = completedSets,
                    completed = current.sets > 0 && completedSets >= current.sets
                )
            },
            cardioElapsedSeconds = existing.cardioElapsedSeconds,
            cardioCompleted = existing.cardioCompleted,
            cardioTimerStartedAt = existing.cardioTimerStartedAt,
            cardioTimerRunning = existing.cardioTimerRunning
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
        val hasExerciseData = log.exercises.any {
            it.weight.isNotBlank() || it.completedSets > 0 || it.completed
        }
        val hasCardioData = log.cardioElapsedSeconds > 0 || log.cardioCompleted ||
            log.cardioTimerRunning || log.cardioTimerStartedAt > 0
        if (!log.completed && !hasExerciseData && !hasCardioData) {
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
                    .put("completedSets", exercise.completedSets)
                    .put("completed", exercise.completed)
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
                    .put("type", CardioType.normalize(log.cardio.type))
            )
            .put("completed", log.completed)
            .put("updatedAt", log.updatedAt)
            .put("cardioElapsedSeconds", log.cardioElapsedSeconds)
            .put("cardioCompleted", log.cardioCompleted)
            .put("cardioTimerStartedAt", log.cardioTimerStartedAt)
            .put("cardioTimerRunning", log.cardioTimerRunning)
        prefs.edit().putString(key, obj.toString()).apply()
    }

    private fun parseLog(obj: JSONObject): TrainingLog {
        val exercises = mutableListOf<ExerciseLog>()
        val exerciseArray = obj.optJSONArray("exercises") ?: org.json.JSONArray()
        for (i in 0 until exerciseArray.length()) {
            val item = exerciseArray.getJSONObject(i)
            val sets = item.optInt("sets", 0)
            val completedSets = item.optInt("completedSets", 0).coerceIn(0, sets)
            exercises.add(
                ExerciseLog(
                    name = item.optString("name", ""),
                    sets = sets,
                    reps = item.optString("reps", ""),
                    weight = item.optString("weight", ""),
                    completedSets = completedSets,
                    completed = item.optBoolean("completed", false) ||
                        (sets > 0 && completedSets >= sets)
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
                cardio.optString("speed", ""),
                CardioType.normalize(cardio.optString("type", CardioType.INCLINE))
            ),
            completed = obj.optBoolean("completed", false),
            updatedAt = obj.optLong("updatedAt", 0L),
            cardioElapsedSeconds = obj.optLong("cardioElapsedSeconds", 0L),
            cardioCompleted = obj.optBoolean("cardioCompleted", false),
            cardioTimerStartedAt = obj.optLong("cardioTimerStartedAt", 0L),
            cardioTimerRunning = obj.optBoolean("cardioTimerRunning", false)
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
