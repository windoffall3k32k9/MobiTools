package com.example.baseproject.ui.counter

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentCounterBinding

/**
 * Công cụ bộ đếm số lượng với phản hồi xúc giác và ghi nhớ giá trị cục bộ.
 */
class TallyCounterFragment : BaseToolFragment<FragmentCounterBinding>() {

    override val toolId: String = "counter"
    override val toolTitleResId: Int = R.string.tool_counter_title

    private var currentCount: Long = 0L
    private var stepSize: Long = 1L

    private val prefs by lazy {
        requireContext().getSharedPreferences("tally_counter_pref", Context.MODE_PRIVATE)
    }

    override fun provideViewBinding(container: ViewGroup?): FragmentCounterBinding {
        return FragmentCounterBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        currentCount = prefs.getLong("saved_count", 0L)
        updateDisplay()
        setupListeners()
    }

    // Thiết lập sự kiện chạm, chuyển bước nhảy và đặt lại bộ đếm.
    private fun setupListeners() {
        viewBinding.btnBigIncrement.setOnClickListener {
            viewBinding.btnBigIncrement.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            currentCount += stepSize
            saveAndDisplay()
        }

        viewBinding.btnDecrement.setOnClickListener {
            viewBinding.btnDecrement.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (currentCount - stepSize >= 0) {
                currentCount -= stepSize
            } else {
                currentCount = 0
            }
            saveAndDisplay()
        }

        viewBinding.btnResetCounter.setOnClickListener {
            currentCount = 0L
            saveAndDisplay()
        }

        viewBinding.toggleStepGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                stepSize = when (checkedId) {
                    R.id.btnStep5 -> 5L
                    R.id.btnStep10 -> 10L
                    else -> 1L
                }
            }
        }
    }

    // Lưu giá trị vào bộ nhớ cục bộ và cập nhật lên màn hình.
    private fun saveAndDisplay() {
        prefs.edit().putLong("saved_count", currentCount).apply()
        updateDisplay()
    }

    // Hiển thị số đếm lên giao diện người dùng.
    private fun updateDisplay() {
        viewBinding.tvCounterValue.text = currentCount.toString()
    }
}
