package com.example.baseproject.ui.timer

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentTimerBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Công cụ đồng hồ đếm ngược với giao diện chọn số 3 cột và mặt vạch tiến trình vòng tròn trực quan.
 */
class CountdownTimerFragment : BaseToolFragment<FragmentTimerBinding>() {

    override val toolId: String = "timer"
    override val toolTitleResId: Int = R.string.tool_timer_title

    private var totalDurationMs: Long = 0L
    private var remainingTimeMs: Long = 0L
    private var isRunning: Boolean = false
    private var countDownTimer: CountDownTimer? = null

    // Khởi tạo ViewBinding liên kết với layout đồng hồ đếm ngược.
    override fun provideViewBinding(container: ViewGroup?): FragmentTimerBinding {
        return FragmentTimerBinding.inflate(layoutInflater, container, false)
    }

    // Thiết lập các bộ cuộn số và gán sự kiện cho các nút điều khiển.
    override fun initViews() {
        setupPickers()
        setupButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }

    // Cấu hình 3 cột NumberPicker Giờ, Phút, Giây với định dạng 2 chữ số chuẩn.
    private fun setupPickers() {
        val twoDigitFormatter = NumberPicker.Formatter { String.format(Locale.US, "%02d", it) }

        viewBinding.pickerHours.apply {
            minValue = 0
            maxValue = 23
            value = 0
            setFormatter(twoDigitFormatter)
        }

        viewBinding.pickerMinutes.apply {
            minValue = 0
            maxValue = 59
            value = 5
            setFormatter(twoDigitFormatter)
        }

        viewBinding.pickerSeconds.apply {
            minValue = 0
            maxValue = 59
            value = 0
            setFormatter(twoDigitFormatter)
        }
    }

    // Gán các sự kiện click cho các nút cộng nhanh thời gian, bắt đầu, tạm dừng và dừng hẳn.
    private fun setupButtons() {
        viewBinding.btnAdd1Min.setOnClickListener { addTimeToPickers(1) }
        viewBinding.btnAdd5Min.setOnClickListener { addTimeToPickers(5) }
        viewBinding.btnAdd10Min.setOnClickListener { addTimeToPickers(10) }
        viewBinding.btnAdd30Min.setOnClickListener { addTimeToPickers(30) }

        viewBinding.btnStartPicker.setOnClickListener {
            val hours = viewBinding.pickerHours.value
            val minutes = viewBinding.pickerMinutes.value
            val seconds = viewBinding.pickerSeconds.value
            val totalSeconds = (hours * 3600L) + (minutes * 60L) + seconds

            if (totalSeconds <= 0) {
                Toast.makeText(requireContext(), "Vui lòng chọn thời gian lớn hơn 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            totalDurationMs = totalSeconds * 1000L
            remainingTimeMs = totalDurationMs

            switchToRunningMode()
            startCountdown()
        }

        // Nút dừng hẳn: hủy đếm ngược và quay lại giao diện chọn số ban đầu.
        viewBinding.btnStopCountdown.setOnClickListener {
            stopAndReturnToPicker()
        }

        // Nút tạm dừng: tạm dừng hoặc tiếp tục tại chỗ mà không quay về chọn số.
        viewBinding.btnPauseCountdown.setOnClickListener {
            if (isRunning) {
                pauseCountdown()
            } else {
                resumeCountdown()
            }
        }
    }

    // Cộng thêm số phút vào cột NumberPicker trong giao diện chọn thời lượng.
    private fun addTimeToPickers(extraMinutes: Int) {
        var newMinutes = viewBinding.pickerMinutes.value + extraMinutes
        var newHours = viewBinding.pickerHours.value

        if (newMinutes >= 60) {
            newHours = (newHours + (newMinutes / 60)).coerceAtMost(23)
            newMinutes %= 60
        }

        viewBinding.pickerMinutes.value = newMinutes
        viewBinding.pickerHours.value = newHours
    }

    // Chuyển sang giao diện mặt đồng hồ tròn đang chạy.
    private fun switchToRunningMode() {
        viewBinding.layoutSelectionView.visibility = View.GONE
        viewBinding.layoutRunningView.visibility = View.VISIBLE

        val hours = (totalDurationMs / (1000 * 3600)).toInt()
        val minutes = ((totalDurationMs / (1000 * 60)) % 60).toInt()
        val seconds = ((totalDurationMs / 1000) % 60).toInt()

        val summaryText = when {
            hours > 0 && minutes > 0 -> "Tổng $hours giờ $minutes phút"
            hours > 0 -> "Tổng $hours giờ"
            minutes > 0 && seconds > 0 -> "Tổng $minutes phút $seconds giây"
            minutes > 0 -> "Tổng $minutes phút"
            else -> "Tổng $seconds giây"
        }
        viewBinding.tvTotalSummary.text = summaryText

        val endTime = Date(System.currentTimeMillis() + totalDurationMs)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        viewBinding.tvEndTime.text = "Kết thúc: ${timeFormat.format(endTime)}"
    }

    // Khởi chạy bộ đếm lùi thời gian với bước cập nhật 100ms.
    private fun startCountdown() {
        countDownTimer?.cancel()
        isRunning = true
        viewBinding.tvRunningStateLabel.text = "Đang đếm ngược…"
        viewBinding.btnPauseCountdown.setImageResource(R.drawable.ic_pause)

        countDownTimer = object : CountDownTimer(remainingTimeMs, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMs = millisUntilFinished
                updateRunningDisplay()
            }

            override fun onFinish() {
                remainingTimeMs = 0
                isRunning = false
                updateRunningDisplay()
                onTimerFinished()
            }
        }.start()
    }

    // Tạm dừng đếm ngược và giữ nguyên thời gian còn lại.
    private fun pauseCountdown() {
        isRunning = false
        countDownTimer?.cancel()
        viewBinding.tvRunningStateLabel.text = "Đã tạm dừng"
        viewBinding.btnPauseCountdown.setImageResource(R.drawable.ic_play_arrow)
    }

    // Tiếp tục đếm ngược từ khoảng thời gian còn lại.
    private fun resumeCountdown() {
        if (remainingTimeMs <= 0) return
        startCountdown()
    }

    // Dừng hẳn đồng hồ và chuyển về giao diện chọn số ban đầu.
    private fun stopAndReturnToPicker() {
        isRunning = false
        countDownTimer?.cancel()
        viewBinding.layoutRunningView.visibility = View.GONE
        viewBinding.layoutSelectionView.visibility = View.VISIBLE
    }

    // Cập nhật chuỗi số thời gian và vòng tròn vạch theo tỷ lệ thời gian còn lại.
    private fun updateRunningDisplay() {
        val totalSec = (remainingTimeMs + 999) / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        viewBinding.tvRunningDigits.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

        val progress = if (totalDurationMs > 0) {
            remainingTimeMs.toFloat() / totalDurationMs.toFloat()
        } else {
            0f
        }
        viewBinding.timerTickDialView.setProgress(progress)
    }

    // Kích hoạt báo hiệu rung và âm thanh chuông khi đồng hồ đếm xong.
    private fun onTimerFinished() {
        viewBinding.tvRunningStateLabel.text = "Hết giờ!"
        viewBinding.btnPauseCountdown.setImageResource(R.drawable.ic_play_arrow)
        triggerAlarmFeedback()
    }

    // Kích hoạt rung phần cứng và chuông báo thức báo kết thúc đếm ngược.
    private fun triggerAlarmFeedback() {
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
                    val timings = longArrayOf(0, 400, 200, 400, 200, 800)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(1000)
                }
            }

            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(requireContext(), notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
