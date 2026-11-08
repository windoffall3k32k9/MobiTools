package com.example.baseproject.ui.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentCalculatorBinding
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Stack
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Công cụ máy tính thông minh hỗ trợ chế độ Tiêu chuẩn và Khoa học mở rộng với theme sáng tối ưu.
 */
class CalculatorFragment : BaseToolFragment<FragmentCalculatorBinding>() {

    override val toolId: String = "calculator"
    override val toolTitleResId: Int = R.string.tool_calculator_title

    // Biểu thức thô và trạng thái nhập liệu
    private var rawExpression: String = ""
    private var isResultCalculated: Boolean = false
    private var isScientificMode: Boolean = false
    private var is2ndActive: Boolean = false
    private var isDegreeMode: Boolean = true

    // Danh sách lưu trữ lịch sử các phép tính
    data class CalculationHistory(val expression: String, val result: String)
    private val calculationHistory = mutableListOf<CalculationHistory>()

    private val prefs by lazy {
        requireContext().getSharedPreferences("calculator_prefs", Context.MODE_PRIVATE)
    }

    // Định dạng số hiển thị theo chuẩn Việt Nam: dấu chấm ngăn cách nghìn, dấu phẩy ngăn cách thập phân
    private val displayFormatSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val resultDecimalFormat = DecimalFormat("#,##0.########", displayFormatSymbols).apply {
        isGroupingUsed = true
    }

    // Tạo đối tượng ViewBinding cho giao diện FragmentCalculator.
    override fun provideViewBinding(container: ViewGroup?): FragmentCalculatorBinding {
        return FragmentCalculatorBinding.inflate(layoutInflater, container, false)
    }

    // Khởi tạo các thành phần giao diện, phục hồi lịch sử tính toán và gắn sự kiện phím bấm.
    override fun initViews() {
        loadHistoryFromPrefs()
        setupToolbarActions()
        setupStandardKeypad()
        setupScientificKeypad()
        updateModeDisplay()
        renderHistoryList()
        updateDisplay()
    }

    // Cấu hình các nút chức năng trên thanh công cụ: Lịch sử tính toán và Menu mở rộng.
    private fun setupToolbarActions() {
        val btnAction = view?.findViewById<ImageView>(R.id.btnToolbarAction)
        btnAction?.let { btn ->
            btn.visibility = View.VISIBLE
            btn.setImageResource(R.drawable.ic_history)
            btn.setOnClickListener { showHistoryDialog() }
        }

        val btnMore = view?.findViewById<ImageView>(R.id.btnToolbarMore)
        btnMore?.let { btn ->
            btn.visibility = View.VISIBLE
            btn.setOnClickListener { anchor -> showOptionsMenu(anchor) }
        }
    }

