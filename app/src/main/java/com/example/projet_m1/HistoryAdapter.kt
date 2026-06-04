package com.example.projet_m1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val historyList: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_row, parent, false)
        return HistoryViewHolder(view)
    }


    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        holder.textName.text = item.name
        holder.textScore.text = "${item.score}%"
        holder.textTime.text = item.time
    }

    override fun getItemCount() = historyList.size


    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textSignName)
        val textScore: TextView = view.findViewById(R.id.textSignScore)
        val textTime: TextView = view.findViewById(R.id.textSignTime)
    }
}