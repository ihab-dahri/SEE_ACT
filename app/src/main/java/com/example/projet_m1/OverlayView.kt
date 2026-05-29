package com.example.projet_m1

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<DetectionResult> = listOf()

    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    private val textPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 50f
        style = Paint.Style.FILL
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }

    fun setResults(newResults: List<DetectionResult>) {
        this.results = newResults
        // Utilise postInvalidate() au lieu de invalidate()
        // car le résultat vient du thread de la caméra
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (result in results) {
            val left = result.left * width
            val top = result.top * height
            val right = result.right * width
            val bottom = result.bottom * height

            // Formatage de la distance (ex: "15.2 m")
            val distanceFormatee = String.format("%.1f", result.distance)

            // Le texte final combine Label + Score + Distance
            val textToDraw = "${result.label} ${result.score}% - $distanceFormatee m"

            canvas.drawRect(left, top, right, bottom, boxPaint)
            // On affiche le texte juste au-dessus du rectangle (ou en dessous s'il touche le haut de l'écran)
            canvas.drawText(textToDraw, left, if (top < 60f) top + 60f else top - 15f, textPaint)
        }
    }
}