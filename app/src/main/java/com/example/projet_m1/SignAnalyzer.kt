package com.example.projet_m1

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.app.Activity

class SignAnalyzer(
    private val overlayView: OverlayView,
    private val detector: SignDetector
) : ImageAnalysis.Analyzer {

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {

        // 1. Transformation en Bitmap
        val bitmap = imageProxy.toBitmap()

        // 2. L'IA analyse l'image
        val results = detector.detect(bitmap)

        // 3. Mise à jour de la vitre (Rectangles)
        overlayView.post {
            overlayView.setResults(results)
        }

        // --- NOUVEAUTÉ : ENVOI À L'HISTORIQUE ---
        if (results.isNotEmpty()) {
            // On prend le meilleur résultat (le premier)
            val bestDetection = results[0]

            // On récupère le contexte de l'overlay pour atteindre la MainActivity
            val activity = overlayView.context as? MainActivity

            activity?.addSignToHistory(bestDetection.label)
        }
        // ----------------------------------------

        // 4. On libère l'image
        imageProxy.close()
    }
}