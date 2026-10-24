package com.chenshanhan.fitplan

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class EditPlanActivity : AppCompatActivity() {

    private data class ExerciseRow(
        val root: View,
        val name: EditText,
        val sets: EditText,
        val reps: EditText
    )

    private lateinit var repository: PlanRepository
    private lateinit var spinnerDay: Spinner
    private lateinit var editFocus: EditText
    private lateinit var btnTime: MaterialButton
    private lateinit var exerciseContainer: LinearLayout
    private lateinit var cardioTypeGroup: MaterialButtonToggleGroup
    private lateinit var cardioConfigTitle: TextView
    private lateinit var cardioFieldsContainer: LinearLayout
    private lateinit var editCardioMinutes: EditText
    private lateinit var inclineFieldContainer: LinearLayout
    private lateinit var speedFieldContainer: LinearLayout
    private lateinit var editCardioIncline: EditText
    private lateinit var editCardioSpeed: EditText

    private val exerciseRows = mutableListOf<ExerciseRow>()
    private var originalDayOfWeek: Int? = null
    private var reminderHour = 18
    private var reminderMinute = 30
    private var cardioType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_plan)
        repository = PlanRepository(this)

        spinnerDay = findViewById(R.id.spinnerDay)
        editFocus = findViewById(R.id.editFocus)
        btnTime = findViewById(R.id.btnTime)
        exerciseContainer = findViewById(R.id.exerciseEditorContainer)
        cardioTypeGroup = findViewById(R.id.cardioTypeGroup)
        cardioConfigTitle = findViewById(R.id.cardioConfigTitle)
        cardioFieldsContainer = findViewById(R.id.cardioFieldsContainer)
        editCardioMinutes = findViewById(R.id.editCardioMinutes)
        inclineFieldContainer = findViewById(R.id.inclineFieldContainer)
        speedFieldContainer = findViewById(R.id.speedFieldContainer)
        editCardioIncline = findViewById(R.id.editCardioIncline)
        editCardioSpeed = findViewById(R.id.editCardioSpeed)
        val btnDelete = findViewById<MaterialButton>(R.id.btnDelete)

        originalDayOfWeek = if (intent.hasExtra(EXTRA_DAY_OF_WEEK)) {
            intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1)
        } else {
            null
        }
        val existing = originalDayOfWeek?.let { dow ->
            repository.getPlans().firstOrNull { it.dayOfWeek == dow }
        }

        findViewById<TextView>(R.id.editTitle).text =
            if (existing == null) "新增训练计划" else "编辑训练计划"
        btnDelete.visibility = if (existing == null) View.GONE else View.VISIBLE

        val dayNames = PlanData.weekOrder.map { weekdayName(it) }
        spinnerDay.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, dayNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        if (existing != null) {
            spinnerDay.setSelection(PlanData.weekOrder.indexOf(existing.dayOfWeek))
            editFocus.setText(existing.focus)
            reminderHour = existing.reminderHour
            reminderMinute = existing.reminderMinute
            cardioType = if (existing.cardio.minutes > 0) {
                CardioType.normalize(existing.cardio.type)
            } else {
                null
            }
            editCardioMinutes.setText(
                if (existing.cardio.minutes > 0) existing.cardio.minutes.toString() else ""
            )
            editCardioIncline.setText(existing.cardio.incline)
            editCardioSpeed.setText(existing.cardio.speed)
            existing.exercises.forEach { addExerciseRow(it) }
        } else {
            val currentPlans = repository.getPlans()
            val firstAvailable = PlanData.weekOrder.firstOrNull { dow ->
                currentPlans.none { it.dayOfWeek == dow }
            } ?: PlanData.weekOrder.first()
            spinnerDay.setSelection(PlanData.weekOrder.indexOf(firstAvailable))
            editCardioMinutes.setText("")
            editCardioIncline.setText("10")
            editCardioSpeed.setText("5")
            addExerciseRow(null)
        }

        cardioType?.let { cardioTypeGroup.check(cardioButtonId(it)) }
        updateCardioTypeUi()
        cardioTypeGroup.addOnButtonCheckedListener { _, _, _ ->
            val checkedId = cardioTypeGroup.checkedButtonId
            cardioType = if (checkedId == View.NO_ID) null else cardioTypeForButton(checkedId)
            updateCardioTypeUi()
        }
        updateTimeText()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        btnTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    reminderHour = hour
                    reminderMinute = minute
                    updateTimeText()
                },
                reminderHour, reminderMinute, true
            ).show()
        }
        findViewById<View>(R.id.btnAddExercise).setOnClickListener {
            addExerciseRow(null)
        }
        findViewById<View>(R.id.btnSave).setOnClickListener { savePlan() }
        btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun updateTimeText() {
        btnTime.text = String.format("提醒时间  %02d:%02d", reminderHour, reminderMinute)
    }

    private fun cardioButtonId(type: String): Int = when (CardioType.normalize(type)) {
        CardioType.RUNNING -> R.id.btnCardioRunning
        CardioType.CYCLING -> R.id.btnCardioCycling
        else -> R.id.btnCardioIncline
    }

    private fun cardioTypeForButton(buttonId: Int): String = when (buttonId) {
        R.id.btnCardioRunning -> CardioType.RUNNING
        R.id.btnCardioCycling -> CardioType.CYCLING
        else -> CardioType.INCLINE
    }

    private fun updateCardioTypeUi() {
        val type = cardioType
        cardioFieldsContainer.visibility = if (type == null) View.GONE else View.VISIBLE
        if (type == null) {
            cardioConfigTitle.text = "有氧训练（可选）"
            return
        }

        val isIncline = type == CardioType.INCLINE
        cardioConfigTitle.text = "有氧 · ${CardioType.label(type)}"
        inclineFieldContainer.visibility = if (isIncline) View.VISIBLE else View.GONE
        val speedParams = speedFieldContainer.layoutParams as LinearLayout.LayoutParams
        speedParams.marginStart = if (isIncline) dp(12) else 0
        speedFieldContainer.layoutParams = speedParams
        editCardioSpeed.hint = when (type) {
            CardioType.RUNNING -> "请输入跑步速度"
            CardioType.CYCLING -> "请输入骑车速度"
            else -> "例如：4.5-5"
        }
    }

    private fun addExerciseRow(exercise: Exercise?) {
        val root = layoutInflater.inflate(
            R.layout.item_exercise_editor, exerciseContainer, false
        )
        val name = root.findViewById<EditText>(R.id.editExerciseName)
        val sets = root.findViewById<EditText>(R.id.editExerciseSets)
        val reps = root.findViewById<EditText>(R.id.editExerciseReps)
        if (exercise != null) {
            name.setText(exercise.name)
            sets.setText(exercise.sets.toString())
            reps.setText(exercise.reps)
        }
        val row = ExerciseRow(root, name, sets, reps)
        root.findViewById<View>(R.id.btnRemoveExercise).setOnClickListener {
            exerciseRows.remove(row)
            exerciseContainer.removeView(root)
        }
        exerciseRows.add(row)
        exerciseContainer.addView(root)
    }

    private fun savePlan() {
        val focus = editFocus.text.toString().trim()
        val exercises = mutableListOf<Exercise>()
        for (row in exerciseRows) {
            val name = row.name.text.toString().trim()
            val setsText = row.sets.text.toString().trim()
            val reps = row.reps.text.toString().trim()
            if (name.isEmpty() && setsText.isEmpty() && reps.isEmpty()) continue
            if (name.isEmpty()) {
                row.name.error = "请输入动作名称"
                row.name.requestFocus()
                return
            }
            val sets = setsText.toIntOrNull()
            if (sets == null || sets <= 0) {
                row.sets.error = "请输入大于 0 的组数"
                row.sets.requestFocus()
                return
            }
            if (reps.isEmpty()) {
                row.reps.error = "请输入次数，如 8-12"
                row.reps.requestFocus()
                return
            }
            exercises.add(Exercise(name, sets, reps))
        }

        val selectedCardioType = cardioType
        val minutesText = editCardioMinutes.text.toString().trim()
        val minutes = if (selectedCardioType == null || minutesText.isEmpty()) {
            0
        } else {
            minutesText.toIntOrNull()
        }
        if (minutes == null || minutes < 0) {
            editCardioMinutes.error = "请输入分钟数，无有氧可填 0 或留空"
            editCardioMinutes.requestFocus()
            return
        }
        val incline = if (selectedCardioType == CardioType.INCLINE) {
            editCardioIncline.text.toString().trim()
        } else {
            ""
        }
        val speed = editCardioSpeed.text.toString().trim()
        if (minutes > 0 && selectedCardioType == CardioType.INCLINE && incline.isEmpty()) {
            editCardioIncline.error = "请输入坡度"
            editCardioIncline.requestFocus()
            return
        }
        if (minutes > 0 && speed.isEmpty()) {
            editCardioSpeed.error = "请输入速度"
            editCardioSpeed.requestFocus()
            return
        }

        val dayOfWeek = PlanData.weekOrder[spinnerDay.selectedItemPosition]
        if (dayOfWeek != originalDayOfWeek && repository.hasPlan(dayOfWeek)) {
            Toast.makeText(
                this, "${weekdayName(dayOfWeek)}已有计划，请直接编辑那一天", Toast.LENGTH_LONG
            ).show()
            return
        }

        if (exercises.isEmpty() && minutes == 0) {
            repository.delete(dayOfWeek)
            originalDayOfWeek?.takeIf { it != dayOfWeek }?.let { repository.delete(it) }
            rescheduleReminders()
            Toast.makeText(this, "已保存，当日无训练计划", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (focus.isEmpty()) {
            editFocus.error = "请输入训练部位或计划名称"
            editFocus.requestFocus()
            return
        }

        val cardio = if (selectedCardioType == null) {
            Cardio(0, "", "")
        } else {
            Cardio(minutes, incline, speed, selectedCardioType)
        }
        val plan = PlanDay(
            dayOfWeek = dayOfWeek,
            focus = focus,
            exercises = exercises,
            cardio = cardio,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute
        )
        repository.upsert(plan, originalDayOfWeek)
        rescheduleReminders()
        Toast.makeText(this, "训练计划已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmDelete() {
        val dow = originalDayOfWeek ?: return
        AlertDialog.Builder(this)
            .setTitle("删除训练计划？")
            .setMessage("将删除${weekdayName(dow)}的计划和对应提醒，本周打卡记录会保留。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                repository.delete(dow)
                rescheduleReminders()
                Toast.makeText(this, "计划已删除", Toast.LENGTH_SHORT).show()
                finish()
            }
            .show()
    }

    private fun rescheduleReminders() {
        ReminderScheduler.cancelAll(this)
        if (Prefs.isNotificationEnabled(this)) {
            ReminderScheduler.scheduleAll(this)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_DAY_OF_WEEK = "day_of_week"
    }
}
