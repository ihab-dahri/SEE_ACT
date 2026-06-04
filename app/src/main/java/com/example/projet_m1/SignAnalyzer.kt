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


        val bitmap = imageProxy.toBitmap()


        val results = detector.detect(bitmap)


        overlayView.post {
            overlayView.setResults(results)
        }


        if (results.isNotEmpty()) {

            val bestDetection = results[0]


            val activity = overlayView.context as? MainActivity

            activity?.addSignToHistory(bestDetection.label)
        }

        imageProxy.close()
    }
}