package com.example.projet_m1

import android.content.Context
import android.graphics.Bitmap
import com.example.projet_m1.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SignDetector(private val context: Context) {

    private val inputSize = 640
    private val numClasses = 64
    private val numBoxes = 8400
    private var labels: List<String>
    private var model: Model

    // --- PSEUDO-LIDAR : TAILLES RÉELLES DES PANNEAUX (en mètres) ---
    private val realHeights = mapOf(
        "stop" to 0.90f,
        "Left Sharp Curve" to 0.90f,
        "pedestrian" to 0.70f,
        "crosswalk sign" to 0.70f,
        "120KPH" to 0.60f,
        "Speed Limit 120" to 0.60f,
        "speed_limit_120" to 0.60f,
        "speed_limit_120_ar" to 0.60f,
        "do_not_enter" to 0.60f,
        "no_entry" to 0.60f
    )
    private val defaultRealHeight = 0.65f
    private val FOCAL_LENGTH = 800f       // À calibrer dans la rue à 3 mètres exacts

    // --- LISSAGE DE LA DISTANCE (Filtre Passe-Bas) ---
    private val distanceHistory = mutableMapOf<String, Float>()
    private val SMOOTHING_FACTOR = 0.15f
    // ---------------------------------------------------------------

    // --- DICTIONNAIRE DES SEUILS DYNAMIQUES ---
    private val classThresholds = mapOf(
        "120KPH" to 0.30f, "Speed Limit 120" to 0.30f, "speed_limit_120" to 0.30f, "speed_limit_120_ar" to 0.30f,
        "do_not_enter" to 0.30f, "no_enter" to 0.30f, "no_entry" to 0.30f,
        "No Stopping" to 0.30f, "No Waiting" to 0.30f, "no_stopping_waiting" to 0.30f, "no_stop" to 0.30f,
        "do_not_u_turn" to 0.30f, "no_uturn" to 0.30f, "regulatory_No_u-turn" to 0.30f,
        "rond point" to 0.30f, "round" to 0.30f, "crosswalk sign" to 0.30f, "pedestrian" to 0.30f, "stop" to 0.30f,
        "speed_limit_100" to 0.20f, "speed_limit_100_ar" to 0.20f, "speed_limit_90" to 0.20f, "speed_limit_90_ar" to 0.20f,
        "traffic_sign_90" to 0.20f, "80KPH" to 0.20f, "speed_limit_80" to 0.20f, "speed_limit_80_ar" to 0.20f,
        "speed_limit_70" to 0.20f, "speed_limit_70_ar" to 0.20f, "speed_limit_60" to 0.20f, "speed_limit_60_ar" to 0.20f,
        "speed_limit_50" to 0.20f, "speed_limit_50_ar" to 0.20f, "speed_limit_40" to 0.20f, "speed_limit_40_en" to 0.20f,
        "Speed Limit 30" to 0.20f, "speed_limit_30" to 0.20f, "speed_limit_30_ar" to 0.20f, "speed_limit_30_en" to 0.20f,
        "20 limit" to 0.20f, "speed_limit_20" to 0.20f, "speed_limit_20_en" to 0.20f, "speed_limit_15" to 0.20f,
        "speed_limit_5" to 0.20f, "speedlimit" to 0.20f,
        "green" to 0.65f, "red" to 0.65f, "yellow" to 0.65f, "yellow_light" to 0.65f, "Traffic Signals Ahead" to 0.65f,
        "Danger Ahead" to 0.60f, "danger" to 0.60f, "Snow Warning Sign" to 0.60f, "14" to 0.60f, "Left Sharp Curve" to 0.60f
    )
    private val defaultThreshold = 0.45f
    // -----------------------------------------------------------------------------------

    init {
        labels = FileUtil.loadLabels(context, "labels.txt")
        val options = org.tensorflow.lite.support.model.Model.Options.Builder()
            .setNumThreads(4)
            .build()
        model = Model.newInstance(context, options)
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 640, 640, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        val outputs = model.process(inputFeature0)
        val outputArray = outputs.outputFeature0AsTensorBuffer.floatArray

        return parseResults(outputArray)
    }

    fun close() {
        model.close()
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, inputSize, inputSize)
        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = intValues[pixel++]
                byteBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value and 0xFF) / 255.0f))
            }
        }
        return byteBuffer
    }

    private fun parseResults(output: FloatArray): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()

        for (i in 0 until numBoxes) {
            var maxClassConfidence = 0f
            var classIndex = -1

            for (j in 0 until numClasses) {
                val conf = output[(j + 4) * numBoxes + i]
                if (conf > maxClassConfidence) {
                    maxClassConfidence = conf
                    classIndex = j
                }
            }

            val labelName = labels.getOrNull(classIndex) ?: "Inconnu"

            // 1. On demande le seuil spécifique
            var requiredThreshold = classThresholds[labelName] ?: defaultThreshold

            // --- 2. ADAPTATION MODE NUIT ---
            // On vérifie la variable globale du MainActivity
            if (MainActivity.isNightModeActive) {
                // S'il fait nuit, on baisse le seuil de 10% (0.10) pour aider l'IA,
                // tout en gardant un minimum de 15% pour éviter les faux positifs extrêmes.
                requiredThreshold = maxOf(0.15f, requiredThreshold - 0.10f)
            }
            // -------------------------------

            if (maxClassConfidence > requiredThreshold) {
                val rawCx = output[0 * numBoxes + i]
                val rawCy = output[1 * numBoxes + i]
                val rawW  = output[2 * numBoxes + i]
                val rawH  = output[3 * numBoxes + i]

                val cx = if (rawCx > 1f) rawCx / 640f else rawCx
                val cy = if (rawCy > 1f) rawCy / 640f else rawCy
                val w  = if (rawW > 1f) rawW / 640f else rawW
                val h  = if (rawH > 1f) rawH / 640f else rawH

                val left = cx - w / 2f
                val top = cy - h / 2f
                val right = cx + w / 2f
                val bottom = cy + h / 2f

                // --- CORRECTION GÉOMÉTRIQUE DE LA PERSPECTIVE ---
                val wInPixels = w * 640f
                val hInPixels = h * 640f
                val bestSizeInPixels = maxOf(wInPixels, hInPixels)

                val realHeight = realHeights[labelName] ?: defaultRealHeight

                // Calcul brut
                val rawDistance = (realHeight * FOCAL_LENGTH) / bestSizeInPixels

                // --- FILTRE DE LISSAGE (Exponentiel) ---
                val previousDistance = distanceHistory[labelName]

                val finalSmoothedDistance = if (previousDistance != null) {
                    (SMOOTHING_FACTOR * rawDistance) + ((1f - SMOOTHING_FACTOR) * previousDistance)
                } else {
                    rawDistance
                }

                // On met en mémoire pour l'image suivante
                distanceHistory[labelName] = finalSmoothedDistance

                results.add(DetectionResult(
                    left, top, right, bottom,
                    labelName,
                    (maxClassConfidence * 100).toInt(),
                    finalSmoothedDistance
                ))
            }
        }
        return results.sortedByDescending { it.score }.take(2)
    }
}

// Mise à jour de la classe de données
data class DetectionResult(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String,
    val score: Int,
    val distance: Float
)