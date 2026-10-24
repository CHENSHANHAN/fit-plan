package com.chenshanhan.fitplan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class PlansFragment : Fragment() {

    private lateinit var repository: PlanRepository
    private lateinit var plansSummary: TextView
    private lateinit var planContainer: LinearLayout
    private lateinit var emptyPlans: TextView
    private lateinit var templateContent: LinearLayout
    private lateinit var templateContainer: LinearLayout
    private lateinit var templateToggleLabel: TextView
    private var templatesExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_plans, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PlanRepository(requireContext())
        plansSummary = view.findViewById(R.id.plansSummary)
        planContainer = view.findViewById(R.id.planContainer)
        emptyPlans = view.findViewById(R.id.emptyPlans)
        templateContent = view.findViewById(R.id.templateContent)
        templateContainer = view.findViewById(R.id.templateContainer)
        templateToggleLabel = view.findViewById(R.id.templateToggleLabel)

        view.findViewById<View>(R.id.btnAddPlan).setOnClickListener {
            startActivity(Intent(requireContext(), EditPlanActivity::class.java))
        }
        view.findViewById<View>(R.id.templateToggle).setOnClickListener {
            toggleTemplates()
        }
        renderPlans()
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            renderPlans()
            if (templatesExpanded) renderTemplates()
        }
    }

    private fun renderPlans() {
        val plans = repository.getPlans()
        planContainer.removeAllViews()
        plansSummary.text = "已安排 ${plans.size} / 7 天"
        emptyPlans.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE

        for (plan in plans) {
            val root = layoutInflater.inflate(R.layout.item_plan_summary, planContainer, false)
            root.findViewById<TextView>(R.id.planDay).text = plan.dayName
            root.findViewById<TextView>(R.id.planFocus).text = plan.focus
            root.findViewById<TextView>(R.id.planReminder).text =
                "提醒 ${plan.reminderTimeText}"
            root.findViewById<TextView>(R.id.planExercises).text =
                plan.exercises.joinToString("\n") {
                    "${it.name}  ${it.sets}×${it.reps}"
                }
            root.findViewById<TextView>(R.id.planCardio).text = if (plan.cardio.minutes > 0) {
                "爬坡 ${plan.cardio.minutes} 分钟（坡度 ${plan.cardio.incline}，速度 ${plan.cardio.speed}）"
            } else {
                "无有氧安排"
            }
            root.findViewById<MaterialButton>(R.id.btnEditPlan).setOnClickListener {
                startActivity(
                    Intent(requireContext(), EditPlanActivity::class.java)
                        .putExtra(EditPlanActivity.EXTRA_DAY_OF_WEEK, plan.dayOfWeek)
                )
            }
            planContainer.addView(root)
        }
    }

    private fun toggleTemplates() {
        templatesExpanded = !templatesExpanded
        templateContent.visibility = if (templatesExpanded) View.VISIBLE else View.GONE
        templateToggleLabel.text = if (templatesExpanded) "收起" else "展开"
        if (templatesExpanded) renderTemplates()
    }

    private fun renderTemplates() {
        templateContainer.removeAllViews()
        for (template in PlanData.templates) {
            val root = layoutInflater.inflate(R.layout.item_template, templateContainer, false)
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
        AlertDialog.Builder(requireContext())
            .setTitle("替换${template.dayName}计划？")
            .setMessage("当前${template.dayName}的自定义内容将被“${template.focus}”模板覆盖。")
            .setNegativeButton("取消", null)
            .setPositiveButton("替换") { _, _ -> addTemplate(template) }
            .show()
    }

    private fun addTemplate(template: PlanDay) {
        repository.upsert(template)
        ReminderScheduler.cancelAll(requireContext())
        if (Prefs.isNotificationEnabled(requireContext())) {
            ReminderScheduler.scheduleAll(requireContext())
        }
        Toast.makeText(
            requireContext(), "已添加${template.dayName} · ${template.focus}", Toast.LENGTH_SHORT
        ).show()
        renderPlans()
        renderTemplates()
    }
}
