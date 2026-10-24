package com.chenshanhan.fitplan

/** 某个训练动作在当天的实际完成情况。weight 为空表示尚未录入重量。 */
data class ExerciseLog(
    val name: String,
    val sets: Int,
    val reps: String,
    val weight: String = "",
    val completedSets: Int = 0,
    val completed: Boolean = false
)

/** 某一天的训练快照，保存后不再随计划模板的修改而变化。 */
data class TrainingLog(
    val date: String,       // yyyy-MM-dd
    val dayOfWeek: Int,
    val focus: String,
    val exercises: List<ExerciseLog>,
    val cardio: Cardio,
    val completed: Boolean,
    val updatedAt: Long,
    val cardioElapsedSeconds: Long = 0L,
    val cardioCompleted: Boolean = false,
    val cardioTimerStartedAt: Long = 0L,
    val cardioTimerRunning: Boolean = false
)
