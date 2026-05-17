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

    // 1. On déclare le modèle ici pour qu'il reste en mémoire
    // 1. Déclaration en haut
    private var model: Model

    init {
        labels = FileUtil.loadLabels(context, "labels.txt")

        // On écrit les chemins COMPLETS pour forcer Android Studio à comprendre
        val options = org.tensorflow.lite.support.model.Model.Options.Builder()
            .setNumThreads(4)
            .build()

        model = Model.newInstance(context, options)
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // (On ne crée plus le modèle ici)

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 640, 640, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        // Inférence immédiate
        val outputs = model.process(inputFeature0)
        val outputArray = outputs.outputFeature0AsTensorBuffer.floatArray

        // (On ne fait surtout pas model.close() ici, sinon on le tue pour l'image suivante !)

        return parseResults(outputArray)
    }

    // N'oublie pas d'ajouter cette fonction pour fermer proprement le modèle quand on quitte l'appli
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
        val threshold = 0.45f

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

            if (maxClassConfidence > threshold) {
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

                val labelName = labels.getOrNull(classIndex) ?: "Inconnu"

                results.add(DetectionResult(
                    left, top, right, bottom,
                    labelName,
                    (maxClassConfidence * 100).toInt()
                ))
            }
        }
        return results.sortedByDescending { it.score }.take(2)
    }
}

// Classe de données pour structurer le résultat
data class DetectionResult(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String,
    val score: Int
)