    // Hiển thị popup menu tùy chọn với các lệnh xóa lịch sử và đổi đơn vị góc.
    private fun showOptionsMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, R.string.calc_history_title)
        val angleTitle = if (isDegreeMode) R.string.calc_angle_deg else R.string.calc_angle_rad
        popup.menu.add(0, 2, 1, angleTitle)
        popup.menu.add(0, 3, 2, R.string.calc_clear_history)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showHistoryDialog()
                    true
                }
                2 -> {
                    toggleAngleMode()
                    true
                }
                3 -> {
                    clearAllHistory()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // Thiết lập sự kiện cho toàn bộ bàn phím chế độ Tiêu chuẩn (4 x 5).
    private fun setupStandardKeypad() {
        val binding = viewBinding

        // Các phím số
        val numMap = mapOf(
            binding.btnStd0 to "0", binding.btnStd1 to "1", binding.btnStd2 to "2",
            binding.btnStd3 to "3", binding.btnStd4 to "4", binding.btnStd5 to "5",
            binding.btnStd6 to "6", binding.btnStd7 to "7", binding.btnStd8 to "8",
            binding.btnStd9 to "9"
        )
        for ((btn, digit) in numMap) {
            btn.setOnClickListener {
                appendDigit(digit)
                vibrateKey()
            }
        }

        // Dấu phẩy thập phân
        binding.btnStdComma.setOnClickListener {
            appendComma()
            vibrateKey()
        }

        // Toán tử cơ bản
        binding.btnStdAdd.setOnClickListener { appendOperator("+"); vibrateKey() }
        binding.btnStdSubtract.setOnClickListener { appendOperator("−"); vibrateKey() }
        binding.btnStdMultiply.setOnClickListener { appendOperator("×"); vibrateKey() }
        binding.btnStdDivide.setOnClickListener { appendOperator("÷"); vibrateKey() }
        binding.btnStdPercent.setOnClickListener { appendOperator("%"); vibrateKey() }

        // Xóa và xóa lùi
        binding.btnStdC.setOnClickListener { clearExpression(); vibrateKey() }
        binding.btnStdBackspace.setOnClickListener { performBackspace(); vibrateKey() }

        // Phím bằng tính kết quả
        binding.btnStdEquals.setOnClickListener { executeCalculate(); vibrateConfirm() }

        // Phím chuyển sang chế độ Khoa học nâng cao
        binding.btnStdToggleMode.setOnClickListener {
            isScientificMode = true
            updateModeDisplay()
            vibrateKey()
        }
    }

    // Thiết lập sự kiện cho toàn bộ bàn phím chế độ Khoa học mở rộng (5 x 7).
    private fun setupScientificKeypad() {
        val binding = viewBinding

        // Các phím số ở bàn phím nâng cao
        val numMap = mapOf(
            binding.btnSci0 to "0", binding.btnSci1 to "1", binding.btnSci2 to "2",
            binding.btnSci3 to "3", binding.btnSci4 to "4", binding.btnSci5 to "5",
            binding.btnSci6 to "6", binding.btnSci7 to "7", binding.btnSci8 to "8",
            binding.btnSci9 to "9"
        )
        for ((btn, digit) in numMap) {
            btn.setOnClickListener {
                appendDigit(digit)
                vibrateKey()
            }
        }

        binding.btnSciComma.setOnClickListener { appendComma(); vibrateKey() }

        // Toán tử
        binding.btnSciAdd.setOnClickListener { appendOperator("+"); vibrateKey() }
        binding.btnSciSubtract.setOnClickListener { appendOperator("−"); vibrateKey() }
        binding.btnSciMultiply.setOnClickListener { appendOperator("×"); vibrateKey() }
        binding.btnSciDivide.setOnClickListener { appendOperator("÷"); vibrateKey() }
        binding.btnSciPercent.setOnClickListener { appendOperator("%"); vibrateKey() }

        // Phím chức năng khoa học
        binding.btnSciOpenParen.setOnClickListener { appendToken("("); vibrateKey() }
        binding.btnSciCloseParen.setOnClickListener { appendToken(")"); vibrateKey() }
        binding.btnSciPower.setOnClickListener { appendOperator("^"); vibrateKey() }
        binding.btnSciFactorial.setOnClickListener { appendOperator("!"); vibrateKey() }
        binding.btnSciReciprocal.setOnClickListener { appendReciprocal(); vibrateKey() }
        binding.btnSciPi.setOnClickListener { appendConstant("π"); vibrateKey() }
        binding.btnSciE.setOnClickListener { appendConstant("e"); vibrateKey() }

        // Chuyển đổi trạng thái 2nd và DEG/RAD
        binding.btnSci2nd.setOnClickListener { toggle2ndMode(); vibrateKey() }
        binding.btnSciAngleMode.setOnClickListener { toggleAngleMode(); vibrateKey() }

        // Hàm lượng giác và logarit
        binding.btnSciSin.setOnClickListener { handleTrigOrInverse("sin", "sin⁻¹"); vibrateKey() }
        binding.btnSciCos.setOnClickListener { handleTrigOrInverse("cos", "cos⁻¹"); vibrateKey() }
        binding.btnSciTan.setOnClickListener { handleTrigOrInverse("tan", "tan⁻¹"); vibrateKey() }
        binding.btnSciLn.setOnClickListener { handleLnOrExp(); vibrateKey() }
        binding.btnSciLog.setOnClickListener { handleLogOrTenPow(); vibrateKey() }
        binding.btnSciSqrt.setOnClickListener { handleSqrtOrSquare(); vibrateKey() }

        // Xóa, lùi, bằng và toggle thu nhỏ
        binding.btnSciC.setOnClickListener { clearExpression(); vibrateKey() }
        binding.btnSciBackspace.setOnClickListener { performBackspace(); vibrateKey() }
        binding.btnSciEquals.setOnClickListener { executeCalculate(); vibrateConfirm() }
        binding.btnSciToggleMode.setOnClickListener {
            isScientificMode = false
            updateModeDisplay()
            vibrateKey()
        }
    }

    // Bật tắt hiển thị giữa hai chế độ bàn phím Tiêu chuẩn và Khoa học.
    private fun updateModeDisplay() {
        viewBinding.layoutStandardKeypad.isVisible = !isScientificMode
        viewBinding.layoutScientificKeypad.isVisible = isScientificMode
    }

    // Bật tắt trạng thái phím chức năng bậc hai (2nd) để hoán đổi hàm lượng giác nghịch đảo.
    private fun toggle2ndMode() {
        is2ndActive = !is2ndActive

        val activeColor = Color.parseColor("#FF7A00")
        val normalColor = ContextCompat.getColor(requireContext(), R.color.textPrimary)
        val activeBg = Color.parseColor("#FFEDD5")
        val normalBg = ContextCompat.getColor(requireContext(), R.color.calcButtonBg)

        viewBinding.btnSci2nd.setTextColor(if (is2ndActive) activeColor else normalColor)
        viewBinding.btnSci2nd.setCardBackgroundColorCompat(if (is2ndActive) activeBg else normalBg)

        // Đổi nhãn các phím lượng giác và hàm tương ứng
        viewBinding.btnSciSin.text = if (is2ndActive) "sin⁻¹" else "sin"
        viewBinding.btnSciCos.text = if (is2ndActive) "cos⁻¹" else "cos"
        viewBinding.btnSciTan.text = if (is2ndActive) "tan⁻¹" else "tan"
        viewBinding.btnSciLn.text = if (is2ndActive) "eˣ" else "ln"
        viewBinding.btnSciLog.text = if (is2ndActive) "10ˣ" else "lg"
        viewBinding.btnSciSqrt.text = if (is2ndActive) "x²" else "√x"
    }

    // Đổi qua lại giữa đơn vị đo góc Độ (DEG) và Radian (RAD).
    private fun toggleAngleMode() {
        isDegreeMode = !isDegreeMode
        viewBinding.btnSciAngleMode.text = if (isDegreeMode) "deg" else "rad"
        val activeColor = if (!isDegreeMode) Color.parseColor("#FF7A00") else ContextCompat.getColor(requireContext(), R.color.textPrimary)
        viewBinding.btnSciAngleMode.setTextColor(activeColor)
        previewResult()
    }

    // Thêm chữ số vào biểu thức và tự động làm mới nếu vừa tính xong kết quả trước.
    private fun appendDigit(digit: String) {
        if (isResultCalculated) {
            rawExpression = ""
            isResultCalculated = false
        }
        rawExpression += digit
        updateDisplay()
        previewResult()
    }

    // Thêm dấu phẩy phân tách thập phân vào biểu thức hợp lệ.
    private fun appendComma() {
        if (isResultCalculated) {
            rawExpression = "0,"
            isResultCalculated = false
            updateDisplay()
            previewResult()
            return
        }

        val lastNumberToken = extractLastNumber(rawExpression)
        if (lastNumberToken.contains(",")) return

        if (lastNumberToken.isEmpty() || rawExpression.endsWith(" ") || rawExpression.endsWith("(") || rawExpression.isEmpty()) {
            rawExpression += "0,"
        } else {
            rawExpression += ","
        }
        updateDisplay()
        previewResult()
    }

    // Thêm toán tử số học với khoảng cách phân tách rõ ràng.
    private fun appendOperator(op: String) {
        if (rawExpression.isEmpty() && op == "−") {
            rawExpression = "−"
            isResultCalculated = false
            updateDisplay()
            return
        }
        if (rawExpression.isEmpty()) return

        isResultCalculated = false
        val trimmed = rawExpression.trimEnd()

        if (trimmed.endsWith("+") || trimmed.endsWith("−") || trimmed.endsWith("×") || trimmed.endsWith("÷") || trimmed.endsWith("^")) {
            rawExpression = trimmed.dropLast(1).trimEnd() + " $op "
        } else if (!trimmed.endsWith("(")) {
            rawExpression = "$trimmed $op "
        }
        updateDisplay()
    }

    // Thêm ký tự token đặc biệt hoặc hàm toán học vào biểu thức.
    private fun appendToken(token: String) {
        if (isResultCalculated && token == "(") {
            rawExpression = ""
            isResultCalculated = false
        } else if (isResultCalculated) {
            isResultCalculated = false
        }
        rawExpression += token
        updateDisplay()
        previewResult()
    }

    // Thêm hằng số toán học Pi hoặc số Euler vào biểu thức.
    private fun appendConstant(constName: String) {
        if (isResultCalculated) {
            rawExpression = ""
            isResultCalculated = false
        }
        if (rawExpression.isNotEmpty() && (rawExpression.last().isDigit() || rawExpression.endsWith(")"))) {
            rawExpression += " × "
        }
        rawExpression += constName
        updateDisplay()
        previewResult()
    }

    // Xử lý chèn hàm nghịch đảo 1/x dạng biểu thức phân số.
    private fun appendReciprocal() {
        if (isResultCalculated) {
            rawExpression = "1/($rawExpression)"
            isResultCalculated = false
        } else {
            rawExpression += "1/("
        }
        updateDisplay()
        previewResult()
    }

    // Xử lý các hàm lượng giác chuẩn hoặc lượng giác nghịch đảo dựa trên phím 2nd.
    private fun handleTrigOrInverse(normal: String, inverse: String) {
        if (isResultCalculated) {
            rawExpression = ""
            isResultCalculated = false
        }
        val func = if (is2ndActive) inverse else normal
        rawExpression += "$func("
        updateDisplay()
        previewResult()
    }

    // Xử lý chèn hàm ln hoặc lũy thừa e^x.
    private fun handleLnOrExp() {
        if (isResultCalculated) isResultCalculated = false
        rawExpression += if (is2ndActive) "e^(" else "ln("
        updateDisplay()
        previewResult()
    }

    // Xử lý chèn hàm logarit cơ số 10 hoặc lũy thừa 10^x.
    private fun handleLogOrTenPow() {
        if (isResultCalculated) isResultCalculated = false
        rawExpression += if (is2ndActive) "10^(" else "lg("
        updateDisplay()
        previewResult()
    }

    // Xử lý căn bậc hai hoặc bình phương dựa trên phím 2nd.
    private fun handleSqrtOrSquare() {
        if (is2ndActive) {
            rawExpression += "^2"
        } else {
            if (isResultCalculated) isResultCalculated = false
            rawExpression += "√("
        }
        updateDisplay()
        previewResult()
    }

    // Xóa toàn bộ biểu thức đang nhập và đặt lại màn hình.
    private fun clearExpression() {
        rawExpression = ""
        isResultCalculated = false
        viewBinding.tvExpression.text = ""
        viewBinding.tvPreviewResult.text = ""
    }

    // Thực hiện xóa lùi ký tự cuối cùng của chuỗi biểu thức.
    private fun performBackspace() {
        if (rawExpression.isNotEmpty()) {
            val trimmed = rawExpression.trimEnd()
            when {
                trimmed.endsWith("sin⁻¹(") || trimmed.endsWith("cos⁻¹(") || trimmed.endsWith("tan⁻¹(") -> {
                    rawExpression = trimmed.dropLast(7)
                }
                trimmed.endsWith("sin(") || trimmed.endsWith("cos(") || trimmed.endsWith("tan(") || trimmed.endsWith("1/(") -> {
                    rawExpression = trimmed.dropLast(4)
                }
                trimmed.endsWith("ln(") || trimmed.endsWith("lg(") || trimmed.endsWith("√(") -> {
                    rawExpression = trimmed.dropLast(3)
                }
                trimmed.endsWith("+") || trimmed.endsWith("−") || trimmed.endsWith("×") || trimmed.endsWith("÷") || trimmed.endsWith("^") -> {
                    rawExpression = trimmed.dropLast(1).trimEnd()
                }
                else -> {
                    rawExpression = rawExpression.dropLast(1)
                }
            }
            isResultCalculated = false
            updateDisplay()
            previewResult()
        }
    }

    // Cập nhật chuỗi biểu thức hiển thị lên giao diện kèm định dạng dấu chấm ngăn cách nghìn.
    private fun updateDisplay() {
        val formatted = formatExpressionForDisplay(rawExpression)
        viewBinding.tvExpression.text = formatted
        viewBinding.scrollExpression.post {
            viewBinding.scrollExpression.fullScroll(View.FOCUS_RIGHT)
        }
    }

    // Tính toán trước kết quả và hiển thị mờ bên dưới dòng biểu thức chính.
    private fun previewResult() {
        if (rawExpression.isBlank()) {
            viewBinding.tvPreviewResult.text = ""
            return
        }
        try {
            val eval = evaluateMath(rawExpression)
            if (!eval.isNaN() && !eval.isInfinite()) {
                val formatted = resultDecimalFormat.format(eval)
                viewBinding.tvPreviewResult.text = "= $formatted"
                viewBinding.scrollPreview.post {
                    viewBinding.scrollPreview.fullScroll(View.FOCUS_RIGHT)
                }
            } else {
                viewBinding.tvPreviewResult.text = ""
            }
        } catch (_: Exception) {
            viewBinding.tvPreviewResult.text = ""
        }
    }

    // Tính toán kết quả chính thức khi nhấn phím =, đưa vào lịch sử và cuộn màn hình.
    private fun executeCalculate() {
        if (rawExpression.isBlank()) return
        try {
            val eval = evaluateMath(rawExpression)
            if (eval.isInfinite()) {
                viewBinding.tvPreviewResult.text = getString(R.string.calc_error_divide_zero)
                return
            }
            if (eval.isNaN()) {
                viewBinding.tvPreviewResult.text = getString(R.string.calc_error_syntax)
                return
            }

            val formattedExpr = formatExpressionForDisplay(rawExpression)
            val formattedRes = resultDecimalFormat.format(eval)

            // Lưu vào danh sách lịch sử tính toán
            val historyItem = CalculationHistory(formattedExpr, formattedRes)
            calculationHistory.add(historyItem)
            saveHistoryToPrefs()

            // Thêm dòng hiển thị lên khu vực lịch sử
            addHistoryRow(historyItem)

            // Đặt lại biểu thức chính về kết quả vừa tính
            rawExpression = formattedRes.replace(".", "")
            isResultCalculated = true

            updateDisplay()
            viewBinding.tvPreviewResult.text = ""
        } catch (_: Exception) {
            viewBinding.tvPreviewResult.text = getString(R.string.calc_error_syntax)
        }
    }

    // Thêm một dòng phép tính hoàn chỉnh vào khung xem lịch sử phía trên.
    private fun addHistoryRow(item: CalculationHistory) {
        val context = requireContext()
        val tv = TextView(context).apply {
            text = "${item.expression} = ${item.result}"
            setTextColor(Color.parseColor("#8E95A2"))
            textSize = 16f
            gravity = Gravity.END
            setPadding(0, dpToPx(3), 0, dpToPx(3))
            setOnClickListener {
                rawExpression = item.result.replace(".", "")
                isResultCalculated = true
                updateDisplay()
                view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            setOnLongClickListener {
                copyToClipboard(item.result)
                true
            }
        }
        viewBinding.layoutHistoryList.addView(tv)
        viewBinding.scrollHistory.post {
            viewBinding.scrollHistory.fullScroll(View.FOCUS_DOWN)
        }
    }

    // Vẽ toàn bộ các bản ghi lịch sử hiện có lên danh sách cuộn.
    private fun renderHistoryList() {
        viewBinding.layoutHistoryList.removeAllViews()
        for (item in calculationHistory) {
            addHistoryRow(item)
        }
    }

    // Mở hộp thoại BottomSheet/Dialog hiển thị toàn bộ lịch sử kèm nút xóa sạch.
    private fun showHistoryDialog() {
        val context = requireContext()
        if (calculationHistory.isEmpty()) {
            Toast.makeText(context, R.string.calc_history_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val items = calculationHistory.map { "${it.expression} = ${it.result}" }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(R.string.calc_history_title)
            .setItems(items) { _, which ->
                val selected = calculationHistory[which]
                rawExpression = selected.result.replace(".", "")
                isResultCalculated = true
                updateDisplay()
                previewResult()
            }
            .setNeutralButton(R.string.calc_clear_history) { _, _ ->
                clearAllHistory()
            }
            .setPositiveButton(R.string.close, null)
            .show()
    }

    // Xóa sạch toàn bộ lịch sử phép tính đã lưu trong bộ nhớ và cơ sở dữ liệu.
    private fun clearAllHistory() {
        calculationHistory.clear()
        saveHistoryToPrefs()
        viewBinding.layoutHistoryList.removeAllViews()
        Toast.makeText(requireContext(), R.string.calc_clear_history, Toast.LENGTH_SHORT).show()
    }

    // Sao chép kết quả số vừa chọn vào bộ nhớ tạm hệ thống.
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Calculator Result", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.calc_copied, Toast.LENGTH_SHORT).show()
    }

    // Lưu danh sách lịch sử phép tính vào SharedPreferences dạng JSON.
    private fun saveHistoryToPrefs() {
        val array = JSONArray()
        for (item in calculationHistory) {
            val obj = JSONObject()
            obj.put("expr", item.expression)
            obj.put("res", item.result)
            array.put(obj)
        }
        prefs.edit().putString("calc_history_json", array.toString()).apply()
    }

    // Khôi phục danh sách lịch sử phép tính từ SharedPreferences.
    private fun loadHistoryFromPrefs() {
        calculationHistory.clear()
        val json = prefs.getString("calc_history_json", null) ?: return
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                calculationHistory.add(CalculationHistory(obj.getString("expr"), obj.getString("res")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Định dạng chuỗi biểu thức với dấu chấm phân tách phần nghìn cho từng nhóm số.
    private fun formatExpressionForDisplay(raw: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c.isDigit()) {
                val numStart = i
                while (i < raw.length && (raw[i].isDigit() || raw[i] == ',')) {
                    i++
                }
                val numStr = raw.substring(numStart, i)
                sb.append(formatNumberToken(numStr))
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    // Định dạng riêng lẻ từng token số nguyên hoặc số thập phân.
    private fun formatNumberToken(numStr: String): String {
        if (!numStr.contains(",")) {
            val intPart = numStr.toLongOrNull() ?: return numStr
            return DecimalFormat("#,###", displayFormatSymbols).format(intPart)
        }
        val parts = numStr.split(",")
        val intPart = parts[0].toLongOrNull()
        val formattedInt = if (intPart != null) DecimalFormat("#,###", displayFormatSymbols).format(intPart) else parts[0]
        return if (parts.size > 1) "$formattedInt,${parts[1]}" else "$formattedInt,"
    }

    // Trích xuất token số cuối cùng đang được người dùng nhập dở.
    private fun extractLastNumber(expr: String): String {
        val tokens = expr.split(' ', '+', '−', '×', '÷', '%', '^', '(', ')')
        return tokens.lastOrNull() ?: ""
    }

    // Phân tích cú pháp và thực hiện tính toán biểu thức khoa học thông qua thuật toán Shunting-Yard.
    private fun evaluateMath(expression: String): Double {
        val prepared = prepareExpressionForEvaluation(expression)
        return evaluateRPN(shuntingYard(prepared))
    }

    // Chuẩn hóa biểu thức ký hiệu hiển thị sang các token toán học nội bộ.
    private fun prepareExpressionForEvaluation(expr: String): String {
        return expr
            .replace(".", "")
            .replace(",", ".")
            .replace("−", "-")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("sin⁻¹", "asin")
            .replace("cos⁻¹", "acos")
            .replace("tan⁻¹", "atan")
            .replace("lg", "log")
            .replace("√", "sqrt")
    }

    // Thuật toán Shunting-Yard biến đổi biểu thức trung tố thành biểu thức hậu tố RPN.
    private fun shuntingYard(expr: String): List<String> {
        val output = mutableListOf<String>()
        val ops = Stack<String>()
        val tokens = tokenizeExpression(expr)

        for (token in tokens) {
            when {
                token.toDoubleOrNull() != null -> output.add(token)
                token == "π" -> output.add(Math.PI.toString())
                token == "e" -> output.add(Math.E.toString())
                isFunctionName(token) -> ops.push(token)
                token == "(" -> ops.push(token)
                token == ")" -> {
                    while (ops.isNotEmpty() && ops.peek() != "(") {
                        output.add(ops.pop())
                    }
                    if (ops.isNotEmpty() && ops.peek() == "(") {
                        ops.pop()
                    }
                    if (ops.isNotEmpty() && isFunctionName(ops.peek())) {
                        output.add(ops.pop())
                    }
                }
                isOperator(token) -> {
                    while (ops.isNotEmpty() && isOperator(ops.peek()) &&
                        (getPrecedence(ops.peek()) > getPrecedence(token) ||
                                (getPrecedence(ops.peek()) == getPrecedence(token) && token != "^"))
                    ) {
                        output.add(ops.pop())
                    }
                    ops.push(token)
                }
            }
        }

        while (ops.isNotEmpty()) {
            output.add(ops.pop())
        }
        return output
    }

    // Tính toán giá trị số thực từ biểu thức ký pháp nghịch đảo Ba Lan (RPN).
    private fun evaluateRPN(tokens: List<String>): Double {
        val stack = Stack<Double>()

        for (token in tokens) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                stack.push(num)
            } else if (isFunctionName(token)) {
                if (stack.isEmpty()) return Double.NaN
                val a = stack.pop()
                val res = when (token) {
                    "sin" -> if (isDegreeMode) sin(Math.toRadians(a)) else sin(a)
                    "cos" -> if (isDegreeMode) cos(Math.toRadians(a)) else cos(a)
                    "tan" -> if (isDegreeMode) tan(Math.toRadians(a)) else tan(a)
                    "asin" -> if (isDegreeMode) Math.toDegrees(asin(a)) else asin(a)
                    "acos" -> if (isDegreeMode) Math.toDegrees(acos(a)) else acos(a)
                    "atan" -> if (isDegreeMode) Math.toDegrees(atan(a)) else atan(a)
                    "ln" -> ln(a)
                    "log" -> log10(a)
                    "sqrt" -> sqrt(a)
                    else -> Double.NaN
                }
                stack.push(res)
            } else if (token == "!") {
                if (stack.isEmpty()) return Double.NaN
                val a = stack.pop()
                stack.push(computeFactorial(a))
            } else if (isOperator(token)) {
                if (stack.size < 2) {
                    if (token == "-" && stack.size == 1) {
                        stack.push(-stack.pop())
                        continue
                    }
                    return Double.NaN
                }
                val b = stack.pop()
                val a = stack.pop()
                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b == 0.0) Double.POSITIVE_INFINITY else a / b
                    "%" -> (a * b) / 100.0
                    "^" -> a.pow(b)
                    else -> Double.NaN
                }
                stack.push(res)
            }
        }
        return if (stack.isNotEmpty()) stack.pop() else Double.NaN
    }

    // Tính giá trị giai thừa n! của một số thực nguyên dương.
    private fun computeFactorial(n: Double): Double {
        if (n < 0 || n != kotlin.math.floor(n)) return Double.NaN
        if (n > 170) return Double.POSITIVE_INFINITY
        var result = 1.0
        val count = n.toInt()
        for (i in 2..count) {
            result *= i
        }
        return result
    }

    // Tách chuỗi biểu thức thành danh sách token số học, hàm và toán tử.
    private fun tokenizeExpression(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        i++
                    }
                    tokens.add(expr.substring(start, i))
                }
                c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/", "%", "^", "(")) -> {
                    // Xử lý dấu âm đơn nguyên
                    i++
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        i++
                    }
                    if (start < i) {
                        tokens.add("-" + expr.substring(start, i))
                    } else {
                        tokens.add("-")
                    }
                }
                c in listOf('+', '-', '*', '/', '%', '^', '(', ')', '!', 'π', 'e') -> {
                    tokens.add(c.toString())
                    i++
                }
                c.isLetter() -> {
                    val start = i
                    while (i < expr.length && expr[i].isLetter()) {
                        i++
                    }
                    tokens.add(expr.substring(start, i))
                }
                else -> i++
            }
        }
        return tokens
    }

    // Kiểm tra token có phải là tên một hàm khoa học hay không.
    private fun isFunctionName(token: String): Boolean {
        return token in listOf("sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt")
    }

    // Kiểm tra token có phải là toán tử số học hay không.
    private fun isOperator(token: String): Boolean {
        return token in listOf("+", "-", "*", "/", "%", "^", "!")
    }

    // Xác định mức độ ưu tiên toán tử trong thuật toán phân tích cú pháp.
    private fun getPrecedence(op: String): Int {
        return when (op) {
            "!" -> 5
            "^" -> 4
            "*", "/", "%" -> 3
            "+", "-" -> 2
            else -> 0
        }
    }

    // Kích hoạt rung phản hồi nhẹ khi chạm phím máy tính.
    private fun vibrateKey() {
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    // Kích hoạt rung phản hồi xác nhận khi tính toán xong kết quả.
    private fun vibrateConfirm() {
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    // Đổi màu nền MaterialButton tương thích ngược.
    private fun View.setCardBackgroundColorCompat(color: Int) {
        if (this is com.google.android.material.button.MaterialButton) {
            this.backgroundTintList = ColorStateList.valueOf(color)
        }
    }

    // Chuyển đổi đơn vị dp sang px cho padding hiển thị lịch sử.
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
