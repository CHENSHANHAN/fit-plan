package com.chenshanhan.fitplan

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.Calendar

class DayAdapter(
    private var days: List<PlanDay>,
    private val onChanged: () -> Unit,
    private val onEdit: (PlanDay) -> Unit
) : RecyclerView.Adapter<DayAdapter.DayVH>() {

    private var todayDow: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    fun updatePlans(newPlans: List<PlanDay>) {
        days = newPlans
        refreshStates()
    }

    fun refreshStates() {
        todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        notifyDataSetChanged()
    }

    inner class DayVH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.card)
        val dayBadge: TextView = view.findViewById(R.id.dayBadge)
        val focusName: TextView = view.findViewById(R.id.focusName)
        val todayBadge: TextView = view.findViewById(R.id.todayBadge)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val exerciseContainer: LinearLayout = view.findViewById(R.id.exerciseContainer)
        val cardioText: TextView = view.findViewById(R.id.cardioText)
        val doneLabel: TextView = view.findViewById(R.id.doneLabel)
        val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
        val btnCheck: MaterialButton = view.findViewById(R.id.btnCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day, parent, false)
        return DayVH(view)
    }

    override fun getItemCount(): Int = days.size

    override fun onBindViewHolder(holder: DayVH, position: Int) {
        val day = days[position]
        val ctx = holder.itemView.context
        val isToday = day.dayOfWeek == todayDow

        holder.dayBadge.text = day.dayName
        holder.focusName.text = day.focus
        holder.todayBadge.visibility = if (isToday) View.VISIBLE else View.GONE
        holder.timeText.text = "提醒 ${day.reminderTimeText}"

        val accent = ContextCompat.getColor(ctx, R.color.accent)
        holder.card.strokeColor = accent
        holder.card.strokeWidth = if (isToday) dp(ctx, 2) else 0

        holder.exerciseContainer.removeAllViews()
        val inflater = LayoutInflater.from(ctx)
        for (ex in day.exercises) {
            val row = inflater.inflate(
                R.layout.item_exercise, holder.exerciseContainer, false
            )
            row.findViewById<TextView>(R.id.exerciseName).text = ex.name
            row.findViewById<TextView>(R.id.exerciseSets).text = "${ex.sets}×${ex.reps}"
            holder.exerciseContainer.addView(row)
        }

        holder.cardioText.visibility =
            if (day.cardio.minutes > 0) View.VISIBLE else View.GONE
        holder.cardioText.text =
            "🏃 有氧 · 爬坡 ${day.cardio.minutes} 分钟（坡度 ${day.cardio.incline}，速度 ${day.cardio.speed}）"

        bindDone(holder, Prefs.isDone(ctx, day.dayOfWeek), isToday)

        holder.btnEdit.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onEdit(days[pos])
        }
        holder.btnCheck.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val item = days[pos]
            val newState = !Prefs.isDone(ctx, item.dayOfWeek)
            Prefs.setDone(ctx, item.dayOfWeek, newState)
            bindDone(holder, newState, item.dayOfWeek == todayDow)
            onChanged()
        }
    }

    private fun bindDone(holder: DayVH, done: Boolean, isToday: Boolean) {
        val ctx = holder.itemView.context
        val accent = ContextCompat.getColor(ctx, R.color.accent)
        val gray = ContextCompat.getColor(ctx, R.color.text_secondary)
        val divider = ContextCompat.getColor(ctx, R.color.divider)

        holder.card.alpha = if (done) 0.82f else 1f
        holder.doneLabel.text = when {
            done -> "今日已完成 · 继续加油"
            isToday -> "今天还没打卡"
            else -> "未打卡"
        }
        holder.doneLabel.setTextColor(if (done) accent else gray)

        holder.btnCheck.text = if (done) "已打卡 ✓" else "完成打卡"
        if (done) {
            holder.btnCheck.backgroundTintList = ColorStateList.valueOf(accent)
            holder.btnCheck.setTextColor(ContextCompat.getColor(ctx, R.color.bg))
            holder.btnCheck.strokeWidth = 0
        } else {
            holder.btnCheck.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.transparent))
            holder.btnCheck.setTextColor(gray)
            holder.btnCheck.strokeColor = ColorStateList.valueOf(divider)
            holder.btnCheck.strokeWidth = dp(ctx, 1)
        }
    }

    private fun dp(ctx: android.content.Context, value: Int): Int =
        (value * ctx.resources.displayMetrics.density).toInt()
}
