package com.example.projet_m1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SignDetector(context: Context) {

    private var interpreter: Interpreter
    private var labels: List<String>

    // Paramètres standards de YOLOv8
    private val inputSize = 640 // La taille des images sur Google Colab
    private val numClasses = 41 // Ton nombre de classes (d'après ton data.yaml)
    private val numElements = 4 + numClasses
    private val numBoxes = 8400 // Le nombre de "zones" analysées par YOLOv8

    init {
        // 1. On charge le cerveau (model.tflite) et on l'optimise pour utiliser 4 cœurs du téléphone
        val modelBuffer = FileUtil.loadMappedFile(context, "model.tflite")
        val options = Interpreter.Options().apply { numThreads = 4 }
        interpreter = Interpreter(modelBuffer, options)

        // 2. On charge le dictionnaire des mots (labels.txt)
        labels = FileUtil.loadLabels(context, "labels.txt")
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        // 3. On redimensionne l'image de la caméra en 640x640 pour l'IA
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // 4. On prépare le tableau vide qui va recevoir les résultats de l'IA
        val output = Array(1) { Array(numElements) { FloatArray(numBoxes) } }

        // 5. BOUM ! L'IA RÉFLÉCHIT ICI.
        interpreter.run(byteBuffer, output)

        // 6. On trie les résultats pour ne garder que les panneaux sûrs
        return parseResults(output[0], bitmap.width, bitmap.height)
    }

    // Fonction technique pour transformer une image en tableau de chiffres (Pixels)
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, inputSize, inputSize)

        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = intValues[pixel++]
                // On normalise les couleurs de 0 à 1 (standard IA)
                byteBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value and 0xFF) / 255.0f))
            }
        }
        return byteBuffer
    }

    // Fonction pour lire les coordonnées données par l'IA et les adapter à l'écran
    private fun parseResults(output: Array<FloatArray>, imgWidth: Int, imgHeight: Int): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()

        for (i in 0 until numBoxes) {
            var maxClassConfidence = 0f
            var classIndex = -1

            // On cherche quelle classe a le plus haut score pour cette zone
            for (j in 0 until numClasses) {
                val confidence = output[4 + j][i]
                if (confidence > maxClassConfidence) {
                    maxClassConfidence = confidence
                    classIndex = j
                }
            }

            // SEUIL DE CONFIANCE : On ne garde que si l'IA est sûre à plus de 50%
            if (maxClassConfidence > 0.1f) {
                android.util.Log.d("IA_TEST", "Je vois un objet de classe $classIndex à ${maxClassConfidence * 100}%")
            }
            if (maxClassConfidence > 0.1f) {
                val cx = output[0][i] // Centre X
                val cy = output[1][i] // Centre Y
                val w = output[2][i]  // Largeur
                val h = output[3][i]  // Hauteur
                // 🕵️‍♂️ ON PLACE L'ESPION ICI (Uniquement pour les panneaux trouvés)
                android.util.Log.d("IA_BOX", "Centre X: $cx, Centre Y: $cy, Largeur: $w")



                // --- LE NOUVEAU CALCUL CORRIGÉ ---
                // Puisque l'IA donne un pourcentage (0.5), on le multiplie juste par la taille de l'écran !
                val left = (cx - w / 2)
                val top = (cy - h / 2)
                val right = (cx + w / 2)
                val bottom = (cy + h / 2)

                val rect = RectF(left, top, right, bottom)
                val className = if (classIndex < labels.size) labels[classIndex] else "Inconnu"
                val score = (maxClassConfidence * 100).toInt()

                results.add(DetectionResult(rect, className, score))
            }

        }
        return results
    }
}