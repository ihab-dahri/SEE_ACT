package com.example.projet_m1

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class ImageAnalysisFragment : Fragment() {

    private lateinit var imageViewResult: ImageView
    private lateinit var textPlaceholder: TextView


    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            analyserImageStatique(bitmap)
        }
    }


    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            analyserImageStatique(bitmap)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageViewResult = view.findViewById(R.id.imageViewResult)
        textPlaceholder = view.findViewById(R.id.textPlaceholder)
        val btnSelectImage = view.findViewById<Button>(R.id.btnSelectImage)


        btnSelectImage.setOnClickListener {
            afficherMenuChoixImage()
        }
    }

    private fun afficherMenuChoixImage() {
        val options = arrayOf("📸 Prendre une photo", "🖼️ Choisir dans la galerie")

        AlertDialog.Builder(requireContext())
            .setTitle("Source de l'image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePictureLauncher.launch(null)
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun analyserImageStatique(bitmapOriginal: Bitmap) {

        textPlaceholder.visibility = View.GONE


        val detector = (requireActivity() as MainActivity).detector


        val resultats = detector.detect(bitmapOriginal)


        val bitmapModifiable = bitmapOriginal.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmapModifiable)


        val boxPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val textPaint = Paint().apply {
            color = Color.YELLOW
            textSize = bitmapModifiable.width / 20f
            setShadowLayer(5f, 0f, 0f, Color.BLACK)
        }


        for (r in resultats) {
            val left = r.left * bitmapModifiable.width
            val top = r.top * bitmapModifiable.height
            val right = r.right * bitmapModifiable.width
            val bottom = r.bottom * bitmapModifiable.height

            canvas.drawRect(left, top, right, bottom, boxPaint)
            canvas.drawText("${r.label} ${r.score}%", left, top - 10f, textPaint)
        }


        imageViewResult.setImageBitmap(bitmapModifiable)
    }
}