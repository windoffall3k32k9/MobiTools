package com.example.baseproject.ui.scanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentBarcodeScannerBinding
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Công cụ quét và giải mã mã vạch, mã QR qua camera sử dụng ML Kit ngoại tuyến.
 */
class BarcodeScannerFragment : BaseToolFragment<FragmentBarcodeScannerBinding>() {

    override val toolId: String = "barcode_scanner"
    override val toolTitleResId: Int = R.string.tool_barcode_scanner_title

    private var camera: Camera? = null
    private var isTorchOn: Boolean = false
    private var isScanningActive: Boolean = true
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewBinding.layoutCameraPermission.visibility = View.GONE
            startCamera()
        } else {
            viewBinding.layoutCameraPermission.visibility = View.VISIBLE
        }
    }

    override fun provideViewBinding(container: ViewGroup?): FragmentBarcodeScannerBinding {
        return FragmentBarcodeScannerBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        setupToolbarButtons()
        setupResultActions()
        checkCameraPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    // Thiết lập sự kiện cho thanh điều hướng và đèn trợ sáng.
    private fun setupToolbarButtons() {
        viewBinding.btnScannerBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewBinding.btnScannerTorch.setOnClickListener {
            isTorchOn = !isTorchOn
            camera?.cameraControl?.enableTorch(isTorchOn)
        }

        viewBinding.btnGrantCameraPermission.setOnClickListener {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Thiết lập các nút tương tác kết quả quét.
    private fun setupResultActions() {
        viewBinding.btnCloseResult.setOnClickListener {
            viewBinding.cardScanResult.visibility = View.GONE
            isScanningActive = true
        }

        viewBinding.btnCopyBarcode.setOnClickListener {
            val text = viewBinding.tvBarcodeContent.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Barcode Result", text))
            Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }

        viewBinding.btnShareBarcode.setOnClickListener {
            val text = viewBinding.tvBarcodeContent.text.toString()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(intent, "Chia sẻ mã qua"))
        }
    }

    // Kiểm tra quyền truy cập Camera trước khi khởi động bộ giải mã.
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewBinding.layoutCameraPermission.visibility = View.GONE
            startCamera()
        } else {
            viewBinding.layoutCameraPermission.visibility = View.VISIBLE
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Khởi tạo luồng xem trước CameraX và cấu hình bộ phân tích ảnh ML Kit.
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = viewBinding.previewView.surfaceProvider
            }

            val barcodeScanner = BarcodeScanning.getClient()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(barcodeScanner, imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Phân tích khung hình từ camera để tìm mã vạch hoặc mã QR.
    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(
        barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
        imageProxy: androidx.camera.core.ImageProxy
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanningActive) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && isScanningActive) {
                        val first = barcodes.first()
                        onBarcodeDetected(first)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    // Xử lý khi phát hiện thành công mã vạch hoặc mã QR.
    private fun onBarcodeDetected(barcode: Barcode) {
        isScanningActive = false
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

        val rawValue = barcode.rawValue ?: ""
        val formatName = getBarcodeFormatName(barcode.format)

        activity?.runOnUiThread {
            viewBinding.tvBarcodeFormat.text = formatName
            viewBinding.tvBarcodeContent.text = rawValue
            viewBinding.cardScanResult.visibility = View.VISIBLE

            if (rawValue.startsWith("http://") || rawValue.startsWith("https://")) {
                viewBinding.btnOpenUrl.visibility = View.VISIBLE
                viewBinding.btnOpenUrl.setOnClickListener {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(rawValue))
                        startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Không thể mở liên kết: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                viewBinding.btnOpenUrl.visibility = View.GONE
            }
        }
    }

    // Lấy tên định dạng mã vạch tương ứng.
    private fun getBarcodeFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "MÃ QR (QR CODE)"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_CODE_128 -> "CODE 128"
            Barcode.FORMAT_CODE_39 -> "CODE 39"
            Barcode.FORMAT_CODE_93 -> "CODE 93"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_DATA_MATRIX -> "DATA MATRIX"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            else -> "MÃ VẠCH (BARCODE)"
        }
    }
}
