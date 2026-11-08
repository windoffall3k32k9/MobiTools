package com.example.baseproject.ui.flashlight

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.DialogFlashlightSettingsBinding
import com.example.baseproject.databinding.FragmentFlashlightBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

/**
 * Màn hình công cụ Đèn pin điều khiển đèn flash camera phần cứng,
 * hỗ trợ 3 chế độ: Sáng liên tục, Nhấp nháy và phát tín hiệu SOS với hộp thoại cài đặt độ trễ linh hoạt.
 */
class FlashlightFragment : BaseToolFragment<FragmentFlashlightBinding>() {

    override val toolId: String = "flashlight"
    override val toolTitleResId: Int = R.string.tool_flashlight_title

    private enum class FlashMode {
        CONTINUOUS,
        STROBE,
        SOS
    }

    private var currentMode: FlashMode = FlashMode.CONTINUOUS
    private var isTorchActive: Boolean = false

    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var hasFlashHardware: Boolean = false

    private val flashHandler = Handler(Looper.getMainLooper())

    private lateinit var prefs: SharedPreferences
    private var strobeDelayMs: Long = 100L
    private var sosPulseDelayMs: Long = 180L
    private var sosCycleDelayMs: Long = 1200L
    private var isHapticEnabled: Boolean = true

    // Mẫu nhấp nháy SOS chuẩn mã Morse tính toán động theo cấu hình độ trễ
    private var sosPattern: LongArray = longArrayOf()
    private var sosStepIndex: Int = 0

    // Runnable điều khiển vòng lặp chớp nháy SOS
    private val sosRunnable = object : Runnable {
        override fun run() {
            if (!isTorchActive || currentMode != FlashMode.SOS) return
            val shouldTurnOn = (sosStepIndex % 2 == 0)
            setHardwareTorch(shouldTurnOn)
            viewBinding.flashlightVisualView.setTorchState(shouldTurnOn, animated = false)

            val delay = if (sosPattern.isNotEmpty()) sosPattern[sosStepIndex] else 200L
            sosStepIndex = (sosStepIndex + 1) % sosPattern.size
            flashHandler.postDelayed(this, delay)
        }
    }

    // Runnable điều khiển chế độ nhấp nháy (Strobe) với độ trễ tùy chỉnh
    private var strobeState: Boolean = false
    private val strobeRunnable = object : Runnable {
        override fun run() {
            if (!isTorchActive || currentMode != FlashMode.STROBE) return
            strobeState = !strobeState
            setHardwareTorch(strobeState)
            viewBinding.flashlightVisualView.setTorchState(strobeState, animated = false)
            flashHandler.postDelayed(this, strobeDelayMs)
        }
    }

    // Nạp layout view binding cho Fragment đèn pin.
    override fun provideViewBinding(container: ViewGroup?): FragmentFlashlightBinding {
        return FragmentFlashlightBinding.inflate(layoutInflater, container, false)
    }

    // Khởi tạo các thành phần giao diện, camera hardware và cấu hình lưu trữ.
    override fun initViews() {
        prefs = requireContext().getSharedPreferences("flashlight_settings", Context.MODE_PRIVATE)
        loadSettingsFromPrefs()
        rebuildSosPattern()

        initCameraHardware()
        setupTopBar()
        setupVisualView()
        setupModeCards()
        updateModeCardsUi(FlashMode.CONTINUOUS)
    }

    // Đọc cấu hình độ trễ nhấp nháy, SOS và rung phản hồi từ SharedPreferences.
    private fun loadSettingsFromPrefs() {
        strobeDelayMs = prefs.getLong("pref_strobe_delay_ms", 100L)
        sosPulseDelayMs = prefs.getLong("pref_sos_pulse_delay_ms", 180L)
        sosCycleDelayMs = prefs.getLong("pref_sos_cycle_delay_ms", 1200L)
        isHapticEnabled = prefs.getBoolean("pref_haptic_feedback", true)
    }

    // Xây dựng lại mảng thời gian nhấp nháy SOS chuẩn mã Morse theo xung nhịp và độ trễ chu kỳ.
    private fun rebuildSosPattern() {
        val u = sosPulseDelayMs
        val c = sosCycleDelayMs
        sosPattern = longArrayOf(
            u, u, u, u, u, 3 * u,              // S: 3 ngắn + khoảng cách ký tự
            3 * u, u, 3 * u, u, 3 * u, 3 * u,  // O: 3 dài + khoảng cách ký tự
            u, u, u, u, u, c                   // S: 3 ngắn + thời gian nghỉ giữa chu kỳ
        )
    }

