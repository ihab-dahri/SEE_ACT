package com.example.projet_m1

import DetectionAdapter
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var detectionAdapter: DetectionAdapter
    private val detectionList = mutableListOf<String>()

    // --- VARIABLES CAPTEUR DE LUMIÈRE ---
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var isCurrentlyNight = false

    // --- VARIABLES BILAN DE TRAJET (Data Analytics) ---
    private var isTripActive = false
    private var totalSignsDetected = 0
    private var dangerSignsDetected = 0
    private var maxSpeedDetected = 0
    // --------------------------------------------------

    // Astuce MLOps : Une variable globale pour que l'IA puisse lire l'état de la nuit depuis n'importe où
    companion object {
        var isNightModeActive = false
    }
    // ------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        setupNavigation()

        // --- INITIALISATION DU CAPTEUR DE LUMIÈRE ---
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // --- GESTION DU BOUTON DE TRAJET DYNAMIQUE ---
        val btnTrip = findViewById<Button>(R.id.btnEndTrip)

        // État initial (Bouton Vert)
        btnTrip.text = "Démarrer le trajet"
        btnTrip.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // Vert

        btnTrip.setOnClickListener {
            if (!isTripActive) {
                // --- ON DÉMARRE LE TRAJET ---
                isTripActive = true

                // On remet les compteurs à zéro
                totalSignsDetected = 0
                dangerSignsDetected = 0
                maxSpeedDetected = 0

                // On change le bouton en mode "Arrêt" (Rouge)
                btnTrip.text = "Fin de trajet"
                btnTrip.setBackgroundColor(android.graphics.Color.parseColor("#FF3B30")) // Rouge
                Toast.makeText(this, "Trajet démarré ! Bonne route.", Toast.LENGTH_SHORT).show()

            } else {
                // --- ON TERMINE LE TRAJET ---
                isTripActive = false

                // On remet le bouton en mode "Démarrer" (Vert)
                btnTrip.text = "Démarrer le trajet"
                btnTrip.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // Vert

                // On affiche le popup de bilan !
                showTripSummary()
            }
        }
        // ---------------------------------------------

        addSignToHistory("Système Prêt")
    }

    // --- GESTION DE LA BATTERIE (Allumer/Éteindre le capteur) ---
    override fun onResume() {
        super.onResume()
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
    // ------------------------------------------------------------

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

    fun addSignToHistory(label: String) {
        // --- 1. MISE À JOUR DES STATISTIQUES (Si le trajet a démarré) ---
        if (isTripActive && label != "Système Prêt") {
            totalSignsDetected++

            // Vérification des panneaux de danger
            val dangerLabels = listOf("Danger Ahead", "danger", "Snow Warning Sign", "Left Sharp Curve", "pedestrian", "crosswalk sign")
            if (dangerLabels.contains(label)) {
                dangerSignsDetected++
            }

            // Vérification des panneaux de vitesse
            if (label.contains("speed", ignoreCase = true) || label.contains("KPH", ignoreCase = true) || label.contains("limit", ignoreCase = true)) {
                val speed = extractSpeedFromString(label)
                if (speed > maxSpeedDetected) {
                    maxSpeedDetected = speed
                }
            }
        }
        // ----------------------------------------------------------------

        runOnUiThread {
            if (::detectionAdapter.isInitialized) {
                detectionAdapter.addDetection(label)
                findViewById<RecyclerView>(R.id.recyclerViewHistory).scrollToPosition(0)
            }
        }
    }

    fun getDetectionAdapter(): DetectionAdapter {
        return detectionAdapter
    }

    // --- FONCTIONS DU BILAN DE TRAJET ---

    // Extrait les chiffres d'un texte (ex: "speed_limit_120" -> 120)
    private fun extractSpeedFromString(label: String): Int {
        val match = Regex("\\d+").find(label)
        return match?.value?.toIntOrNull() ?: 0
    }

    // Affiche le Popup à la fin
    private fun showTripSummary() {
        val summaryText = """
            🚦 Panneaux rencontrés : $totalSignsDetected
            ⚠️ Dangers anticipés : $dangerSignsDetected
            🏎️ Vitesse Max lue : ${if (maxSpeedDetected > 0) "$maxSpeedDetected km/h" else "Aucune"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("🏁 Bilan du Trajet")
            .setMessage(summaryText)
            .setCancelable(false)
            .setPositiveButton("Fermer") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // --- LE CERVEAU DU MODE NUIT (Lecture des Lux) ---
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]

            // Si la lumière est sous 10 Lux, c'est qu'il fait nuit
            val isNightNow = lux < 10.0f

            // Si on change d'état (jour -> nuit, ou nuit -> jour)
            if (isNightNow != isCurrentlyNight) {
                isCurrentlyNight = isNightNow

                // 1. On met à jour la variable globale pour le SignDetector
                isNightModeActive = isCurrentlyNight

                // 2. On prévient le conducteur
                runOnUiThread {
                    if (isCurrentlyNight) {
                        Toast.makeText(this, "🌙 Nuit détectée : Sensibilité IA augmentée", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "☀️ Jour détecté : IA en mode normal", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Obligatoire pour l'interface, on laisse vide
    }
}