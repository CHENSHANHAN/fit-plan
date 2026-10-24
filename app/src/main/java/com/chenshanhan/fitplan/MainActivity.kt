package com.chenshanhan.fitplan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: DayAdapter
    private lateinit var switchNotif: SwitchMaterial
    private lateinit var weekProgress: TextView
    private lateinit var reminderSummary: TextView
    private lateinit var emptyText: TextView
    private lateinit var recycler: RecyclerView
    private var plans: List<PlanDay> = emptyList()

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && Prefs.isNotificationEnabled(this)) {
                ReminderScheduler.scheduleAll(this)
                Toast.makeText(this, "提醒已开启", Toast.LENGTH_SHORT).show()
            } else if (!granted) {
                Toast.makeText(
                    this, "未授予通知权限，提醒无法正常显示", Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ReminderScheduler.ensureChannel(this)

        switchNotif = findViewById(R.id.switchNotif)
        weekProgress = findViewById(R.id.weekProgress)
        reminderSummary = findViewById(R.id.reminderSummary)
        emptyText = findViewById(R.id.emptyText)
        recycler = findViewById(R.id.recycler)

        adapter = DayAdapter(
            days = emptyList(),
            onChanged = { refreshHeader() },
            onEdit = { openEditor(it.dayOfWeek) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<View>(R.id.btnAddPlan).setOnClickListener {
            startActivity(Intent(this, EditPlanActivity::class.java))
        }
        findViewById<View>(R.id.btnTemplates).setOnClickListener {
            startActivity(Intent(this, TemplateActivity::class.java))
        }

        switchNotif.isChecked = Prefs.isNotificationEnabled(this)
        switchNotif.setOnCheckedChangeListener { _, checked ->
            Prefs.setNotificationEnabled(this, checked)
            if (checked) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    ReminderScheduler.scheduleAll(this)
                    Toast.makeText(this, "提醒已开启", Toast.LENGTH_SHORT).show()
                }
            } else {
                ReminderScheduler.cancelAll(this)
                Toast.makeText(this, "提醒已关闭", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reloadPlans()
    }

    private fun openEditor(dayOfWeek: Int) {
        startActivity(
            Intent(this, EditPlanActivity::class.java)
                .putExtra(EditPlanActivity.EXTRA_DAY_OF_WEEK, dayOfWeek)
        )
    }

    private fun reloadPlans() {
        plans = PlanRepository(this).getPlans()
        adapter.updatePlans(plans)
        emptyText.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE
        recycler.visibility = if (plans.isEmpty()) View.GONE else View.VISIBLE
        reminderSummary.text = if (plans.isEmpty()) {
            "添加计划后可开启定时提醒"
        } else {
            plans.joinToString(" · ") { "${it.dayName} ${it.reminderTimeText}" }
        }
        refreshHeader()
    }

    private fun refreshHeader() {
        weekProgress.text = if (plans.isEmpty()) {
            "本周还没有训练计划"
        } else {
            "本周已完成 ${Prefs.weekDoneCount(this, plans)} / ${plans.size} 次打卡"
        }
    }
}
