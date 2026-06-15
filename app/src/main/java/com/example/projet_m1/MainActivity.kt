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


    lateinit var detector: SignDetector

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var isCurrentlyNight = false


    private var isTripActive = false
    private var totalSignsDetected = 0
    private var dangerSignsDetected = 0
    private var maxSpeedDetected = 0



    companion object {
        var isNightModeActive = false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        detector = SignDetector(this)


        setupRecyclerView()
        setupNavigation()


        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)


        val btnTrip = findViewById<Button>(R.id.btnEndTrip)


        btnTrip.text = "Démarrer le trajet"
        btnTrip.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E293B"))
        btnTrip.setTextColor(android.graphics.Color.parseColor("#38BDF8"))

        btnTrip.setOnClickListener {
            if (!isTripActive) {
                isTripActive = true
                totalSignsDetected = 0
                dangerSignsDetected = 0
                maxSpeedDetected = 0

                btnTrip.text = "Finir le trajet"
                btnTrip.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EF4444"))
                btnTrip.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))

                Toast.makeText(this, "Trajet démarré ! Bonne route.", Toast.LENGTH_SHORT).show()
            } else {
                isTripActive = false
                btnTrip.text = "Démarrer le trajet"

                btnTrip.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E293B"))
                btnTrip.setTextColor(android.graphics.Color.parseColor("#38BDF8"))

                showTripSummary()
            }
        }

        addSignToHistory("Système Prêt")
    }


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


    override fun onDestroy() {
        super.onDestroy()
        if (::detector.isInitialized) {
            detector.close()
        }
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


    fun getSignDetector(): SignDetector {
        return detector
    }


    fun getDetectionAdapter(): DetectionAdapter {
        return detectionAdapter
    }

    fun addSignToHistory(label: String) {
        if (isTripActive && label != "Système Prêt") {
            totalSignsDetected++


            val dangerLabels = listOf(
                "Att-danger",
                "Att-eboulement",
                "Att-passage pietons",
                "Att-travaux",
                "virage"
            )
            if (dangerLabels.contains(label) || label.contains("danger", ignoreCase = true)) {
                dangerSignsDetected++
            }


            if (label.contains("speed", ignoreCase = true) || label.contains("KPH", ignoreCase = true) || label.contains("limit", ignoreCase = true)) {
                val speed = extractSpeedFromString(label)
                if (speed > maxSpeedDetected) {
                    maxSpeedDetected = speed
                }
            }
        }

        runOnUiThread {
            if (::detectionAdapter.isInitialized) {
                detectionAdapter.addDetection(label)
                findViewById<RecyclerView>(R.id.recyclerViewHistory).scrollToPosition(0)
            }
        }
    }

    private fun extractSpeedFromString(label: String): Int {
        val match = Regex("\\d+").find(label)
        return match?.value?.toIntOrNull() ?: 0
    }

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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            val isNightNow = lux < 10.0f

            if (isNightNow != isCurrentlyNight) {
                isCurrentlyNight = isNightNow
                isNightModeActive = isCurrentlyNight

                runOnUiThread {
                    if (isCurrentlyNight) {
                        Toast.makeText(this, "Nuit détectée : Sensibilité IA augmentée", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Jour détecté : IA en mode normal", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}