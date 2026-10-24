package com.chenshanhan.fitplan

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    private lateinit var repository: TrainingLogRepository
    private lateinit var adapter: HistoryAdapter
    private lateinit var summary: TextView
    private lateinit var empty: TextView
    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        repository = TrainingLogRepository(this)
        repository.migrateLegacyDone(PlanRepository(this).getPlans())

        summary = findViewById(R.id.historySummary)
        empty = findViewById(R.id.emptyHistory)
        recycler = findViewById(R.id.historyRecycler)
        adapter = HistoryAdapter(emptyList())
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val logs = repository.getAllLogs()
        adapter.updateLogs(logs)
        summary.text = "共 ${logs.count { it.completed }} 次打卡 · ${logs.size} 条训练记录"
        empty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        recycler.visibility = if (logs.isEmpty()) View.GONE else View.VISIBLE
    }
}
