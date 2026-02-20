package com.game.ocrapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.game.ocrapp.databinding.FragmentFirstBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private var isProcessing = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Camera permission is required for OCR", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isProcessing = true

        binding.btnCapture.setOnClickListener {
            // UNLOCK scanning for one frame
            isProcessing = false
            Toast.makeText(context, "Recognizing...", Toast.LENGTH_SHORT).show()

            // Hide button while showing results
            binding.btnCapture.visibility = View.GONE
        }
        // Check permission first. startCamera() will be called from the launcher
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 1. Use a standard Aspect Ratio to match Preview and Analysis
            val preview = Preview.Builder()
                .setTargetResolution(Size(640, 480)) // Standardizing resolution
                .build().also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                // 2. VGA resolution is standard for OCR to avoid hardware overload
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(requireContext())) { imageProxy ->
                        if (!isProcessing) processImage(imageProxy) else imageProxy.close()
                    }
                }

            try {
                // 3. CRITICAL: Unbind everything before rebinding to prevent the Surface Error
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
                Toast.makeText(context, "Camera binding failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isNotEmpty()) {
                    analyzeTextStructure(visionText)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun analyzeTextStructure(visionText: Text) {
        // If already showing a popup, stop processing new frames
        if (isProcessing) return

        val tempFields = mutableListOf<IDField>()
        val allLines = visionText.textBlocks.flatMap { it.lines }

        // Grab everything found in the camera view
        for (line in allLines) {
            tempFields.add(IDField("Captured Data", line.text))
        }

        // Trigger popup if we found any text
        if (tempFields.isNotEmpty()) {
            isProcessing = true // Lock scanning
            showResults(tempFields)
        }
    }

    private fun showResults(fields: List<IDField>) {
        requireActivity().runOnUiThread {
            binding.resultCard.visibility = View.VISIBLE
            setupRecyclerView(fields)
        }
    }

    private fun setupRecyclerView(fields: List<IDField>) {
        binding.rvExtractedData.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = AdapterIdDetails(fields)
        }

        binding.btnConfirm.setOnClickListener {
            // Reset for next scan or navigate away
            isProcessing = true
            binding.resultCard.visibility = View.GONE
            binding.btnCapture.visibility = View.VISIBLE
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}