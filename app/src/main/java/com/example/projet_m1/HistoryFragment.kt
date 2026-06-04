package com.example.projet_m1

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryFragment : Fragment(R.layout.fragment_history) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFullHistory)


        val mainActivity = activity as? MainActivity

        mainActivity?.let {
            recyclerView.layoutManager = LinearLayoutManager(context)


            recyclerView.adapter = it.getDetectionAdapter()
        }
    }
}