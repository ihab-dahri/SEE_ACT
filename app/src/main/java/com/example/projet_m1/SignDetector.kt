package com.example.projet_m1

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.example.projet_m1.ml.Model // Assure-toi que le nom de ton fichier tflite est bien "Model"

class SignDetector(private val context: Context) {

    private val inputSize = 640
    private val numClasses = 47
    private val numBoxes = 8400
    private var labels: List<String>
    private var model: Model

    private val FOCAL_LENGTH = 1000f
    private val defaultRealHeight = 0.65f
    private val defaultThreshold = 0.45f

    init {
        // Charge les 47 labels depuis assets/labels.txt
        labels = FileUtil.loadLabels(context, "labels.txt")
        val options = org.tensorflow.lite.support.model.Model.Options.Builder()
            .setNumThreads(4)
            .build()
        model = Model.newInstance(context, options)
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // Initialisation buffer entrée
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 640, 640, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        val outputs = model.process(inputFeature0)
        val outputArray = outputs.outputFeature0AsTensorBuffer.floatArray

        return parseResults(outputArray)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, inputSize, inputSize)

        for (pixel in intValues) {
            byteBuffer.putFloat(((pixel shr 16 and 0xFF) / 255.0f))
            byteBuffer.putFloat(((pixel shr 8 and 0xFF) / 255.0f))
            byteBuffer.putFloat(((pixel and 0xFF) / 255.0f))
        }
        return byteBuffer
    }

    private fun parseResults(output: FloatArray): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()

        for (i in 0 until numBoxes) {
            var maxClassConfidence = 0f
            var classIndex = -1

            // Analyse des 47 scores de classe
            for (j in 0 until numClasses) {
                // Le modèle YOLOv8 TFLite exporté a les coordonnées en 0..3 et les scores en 4..50
                val conf = output[(j + 4) * numBoxes + i]
                if (conf > maxClassConfidence) {
                    maxClassConfidence = conf
                    classIndex = j
                }
            }

            if (classIndex != -1 && maxClassConfidence > defaultThreshold) {
                val labelName = labels.getOrNull(classIndex) ?: "Inconnu"

                // 1. Extraction des coordonnées brutes (déjà normalisées 0..1)
                val cx = output[0 * numBoxes + i]
                val cy = output[1 * numBoxes + i]
                val w  = output[2 * numBoxes + i]
                val h  = output[3 * numBoxes + i]

                // 2. Calcul des bords (0.0 à 1.0) - PAS DE DIVISION PAR inputSize ICI
                val left   = cx - w / 2f
                val top    = cy - h / 2f
                val right  = cx + w / 2f
                val bottom = cy + h / 2f

                // 3. Calcul de la distance :
                // On a besoin de la largeur/hauteur en PIXELS pour la formule
                val wInPixels = w * inputSize
                val hInPixels = h * inputSize
                val distance = (defaultRealHeight * FOCAL_LENGTH) / maxOf(wInPixels, hInPixels)

                results.add(DetectionResult(
                    left, top, right, bottom,
                    labelName,
                    (maxClassConfidence * 100).toInt(),
                    distance
                ))
            }
        }
        return results.sortedByDescending { it.score }.take(3)
    }

    fun close() = model.close()
}

data class DetectionResult(
    val left: Float, val top: Float, val right: Float, val bottom: Float,
    val label: String, val score: Int, val distance: Float
)