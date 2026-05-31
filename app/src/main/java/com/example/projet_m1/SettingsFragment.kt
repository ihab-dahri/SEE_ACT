package com.example.projet_m1

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.slider.Slider

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val slider = view.findViewById<Slider>(R.id.confidenceSlider)
        val textValue = view.findViewById<TextView>(R.id.textThresholdValue)

        // 1. On ouvre le MÊME fichier que le SignDetector ("Settings")
        val sharedPreferences = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // 2. On récupère la valeur avec la MÊME clé ("manual_threshold"). Par défaut: -1f (Auto)
        val savedThreshold = sharedPreferences.getFloat("manual_threshold", -1f)

        // 3. On met à jour l'affichage au lancement
        if (savedThreshold == -1f) {
            slider.value = 45f // Position par défaut visuelle du curseur
            textValue.text = "Seuil actuel : Automatique (Intelligent)"
        } else {
            slider.value = savedThreshold * 100f
            textValue.text = "Seuil actuel : ${(savedThreshold * 100).toInt()} %"
        }

        // 4. Quand l'utilisateur glisse le doigt sur le curseur...
        slider.addOnChangeListener { _, value, _ ->
            // On met à jour le texte
            textValue.text = "Seuil actuel : ${value.toInt()} %"

            // On sauvegarde la nouvelle valeur avec la bonne clé (ex: 80% -> 0.8f)
            sharedPreferences.edit().putFloat("manual_threshold", value / 100f).apply()
        }
    }
}