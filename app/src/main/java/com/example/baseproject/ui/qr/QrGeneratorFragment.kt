package com.example.baseproject.ui.qr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentQrGeneratorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Màn hình công cụ Tạo mã QR hỗ trợ 3 định dạng: Văn bản, URL và Wi-Fi.
 * Tự động gắn biểu tượng nhận diện vào tâm mã QR, lưu vào thư viện, chia sẻ và quản lý lịch sử tạo mã.
 */
class QrGeneratorFragment : BaseToolFragment<FragmentQrGeneratorBinding>() {

    override val toolId: String = "qr_generator"
    override val toolTitleResId: Int = R.string.tool_qr_generator_title

    private enum class QrType {
        TEXT,
        URL,
        WIFI
    }

    private data class QrHistoryItem(
        val type: QrType,
        val title: String,
        val rawContent: String,
        val timestamp: Long
    )

    private var currentType: QrType = QrType.TEXT
    private var generatedBitmap: Bitmap? = null
    private var currentRawContent: String = ""

    private var isPasswordVisible: Boolean = false
    private var selectedSecurityType: String = "WPA/WPA2"

    private lateinit var prefs: SharedPreferences
    private val historyList = mutableListOf<QrHistoryItem>()

    // Nạp layout view binding cho Fragment tạo mã QR.
    override fun provideViewBinding(container: ViewGroup?): FragmentQrGeneratorBinding {
        return FragmentQrGeneratorBinding.inflate(layoutInflater, container, false)
    }

    // Khởi tạo các thành phần giao diện, dữ liệu mặc định và lắng nghe thao tác người dùng.
    override fun initViews() {
        prefs = requireContext().getSharedPreferences("qr_generator_prefs", Context.MODE_PRIVATE)
        loadHistoryFromPrefs()

        setupTopBar()
        setupTypeTabs()
        setupInputActions()
        setupBottomActions()

        // Thiết lập giá trị mặc định ban đầu
        viewBinding.etInputText.setText("MobiTools - Bộ tiện ích hữu ích cho điện thoại Android.")
        viewBinding.etInputUrl.setText("https://github.com/windoffall303209")
        viewBinding.etInputWifiSsid.setText("MobiTools")
        viewBinding.etInputWifiPassword.setText("12345678")

        // Kích hoạt tab văn bản ban đầu và tạo mã QR mẫu
        switchTab(QrType.TEXT)
        generateCurrentQrCode(saveHistory = false)
    }

    // Thiết lập sự kiện thanh tiêu đề gồm nút quay lại và nút mở lịch sử tạo mã.
    private fun setupTopBar() {
        viewBinding.btnQrBackCard.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            parentFragmentManager.popBackStack()
        }

