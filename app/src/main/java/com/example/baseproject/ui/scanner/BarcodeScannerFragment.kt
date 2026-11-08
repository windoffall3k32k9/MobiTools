package com.example.baseproject.ui.scanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentBarcodeScannerBinding
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Công cụ quét mã QR và mã vạch hiện đại với khung quét bo tròn, đèn trợ sáng và quét ảnh từ thư viện.
 */
class BarcodeScannerFragment : BaseToolFragment<FragmentBarcodeScannerBinding>() {

    override val toolId: String = "barcode_scanner"
    override val toolTitleResId: Int = R.string.tool_barcode_scanner_title

    private var camera: Camera? = null
    private var isTorchOn: Boolean = false
    private var isScanningActive: Boolean = true
    private var isQrMode: Boolean = true

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // Cấu trúc dữ liệu lịch sử quét mã
    data class ScanHistoryRecord(val content: String, val format: String, val timestamp: Long)
    private val scanHistoryList = mutableListOf<ScanHistoryRecord>()

    private val prefs by lazy {
        requireContext().getSharedPreferences("scanner_prefs", Context.MODE_PRIVATE)
    }

    // Launcher yêu cầu quyền truy cập Camera từ hệ thống
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewBinding.layoutCameraPermission.visibility = View.GONE
            viewBinding.scannerOverlayView.visibility = View.VISIBLE
            startCamera()
        } else {
            viewBinding.layoutCameraPermission.visibility = View.VISIBLE
            viewBinding.scannerOverlayView.visibility = View.GONE
        }
    }

    // Launcher chọn ảnh từ thư viện thiết bị để quét mã ngoại tuyến
    private val galleryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            decodeBarcodeFromUri(uri)
        }
    }

    // Khởi tạo đối tượng ViewBinding cho giao diện FragmentBarcodeScanner.
    override fun provideViewBinding(container: ViewGroup?): FragmentBarcodeScannerBinding {
        return FragmentBarcodeScannerBinding.inflate(layoutInflater, container, false)
    }

    // Khởi tạo các thành phần giao diện, phục hồi lịch sử và thiết lập sự kiện người dùng.
    override fun initViews() {
        loadHistoryFromPrefs()
        setupTopBarActions()
        setupModePills()
        setupBottomActions()
        setupResultActions()
        checkCameraPermission()
    }

    // Dừng tia quét và tắt luồng xử lý CameraX khi hủy giao diện.
    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding.scannerOverlayView.stopLaserAnimation()
        cameraExecutor.shutdown()
    }

    // Cấu hình nút quay lại và nút bật tắt đèn pin trợ sáng trên thanh tiêu đề.
    private fun setupTopBarActions() {
        viewBinding.btnScannerBackCard.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewBinding.btnScannerTorchCard.setOnClickListener {
            toggleTorch()
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }

        viewBinding.btnGrantCameraPermission.setOnClickListener {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Bật tắt đèn pin Flash của thiết bị và cập nhật hiệu ứng màu nền nút.
    private fun toggleTorch() {
        isTorchOn = !isTorchOn
        camera?.cameraControl?.enableTorch(isTorchOn)

        val activeBg = ContextCompat.getColor(requireContext(), R.color.scanElectricBlue)
        val inactiveBg = ContextCompat.getColor(requireContext(), R.color.scanLightBlue)
        val activeIcon = Color.WHITE
        val inactiveIcon = ContextCompat.getColor(requireContext(), R.color.scanElectricBlue)

        viewBinding.btnScannerTorchCard.setCardBackgroundColor(if (isTorchOn) activeBg else inactiveBg)
        viewBinding.ivScannerTorch.imageTintList = ColorStateList.valueOf(if (isTorchOn) activeIcon else inactiveIcon)
    }

    // Thiết lập sự kiện chuyển đổi giữa hai chế độ quét: QR (vuông) và Mã vạch (chữ nhật).
    private fun setupModePills() {
        viewBinding.tabScanQr.setOnClickListener {
            switchScanMode(true)
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }

        viewBinding.tabScanBarcode.setOnClickListener {
            switchScanMode(false)
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    // Cập nhật trạng thái màu sắc bộ chuyển đổi chế độ và thay đổi khung ngắm camera.
    private fun switchScanMode(isQr: Boolean) {
        isQrMode = isQr
        val colorActive = ContextCompat.getColor(requireContext(), R.color.scanElectricBlue)
        val colorTransparent = Color.TRANSPARENT
        val colorWhite = Color.WHITE
        val colorSecondary = ContextCompat.getColor(requireContext(), R.color.textSecondary)

        // Cập nhật tab QR
        viewBinding.tabScanQr.setCardBackgroundColor(if (isQr) colorActive else colorTransparent)
        viewBinding.ivTabQr.imageTintList = ColorStateList.valueOf(if (isQr) colorWhite else colorSecondary)
        viewBinding.tvTabQr.setTextColor(if (isQr) colorWhite else colorSecondary)

        // Cập nhật tab Mã vạch
        viewBinding.tabScanBarcode.setCardBackgroundColor(if (!isQr) colorActive else colorTransparent)
        viewBinding.ivTabBarcode.imageTintList = ColorStateList.valueOf(if (!isQr) colorWhite else colorSecondary)
        viewBinding.tvTabBarcode.setTextColor(if (!isQr) colorWhite else colorSecondary)

        // Cập nhật hình dáng khung ngắm trên lớp phủ
        viewBinding.scannerOverlayView.setScanMode(isQr)
    }

    // Cấu hình 3 nút hành động dưới cùng: Chọn ảnh thư viện, Nút quét trung tâm và Lịch sử.
    private fun setupBottomActions() {
        // Nút mở thư viện
        viewBinding.btnGalleryCard.setOnClickListener {
            galleryPickerLauncher.launch("image/*")
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }

        // Nút quét chính giữa
        viewBinding.btnCenterScan.setOnClickListener {
            resumeScanning()
            triggerAutoFocus()
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }

        // Nút lịch sử quét
        viewBinding.btnHistoryCard.setOnClickListener {
            showHistoryDialog()
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    // Thiết lập các thao tác tương tác sau khi quét xong: Đóng, Sao chép, Chia sẻ, Quét tiếp.
    private fun setupResultActions() {
        viewBinding.btnCloseResult.setOnClickListener {
            resumeScanning()
        }

        viewBinding.btnRescan.setOnClickListener {
            resumeScanning()
        }

        viewBinding.btnCopyBarcode.setOnClickListener {
            val text = viewBinding.tvBarcodeContent.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Scanned Code", text))
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

    // Tiếp tục chu trình quét mới, kích hoạt lại tia laser và ẩn thẻ kết quả.
    private fun resumeScanning() {
        viewBinding.cardScanResult.visibility = View.GONE
        viewBinding.scannerOverlayView.startLaserAnimation()
        isScanningActive = true
    }

    // Kích hoạt tính năng lấy nét tự động vào tâm khung ngắm của camera.
    private fun triggerAutoFocus() {
        val cam = camera ?: return
        val factory = SurfaceOrientedMeteringPointFactory(
            viewBinding.previewView.width.toFloat(),
            viewBinding.previewView.height.toFloat()
        )
        val centerPoint = factory.createPoint(
            viewBinding.previewView.width / 2f,
            viewBinding.previewView.height / 2f
        )
        val action = FocusMeteringAction.Builder(centerPoint).build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    // Kiểm tra quyền Camera trước khi bắt đầu xem trước từ phần cứng.
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewBinding.layoutCameraPermission.visibility = View.GONE
            viewBinding.scannerOverlayView.visibility = View.VISIBLE
            startCamera()
        } else {
            viewBinding.layoutCameraPermission.visibility = View.VISIBLE
            viewBinding.scannerOverlayView.visibility = View.GONE
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Cấu hình và kết nối luồng CameraX với bộ quét mã vạch Google ML Kit.
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

    // Giải mã mã QR hoặc mã vạch từ tập tin ảnh được chọn trong thư viện.
    private fun decodeBarcodeFromUri(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(requireContext(), uri)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        onBarcodeDetected(barcodes.first())
                    } else {
                        Toast.makeText(requireContext(), R.string.scanner_no_code_found, Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), R.string.scanner_no_code_found, Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi đọc tệp ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Xử lý khi phát hiện thành công mã vạch hoặc mã QR, lưu lịch sử và hiển thị kết quả.
    private fun onBarcodeDetected(barcode: Barcode) {
        isScanningActive = false
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

        val rawValue = barcode.rawValue ?: ""
        val formatName = getBarcodeFormatName(barcode.format)

        // Lưu vào danh sách lịch sử quét
        scanHistoryList.add(0, ScanHistoryRecord(rawValue, formatName, System.currentTimeMillis()))
        saveHistoryToPrefs()

        activity?.runOnUiThread {
            viewBinding.scannerOverlayView.stopLaserAnimation()
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

    // Mở hộp thoại hiển thị danh sách lịch sử các mã đã quét thành công.
    private fun showHistoryDialog() {
        val context = requireContext()
        if (scanHistoryList.isEmpty()) {
            Toast.makeText(context, R.string.scanner_history_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val items = scanHistoryList.map { "[${it.format}]\n${it.content}" }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(R.string.scanner_history)
            .setItems(items) { _, which ->
                val record = scanHistoryList[which]
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Scanned Code", record.content))
                Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton(R.string.scanner_clear_history) { _, _ ->
                scanHistoryList.clear()
                saveHistoryToPrefs()
                Toast.makeText(context, R.string.scanner_clear_history, Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton(R.string.close, null)
            .show()
    }

    // Lưu danh sách lịch sử quét vào SharedPreferences dưới định dạng JSON.
    private fun saveHistoryToPrefs() {
        val array = JSONArray()
        for (item in scanHistoryList.take(50)) {
            val obj = JSONObject()
            obj.put("content", item.content)
            obj.put("format", item.format)
            obj.put("time", item.timestamp)
            array.put(obj)
        }
        prefs.edit().putString("scan_history_json", array.toString()).apply()
    }

    // Khôi phục danh sách lịch sử quét từ SharedPreferences.
    private fun loadHistoryFromPrefs() {
        scanHistoryList.clear()
        val json = prefs.getString("scan_history_json", null) ?: return
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                scanHistoryList.add(
                    ScanHistoryRecord(
                        obj.getString("content"),
                        obj.getString("format"),
                        obj.getLong("time")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Lấy tên định dạng chuẩn của loại mã vạch hoặc mã QR được phát hiện.
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
