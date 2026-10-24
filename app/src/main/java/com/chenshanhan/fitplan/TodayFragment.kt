package com.chenshanhan.fitplan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar

class TodayFragment : Fragment() {

    private lateinit var adapter: DayAdapter
    private lateinit var switchNotif: SwitchMaterial
    private lateinit var reminderSummary: TextView
    private lateinit var currentDateTime: TextView
    private lateinit var emptyToday: LinearLayout
    private lateinit var recycler: RecyclerView
    private var todayPlan: PlanDay? = null

    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeTicker = object : Runnable {
        override fun run() {
            updateCurrentTime()
            timeHandler.postDelayed(this, 30_000L)
        }
    }

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && Prefs.isNotificationEnabled(requireContext())) {
                ReminderScheduler.scheduleAll(requireContext())
                Toast.makeText(requireContext(), "提醒已开启", Toast.LENGTH_SHORT).show()
            } else if (!granted) {
                Toast.makeText(
                    requireContext(), "未授予通知权限，提醒无法正常显示", Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_today, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        switchNotif = view.findViewById(R.id.switchNotif)
        reminderSummary = view.findViewById(R.id.reminderSummary)
        currentDateTime = view.findViewById(R.id.currentDateTime)
        emptyToday = view.findViewById(R.id.emptyToday)
        recycler = view.findViewById(R.id.todayRecycler)

        adapter = DayAdapter(
            days = emptyList(),
            onChanged = {},
            onOpen = { plan ->
                startActivity(
                    Intent(requireContext(), WorkoutActivity::class.java)
                        .putExtra(WorkoutActivity.EXTRA_DAY_OF_WEEK, plan.dayOfWeek)
                )
            }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<View>(R.id.btnGoPlans).setOnClickListener {
            (requireActivity() as? MainActivity)?.showPlans()
        }

        switchNotif.isChecked = Prefs.isNotificationEnabled(requireContext())
        switchNotif.setOnCheckedChangeListener { _, checked ->
            Prefs.setNotificationEnabled(requireContext(), checked)
            if (checked) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    requireActivity().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    ReminderScheduler.scheduleAll(requireContext())
                    Toast.makeText(requireContext(), "提醒已开启", Toast.LENGTH_SHORT).show()
                }
            } else {
                ReminderScheduler.cancelAll(requireContext())
                Toast.makeText(requireContext(), "提醒已关闭", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        timeHandler.removeCallbacks(timeTicker)
        timeHandler.post(timeTicker)
        reloadTodayPlan()
    }

    override fun onPause() {
        timeHandler.removeCallbacks(timeTicker)
        super.onPause()
    }

    private fun updateCurrentTime() {
        val now = Calendar.getInstance()
        currentDateTime.text = String.format(
            "%d年%d月%d日 %s · %02d:%02d",
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH),
            weekdayName(now.get(Calendar.DAY_OF_WEEK)),
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE)
        )
    }

    private fun reloadTodayPlan() {
        val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        todayPlan = PlanRepository(requireContext()).getPlans()
            .firstOrNull { it.dayOfWeek == todayDow }
        adapter.updatePlans(listOfNotNull(todayPlan))

        val hasTraining = todayPlan != null
        emptyToday.visibility = if (hasTraining) View.GONE else View.VISIBLE
        recycler.visibility = if (hasTraining) View.VISIBLE else View.GONE
        reminderSummary.text = todayPlan?.let {
            "今天 ${it.reminderTimeText} · 按当日计划提醒"
        } ?: "今天无训练提醒"
    }
}
