package com.chenshanhan.fitplan

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private data class TabViews(
        val root: View,
        val icon: ImageView,
        val label: TextView
    )

    private lateinit var tabs: List<TabViews>
    private var currentTab = R.id.nav_today

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ReminderScheduler.ensureChannel(this)
        TrainingLogRepository(this).migrateLegacyDone(PlanRepository(this).getPlans())

        tabs = listOf(
            bindTab(R.id.nav_today, R.id.navTodayIcon, R.id.navTodayLabel),
            bindTab(R.id.nav_plans, R.id.navPlansIcon, R.id.navPlansLabel),
            bindTab(R.id.nav_history, R.id.navHistoryIcon, R.id.navHistoryLabel)
        )
        tabs.forEach { tab ->
            tab.root.setOnClickListener { showTab(tab.root.id) }
        }

        currentTab = savedInstanceState?.getInt(KEY_SELECTED_TAB, R.id.nav_today)
            ?: R.id.nav_today
        showTab(currentTab, animate = false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SELECTED_TAB, currentTab)
        super.onSaveInstanceState(outState)
    }

    fun showPlans() {
        showTab(R.id.nav_plans)
    }

    private fun bindTab(rootId: Int, iconId: Int, labelId: Int): TabViews {
        val root = findViewById<View>(rootId)
        return TabViews(
            root = root,
            icon = root.findViewById(iconId),
            label = root.findViewById(labelId)
        )
    }

    private fun showTab(itemId: Int, animate: Boolean = true) {
        val existing = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (existing != null && currentTab == itemId) {
            updateTabStyles(itemId, animate = false)
            return
        }

        currentTab = itemId
        updateTabStyles(itemId, animate)
        val (fragment, tag) = when (itemId) {
            R.id.nav_plans -> PlansFragment() to TAG_PLANS
            R.id.nav_history -> HistoryFragment() to TAG_HISTORY
            else -> TodayFragment() to TAG_TODAY
        }
        supportFragmentManager.beginTransaction().apply {
            if (animate) {
                setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
            }
            replace(R.id.fragmentContainer, fragment, tag)
            commit()
        }
    }

    private fun updateTabStyles(selectedId: Int, animate: Boolean) {
        val accent = ContextCompat.getColor(this, R.color.accent)
        val secondary = ContextCompat.getColor(this, R.color.text_secondary)
        tabs.forEach { tab ->
            val selected = tab.root.id == selectedId
            tab.root.isSelected = selected
            tab.icon.imageTintList = ColorStateList.valueOf(if (selected) accent else secondary)
            tab.label.setTextColor(if (selected) accent else secondary)
            tab.label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            tab.root.animate().cancel()
            val scale = if (selected) 1.02f else 1f
            if (animate) {
                tab.root.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(160L)
                    .start()
            } else {
                tab.root.scaleX = scale
                tab.root.scaleY = scale
            }
        }
    }

    private companion object {
        const val KEY_SELECTED_TAB = "selected_tab"
        const val TAG_TODAY = "today"
        const val TAG_PLANS = "plans"
        const val TAG_HISTORY = "history"
    }
}
