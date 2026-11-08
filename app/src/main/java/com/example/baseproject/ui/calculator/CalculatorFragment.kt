package com.example.baseproject.ui.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentCalculatorBinding
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
 * Công cụ máy tính thông minh hỗ trợ chế độ Tiêu chuẩn (4x5) và Khoa học (5x7)
 * với giao diện phẳng sáng tối ưu, kết nối tới trang Lịch sử tính toán riêng biệt.
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

    // Định dạng số hiển thị theo chuẩn Việt Nam: dấu chấm phân tách nghìn, dấu phẩy phân tách thập phân
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

    // Khởi tạo các thành phần giao diện, phím bấm và đăng ký nhận kết quả từ trang lịch sử.
    override fun initViews() {
        setupTopBar()
        setupStandardKeypad()
        setupScientificKeypad()
        setupHistoryResultListener()
        updateModeDisplay()
        updateDisplay()
    }

    // Thiết lập thanh tiêu đề với nút quay lại và nút mở trang Lịch sử tính toán riêng biệt.
    private fun setupTopBar() {
        viewBinding.btnCalcBack.setOnClickListener {
            vibrateKey()
            parentFragmentManager.popBackStack()
        }

        viewBinding.btnCalcHistory.setOnClickListener {
            vibrateKey()
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, CalculatorHistoryFragment.newInstance())
                addToBackStack(null)
            }
        }
    }

    // Đăng ký lắng nghe khi người dùng chọn một phép tính từ trang Lịch sử để nạp lại vào máy tính.
    private fun setupHistoryResultListener() {
        parentFragmentManager.setFragmentResultListener(
            CalculatorHistoryFragment.REQUEST_KEY_HISTORY_SELECT,
            viewLifecycleOwner
        ) { _, bundle ->
            val exp = bundle.getString(CalculatorHistoryFragment.EXTRA_EXPRESSION)
            val res = bundle.getString(CalculatorHistoryFragment.EXTRA_RESULT)
            if (!exp.isNullOrEmpty() && !res.isNullOrEmpty()) {
                rawExpression = res.replace(".", "").replace(",", ".")
                isResultCalculated = true
                viewBinding.tvExpression.text = exp
                viewBinding.tvPreviewResult.text = "= $res"
            }
        }
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
        binding.btnSciFact.setOnClickListener { appendOperator("!"); vibrateKey() }
        binding.btnSciInverse.setOnClickListener { appendReciprocal(); vibrateKey() }
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
        binding.btnSciLg.setOnClickListener { handleLogOrTenPow(); vibrateKey() }
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
        viewBinding.btnSci2nd.backgroundTintList = ColorStateList.valueOf(if (is2ndActive) activeBg else normalBg)

        // Đổi nhãn các phím lượng giác và hàm tương ứng
        viewBinding.btnSciSin.text = if (is2ndActive) "sin⁻¹" else "sin"
        viewBinding.btnSciCos.text = if (is2ndActive) "cos⁻¹" else "cos"
        viewBinding.btnSciTan.text = if (is2ndActive) "tan⁻¹" else "tan"
        viewBinding.btnSciLn.text = if (is2ndActive) "eˣ" else "ln"
        viewBinding.btnSciLg.text = if (is2ndActive) "10ˣ" else "lg"
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

    // Thêm hàm nghịch đảo 1/x hoặc bao quanh số trước đó.
    private fun appendReciprocal() {
        if (rawExpression.isNotEmpty()) {
            val lastNum = extractLastNumber(rawExpression)
            if (lastNum.isNotEmpty()) {
                rawExpression = rawExpression.dropLast(lastNum.length) + "1 ÷ ($lastNum)"
            } else {
                rawExpression += "1 ÷ ("
            }
        } else {
            rawExpression = "1 ÷ ("
        }
        isResultCalculated = false
        updateDisplay()
        previewResult()
    }

    // Xử lý các hàm lượng giác chuẩn hoặc lượng giác nghịch đảo khi phím 2nd bật.
    private fun handleTrigOrInverse(normalFunc: String, inverseFunc: String) {
        val func = if (is2ndActive) inverseFunc else normalFunc
        appendFunctionWithParen(func)
    }

    // Xử lý hàm logarit tự nhiên ln hoặc hàm mũ e^x khi phím 2nd bật.
    private fun handleLnOrExp() {
        if (is2ndActive) {
            appendConstant("e")
            appendOperator("^")
        } else {
            appendFunctionWithParen("ln")
        }
    }

    // Xử lý hàm logarit cơ số 10 hoặc lũy thừa 10^x khi phím 2nd bật.
    private fun handleLogOrTenPow() {
        if (is2ndActive) {
            rawExpression += "10 ^ "
            isResultCalculated = false
            updateDisplay()
        } else {
            appendFunctionWithParen("lg")
        }
    }

    // Xử lý hàm căn bậc hai hoặc hàm bình phương x^2 khi phím 2nd bật.
    private fun handleSqrtOrSquare() {
        if (is2ndActive) {
            appendOperator("^")
            rawExpression += "2"
            updateDisplay()
            previewResult()
        } else {
            appendFunctionWithParen("√")
        }
    }

    // Chèn tên hàm toán học kèm mở ngoặc và tự động chèn dấu nhân nếu đứng sau số.
    private fun appendFunctionWithParen(funcName: String) {
        if (isResultCalculated) {
            rawExpression = ""
            isResultCalculated = false
        }
        if (rawExpression.isNotEmpty() && (rawExpression.last().isDigit() || rawExpression.endsWith(")"))) {
            rawExpression += " × "
        }
        rawExpression += "$funcName("
        updateDisplay()
        previewResult()
    }

    // Xóa ký tự cuối cùng khỏi biểu thức khi bấm phím Backspace.
    private fun performBackspace() {
        if (isResultCalculated) {
            clearExpression()
            return
        }
        if (rawExpression.isEmpty()) return

        val functions = listOf("sin⁻¹(", "cos⁻¹(", "tan⁻¹(", "sin(", "cos(", "tan(", "ln(", "lg(", "√(")
        for (f in functions) {
            if (rawExpression.endsWith(f)) {
                rawExpression = rawExpression.dropLast(f.length)
                updateDisplay()
                previewResult()
                return
            }
        }

        if (rawExpression.endsWith(" ")) {
            rawExpression = rawExpression.trimEnd()
            if (rawExpression.endsWith("+") || rawExpression.endsWith("−") || rawExpression.endsWith("×") || rawExpression.endsWith("÷") || rawExpression.endsWith("^")) {
                rawExpression = rawExpression.dropLast(1).trimEnd()
            }
        } else {
            rawExpression = rawExpression.dropLast(1)
        }
        updateDisplay()
        previewResult()
    }

    // Xóa toàn bộ biểu thức đang nhập và làm sạch dòng xem trước.
    private fun clearExpression() {
        rawExpression = ""
        isResultCalculated = false
        viewBinding.tvExpression.text = ""
        viewBinding.tvPreviewResult.text = ""
    }

    // Thực hiện tính toán kết quả cuối cùng khi người dùng bấm phím bằng (=).
    private fun executeCalculate() {
        if (rawExpression.isEmpty()) return

        val cleanExpr = rawExpression.trim()
        val evaluated = evaluateExpression(cleanExpr)

        if (evaluated != null) {
            val formattedResult = formatResultNumber(evaluated)
            val formattedExpr = formatExpressionForDisplay(cleanExpr)

            // Lưu phép tính vào lịch sử theo ngày
            CalculatorHistoryManager.addRecord(requireContext(), formattedExpr, formattedResult)

            viewBinding.tvExpression.text = formattedExpr
            viewBinding.tvPreviewResult.text = "= $formattedResult"
            isResultCalculated = true
        } else {
            viewBinding.tvPreviewResult.text = getString(R.string.calc_error_syntax)
        }
    }

    // Tính toán trước kết quả và cập nhật hiển thị mờ bên dưới trong khi người dùng đang nhập.
    private fun previewResult() {
        if (rawExpression.isEmpty() || isResultCalculated) {
            viewBinding.tvPreviewResult.text = ""
            return
        }
        val cleanExpr = rawExpression.trim()
        if (cleanExpr.endsWith("+") || cleanExpr.endsWith("−") || cleanExpr.endsWith("×") ||
            cleanExpr.endsWith("÷") || cleanExpr.endsWith("(") || cleanExpr.endsWith("^")
        ) {
            viewBinding.tvPreviewResult.text = ""
            return
        }
        val evaluated = evaluateExpression(cleanExpr)
        if (evaluated != null && !evaluated.isNaN() && !evaluated.isInfinite()) {
            val formatted = formatResultNumber(evaluated)
            viewBinding.tvPreviewResult.text = "= $formatted"
        } else {
            viewBinding.tvPreviewResult.text = ""
        }
    }

    // Cập nhật chuỗi biểu thức hiển thị lên TextView và tự động cuộn về phía cuối.
    private fun updateDisplay() {
        val formatted = formatExpressionForDisplay(rawExpression)
        viewBinding.tvExpression.text = formatted
        viewBinding.scrollExpression.post {
            viewBinding.scrollExpression.fullScroll(View.FOCUS_RIGHT)
        }
    }

    // Chuẩn hóa định dạng hiển thị cho biểu thức: gom nhóm chữ số với dấu chấm ngăn cách.
    private fun formatExpressionForDisplay(expr: String): String {
        val regex = Regex("(\\d[\\d,.]*)")
        return regex.replace(expr) { matchResult ->
            val numStr = matchResult.value
            formatNumberString(numStr)
        }
    }

    // Định dạng chuỗi số với dấu chấm ngăn cách hàng nghìn và dấu phẩy phân tách thập phân.
    private fun formatNumberString(str: String): String {
        val parts = str.split(",")
        val intPart = parts[0].replace(".", "")
        if (intPart.isEmpty()) return str

        val formattedInt = try {
            val d = intPart.toDouble()
            val df = DecimalFormat("#,##0", displayFormatSymbols).apply { isGroupingUsed = true }
            df.format(d)
        } catch (e: Exception) {
            intPart
        }

        return if (parts.size > 1) {
            "$formattedInt,${parts[1]}"
        } else {
            formattedInt
        }
    }

    // Định dạng kết quả số học sang chuỗi hiển thị gọn gàng theo chuẩn Việt Nam.
    private fun formatResultNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        return if (abs(value) >= 1e12 || (abs(value) > 0 && abs(value) < 1e-6)) {
            val sciFormat = DecimalFormat("0.######E0", displayFormatSymbols)
            sciFormat.format(value).replace("E", " × 10^")
        } else {
            resultDecimalFormat.format(value)
        }
    }

    // Trích xuất số cuối cùng đang được gõ ở cuối biểu thức.
    private fun extractLastNumber(expr: String): String {
        var i = expr.length - 1
        while (i >= 0 && (expr[i].isDigit() || expr[i] == ',' || expr[i] == '.')) {
            i--
        }
        return expr.substring(i + 1)
    }

    // Đánh giá biểu thức toán học và trả về kết quả số thực dạng Double.
    private fun evaluateExpression(expression: String): Double? {
        return try {
            val tokens = tokenizeExpression(expression)
            val rpn = infixToRPN(tokens)
            evaluateRPN(rpn)
        } catch (e: Exception) {
            null
        }
    }

    // Tách chuỗi biểu thức thành danh sách các token toán học riêng rẽ.
    private fun tokenizeExpression(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val s = expr.replace(" ", "")

        while (i < s.length) {
            val c = s[i]

            val matchedFunc = listOf("sin⁻¹", "cos⁻¹", "tan⁻¹", "sin", "cos", "tan", "ln", "lg").firstOrNull { s.startsWith(it, i) }
            if (matchedFunc != null) {
                tokens.add(matchedFunc)
                i += matchedFunc.length
                continue
            }

            if (c.isDigit() || c == ',' || (c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "−", "×", "÷", "(", "^")))) {
                val sb = StringBuilder()
                if (c == '-') {
                    sb.append('-')
                    i++
                }
                while (i < s.length && (s[i].isDigit() || s[i] == ',' || s[i] == '.')) {
                    if (s[i] == ',') sb.append('.') else if (s[i] != '.') sb.append(s[i])
                    i++
                }
                tokens.add(sb.toString())
                continue
            }

            when (c) {
                '+', '−', '-', '×', '*', '÷', '/', '%', '^', '!', '(', ')', '√', 'π', 'e' -> {
                    tokens.add(c.toString())
                    i++
                }
                else -> i++
            }
        }
        return tokens
    }

    // Chuyển đổi biểu thức từ dạng trung tố (Infix) sang hậu tố (RPN) theo thuật toán Shunting-Yard.
    private fun infixToRPN(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val opStack = Stack<String>()

        fun precedence(op: String): Int = when (op) {
            "+", "−", "-" -> 1
            "×", "*", "÷", "/", "%" -> 2
            "^" -> 3
            "!", "√", "sin", "cos", "tan", "sin⁻¹", "cos⁻¹", "tan⁻¹", "ln", "lg" -> 4
            else -> 0
        }

        fun isRightAssociative(op: String): Boolean = op == "^"

        for (token in tokens) {
            if (token.toDoubleOrNull() != null) {
                output.add(token)
            } else if (token == "π") {
                output.add(Math.PI.toString())
            } else if (token == "e") {
                output.add(Math.E.toString())
            } else if (token in listOf("sin", "cos", "tan", "sin⁻¹", "cos⁻¹", "tan⁻¹", "ln", "lg", "√")) {
                opStack.push(token)
            } else if (token == "(") {
                opStack.push(token)
            } else if (token == ")") {
                while (opStack.isNotEmpty() && opStack.peek() != "(") {
                    output.add(opStack.pop())
                }
                if (opStack.isNotEmpty() && opStack.peek() == "(") {
                    opStack.pop()
                }
                if (opStack.isNotEmpty() && opStack.peek() in listOf("sin", "cos", "tan", "sin⁻¹", "cos⁻¹", "tan⁻¹", "ln", "lg", "√")) {
                    output.add(opStack.pop())
                }
            } else {
                while (opStack.isNotEmpty() && opStack.peek() != "(" &&
                    (precedence(opStack.peek()) > precedence(token) ||
                            (precedence(opStack.peek()) == precedence(token) && !isRightAssociative(token)))
                ) {
                    output.add(opStack.pop())
                }
                opStack.push(token)
            }
        }

        while (opStack.isNotEmpty()) {
            val top = opStack.pop()
            if (top != "(" && top != ")") {
                output.add(top)
            }
        }
        return output
    }

    // Đánh giá danh sách token hậu tố RPN và trả về kết quả số thực.
    private fun evaluateRPN(tokens: List<String>): Double {
        val stack = Stack<Double>()

        for (token in tokens) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                stack.push(num)
                continue
            }

            when (token) {
                "+" -> {
                    val b = stack.pop()
                    val a = stack.pop()
                    stack.push(a + b)
                }
                "−", "-" -> {
                    val b = stack.pop()
                    val a = stack.pop()
                    stack.push(a - b)
                }
                "×", "*" -> {
                    val b = stack.pop()
                    val a = stack.pop()
                    stack.push(a * b)
                }
                "÷", "/" -> {
                    val b = stack.pop()
                    val a = stack.pop()
                    if (b == 0.0) throw ArithmeticException("Divide by zero")
                    stack.push(a / b)
                }
                "%" -> {
                    val a = stack.pop()
                    stack.push(a / 100.0)
                }
                "^" -> {
                    val b = stack.pop()
                    val a = stack.pop()
                    stack.push(a.pow(b))
                }
                "!" -> {
                    val a = stack.pop()
                    stack.push(factorial(a.toInt()).toDouble())
                }
                "√" -> {
                    val a = stack.pop()
                    stack.push(sqrt(a))
                }
                "sin" -> {
                    val a = stack.pop()
                    val rad = if (isDegreeMode) Math.toRadians(a) else a
                    stack.push(sin(rad))
                }
                "cos" -> {
                    val a = stack.pop()
                    val rad = if (isDegreeMode) Math.toRadians(a) else a
                    stack.push(cos(rad))
                }
                "tan" -> {
                    val a = stack.pop()
                    val rad = if (isDegreeMode) Math.toRadians(a) else a
                    stack.push(tan(rad))
                }
                "sin⁻¹" -> {
                    val a = stack.pop()
                    val rad = asin(a)
                    stack.push(if (isDegreeMode) Math.toDegrees(rad) else rad)
                }
                "cos⁻¹" -> {
                    val a = stack.pop()
                    val rad = acos(a)
                    stack.push(if (isDegreeMode) Math.toDegrees(rad) else rad)
                }
                "tan⁻¹" -> {
                    val a = stack.pop()
                    val rad = atan(a)
                    stack.push(if (isDegreeMode) Math.toDegrees(rad) else rad)
                }
                "ln" -> {
                    val a = stack.pop()
                    stack.push(ln(a))
                }
                "lg" -> {
                    val a = stack.pop()
                    stack.push(log10(a))
                }
            }
        }
        return if (stack.isNotEmpty()) stack.pop() else 0.0
    }

    // Tính giai thừa số nguyên dương n!
    private fun factorial(n: Int): Long {
        if (n < 0) return 0
        var res = 1L
        for (i in 2..n.coerceAtMost(20)) {
            res *= i
        }
        return res
    }

    // Phát rung xúc giác nhẹ khi bấm các phím số và toán tử.
    private fun vibrateKey() {
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    // Phát rung phản hồi mạnh hơn khi hoàn thành phép tính bằng dấu =.
    private fun vibrateConfirm() {
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
}
