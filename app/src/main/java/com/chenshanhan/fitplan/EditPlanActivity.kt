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
    private lateinit var editCardioMinutes: EditText
    private lateinit var editCardioIncline: EditText
    private lateinit var editCardioSpeed: EditText

    private val exerciseRows = mutableListOf<ExerciseRow>()
    private var originalDayOfWeek: Int? = null
    private var reminderHour = 18
    private var reminderMinute = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_plan)
        repository = PlanRepository(this)

        spinnerDay = findViewById(R.id.spinnerDay)
        editFocus = findViewById(R.id.editFocus)
        btnTime = findViewById(R.id.btnTime)
        exerciseContainer = findViewById(R.id.exerciseEditorContainer)
        editCardioMinutes = findViewById(R.id.editCardioMinutes)
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
            editCardioMinutes.setText(existing.cardio.minutes.toString())
            editCardioIncline.setText(existing.cardio.incline)
            editCardioSpeed.setText(existing.cardio.speed)
            existing.exercises.forEach { addExerciseRow(it) }
        } else {
            val currentPlans = repository.getPlans()
            val firstAvailable = PlanData.weekOrder.firstOrNull { dow ->
                currentPlans.none { it.dayOfWeek == dow }
            } ?: PlanData.weekOrder.first()
            spinnerDay.setSelection(PlanData.weekOrder.indexOf(firstAvailable))
            editCardioMinutes.setText("20")
            editCardioIncline.setText("10")
            editCardioSpeed.setText("5")
            addExerciseRow(null)
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
        if (focus.isEmpty()) {
            editFocus.error = "请输入训练部位或计划名称"
            editFocus.requestFocus()
            return
        }

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
        if (exercises.isEmpty()) {
            Toast.makeText(this, "请至少添加一个训练动作", Toast.LENGTH_SHORT).show()
            return
        }

        val minutes = editCardioMinutes.text.toString().trim().toIntOrNull()
        if (minutes == null || minutes < 0) {
            editCardioMinutes.error = "请输入分钟数，无有氧可填 0"
            editCardioMinutes.requestFocus()
            return
        }
        val incline = editCardioIncline.text.toString().trim()
        val speed = editCardioSpeed.text.toString().trim()
        if (minutes > 0 && incline.isEmpty()) {
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

        val plan = PlanDay(
            dayOfWeek = dayOfWeek,
            focus = focus,
            exercises = exercises,
            cardio = Cardio(minutes, incline, speed),
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

    companion object {
        const val EXTRA_DAY_OF_WEEK = "day_of_week"
    }
}
