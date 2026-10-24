package com.chenshanhan.fitplan

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.util.Locale
import kotlin.math.max

class WorkoutActivity : AppCompatActivity() {

    private lateinit var repository: TrainingLogRepository
    private lateinit var plan: PlanDay
    private lateinit var recordDate: String
    private lateinit var exerciseContainer: LinearLayout
    private lateinit var strengthSectionTitle: TextView
    private lateinit var workoutSummary: TextView
    private lateinit var workoutProgressText: TextView
    private lateinit var overallProgress: ProgressBar
    private lateinit var cardioCard: View
    private lateinit var cardioTitle: TextView
    private lateinit var cardioPlan: TextView
    private lateinit var cardioTimerText: TextView
    private lateinit var cardioTimerStatus: TextView
    private lateinit var cardioProgress: ProgressBar
    private lateinit var btnTimer: MaterialButton
    private lateinit var btnCompleteCardio: MaterialButton

    private var cardioElapsedSeconds = 0L
    private var cardioCompleted = false
    private var timerStartedAt = 0L
    private var timerRunning = false

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerTicker = object : Runnable {
        override fun run() {
            renderCardio()
            if (timerRunning && !cardioCompleted) {
                timerHandler.postDelayed(this, 1_000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)
        repository = TrainingLogRepository(this)

        val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1)
        val selectedPlan = PlanRepository(this).getPlans()
            .firstOrNull { it.dayOfWeek == dayOfWeek }
        if (selectedPlan == null) {
            Toast.makeText(this, "当日训练计划已调整", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        plan = selectedPlan
        recordDate = repository.today()

        exerciseContainer = findViewById(R.id.workoutExerciseContainer)
        strengthSectionTitle = findViewById(R.id.strengthSectionTitle)
        workoutSummary = findViewById(R.id.workoutSummary)
        workoutProgressText = findViewById(R.id.workoutProgressText)
        overallProgress = findViewById(R.id.overallProgress)
        cardioCard = findViewById(R.id.cardioCard)
        cardioTitle = findViewById(R.id.workoutCardioTitle)
        cardioPlan = findViewById(R.id.workoutCardioPlan)
        cardioTimerText = findViewById(R.id.cardioTimerText)
        cardioTimerStatus = findViewById(R.id.cardioTimerStatus)
        cardioProgress = findViewById(R.id.cardioProgress)
        btnTimer = findViewById(R.id.btnTimer)
        btnCompleteCardio = findViewById(R.id.btnCompleteCardio)

        findViewById<TextView>(R.id.workoutPlanTitle).text =
            "${plan.dayName} · ${plan.focus}"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        btnTimer.setOnClickListener { toggleTimer() }
        btnCompleteCardio.setOnClickListener { completeCardio() }
    }

    override fun onResume() {
        super.onResume()
        if (::plan.isInitialized) renderSession()
    }

    override fun onPause() {
        persistCardioSession()
        timerHandler.removeCallbacks(timerTicker)
        super.onPause()
    }

    private fun renderSession() {
        val log = repository.getLog(recordDate, plan.dayOfWeek)
        restoreCardioState(log)
        renderExercises(log)
        renderCardio()
        renderOverallProgress(log)
        updateTicker()
    }

    private fun renderOverallProgress(log: TrainingLog?) {
        val totalSets = plan.exercises.sumOf { it.sets }
        val completedSets = plan.exercises.sumOf { exercise ->
            log?.exercises?.firstOrNull { it.name == exercise.name }
                ?.completedSets
                ?.coerceIn(0, exercise.sets)
                ?: 0
        }
        val hasCardio = plan.cardio.minutes > 0
        overallProgress.max = (totalSets + if (hasCardio) 1 else 0).coerceAtLeast(1)
        overallProgress.progress = completedSets + if (hasCardio && cardioCompleted) 1 else 0

        val strengthText = if (totalSets > 0) {
            "力量 $completedSets / $totalSets 组"
        } else {
            "无力量动作"
        }
        val cardioText = when {
            !hasCardio -> "无有氧安排"
            cardioCompleted -> "有氧已完成"
            timerRunning -> "有氧计时中"
            cardioElapsedSeconds > 0 -> "有氧已记录 ${formatDuration(cardioElapsedSeconds)}"
            else -> "有氧未开始"
        }
        workoutSummary.text = "$strengthText · $cardioText"
        workoutProgressText.text = if (totalSets > 0) {
            "已完成 $completedSets / $totalSets 组"
        } else if (hasCardio) {
            if (cardioCompleted) "有氧项目已完成" else "完成有氧后点击“完成此项”"
        } else {
            "暂无训练项目"
        }
    }

    private fun renderExercises(log: TrainingLog?) {
        exerciseContainer.removeAllViews()
        strengthSectionTitle.visibility = if (plan.exercises.isEmpty()) View.GONE else View.VISIBLE
        for (exercise in plan.exercises) {
            val exerciseLog = log?.exercises?.firstOrNull { it.name == exercise.name }
            val completedSets = (exerciseLog?.completedSets ?: 0).coerceIn(0, exercise.sets)
            val completed = completedSets >= exercise.sets
            val root = layoutInflater.inflate(
                R.layout.item_workout_exercise, exerciseContainer, false
            )
            root.findViewById<TextView>(R.id.workoutExerciseName).text = exercise.name
            root.findViewById<TextView>(R.id.workoutExercisePlan).text =
                "${exercise.sets} 组 × ${exercise.reps} 次"
            root.findViewById<TextView>(R.id.workoutExerciseStatus).text =
                if (completed) "已完成 ✓" else "进行中"
            val progress = root.findViewById<ProgressBar>(R.id.exerciseSetProgress)
            progress.max = exercise.sets
            progress.progress = completedSets
            root.findViewById<TextView>(R.id.exerciseSetProgressText).text =
                "$completedSets / ${exercise.sets} 组"

            val weightInput = root.findViewById<EditText>(R.id.workoutExerciseWeight)
            weightInput.setText(exerciseLog?.weight.orEmpty())
            weightInput.setSelection(weightInput.text.length)
            weightInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) = Unit

                override fun afterTextChanged(s: Editable?) {
                    repository.updateWeight(
                        recordDate, plan, exercise.name, s?.toString().orEmpty()
                    )
                }
            })

            val btnRecordSet = root.findViewById<MaterialButton>(R.id.btnRecordSet)
            btnRecordSet.text = if (completed) "全部完成" else "完成一组"
            btnRecordSet.isEnabled = !completed
            btnRecordSet.alpha = if (completed) 0.5f else 1f
            btnRecordSet.setOnClickListener {
                repository.recordSet(recordDate, plan, exercise.name)
                renderSession()
            }

            val btnComplete = root.findViewById<MaterialButton>(R.id.btnCompleteExercise)
            btnComplete.text = if (completed) "已完成 ✓" else "完成此项"
            btnComplete.isEnabled = !completed
            btnComplete.alpha = if (completed) 0.65f else 1f
            btnComplete.setOnClickListener {
                repository.completeExercise(recordDate, plan, exercise.name)
                renderSession()
            }
            exerciseContainer.addView(root)
        }
    }

