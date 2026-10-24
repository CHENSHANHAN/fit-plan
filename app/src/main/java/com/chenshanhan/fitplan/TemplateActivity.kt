package com.chenshanhan.fitplan

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class TemplateActivity : AppCompatActivity() {

    private lateinit var repository: PlanRepository
    private lateinit var templateContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_templates)
        repository = PlanRepository(this)
        templateContainer = findViewById(R.id.templateContainer)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        renderTemplates()
    }

    private fun renderTemplates() {
        templateContainer.removeAllViews()
        for (template in PlanData.templates) {
            val root = layoutInflater.inflate(
                R.layout.item_template, templateContainer, false
            )
            root.findViewById<TextView>(R.id.templateTitle).text =
                "${template.dayName} · ${template.focus}"
            root.findViewById<TextView>(R.id.templateTime).text =
                "提醒 ${template.reminderTimeText}"
            root.findViewById<TextView>(R.id.templateExercises).text =
                template.exercises.joinToString("\n") {
                    "${it.name}  ${it.sets}×${it.reps}"
                }
            root.findViewById<TextView>(R.id.templateCardio).text =
                "爬坡 ${template.cardio.minutes} 分钟（坡度 ${template.cardio.incline}，速度 ${template.cardio.speed}）"

            val addButton = root.findViewById<MaterialButton>(R.id.btnAddTemplate)
            addButton.text = if (repository.hasPlan(template.dayOfWeek)) "替换" else "一键添加"
            addButton.setOnClickListener {
                if (repository.hasPlan(template.dayOfWeek)) {
                    confirmReplace(template)
                } else {
                    addTemplate(template)
                }
            }
            templateContainer.addView(root)
        }
    }

    private fun confirmReplace(template: PlanDay) {
        AlertDialog.Builder(this)
            .setTitle("替换${template.dayName}计划？")
            .setMessage("当前${template.dayName}的自定义内容将被“${template.focus}”模板覆盖。")
            .setNegativeButton("取消", null)
            .setPositiveButton("替换") { _, _ -> addTemplate(template) }
            .show()
    }

    private fun addTemplate(template: PlanDay) {
        repository.upsert(template)
        ReminderScheduler.cancelAll(this)
        if (Prefs.isNotificationEnabled(this)) {
            ReminderScheduler.scheduleAll(this)
        }
        Toast.makeText(this, "已添加${template.dayName} · ${template.focus}", Toast.LENGTH_SHORT)
            .show()
        renderTemplates()
    }
}
