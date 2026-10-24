package com.chenshanhan.fitplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryFragment : Fragment() {

    private lateinit var repository: TrainingLogRepository
    private lateinit var adapter: HistoryAdapter
    private lateinit var summary: TextView
    private lateinit var empty: TextView
    private lateinit var recycler: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = TrainingLogRepository(requireContext())
        summary = view.findViewById(R.id.historySummary)
        empty = view.findViewById(R.id.emptyHistory)
        recycler = view.findViewById(R.id.historyRecycler)
        adapter = HistoryAdapter(emptyList())
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        if (!::repository.isInitialized) return
        val logs = repository.getAllLogs()
        adapter.updateLogs(logs)
        summary.text = "共 ${logs.count { it.completed }} 次打卡 · ${logs.size} 条训练记录"
        empty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        recycler.visibility = if (logs.isEmpty()) View.GONE else View.VISIBLE
    }
}