    private fun restoreCardioState(log: TrainingLog?) {
        cardioElapsedSeconds = log?.cardioElapsedSeconds ?: 0L
        cardioCompleted = log?.cardioCompleted ?: false
        timerStartedAt = log?.cardioTimerStartedAt ?: 0L
        timerRunning = (log?.cardioTimerRunning ?: false) &&
            timerStartedAt > 0L && !cardioCompleted
        if (timerRunning) cardioElapsedSeconds = currentElapsedSeconds()
    }

    private fun renderCardio() {
        if (!::plan.isInitialized) return
        if (plan.cardio.minutes <= 0) {
            cardioCard.visibility = View.GONE
            return
        }
        cardioCard.visibility = View.VISIBLE
        cardioTitle.text = "有氧 · ${CardioType.label(plan.cardio.type)}"
        cardioPlan.text = cardioDescription(plan.cardio)
        val elapsed = currentElapsedSeconds()
        cardioTimerText.text = formatDuration(elapsed)
        cardioProgress.max = (plan.cardio.minutes * 60).coerceAtLeast(1)
        cardioProgress.progress = if (cardioCompleted) {
            cardioProgress.max
        } else {
            elapsed.coerceAtMost(cardioProgress.max.toLong()).toInt()
        }
        cardioTimerStatus.text = when {
            cardioCompleted -> "已完成 · 实际计时 ${formatDuration(elapsed)}"
            timerRunning -> "计时中 · 离开本页也会继续"
            elapsed > 0 -> "已暂停 · 实际计时 ${formatDuration(elapsed)}"
            else -> "目标 ${plan.cardio.minutes} 分钟 · 准备好后开始计时"
        }
        btnTimer.text = when {
            cardioCompleted -> "计时已完成"
            timerRunning -> "暂停计时"
            elapsed > 0 -> "继续计时"
            else -> "开始计时"
        }
        btnTimer.isEnabled = !cardioCompleted
        btnTimer.alpha = if (cardioCompleted) 0.5f else 1f
        btnCompleteCardio.text = if (cardioCompleted) "已完成 ✓" else "完成此项"
        btnCompleteCardio.isEnabled = !cardioCompleted
        btnCompleteCardio.alpha = if (cardioCompleted) 0.65f else 1f
    }

    private fun toggleTimer() {
        if (cardioCompleted) return
        if (timerRunning) {
            cardioElapsedSeconds = currentElapsedSeconds()
            timerRunning = false
        } else {
            timerStartedAt = System.currentTimeMillis() - cardioElapsedSeconds * 1_000L
            timerRunning = true
        }
        persistCardioSession()
        renderCardio()
        renderOverallProgress(repository.getLog(recordDate, plan.dayOfWeek))
        updateTicker()
    }

    private fun completeCardio() {
        if (cardioCompleted) return
        cardioElapsedSeconds = currentElapsedSeconds()
        timerRunning = false
        cardioCompleted = true
        persistCardioSession()
        timerHandler.removeCallbacks(timerTicker)
        renderCardio()
        renderOverallProgress(repository.getLog(recordDate, plan.dayOfWeek))
    }

    private fun persistCardioSession() {
        if (!::plan.isInitialized || !::recordDate.isInitialized) return
        cardioElapsedSeconds = currentElapsedSeconds()
        repository.updateCardioSession(
            date = recordDate,
            plan = plan,
            elapsedSeconds = cardioElapsedSeconds,
            completed = cardioCompleted,
            timerStartedAt = timerStartedAt,
            timerRunning = timerRunning
        )
    }

    private fun currentElapsedSeconds(): Long {
        if (!timerRunning || timerStartedAt <= 0L) return cardioElapsedSeconds
        val elapsed = (System.currentTimeMillis() - timerStartedAt) / 1_000L
        return max(cardioElapsedSeconds, elapsed)
    }

    private fun updateTicker() {
        timerHandler.removeCallbacks(timerTicker)
        if (timerRunning && !cardioCompleted) timerHandler.post(timerTicker)
    }

    private fun formatDuration(totalSeconds: Long): String {
        val seconds = totalSeconds.coerceAtLeast(0L)
        val hours = seconds / 3_600
        val minutes = (seconds % 3_600) / 60
        val remainder = seconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, remainder)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, remainder)
        }
    }

    companion object {
        const val EXTRA_DAY_OF_WEEK = "workout_day_of_week"
    }
}
