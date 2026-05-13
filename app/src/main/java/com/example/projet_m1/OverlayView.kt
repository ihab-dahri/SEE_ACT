package com.example.projet_m1

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // La liste qui contiendra les panneaux détectés par l'IA
    private var results: List<DetectionResult> = emptyList()

    // Les "pinceaux" pour dessiner
    private val boxPaint = Paint()
    private val textPaint = Paint()
    private val textBackgroundPaint = Paint()

    init {
        // Le pinceau pour le rectangle (Contour rouge, vide à l'intérieur)
        boxPaint.color = Color.RED
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 8f

        // Le pinceau pour le texte (Blanc)
        textPaint.color = Color.WHITE
        textPaint.textSize = 50f
        textPaint.style = Paint.Style.FILL

        // Le pinceau pour le fond du texte (Noir un peu transparent)
        textBackgroundPaint.color = Color.parseColor("#80000000")
        textBackgroundPaint.style = Paint.Style.FILL
    }

    // L'IA appellera cette fonction des dizaines de fois par seconde
    fun setResults(newResults: List<DetectionResult>) {
        results = newResults
        invalidate() // Le mot magique qui force Android à effacer l'écran et redessiner
    }

    // La fonction qui dessine réellement sur l'écran
    // La fonction qui dessine réellement sur l'écran
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // On récupère la vraie taille de l'écran de ton téléphone
        val screenW = width.toFloat()
        val screenH = height.toFloat()

        for (result in results) {
            // 1. On transforme les pourcentages de l'IA en vrais pixels pour ton écran
            val mappedRect = RectF(
                result.boundingBox.left * screenW,
                result.boundingBox.top * screenH,
                result.boundingBox.right * screenW,
                result.boundingBox.bottom * screenH
            )

            // 2. Dessiner le rectangle au bon endroit
            canvas.drawRect(mappedRect, boxPaint)

            // 3. Préparer le texte
            val text = "${result.className} ${result.score}%"
            val textWidth = textPaint.measureText(text)

            // 4. Dessiner le fond noir au-dessus du nouveau rectangle
            canvas.drawRect(
                mappedRect.left,
                mappedRect.top - 60f,
                mappedRect.left + textWidth + 20f,
                mappedRect.top,
                textBackgroundPaint
            )

            // 5. Écrire le texte blanc
            canvas.drawText(text, mappedRect.left + 10f, mappedRect.top - 15f, textPaint)
        }
    }
}

// Une petite "boîte" de données pour ranger les infos de chaque panneau trouvé
data class DetectionResult(
    val boundingBox: RectF, // Les coordonnées du rectangle
    val className: String,  // Le nom (ex: "stop")
    val score: Int          // La confiance de l'IA (ex: 95)
)