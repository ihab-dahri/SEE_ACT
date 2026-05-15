package com.example.projet_m1

import DetectionAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var detectionAdapter: DetectionAdapter
    private val detectionList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        setupNavigation()

        // --- TEST DE CONNEXION ---
        // Si tu vois cette ligne au lancement, ton historique marche !
        addSignToHistory("Système Prêt")
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewHistory)
        detectionAdapter = DetectionAdapter(detectionList)
        recyclerView.adapter = detectionAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        bottomNavigationView.setupWithNavController(navController)
    }

    // Cette fonction est le pont entre l'IA et l'écran
    fun addSignToHistory(label: String) {
        runOnUiThread {
            // On vérifie que l'adapter est bien là pour éviter un crash
            if (::detectionAdapter.isInitialized) {
                detectionAdapter.addDetection(label)
                findViewById<RecyclerView>(R.id.recyclerViewHistory).scrollToPosition(0)
            }
        }
    }
    // À ajouter dans MainActivity.kt
    fun getDetectionAdapter(): DetectionAdapter {
        return detectionAdapter
    }
}