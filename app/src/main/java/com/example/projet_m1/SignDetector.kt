package com.example.projet_m1

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.example.projet_m1.ml.Model

class SignDetector(private val context: Context) {

    private val inputSize = 640
    private val numClasses = 47
    private val numBoxes = 8400
    private var labels: List<String>
    private var model: Model

    private val FOCAL_LENGTH = 600f
    private val defaultRealHeight = 0.65f
    private val defaultThreshold = 0.45f


    private val classThresholds = mapOf(
        "Att-STOP" to 0.40f,
        "Feu rouge" to 0.60f,
        "Feu vert" to 0.60f,
        "Inter-sens" to 0.35f,
        "Inter-vitesse limitee a -50km-h-" to 0.30f,
        "Att-danger" to 0.35f

    )

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


        val sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val userManualThreshold = sharedPreferences.getFloat("manual_threshold", -1f)

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

            if (classIndex != -1) {
                val labelName = labels.getOrNull(classIndex) ?: "Inconnu"


                val finalThreshold = when {

                    userManualThreshold != -1f -> userManualThreshold


                    else -> {
                        var threshold = classThresholds[labelName] ?: defaultThreshold


                        if (MainActivity.isNightModeActive) {
                            threshold = maxOf(0.15f, threshold - 0.10f)
                        }
                        threshold
                    }
                }


                if (maxClassConfidence > finalThreshold) {
                    val cx = output[0 * numBoxes + i]
                    val cy = output[1 * numBoxes + i]
                    val w  = output[2 * numBoxes + i]
                    val h  = output[3 * numBoxes + i]

                    val left   = cx - w / 2f
                    val top    = cy - h / 2f
                    val right  = cx + w / 2f
                    val bottom = cy + h / 2f

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
        }
        return results.sortedByDescending { it.score }.take(3)
    }

    fun close() = model.close()
}

data class DetectionResult(
    val left: Float, val top: Float, val right: Float, val bottom: Float,
    val label: String, val score: Int, val distance: Float
)