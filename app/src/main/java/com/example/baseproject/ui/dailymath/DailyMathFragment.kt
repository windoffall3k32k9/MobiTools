package com.example.baseproject.ui.dailymath

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentDailyMathBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Công cụ tính toán mở rộng phục vụ đời sống thường ngày: chia hóa đơn, tính phần trăm và khoảng cách ngày.
 */
class DailyMathFragment : BaseToolFragment<FragmentDailyMathBinding>() {

    override val toolId: String = "daily_math"
    override val toolTitleResId: Int = R.string.tool_daily_math_title

    private val currencyFormat = DecimalFormat("#,###")
    private val percentFormat = DecimalFormat("#,##0.0")

    private var tipPercentage = 0.10 // Mặc định 10%
    private var startDateCalendar: Calendar = Calendar.getInstance()
    private var endDateCalendar: Calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun provideViewBinding(container: ViewGroup?): FragmentDailyMathBinding {
        return FragmentDailyMathBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        setupModeToggle()
        setupSplitBillListeners()
        setupPercentListeners()
        setupDatePickers()
        calculateSplitBill()
        calculateDateInterval()
    }

    // Thiết lập chuyển đổi giữa 3 tab tiện ích tính toán.
    private fun setupModeToggle() {
        viewBinding.toggleMathMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewBinding.layoutSplitBill.visibility =
                    if (checkedId == R.id.btnModeSplitBill) View.VISIBLE else View.GONE
                viewBinding.layoutPercentCalc.visibility =
                    if (checkedId == R.id.btnModePercent) View.VISIBLE else View.GONE
                viewBinding.layoutDateInterval.visibility =
                    if (checkedId == R.id.btnModeDateInterval) View.VISIBLE else View.GONE
            }
        }
    }

    // Thiết lập tính toán chia hóa đơn và tiền Tip theo thời gian thực.
    private fun setupSplitBillListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateSplitBill()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        }

        viewBinding.etBillAmount.addTextChangedListener(watcher)
        viewBinding.etPeopleCount.addTextChangedListener(watcher)

        viewBinding.toggleTipPercent.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                tipPercentage = when (checkedId) {
                    R.id.btnTip0 -> 0.0
                    R.id.btnTip5 -> 0.05
                    R.id.btnTip15 -> 0.15
                    else -> 0.10
                }
                calculateSplitBill()
            }
        }
    }

    // Tính toán số tiền mỗi người phải trả và tổng thanh toán.
    private fun calculateSplitBill() {
        val bill = viewBinding.etBillAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
        val people = (viewBinding.etPeopleCount.text?.toString()?.toIntOrNull() ?: 1).coerceAtLeast(1)

        val tipAmount = bill * tipPercentage
        val total = bill + tipAmount
        val perPerson = total / people

        viewBinding.tvPerPersonAmount.text = "${currencyFormat.format(perPerson)} đ"
        viewBinding.tvTipTotal.text = "${currencyFormat.format(tipAmount)} đ"
        viewBinding.tvTotalBillWithTip.text = "${currencyFormat.format(total)} đ"
    }

    // Thiết lập tính toán phần trăm tăng giảm giữa hai giá trị.
    private fun setupPercentListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculatePercentChange()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        }

        viewBinding.etOriginalValue.addTextChangedListener(watcher)
        viewBinding.etNewValue.addTextChangedListener(watcher)
    }

    // Tính mức độ phần trăm thay đổi.
    private fun calculatePercentChange() {
        val orig = viewBinding.etOriginalValue.text?.toString()?.toDoubleOrNull() ?: 0.0
        val newVal = viewBinding.etNewValue.text?.toString()?.toDoubleOrNull() ?: 0.0

        if (orig == 0.0) {
            viewBinding.tvPercentChangeResult.text = "Không xác định"
            return
        }

        val change = ((newVal - orig) / orig) * 100.0
        val sign = if (change > 0) "+" else ""
        viewBinding.tvPercentChangeResult.text = "$sign${percentFormat.format(change)}%"
    }

    // Thiết lập chọn ngày qua DatePickerDialog cho ngày bắt đầu và kết thúc.
    private fun setupDatePickers() {
        updateDateButtons()

        viewBinding.btnSelectStartDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    startDateCalendar.set(year, month, dayOfMonth)
                    updateDateButtons()
                    calculateDateInterval()
                },
                startDateCalendar.get(Calendar.YEAR),
                startDateCalendar.get(Calendar.MONTH),
                startDateCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        viewBinding.btnSelectEndDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    endDateCalendar.set(year, month, dayOfMonth)
                    updateDateButtons()
                    calculateDateInterval()
                },
                endDateCalendar.get(Calendar.YEAR),
                endDateCalendar.get(Calendar.MONTH),
                endDateCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    // Cập nhật nhãn ngày tháng trên các nút chọn.
    private fun updateDateButtons() {
        viewBinding.btnSelectStartDate.text = "Ngày bắt đầu: ${dateFormatter.format(startDateCalendar.time)}"
        viewBinding.btnSelectEndDate.text = "Ngày kết thúc: ${dateFormatter.format(endDateCalendar.time)}"
    }

    // Tính toán số ngày và số tuần giữa hai mốc thời gian đã chọn.
    private fun calculateDateInterval() {
        val diffMs = abs(endDateCalendar.timeInMillis - startDateCalendar.timeInMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        val weeks = days / 7
        val remDays = days % 7

        viewBinding.tvDateIntervalResult.text = if (weeks > 0) {
            "$days ngày ($weeks tuần $remDays ngày)"
        } else {
            "$days ngày"
        }
    }
}