    // Dừng toàn bộ đèn flash camera và các luồng hẹn giờ khi rời màn hình.
    override fun onPause() {
        super.onPause()
        turnOffTorch()
    }

    // Dọn dẹp tài nguyên khi view bị hủy.
    override fun onDestroyView() {
        super.onDestroyView()
        turnOffTorch()
    }

    // Khởi tạo phần cứng máy ảnh và phát hiện khả năng sử dụng đèn flash camera.
    private fun initCameraHardware() {
        val context = requireContext()
        hasFlashHardware = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

        try {
            val cameraIds = cameraManager?.cameraIdList
            if (cameraIds != null) {
                for (id in cameraIds) {
                    val characteristics = cameraManager?.getCameraCharacteristics(id)
                    val flashAvailable = characteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    if (flashAvailable) {
                        cameraId = id
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!hasFlashHardware || cameraId == null) {
            viewBinding.tvFlashlightWarning.visibility = View.VISIBLE
            viewBinding.tvFlashlightWarning.setText(R.string.flash_not_available)
        }
    }

    // Thiết lập thanh tiêu đề với nút quay lại và nút mở hộp thoại cài đặt độ trễ.
    private fun setupTopBar() {
        viewBinding.btnFlashlightBackCard.setOnClickListener {
            triggerHapticFeedback(it)
            parentFragmentManager.popBackStack()
        }

        viewBinding.btnFlashlightSettingsCard.setOnClickListener {
            triggerHapticFeedback(it)
            showSettingsDialog()
        }
    }

    // Thiết lập tương tác chạm trên mô phỏng đèn pin trực quan.
    private fun setupVisualView() {
        viewBinding.flashlightVisualView.setOnPowerClickListener {
            triggerHapticFeedback(viewBinding.flashlightVisualView)
            toggleTorchState()
        }
    }

    // Thiết lập sự kiện chuyển đổi giữa các thẻ chế độ: Liên tục, Nhấp nháy và SOS.
    private fun setupModeCards() {
        viewBinding.cardModeContinuous.setOnClickListener {
            triggerHapticFeedback(it)
            selectMode(FlashMode.CONTINUOUS)
        }

        viewBinding.cardModeStrobe.setOnClickListener {
            triggerHapticFeedback(it)
            selectMode(FlashMode.STROBE)
        }

        viewBinding.cardModeSos.setOnClickListener {
            triggerHapticFeedback(it)
            selectMode(FlashMode.SOS)
        }
    }

    // Chuyển đổi sang chế độ được chọn và điều chỉnh ngay lập tức nếu đèn đang hoạt động.
    private fun selectMode(mode: FlashMode) {
        if (currentMode == mode) return
        currentMode = mode
        updateModeCardsUi(mode)

        if (isTorchActive) {
            stopAllLoops()
            when (mode) {
                FlashMode.CONTINUOUS -> {
                    setHardwareTorch(true)
                    viewBinding.flashlightVisualView.setTorchState(true, animated = true)
                }
                FlashMode.STROBE -> {
                    startStrobeLoop()
                }
                FlashMode.SOS -> {
                    startSosLoop()
                }
            }
        }
    }

    // Cập nhật phong cách viền và màu sắc của các thẻ chế độ theo lựa chọn hiện tại.
    private fun updateModeCardsUi(activeMode: FlashMode) {
        val context = requireContext()
        val activeBg = ContextCompat.getColor(context, R.color.flashlightBlueSoft)
        val defaultBg = ContextCompat.getColor(context, R.color.colorCardBackground)
        val activeStrokeColor = ContextCompat.getColor(context, R.color.flashlightBlue)
        val defaultStrokeColor = ContextCompat.getColor(context, R.color.colorBorder)
        val activeTextColor = ContextCompat.getColor(context, R.color.flashlightBlue)
        val defaultTextColor = ContextCompat.getColor(context, R.color.textPrimary)

        val activeStrokeWidth = (1.8f * resources.displayMetrics.density).toInt()
        val defaultStrokeWidth = (1f * resources.displayMetrics.density).toInt()

        // 1. Thẻ Sáng liên tục
        val isContinuous = (activeMode == FlashMode.CONTINUOUS)
        viewBinding.cardModeContinuous.setCardBackgroundColor(if (isContinuous) activeBg else defaultBg)
        viewBinding.cardModeContinuous.strokeColor = if (isContinuous) activeStrokeColor else defaultStrokeColor
        viewBinding.cardModeContinuous.strokeWidth = if (isContinuous) activeStrokeWidth else defaultStrokeWidth
        viewBinding.tvModeContinuousText.setTextColor(if (isContinuous) activeTextColor else defaultTextColor)

        // 2. Thẻ Nhấp nháy
        val isStrobe = (activeMode == FlashMode.STROBE)
        viewBinding.cardModeStrobe.setCardBackgroundColor(if (isStrobe) activeBg else defaultBg)
        viewBinding.cardModeStrobe.strokeColor = if (isStrobe) activeStrokeColor else defaultStrokeColor
        viewBinding.cardModeStrobe.strokeWidth = if (isStrobe) activeStrokeWidth else defaultStrokeWidth
        viewBinding.tvModeStrobeText.setTextColor(if (isStrobe) activeTextColor else defaultTextColor)

        // 3. Thẻ SOS
        val isSos = (activeMode == FlashMode.SOS)
        viewBinding.cardModeSos.setCardBackgroundColor(if (isSos) activeBg else defaultBg)
        viewBinding.cardModeSos.strokeColor = if (isSos) activeStrokeColor else defaultStrokeColor
        viewBinding.cardModeSos.strokeWidth = if (isSos) activeStrokeWidth else defaultStrokeWidth
        viewBinding.tvModeSosText.setTextColor(if (isSos) activeTextColor else defaultTextColor)
    }

    // Đảo trạng thái bật tắt của đèn pin khi người dùng nhấn nút nguồn.
    private fun toggleTorchState() {
        if (isTorchActive) {
            turnOffTorch()
        } else {
            turnOnTorch()
        }
    }

    // Kích hoạt đèn pin theo chế độ đang được cấu hình.
    private fun turnOnTorch() {
        isTorchActive = true
        viewBinding.flashlightVisualView.setTorchState(true, animated = true)

        when (currentMode) {
            FlashMode.CONTINUOUS -> {
                setHardwareTorch(true)
            }
            FlashMode.STROBE -> {
                startStrobeLoop()
            }
            FlashMode.SOS -> {
                startSosLoop()
            }
        }
    }

    // Tắt toàn bộ đèn pin phần cứng và ngừng các chu kỳ nhấp nháy.
    private fun turnOffTorch() {
        isTorchActive = false
        stopAllLoops()
        setHardwareTorch(false)
        viewBinding.flashlightVisualView.setTorchState(false, animated = true)
    }

    // Dừng các luồng Handler đang chạy cho hiệu ứng nhấp nháy và phát mã SOS.
    private fun stopAllLoops() {
        flashHandler.removeCallbacks(strobeRunnable)
        flashHandler.removeCallbacks(sosRunnable)
    }

    // Khởi động chu kỳ chớp nháy tần số cao (Strobe) theo độ trễ đã cấu hình.
    private fun startStrobeLoop() {
        stopAllLoops()
        strobeState = false
        flashHandler.post(strobeRunnable)
    }

    // Khởi động chuỗi chớp nháy phát tín hiệu khẩn cấp SOS chuẩn Morse.
    private fun startSosLoop() {
        stopAllLoops()
        sosStepIndex = 0
        flashHandler.post(sosRunnable)
    }

    // Điều khiển bật tắt đèn flash camera phần cứng.
    private fun setHardwareTorch(enable: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager?.setTorchMode(id, enable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Phát rung phản hồi xúc giác nhẹ khi tương tác với các phím bấm.
    private fun triggerHapticFeedback(view: View) {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Hiển thị hộp thoại cài đặt tùy chỉnh độ trễ nhấp nháy (Strobe) và độ trễ chu kỳ phát SOS.
    private fun showSettingsDialog() {
        val dialogBinding = DialogFlashlightSettingsBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        var tempStrobeDelay = strobeDelayMs
        var tempSosPulse = sosPulseDelayMs
        var tempSosCycle = sosCycleDelayMs
        var tempHaptic = isHapticEnabled

        // 1. Cấu hình SeekBar độ trễ nhấp nháy (30ms - 800ms)
        fun updateStrobeText(delayMs: Long) {
            val freq = 1000.0 / (2 * delayMs)
            dialogBinding.tvStrobeDelayValue.text = "$delayMs ms (~${String.format(Locale.US, "%.1f", freq)} Hz)"
        }
        val strobeProgress = ((tempStrobeDelay - 30) / 10).toInt().coerceIn(0, 77)
        dialogBinding.seekStrobeDelay.progress = strobeProgress
        updateStrobeText(tempStrobeDelay)

        dialogBinding.seekStrobeDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tempStrobeDelay = 30L + progress * 10L
                updateStrobeText(tempStrobeDelay)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 2. Cấu hình SeekBar độ trễ xung nhịp SOS (80ms - 400ms)
        fun updateSosPulseText(pulseMs: Long) {
            dialogBinding.tvSosPulseValue.text = "$pulseMs ms"
        }
        val sosPulseProgress = ((tempSosPulse - 80) / 10).toInt().coerceIn(0, 32)
        dialogBinding.seekSosPulse.progress = sosPulseProgress
        updateSosPulseText(tempSosPulse)

        dialogBinding.seekSosPulse.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tempSosPulse = 80L + progress * 10L
                updateSosPulseText(tempSosPulse)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 3. Cấu hình SeekBar độ trễ nghỉ giữa chu kỳ SOS (500ms - 4000ms)
        fun updateSosCycleText(cycleMs: Long) {
            val seconds = cycleMs / 1000.0
            dialogBinding.tvSosCycleValue.text = "${String.format(Locale.US, "%.1f", seconds)} s"
        }
        val sosCycleProgress = ((tempSosCycle - 500) / 100).toInt().coerceIn(0, 35)
        dialogBinding.seekSosCycle.progress = sosCycleProgress
        updateSosCycleText(tempSosCycle)

        dialogBinding.seekSosCycle.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tempSosCycle = 500L + progress * 100L
                updateSosCycleText(tempSosCycle)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 4. Rung phản hồi
        dialogBinding.switchHapticFeedback.isChecked = tempHaptic
        dialogBinding.switchHapticFeedback.setOnCheckedChangeListener { _, isChecked ->
            tempHaptic = isChecked
        }

        // 5. Nút Khôi phục mặc định
        dialogBinding.btnResetSettings.setOnClickListener {
            tempStrobeDelay = 100L
            tempSosPulse = 180L
            tempSosCycle = 1200L
            tempHaptic = true

            dialogBinding.seekStrobeDelay.progress = ((tempStrobeDelay - 30) / 10).toInt()
            dialogBinding.seekSosPulse.progress = ((tempSosPulse - 80) / 10).toInt()
            dialogBinding.seekSosCycle.progress = ((tempSosCycle - 500) / 100).toInt()
            dialogBinding.switchHapticFeedback.isChecked = true

            updateStrobeText(tempStrobeDelay)
            updateSosPulseText(tempSosPulse)
            updateSosCycleText(tempSosCycle)
        }

        // 6. Nút Lưu cài đặt
        dialogBinding.btnSaveSettings.setOnClickListener {
            strobeDelayMs = tempStrobeDelay
            sosPulseDelayMs = tempSosPulse
            sosCycleDelayMs = tempSosCycle
            isHapticEnabled = tempHaptic

            prefs.edit()
                .putLong("pref_strobe_delay_ms", strobeDelayMs)
                .putLong("pref_sos_pulse_delay_ms", sosPulseDelayMs)
                .putLong("pref_sos_cycle_delay_ms", sosCycleDelayMs)
                .putBoolean("pref_haptic_feedback", isHapticEnabled)
                .apply()

            rebuildSosPattern()

            // Khởi động lại luồng nếu đang chạy để áp dụng tức thì
            if (isTorchActive) {
                when (currentMode) {
                    FlashMode.STROBE -> startStrobeLoop()
                    FlashMode.SOS -> startSosLoop()
                    FlashMode.CONTINUOUS -> {}
                }
            }

            dialog.dismiss()
        }

        dialog.show()
    }
}
