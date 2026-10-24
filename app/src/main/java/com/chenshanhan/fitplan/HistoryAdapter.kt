package com.chenshanhan.fitplan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    private var logs: List<TrainingLog>
) : RecyclerView.Adapter<HistoryAdapter.HistoryVH>() {

    fun updateLogs(newLogs: List<TrainingLog>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    inner class HistoryVH(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.historyDate)
        val focus: TextView = view.findViewById(R.id.historyFocus)
        val status: TextView = view.findViewById(R.id.historyStatus)
        val exercises: LinearLayout = view.findViewById(R.id.historyExercises)
        val cardio: TextView = view.findViewById(R.id.historyCardio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryVH {
        return HistoryVH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        )
    }

    override fun getItemCount(): Int = logs.size

    override fun onBindViewHolder(holder: HistoryVH, position: Int) {
        val log = logs[position]
        val ctx = holder.itemView.context
        holder.date.text = "${displayDate(log.date)} · ${weekdayName(log.dayOfWeek)}"
        holder.focus.text = log.focus
        holder.status.text = if (log.completed) "已打卡 ✓" else "已录重量 · 未打卡"
        holder.status.setTextColor(
            ContextCompat.getColor(ctx, if (log.completed) R.color.accent else R.color.text_secondary)
        )

        holder.exercises.removeAllViews()
        val inflater = LayoutInflater.from(ctx)
        for (exercise in log.exercises) {
            val row = inflater.inflate(
                R.layout.item_history_exercise, holder.exercises, false
            )
            row.findViewById<TextView>(R.id.historyExerciseName).text = exercise.name
            row.findViewById<TextView>(R.id.historyExercisePlan).text =
                "${exercise.sets}×${exercise.reps}"
            val weight = row.findViewById<TextView>(R.id.historyExerciseWeight)
            weight.text = if (exercise.weight.isBlank()) "未录重量" else "${exercise.weight} kg"
            weight.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (exercise.weight.isBlank()) R.color.text_secondary else R.color.accent
                )
            )
            holder.exercises.addView(row)
        }

        holder.cardio.visibility = if (log.cardio.minutes > 0) View.VISIBLE else View.GONE
        holder.cardio.text =
            "🏃 爬坡 ${log.cardio.minutes} 分钟（坡度 ${log.cardio.incline}，速度 ${log.cardio.speed}）"
    }

    private fun displayDate(date: String): String {
        val parts = date.split("-")
        return if (parts.size == 3) {
            "${parts[0]}年${parts[1].toIntOrNull() ?: parts[1]}月${parts[2].toIntOrNull() ?: parts[2]}日"
        } else {
            date
        }
    }
}
