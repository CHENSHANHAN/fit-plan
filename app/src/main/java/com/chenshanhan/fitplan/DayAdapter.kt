package com.chenshanhan.fitplan

import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
        val logRepository = TrainingLogRepository(ctx)
        val today = logRepository.today()
        val todayLog = if (isToday) logRepository.getLog(today, day.dayOfWeek) else null

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
            val weightInput = row.findViewById<EditText>(R.id.editActualWeight)
            val savedWeight = todayLog?.exercises
                ?.firstOrNull { it.name == ex.name }?.weight.orEmpty()
            weightInput.setText(savedWeight)
            weightInput.setSelection(weightInput.text.length)
            weightInput.isEnabled = isToday
            weightInput.alpha = if (isToday) 1f else 0.45f
            weightInput.hint = if (isToday) "重量kg" else "当天录入"
            // 每次绑定都使用新行和新监听器，setText 后再注册，避免覆盖已有记录。
            weightInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if (!isToday) return
                    logRepository.updateWeight(
                        today, day, ex.name, s?.toString().orEmpty()
                    )
                }
            })
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
            if (item.dayOfWeek != todayDow) return@setOnClickListener
            val newState = !Prefs.isDone(ctx, item.dayOfWeek)
            Prefs.setDone(ctx, item.dayOfWeek, newState)
            logRepository.setCompleted(today, item, newState)
            bindDone(holder, newState, true)
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
            else -> "训练日当天可打卡"
        }
        holder.doneLabel.setTextColor(if (done) accent else gray)
        holder.btnCheck.visibility = if (isToday) View.VISIBLE else View.GONE

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
