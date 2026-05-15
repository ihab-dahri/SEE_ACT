package com.example.projet_m1

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryFragment : Fragment(R.layout.fragment_history) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. On récupère le RecyclerView de ton nouveau design fragment_history.xml
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFullHistory)

        // 2. On demande l'adapter à la MainActivity
        val mainActivity = activity as? MainActivity

        mainActivity?.let {
            recyclerView.layoutManager = LinearLayoutManager(context)

            // On branche le RecyclerView sur l'unique adapter de l'appli
            recyclerView.adapter = it.getDetectionAdapter()
        }
    }
}