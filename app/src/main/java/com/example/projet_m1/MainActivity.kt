package com.example.projet_m1

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// N'oublie pas l'import de ton interface si ça souligne en rouge
import com.example.projet_m1.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    // NOTRE CERVEAU IA
    private lateinit var signDetector: SignDetector

    private var isFrontCamera = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Permission refusée.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // On initialise le cerveau ici
        signDetector = SignDetector(this)

        binding.btnFlipCamera.setOnClickListener {
            isFrontCamera = !isFrontCamera
            startCamera()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // --- NOUVEAU : L'ANALYSEUR D'IMAGES ---
            // Il va extraire les images de la vidéo pour les donner à l'IA
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // On garde que l'image la plus récente pour éviter les lags
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                // On ajoute l'imageAnalyzer dans la caméra !
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch(exc: Exception) {
                Log.e("SignDetector", "Erreur lors de l'ouverture de la caméra", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // --- LA FONCTION QUI FAIT LE LIEN ENTRE LA CAMÉRA ET L'IA ---
    private fun processImageProxy(imageProxy: ImageProxy) {
        // 1. On transforme l'image de la caméra en Bitmap (photo classique)
        val bitmap = imageProxy.toBitmap()

        // 2. On la fait pivoter pour qu'elle soit dans le bon sens (mode portrait)
        val matrix = Matrix()
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

        // Si on est en mode selfie, on fait un effet miroir
        if (isFrontCamera) {
            matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }

        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // 3. ON LANCE LA DÉTECTION (L'IA réfléchit)
        val results = signDetector.detect(rotatedBitmap)

        // 4. On envoie les résultats à la vitre transparente pour dessiner les carrés rouges
        runOnUiThread {
            binding.overlay.setResults(results)
        }

        // 5. TRÈS IMPORTANT : On libère l'image pour que la caméra puisse envoyer la suivante
        imageProxy.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}