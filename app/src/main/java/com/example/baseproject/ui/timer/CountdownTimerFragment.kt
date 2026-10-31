package com.example.baseproject.ui.timer

import android.content.Context
import android.os.Build
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentTimerBinding
import java.util.Locale

/**
 * Công cụ đồng hồ đếm ngược với giao diện tiến trình vòng tròn và cảnh báo rung khi kết thúc.
 */
class CountdownTimerFragment : BaseToolFragment<FragmentTimerBinding>() {

    override val toolId: String = "timer"
    override val toolTitleResId: Int = R.string.tool_timer_title

    private var totalDurationMs: Long = 5 * 60 * 1000L // Mặc định 5 phút
    private var remainingTimeMs: Long = totalDurationMs
    private var isRunning: Boolean = false
    private var countDownTimer: CountDownTimer? = null

    override fun provideViewBinding(container: ViewGroup?): FragmentTimerBinding {
        return FragmentTimerBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        updateTimerDisplay()
        setupButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }

    // Thiết lập sự kiện cho các nút điều khiển đếm ngược.
    private fun setupButtons() {
        viewBinding.btnAdd1Min.setOnClickListener { addTime(60 * 1000L) }
        viewBinding.btnAdd5Min.setOnClickListener { addTime(5 * 60 * 1000L) }
        viewBinding.btnAdd10Min.setOnClickListener { addTime(10 * 60 * 1000L) }
        viewBinding.btnAdd30Min.setOnClickListener { addTime(30 * 60 * 1000L) }

        viewBinding.btnStartTimer.setOnClickListener {
            if (!isRunning) {
                startTimer()
            } else {
                pauseTimer()
            }
        }

        viewBinding.btnResetTimer.setOnClickListener {
            resetTimer()
        }
    }

    // Tăng thêm thời lượng đếm ngược khi chưa kích hoạt chạy.
    private fun addTime(extraMs: Long) {
        if (!isRunning) {
            totalDurationMs += extraMs
            remainingTimeMs = totalDurationMs
            updateTimerDisplay()
        }
    }

    // Bắt đầu hoặc tiếp tục quá trình đếm ngược.
    private fun startTimer() {
        if (remainingTimeMs <= 0) return

        isRunning = true
        viewBinding.btnStartTimer.setText(R.string.pause)
        viewBinding.btnStartTimer.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorWarning)
        viewBinding.tvTimerStatus.text = "Đang đếm ngược…"
        viewBinding.layoutPresets.visibility = android.view.View.GONE

        countDownTimer = object : CountDownTimer(remainingTimeMs, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMs = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                remainingTimeMs = 0
                isRunning = false
                updateTimerDisplay()
                onTimerFinished()
            }
        }.start()
    }

    // Tạm dừng quá trình đếm ngược.
    private fun pauseTimer() {
        isRunning = false
        countDownTimer?.cancel()
        viewBinding.btnStartTimer.setText(R.string.resume)
        viewBinding.btnStartTimer.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)
        viewBinding.tvTimerStatus.text = "Đã tạm dừng"
    }

    // Đặt lại thời gian đếm ngược về trạng thái ban đầu.
    private fun resetTimer() {
        isRunning = false
        countDownTimer?.cancel()
        remainingTimeMs = totalDurationMs
        updateTimerDisplay()

        viewBinding.btnStartTimer.setText(R.string.start)
        viewBinding.btnStartTimer.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)
        viewBinding.tvTimerStatus.text = "Sẵn sàng"
        viewBinding.layoutPresets.visibility = android.view.View.VISIBLE
    }

    // Xử lý thông báo và rung khi hết thời gian đếm ngược.
    private fun onTimerFinished() {
        viewBinding.tvTimerStatus.text = "Hết giờ!"
        viewBinding.btnStartTimer.setText(R.string.start)
        viewBinding.btnStartTimer.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)
        viewBinding.layoutPresets.visibility = android.view.View.VISIBLE

        triggerAlarmVibration()
    }

    // Kích hoạt chuỗi rung phản hồi trên phần cứng điện thoại.
    private fun triggerAlarmVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 500, 200, 500, 200, 800)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(1000)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Cập nhật giao diện số và thanh tròn tiến trình theo thời gian còn lại.
    private fun updateTimerDisplay() {
        val seconds = (remainingTimeMs / 1000) % 60
        val minutes = (remainingTimeMs / (1000 * 60)) % 60
        val hours = (remainingTimeMs / (1000 * 60 * 60))
        viewBinding.tvTimerDisplay.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

        val progress = if (totalDurationMs > 0) {
            ((remainingTimeMs.toFloat() / totalDurationMs.toFloat()) * 100).toInt()
        } else {
            0
        }
        viewBinding.progressIndicator.progress = progress
    }
}
