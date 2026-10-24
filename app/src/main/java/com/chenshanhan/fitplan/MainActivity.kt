package com.chenshanhan.fitplan

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ReminderScheduler.ensureChannel(this)
        TrainingLogRepository(this).migrateLegacyDone(PlanRepository(this).getPlans())

        bottomNavigation = findViewById(R.id.bottomNavigation)
        val initialTab = savedInstanceState?.getInt(KEY_SELECTED_TAB, R.id.nav_today)
            ?: R.id.nav_today
        bottomNavigation.selectedItemId = initialTab
        showTab(initialTab)
        bottomNavigation.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SELECTED_TAB, bottomNavigation.selectedItemId)
        super.onSaveInstanceState(outState)
    }

    fun showPlans() {
        bottomNavigation.selectedItemId = R.id.nav_plans
    }

    private fun showTab(itemId: Int) {
        val (fragment, tag) = when (itemId) {
            R.id.nav_plans -> PlansFragment() to TAG_PLANS
            R.id.nav_history -> HistoryFragment() to TAG_HISTORY
            else -> TodayFragment() to TAG_TODAY
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
    }

    private companion object {
        const val KEY_SELECTED_TAB = "selected_tab"
        const val TAG_TODAY = "today"
        const val TAG_PLANS = "plans"
        const val TAG_HISTORY = "history"
    }
}
