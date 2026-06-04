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


        val sharedPreferences = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)


        val savedThreshold = sharedPreferences.getFloat("manual_threshold", -1f)


        if (savedThreshold == -1f) {
            slider.value = 45f
            textValue.text = "Seuil actuel : Automatique (Intelligent)"
        } else {
            slider.value = savedThreshold * 100f
            textValue.text = "Seuil actuel : ${(savedThreshold * 100).toInt()} %"
        }


        slider.addOnChangeListener { _, value, _ ->

            textValue.text = "Seuil actuel : ${value.toInt()} %"


            sharedPreferences.edit().putFloat("manual_threshold", value / 100f).apply()
        }
    }
}