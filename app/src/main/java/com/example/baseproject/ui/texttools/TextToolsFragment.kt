package com.example.baseproject.ui.texttools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentTextToolsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bản ghi lưu trữ lịch sử xử lý và chuyển đổi văn bản.
 */
data class TextHistoryItem(
    val input: String,
    val result: String,
    val modeName: String,
    val timestamp: Long
)

/**
 * Màn hình xử lý văn bản tiện ích hỗ trợ 6 chế độ chuyển đổi và thống kê thời gian thực.
 */
class TextToolsFragment : BaseToolFragment<FragmentTextToolsBinding>() {

    override val toolId: String = "text_tools"
    override val toolTitleResId: Int = R.string.tool_text_tools_title

    private var currentMode: TextTransformMode = TextTransformMode.UPPERCASE
    private val historyList = mutableListOf<TextHistoryItem>()

    private val prefs by lazy {
        requireContext().getSharedPreferences("text_tools_prefs", Context.MODE_PRIVATE)
    }

    override fun provideViewBinding(container: ViewGroup?): FragmentTextToolsBinding {
        return FragmentTextToolsBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        setupTopBar()
        setupInputCard()
        setupActionCards()
        setupOutputCard()
        loadHistoryFromPrefs()

        // Khởi tạo văn bản mẫu mặc định nếu trường nhập liệu đang trống
        val defaultText = "MobiTools là ứng dụng tiện ích đa năng, giúp bạn xử lý văn bản, chuyển đổi đơn vị và nhiều công cụ hữu ích khác."
        if (viewBinding.etInputText.text.isNullOrEmpty()) {
            viewBinding.etInputText.setText(defaultText)
            viewBinding.etInputText.setSelection(defaultText.length)
        }

        updateCardSelection(currentMode)
        applyCurrentTransformation()
    }

    // Thiết lập thanh tiêu đề với nút quay lại và nút mở lịch sử.
    private fun setupTopBar() {
        viewBinding.btnTextBackCard.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewBinding.btnTextHistoryCard.setOnClickListener {
            showHistoryDialog()
        }
    }

