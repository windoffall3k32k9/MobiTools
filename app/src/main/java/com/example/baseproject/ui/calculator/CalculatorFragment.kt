package com.example.baseproject.ui.calculator

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentCalculatorBinding
import java.text.DecimalFormat
import java.util.Stack

/**
 * Công cụ máy tính bỏ túi hỗ trợ các phép toán cơ bản, ngoặc đơn và phần trăm.
 */
class CalculatorFragment : BaseToolFragment<FragmentCalculatorBinding>() {

    override val toolId: String = "calculator"
    override val toolTitleResId: Int = R.string.tool_calculator_title

    private var currentExpression: String = ""
    private val decimalFormat = DecimalFormat("#,###.########")

    override fun provideViewBinding(container: ViewGroup?): FragmentCalculatorBinding {
        return FragmentCalculatorBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        setupClickListeners()
        updateDisplay()
    }

    // Gắn sự kiện cho toàn bộ bàn phím máy tính.
    private fun setupClickListeners() {
        val numberButtons = listOf(
            viewBinding.btn0 to "0",
            viewBinding.btn1 to "1",
            viewBinding.btn2 to "2",
            viewBinding.btn3 to "3",
            viewBinding.btn4 to "4",
            viewBinding.btn5 to "5",
            viewBinding.btn6 to "6",
            viewBinding.btn7 to "7",
            viewBinding.btn8 to "8",
            viewBinding.btn9 to "9",
            viewBinding.btnDot to "."
        )

        for ((btn, text) in numberButtons) {
            btn.setOnClickListener {
                appendCharacter(text)
            }
        }

        viewBinding.btnAdd.setOnClickListener { appendOperator("+") }
        viewBinding.btnSubtract.setOnClickListener { appendOperator("-") }
        viewBinding.btnMultiply.setOnClickListener { appendOperator("×") }
        viewBinding.btnDivide.setOnClickListener { appendOperator("÷") }
        viewBinding.btnPercent.setOnClickListener { appendOperator("%") }

        viewBinding.btnParentheses.setOnClickListener { handleParentheses() }
        viewBinding.btnAllClear.setOnClickListener { clearAll() }
        viewBinding.btnBackspace.setOnClickListener { handleBackspace() }
        viewBinding.btnEquals.setOnClickListener { calculateFinalResult() }
    }

    // Thêm số hoặc dấu chấm thập phân vào biểu thức hiện tại.
    private fun appendCharacter(char: String) {
        if (char == ".") {
            val lastPart = currentExpression.split('+', '-', '×', '÷', '%', '(', ')').lastOrNull() ?: ""
            if (lastPart.contains(".")) return
            if (lastPart.isEmpty()) {
                currentExpression += "0"
            }
        }
        currentExpression += char
        updateDisplay()
        previewResult()
    }

    // Thêm toán tử toán học vào biểu thức.
    private fun appendOperator(op: String) {
        if (currentExpression.isEmpty() && op == "-") {
            currentExpression = "-"
            updateDisplay()
            return
        }
        if (currentExpression.isEmpty()) return

        val lastChar = currentExpression.last()
        if (lastChar in "+-×÷%") {
            currentExpression = currentExpression.dropLast(1) + op
        } else if (lastChar != '(') {
            currentExpression += op
        }
        updateDisplay()
    }

    // Xử lý đóng mở ngoặc đơn tự động theo số lượng ngoặc mở còn thiếu.
    private fun handleParentheses() {
        val openCount = currentExpression.count { it == '(' }
        val closeCount = currentExpression.count { it == ')' }
        val lastChar = currentExpression.lastOrNull()

        if (openCount > closeCount && lastChar != null && lastChar !in "+-×÷%(") {
            currentExpression += ")"
        } else {
            if (lastChar != null && (lastChar.isDigit() || lastChar == ')')) {
                currentExpression += "×("
            } else {
                currentExpression += "("
            }
        }
        updateDisplay()
        previewResult()
    }

    // Xóa toàn bộ biểu thức và kết quả đang nhập.
    private fun clearAll() {
        currentExpression = ""
        viewBinding.tvExpression.text = ""
        viewBinding.tvResult.text = "0"
    }