        viewBinding.btnQrHistoryCard.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            showHistoryDialog()
        }
    }

    // Thiết lập sự kiện chuyển tab giữa Văn bản, URL và Wi-Fi.
    private fun setupTypeTabs() {
        viewBinding.tabQrText.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchTab(QrType.TEXT)
        }

        viewBinding.tabQrUrl.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchTab(QrType.URL)
        }

        viewBinding.tabQrWifi.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchTab(QrType.WIFI)
        }
    }

    // Cập nhật giao diện tab, khối tiêu đề và hiển thị form tương ứng với loại QR được chọn.
    private fun switchTab(type: QrType) {
        currentType = type
        val context = requireContext()
        val activeBg = ContextCompat.getColor(context, R.color.qrBlue)
        val defaultBg = ContextCompat.getColor(context, R.color.colorCardBackground)
        val activeTextColor = ContextCompat.getColor(context, R.color.white)
        val defaultTextColor = ContextCompat.getColor(context, R.color.textPrimary)
        val activeStroke = ContextCompat.getColor(context, R.color.qrBlue)
        val defaultStroke = ContextCompat.getColor(context, R.color.colorBorder)

        // 1. Cập nhật Tab Văn bản
        val isText = (type == QrType.TEXT)
        viewBinding.tabQrText.setCardBackgroundColor(if (isText) activeBg else defaultBg)
        viewBinding.tabQrText.strokeColor = if (isText) activeStroke else defaultStroke
        viewBinding.ivTabTextIcon.setColorFilter(if (isText) activeTextColor else defaultTextColor)
        viewBinding.tvTabTextTitle.setTextColor(if (isText) activeTextColor else defaultTextColor)

        // 2. Cập nhật Tab URL
        val isUrl = (type == QrType.URL)
        viewBinding.tabQrUrl.setCardBackgroundColor(if (isUrl) activeBg else defaultBg)
        viewBinding.tabQrUrl.strokeColor = if (isUrl) activeStroke else defaultStroke
        viewBinding.ivTabUrlIcon.setColorFilter(if (isUrl) activeTextColor else defaultTextColor)
        viewBinding.tvTabUrlTitle.setTextColor(if (isUrl) activeTextColor else defaultTextColor)

        // 3. Cập nhật Tab Wi-Fi
        val isWifi = (type == QrType.WIFI)
        viewBinding.tabQrWifi.setCardBackgroundColor(if (isWifi) activeBg else defaultBg)
        viewBinding.tabQrWifi.strokeColor = if (isWifi) activeStroke else defaultStroke
        viewBinding.ivTabWifiIcon.setColorFilter(if (isWifi) activeTextColor else defaultTextColor)
        viewBinding.tvTabWifiTitle.setTextColor(if (isWifi) activeTextColor else defaultTextColor)

        // 4. Ẩn hiện các khung nhập liệu và cập nhật tiêu đề phân đoạn
        when (type) {
            QrType.TEXT -> {
                viewBinding.ivSectionIcon.setImageResource(R.drawable.ic_qr_type_text)
                viewBinding.tvSectionTitle.setText(R.string.qr_header_text_title)
                viewBinding.tvSectionSub.setText(R.string.qr_header_text_sub)
                viewBinding.layoutInputText.visibility = View.VISIBLE
                viewBinding.layoutInputUrl.visibility = View.GONE
                viewBinding.layoutInputWifi.visibility = View.GONE
            }
            QrType.URL -> {
                viewBinding.ivSectionIcon.setImageResource(R.drawable.ic_qr_type_url)
                viewBinding.tvSectionTitle.setText(R.string.qr_header_url_title)
                viewBinding.tvSectionSub.setText(R.string.qr_header_url_sub)
                viewBinding.layoutInputText.visibility = View.GONE
                viewBinding.layoutInputUrl.visibility = View.VISIBLE
                viewBinding.layoutInputWifi.visibility = View.GONE
            }
            QrType.WIFI -> {
                viewBinding.ivSectionIcon.setImageResource(R.drawable.ic_qr_type_wifi)
                viewBinding.tvSectionTitle.setText(R.string.qr_header_wifi_title)
                viewBinding.tvSectionSub.setText(R.string.qr_header_wifi_sub)
                viewBinding.layoutInputText.visibility = View.GONE
                viewBinding.layoutInputUrl.visibility = View.GONE
                viewBinding.layoutInputWifi.visibility = View.VISIBLE
            }
        }
    }

    // Thiết lập các nút xóa văn bản nhanh, ẩn hiện mật khẩu và chọn chuẩn bảo mật Wi-Fi.
    private fun setupInputActions() {
        viewBinding.btnClearText.setOnClickListener {
            viewBinding.etInputText.setText("")
        }

        viewBinding.btnClearUrl.setOnClickListener {
            viewBinding.etInputUrl.setText("")
        }

        viewBinding.btnClearWifiSsid.setOnClickListener {
            viewBinding.etInputWifiSsid.setText("")
        }

        viewBinding.btnToggleWifiPassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            val selection = viewBinding.etInputWifiPassword.selectionEnd
            if (isPasswordVisible) {
                viewBinding.etInputWifiPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                viewBinding.btnToggleWifiPassword.setImageResource(R.drawable.ic_visibility_off_eye)
            } else {
                viewBinding.etInputWifiPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                viewBinding.btnToggleWifiPassword.setImageResource(R.drawable.ic_visibility_eye)
            }
            viewBinding.etInputWifiPassword.setSelection(selection.coerceAtLeast(0))
        }

        viewBinding.layoutWifiSecurity.setOnClickListener {
            showSecuritySelectorDialog()
        }

        viewBinding.btnGenerateQr.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            generateCurrentQrCode(saveHistory = true)
        }
    }

    // Hiển thị hộp thoại chọn chuẩn bảo mật mạng Wi-Fi (WPA/WPA2, WEP, Không có bảo mật).
    private fun showSecuritySelectorDialog() {
        val options = arrayOf("WPA/WPA2", "WEP", "Không có bảo mật")
        var selectedIndex = options.indexOf(selectedSecurityType).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.qr_wifi_security_label)
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                selectedSecurityType = options[selectedIndex]
                viewBinding.tvWifiSecurityValue.text = selectedSecurityType
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // Xác thực dữ liệu đầu vào và tiến hành tạo ảnh mã QR kèm biểu tượng nhận diện trung tâm.
    private fun generateCurrentQrCode(saveHistory: Boolean) {
        val (isValid, rawContent, displayTitle) = validateAndBuildContent()
        if (!isValid) return

        currentRawContent = rawContent
        val iconResId = when (currentType) {
            QrType.TEXT -> R.drawable.ic_qr_type_text
            QrType.URL -> R.drawable.ic_qr_type_url
            QrType.WIFI -> R.drawable.ic_qr_type_wifi
        }

        val bitmap = renderQrCodeWithCenterBadge(rawContent, iconResId)
        if (bitmap != null) {
            generatedBitmap = bitmap
            viewBinding.ivQrCode.setImageBitmap(bitmap)
            viewBinding.cardQrPreview.visibility = View.VISIBLE
            viewBinding.layoutQrActions.visibility = View.VISIBLE

            if (saveHistory) {
                historyList.removeAll { it.rawContent == rawContent }
                historyList.add(0, QrHistoryItem(currentType, displayTitle, rawContent, System.currentTimeMillis()))
                saveHistoryToPrefs()
            }
        }
    }

    // Xác thực tính hợp lệ của dữ liệu đầu vào theo từng loại nội dung mã QR.
    private fun validateAndBuildContent(): Triple<Boolean, String, String> {
        when (currentType) {
            QrType.TEXT -> {
                val text = viewBinding.etInputText.text?.toString()?.trim().orEmpty()
                if (text.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.qr_error_empty_input, Toast.LENGTH_SHORT).show()
                    viewBinding.etInputText.requestFocus()
                    return Triple(false, "", "")
                }
                return Triple(true, text, text.take(40))
            }
            QrType.URL -> {
                var url = viewBinding.etInputUrl.text?.toString()?.trim().orEmpty()
                if (url.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.qr_error_empty_input, Toast.LENGTH_SHORT).show()
                    viewBinding.etInputUrl.requestFocus()
                    return Triple(false, "", "")
                }
                if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                    url = "https://$url"
                }
                return Triple(true, url, url)
            }
            QrType.WIFI -> {
                val ssid = viewBinding.etInputWifiSsid.text?.toString()?.trim().orEmpty()
                val password = viewBinding.etInputWifiPassword.text?.toString().orEmpty()

                if (ssid.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.qr_error_empty_ssid, Toast.LENGTH_SHORT).show()
                    viewBinding.etInputWifiSsid.requestFocus()
                    return Triple(false, "", "")
                }

                val securityKey = when (selectedSecurityType) {
                    "WPA/WPA2" -> "WPA"
                    "WEP" -> "WEP"
                    else -> "nopass"
                }

                val escapedSsid = escapeWifiString(ssid)
                val escapedPassword = escapeWifiString(password)
                val wifiFormat = "WIFI:T:$securityKey;S:$escapedSsid;P:$escapedPassword;;"
                return Triple(true, wifiFormat, "Wi-Fi: $ssid")
            }
        }
    }

    // Chuẩn hóa và thoát các ký tự đặc biệt trong thông số Wi-Fi chuẩn ZXing.
    private fun escapeWifiString(raw: String): String {
        return raw.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
            .replace("\"", "\\\"")
    }

    // Sinh ảnh bitmap QR code độ phân giải cao và chèn biểu tượng tròn ở tâm.
    private fun renderQrCodeWithCenterBadge(content: String, centerIconResId: Int): Bitmap? {
        return try {
            val hints = HashMap<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }

            val writer = QRCodeWriter()
            val matrix = writer.encode(content, BarcodeFormat.QR_CODE, 600, 600, hints)
            val width = matrix.width
            val height = matrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            drawCenterIconBadge(bitmap, centerIconResId)
            bitmap
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi khi tạo mã QR: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // Vẽ huy hiệu tròn trắng và biểu tượng màu xanh vào trung tâm của mã QR.
    private fun drawCenterIconBadge(qrBitmap: Bitmap, iconResId: Int) {
        val canvas = Canvas(qrBitmap)
        val centerX = qrBitmap.width / 2f
        val centerY = qrBitmap.height / 2f
        val badgeRadius = qrBitmap.width * 0.09f

        // Vòng tròn trắng đệm nền
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, badgeRadius, circlePaint)

        // Viền tròn mỏng quanh huy hiệu
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawCircle(centerX, centerY, badgeRadius, borderPaint)

        // Vẽ biểu tượng vector màu xanh nổi bật
        val drawable = ContextCompat.getDrawable(requireContext(), iconResId) ?: return
        drawable.mutate()
        drawable.setTint(Color.parseColor("#0066FF"))

        val iconSize = (badgeRadius * 1.15f).toInt()
        val left = (centerX - iconSize / 2f).toInt()
        val top = (centerY - iconSize / 2f).toInt()
        drawable.setBounds(left, top, left + iconSize, top + iconSize)
        drawable.draw(canvas)
    }

    // Thiết lập hành động cho bộ 3 nút Tải xuống, Chia sẻ và Sao chép.
    private fun setupBottomActions() {
        viewBinding.cardQrDownload.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            saveQrImageToGallery()
        }

        viewBinding.cardQrShare.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            shareQrCodeImage()
        }

        viewBinding.cardQrCopy.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            copyQrContentToClipboard()
        }
    }

    // Lưu ảnh mã QR vào thư viện hình ảnh thiết bị thông qua MediaStore.
    private fun saveQrImageToGallery() {
        val bitmap = generatedBitmap ?: return
        val resolver = requireContext().contentResolver
        val filename = "QR_${System.currentTimeMillis()}.png"

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                Toast.makeText(requireContext(), R.string.qr_saved_successfully, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi khi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Chia sẻ tệp ảnh mã QR qua bảng chia sẻ hệ thống Intent.ACTION_SEND.
    private fun shareQrCodeImage() {
        val bitmap = generatedBitmap ?: return
        try {
            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "qr_share.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.qr_action_share)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi khi chia sẻ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Sao chép nội dung gốc của mã QR vào bộ nhớ tạm hệ thống.
    private fun copyQrContentToClipboard() {
        if (currentRawContent.isEmpty()) return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Mã QR", currentRawContent)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.qr_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    // Hiển thị danh sách lịch sử các mã QR đã tạo gần đây để nạp lại nhanh.
    private fun showHistoryDialog() {
        if (historyList.isEmpty()) {
            Toast.makeText(requireContext(), R.string.qr_history_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val displayItems = historyList.map {
            val typeLabel = when (it.type) {
                QrType.TEXT -> "Văn bản"
                QrType.URL -> "URL"
                QrType.WIFI -> "Wi-Fi"
            }
            "[$typeLabel] ${it.title}\n${dateFormat.format(Date(it.timestamp))}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.qr_history_title)
            .setItems(displayItems) { _, which ->
                val selected = historyList[which]
                loadHistoryRecord(selected)
            }
            .setNeutralButton(R.string.qr_clear_history) { _, _ ->
                historyList.clear()
                saveHistoryToPrefs()
                Toast.makeText(requireContext(), R.string.qr_clear_history, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // Nạp lại dữ liệu của một bản ghi lịch sử vào giao diện và tái tạo mã QR.
    private fun loadHistoryRecord(item: QrHistoryItem) {
        switchTab(item.type)
        when (item.type) {
            QrType.TEXT -> {
                viewBinding.etInputText.setText(item.rawContent)
            }
            QrType.URL -> {
                viewBinding.etInputUrl.setText(item.rawContent)
            }
            QrType.WIFI -> {
                parseWifiStringToFields(item.rawContent)
            }
        }
        generateCurrentQrCode(saveHistory = false)
    }

    // Phân tích chuỗi Wi-Fi định dạng ZXing để nạp lại vào các ô nhập SSID và Mật khẩu.
    private fun parseWifiStringToFields(wifiString: String) {
        try {
            val ssidRegex = "S:([^;]+);".toRegex()
            val passwordRegex = "P:([^;]+);".toRegex()
            val typeRegex = "T:([^;]+);".toRegex()

            val ssidMatch = ssidRegex.find(wifiString)?.groupValues?.get(1).orEmpty()
            val passMatch = passwordRegex.find(wifiString)?.groupValues?.get(1).orEmpty()
            val typeMatch = typeRegex.find(wifiString)?.groupValues?.get(1).orEmpty()

            viewBinding.etInputWifiSsid.setText(ssidMatch.replace("\\;", ";").replace("\\:", ":"))
            viewBinding.etInputWifiPassword.setText(passMatch.replace("\\;", ";").replace("\\:", ":"))
            selectedSecurityType = when (typeMatch) {
                "WPA" -> "WPA/WPA2"
                "WEP" -> "WEP"
                else -> "Không có bảo mật"
            }
            viewBinding.tvWifiSecurityValue.text = selectedSecurityType
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Lưu danh sách lịch sử tạo mã QR vào SharedPreferences dưới dạng chuỗi JSON.
    private fun saveHistoryToPrefs() {
        val array = JSONArray()
        for (item in historyList.take(30)) {
            val obj = JSONObject().apply {
                put("type", item.type.name)
                put("title", item.title)
                put("content", item.rawContent)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString("qr_history_json", array.toString()).apply()
    }

    // Đọc và khôi phục danh sách lịch sử tạo mã QR từ SharedPreferences.
    private fun loadHistoryFromPrefs() {
        historyList.clear()
        val json = prefs.getString("qr_history_json", null) ?: return
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = try {
                    QrType.valueOf(obj.getString("type"))
                } catch (e: Exception) {
                    QrType.TEXT
                }
                historyList.add(
                    QrHistoryItem(
                        type = type,
                        title = obj.getString("title"),
                        rawContent = obj.getString("content"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