    // Thiết lập ô nhập liệu văn bản với bộ đếm và nút xóa nhanh nội dung.
    private fun setupInputCard() {
        viewBinding.etInputText.filters = arrayOf(InputFilter.LengthFilter(5000))

        viewBinding.btnClearInputText.setOnClickListener {
            viewBinding.etInputText.text?.clear()
        }

        viewBinding.etInputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                viewBinding.tvInputCharCount.text = String.format(Locale.GERMANY, "%,d / 5.000", length)
                applyCurrentTransformation()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    // Thiết lập sự kiện nhấn cho 6 thẻ chuyển đổi cấu trúc văn bản.
    private fun setupActionCards() {
        viewBinding.cardActionUpper.setOnClickListener {
            setMode(TextTransformMode.UPPERCASE)
        }

        viewBinding.cardActionLower.setOnClickListener {
            setMode(TextTransformMode.LOWERCASE)
        }

        viewBinding.cardActionTitle.setOnClickListener {
            setMode(TextTransformMode.TITLE_CASE)
        }

        viewBinding.cardActionSentence.setOnClickListener {
            setMode(TextTransformMode.SENTENCE_CASE)
        }

        viewBinding.cardActionTrimSpaces.setOnClickListener {
            setMode(TextTransformMode.TRIM_SPACES)
        }

        viewBinding.cardActionRemoveLines.setOnClickListener {
            setMode(TextTransformMode.REMOVE_LINES)
        }
    }

    // Thiết lập ô hiển thị kết quả và nút sao chép văn bản đã xử lý.
    private fun setupOutputCard() {
        viewBinding.btnCopyResult.setOnClickListener {
            copyResultText()
        }
    }

    // Cập nhật chế độ chuyển đổi văn bản hiện tại và kích hoạt tính toán lại.
    private fun setMode(mode: TextTransformMode) {
        if (currentMode == mode) return
        currentMode = mode
        updateCardSelection(mode)
        applyCurrentTransformation()
    }

    // Cập nhật màu sắc giao diện tương ứng với thẻ chức năng đang được kích hoạt.
    private fun updateCardSelection(activeMode: TextTransformMode) {
        val colorActiveBg = ContextCompat.getColor(requireContext(), R.color.qrBlue)
        val colorInactiveBg = ContextCompat.getColor(requireContext(), R.color.colorCardBackground)
        val colorBorder = ContextCompat.getColor(requireContext(), R.color.colorBorder)
        val colorWhite = ContextCompat.getColor(requireContext(), R.color.white)
        val colorPrimaryText = ContextCompat.getColor(requireContext(), R.color.textPrimary)
        val colorBlue = ContextCompat.getColor(requireContext(), R.color.qrBlue)

        // 1. VIẾT HOA TOÀN BỘ
        val isUpper = activeMode == TextTransformMode.UPPERCASE
        viewBinding.cardActionUpper.setCardBackgroundColor(if (isUpper) colorActiveBg else colorInactiveBg)
        viewBinding.cardActionUpper.strokeColor = if (isUpper) colorActiveBg else colorBorder
        viewBinding.tvUpperIcon.setTextColor(if (isUpper) colorWhite else colorBlue)
        viewBinding.tvUpperLabel.setTextColor(if (isUpper) colorWhite else colorPrimaryText)

        // 2. viết thường toàn bộ
        val isLower = activeMode == TextTransformMode.LOWERCASE
        viewBinding.cardActionLower.setCardBackgroundColor(if (isLower) colorActiveBg else colorInactiveBg)
        viewBinding.cardActionLower.strokeColor = if (isLower) colorActiveBg else colorBorder
        viewBinding.tvLowerIcon.setTextColor(if (isLower) colorWhite else colorBlue)
        viewBinding.tvLowerLabel.setTextColor(if (isLower) colorWhite else colorPrimaryText)

        // 3. Viết Hoa Chữ Cái Đầu
        val isTitle = activeMode == TextTransformMode.TITLE_CASE
        viewBinding.cardActionTitle.setCardBackgroundColor(if (isTitle) colorActiveBg else colorInactiveBg)
        viewBinding.cardActionTitle.strokeColor = if (isTitle) colorActiveBg else colorBorder
        viewBinding.tvTitleIcon.setTextColor(if (isTitle) colorWhite else colorBlue)
        viewBinding.tvTitleLabel.setTextColor(if (isTitle) colorWhite else colorPrimaryText)

        // 4. Viết hoa ký tự đầu tiên
        val isSentence = activeMode == TextTransformMode.SENTENCE_CASE
        viewBinding.cardActionSentence.setCardBackgroundColor(if (isSentence) colorActiveBg else colorInactiveBg)
        viewBinding.cardActionSentence.strokeColor = if (isSentence) colorActiveBg else colorBorder
        viewBinding.ivSentenceIcon.imageTintList = ColorStateList.valueOf(if (isSentence) colorWhite else colorBlue)
        viewBinding.tvSentenceLabel.setTextColor(if (isSentence) colorWhite else colorPrimaryText)

        // 5. Xóa dấu cách
        val isTrim = activeMode == TextTransformMode.TRIM_SPACES
        viewBinding.cardActionTrimSpaces.setCardBackgroundColor(if (isTrim) colorActiveBg else colorInactiveBg)
        viewBinding.cardActionTrimSpaces.strokeColor = if (isTrim) colorActiveBg else colorBorder
        viewBinding.ivTrimSpacesIcon.imageTintList = ColorStateList.valueOf(if (isTrim) colorWhite else colorBlue)
        viewBinding.tvTrimSpacesLabel.setTextColor(if (isTrim) colorWhite else colorPrimaryText)

        // 6. Xóa xuống dòng
        val isRemoveLines = activeMode == TextTransformMode.REMOVE_LINES
        viewBinding.cardActionRemoveLines.setCardBackgroundColor(if (isRemoveLines) colorActiveBg else colorInactiveBg)
        viewBinding.cardActionRemoveLines.strokeColor = if (isRemoveLines) colorActiveBg else colorBorder
        viewBinding.ivRemoveLinesIcon.imageTintList = ColorStateList.valueOf(if (isRemoveLines) colorWhite else colorBlue)
        viewBinding.tvRemoveLinesLabel.setTextColor(if (isRemoveLines) colorWhite else colorPrimaryText)
    }

    // Áp dụng thuật toán chuyển đổi và hiển thị văn bản kết quả cùng số liệu thống kê.
    private fun applyCurrentTransformation() {
        val input = viewBinding.etInputText.text?.toString() ?: ""
        if (input.isEmpty()) {
            viewBinding.tvResultText.text = ""
            updateStatistics("")
            return
        }

        val result = TextTransformEngine.transform(input, currentMode)
        viewBinding.tvResultText.text = result
        updateStatistics(result)
    }

    // Cập nhật số lượng ký tự, từ và dòng trên các ô thống kê trực quan.
    private fun updateStatistics(text: String) {
        val chars = text.length
        val words = TextTransformEngine.countWords(text)
        val lines = TextTransformEngine.countLines(text)

        viewBinding.tvStatChars.text = String.format(Locale.GERMANY, "%,d", chars)
        viewBinding.tvStatWords.text = String.format(Locale.GERMANY, "%,d", words)
        viewBinding.tvStatLines.text = String.format(Locale.GERMANY, "%,d", lines)
    }

    // Sao chép kết quả đã xử lý vào bộ nhớ tạm và lưu bản ghi vào lịch sử.
    private fun copyResultText() {
        val result = viewBinding.tvResultText.text?.toString() ?: ""
        if (result.isBlank()) {
            Toast.makeText(requireContext(), R.string.enter_text_here, Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MobiTools Text", result))
        Toast.makeText(requireContext(), R.string.text_tools_copied, Toast.LENGTH_SHORT).show()

        val input = viewBinding.etInputText.text?.toString() ?: ""
        saveHistoryRecord(input, result, currentMode)
    }

    // Ghi nhận bản ghi xử lý mới vào danh sách lịch sử cục bộ.
    private fun saveHistoryRecord(input: String, result: String, mode: TextTransformMode) {
        val newItem = TextHistoryItem(
            input = input,
            result = result,
            modeName = mode.name,
            timestamp = System.currentTimeMillis()
        )
        historyList.removeAll { it.input == input && it.modeName == mode.name }
        historyList.add(0, newItem)
        saveHistoryToPrefs()
    }

    // Lưu danh sách lịch sử vào bộ nhớ chia sẻ SharedPreferences.
    private fun saveHistoryToPrefs() {
        val array = JSONArray()
        for (item in historyList.take(40)) {
            val obj = JSONObject().apply {
                put("input", item.input)
                put("result", item.result)
                put("modeName", item.modeName)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString("text_tools_history_json", array.toString()).apply()
    }

    // Nạp lịch sử xử lý từ bộ nhớ chia sẻ SharedPreferences.
    private fun loadHistoryFromPrefs() {
        historyList.clear()
        val json = prefs.getString("text_tools_history_json", null) ?: return
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                historyList.add(
                    TextHistoryItem(
                        input = obj.getString("input"),
                        result = obj.getString("result"),
                        modeName = obj.optString("modeName", TextTransformMode.UPPERCASE.name),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {
            historyList.clear()
        }
    }

    // Hiển thị hộp thoại danh sách lịch sử cho phép nạp lại nội dung hoặc xóa lịch sử.
    private fun showHistoryDialog() {
        if (historyList.isEmpty()) {
            Toast.makeText(requireContext(), R.string.text_tools_history_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val displayItems = historyList.map { item ->
            val modeTitle = getModeDisplayName(item.modeName)
            val time = dateFormat.format(Date(item.timestamp))
            val preview = if (item.result.length > 50) item.result.take(50) + "…" else item.result
            "[$modeTitle • $time]\n$preview"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.text_tools_history_title)
            .setItems(displayItems) { _, which ->
                val selected = historyList[which]
                viewBinding.etInputText.setText(selected.input)
                val targetMode = try {
                    TextTransformMode.valueOf(selected.modeName)
                } catch (_: Exception) {
                    TextTransformMode.UPPERCASE
                }
                setMode(targetMode)
            }
            .setNeutralButton(R.string.calc_clear_history) { _, _ ->
                historyList.clear()
                saveHistoryToPrefs()
                Toast.makeText(requireContext(), R.string.text_tools_history_empty, Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    // Chuyển đổi tên enum chế độ sang tên hiển thị tiếng Việt tương ứng.
    private fun getModeDisplayName(modeName: String): String {
        return when (modeName) {
            TextTransformMode.UPPERCASE.name -> getString(R.string.text_tools_action_uppercase)
            TextTransformMode.LOWERCASE.name -> getString(R.string.text_tools_action_lowercase)
            TextTransformMode.TITLE_CASE.name -> getString(R.string.text_tools_action_titlecase)
            TextTransformMode.SENTENCE_CASE.name -> getString(R.string.text_tools_action_sentencecase)
            TextTransformMode.TRIM_SPACES.name -> getString(R.string.text_tools_action_trim_spaces)
            TextTransformMode.REMOVE_LINES.name -> getString(R.string.text_tools_action_remove_lines)
            else -> modeName
        }
    }
}