    // Xóa ký tự cuối cùng của biểu thức.
    private fun handleBackspace() {
        if (currentExpression.isNotEmpty()) {
            currentExpression = currentExpression.dropLast(1)
            updateDisplay()
            previewResult()
        }
    }

    // Hiển thị biểu thức ra màn hình người dùng.
    private fun updateDisplay() {
        viewBinding.tvExpression.text = currentExpression
    }

    // Xem trước kết quả tính toán theo thời gian thực khi đang nhập biểu thức.
    private fun previewResult() {
        if (currentExpression.isBlank()) {
            viewBinding.tvResult.text = "0"
            return
        }
        try {
            val result = evaluateExpression(currentExpression)
            if (!result.isNaN() && !result.isInfinite()) {
                viewBinding.tvResult.text = decimalFormat.format(result)
            }
        } catch (_: Exception) {
            // Biểu thức chưa hoàn thiện trong quá trình gõ, bỏ qua lỗi preview.
        }
    }

    // Tính toán kết quả cuối cùng khi người dùng nhấn phím bằng.
    private fun calculateFinalResult() {
        if (currentExpression.isBlank()) return
        try {
            val result = evaluateExpression(currentExpression)
            if (result.isInfinite() || result.isNaN()) {
                viewBinding.tvResult.text = "Không thể chia cho 0"
            } else {
                val formatted = decimalFormat.format(result)
                viewBinding.tvResult.text = formatted
                currentExpression = formatted.replace(",", "")
                updateDisplay()
            }
        } catch (_: Exception) {
            viewBinding.tvResult.text = "Lỗi biểu thức"
        }
    }

    // Phân tích và tính toán giá trị biểu thức số học qua thuật toán Shunting-yard.
    private fun evaluateExpression(expr: String): Double {
        val cleanExpr = expr.replace("×", "*").replace("÷", "/")
        val tokens = tokenize(cleanExpr)
        val values = Stack<Double>()
        val operators = Stack<Char>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token.toDoubleOrNull() != null -> {
                    values.push(token.toDouble())
                }
                token == "(" -> operators.push('(')
                token == ")" -> {
                    while (operators.isNotEmpty() && operators.peek() != '(') {
                        applyTopOperator(values, operators.pop())
                    }
                    if (operators.isNotEmpty()) operators.pop()
                }
                token.length == 1 && token[0] in "+-*/%" -> {
                    val op = token[0]
                    while (operators.isNotEmpty() && precedence(operators.peek()) >= precedence(op)) {
                        applyTopOperator(values, operators.pop())
                    }
                    operators.push(op)
                }
            }
            i++
        }

        while (operators.isNotEmpty()) {
            applyTopOperator(values, operators.pop())
        }

        return if (values.isNotEmpty()) values.pop() else 0.0
    }

    // Tách chuỗi biểu thức thành danh sách các token số và toán tử.
    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var numberBuffer = StringBuilder()

        for (i in expr.indices) {
            val c = expr[i]
            if (c.isDigit() || c == '.') {
                numberBuffer.append(c)
            } else {
                if (numberBuffer.isNotEmpty()) {
                    tokens.add(numberBuffer.toString())
                    numberBuffer = StringBuilder()
                }
                if (c == '-' && (i == 0 || expr[i - 1] in "+-*/(")) {
                    numberBuffer.append(c)
                } else if (c in "+-*/%()") {
                    tokens.add(c.toString())
                }
            }
        }
        if (numberBuffer.isNotEmpty()) {
            tokens.add(numberBuffer.toString())
        }
        return tokens
    }

    // Xác định độ ưu tiên của toán tử số học.
    private fun precedence(op: Char): Int {
        return when (op) {
            '+', '-' -> 1
            '*', '/', '%' -> 2
            else -> 0
        }
    }

    // Áp dụng toán tử lên hai toán hạng trên đỉnh ngăn xếp.
    private fun applyTopOperator(values: Stack<Double>, op: Char) {
        if (values.size < 2) return
        val b = values.pop()
        val a = values.pop()
        val res = when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> if (b == 0.0) Double.POSITIVE_INFINITY else a / b
            '%' -> (a * b) / 100.0
            else -> 0.0
        }
        values.push(res)
    }
}
