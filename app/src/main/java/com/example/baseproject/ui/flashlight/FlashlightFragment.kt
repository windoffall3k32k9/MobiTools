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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentFlashlightBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Màn hình công cụ Đèn pin điều khiển đèn flash camera phần cứng,
 * tùy chỉnh mức sáng (Android 13+), chế độ chớp nháy Strobe, mã Morse SOS và hẹn giờ tự tắt.
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
    private var currentBrightness: Int = 100

    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var hasFlashHardware: Boolean = false
    private var maxStrengthLevel: Int = 1

    private val flashHandler = Handler(Looper.getMainLooper())
    private val autoOffHandler = Handler(Looper.getMainLooper())

    private lateinit var prefs: SharedPreferences
    private var autoOffMinutes: Int = 0
    private var isHapticEnabled: Boolean = true

    // Mẫu nhấp nháy SOS chuẩn mã Morse (độ dài ms)
    private val sosPattern = longArrayOf(
        180, 180, 180, 180, 180, 420, // S: 3 ngắn + khoảng cách ký tự
        540, 180, 540, 180, 540, 420, // O: 3 dài + khoảng cách ký tự
        180, 180, 180, 180, 180, 1200 // S: 3 ngắn + khoảng cách từ
    )
    private var sosStepIndex: Int = 0

    // Runnable điều khiển vòng lặp chớp nháy SOS
    private val sosRunnable = object : Runnable {
        override fun run() {
            if (!isTorchActive || currentMode != FlashMode.SOS) return
            val shouldTurnOn = (sosStepIndex % 2 == 0)
            setHardwareTorch(shouldTurnOn)
            viewBinding.flashlightVisualView.setTorchState(shouldTurnOn, animated = false)

            val delay = sosPattern[sosStepIndex]
            sosStepIndex = (sosStepIndex + 1) % sosPattern.size
            flashHandler.postDelayed(this, delay)
        }
    }

    // Runnable điều khiển chế độ nhấp nháy liên tục (Strobe ~6Hz)
    private var strobeState: Boolean = false
    private val strobeRunnable = object : Runnable {
        override fun run() {
            if (!isTorchActive || currentMode != FlashMode.STROBE) return
            strobeState = !strobeState
            setHardwareTorch(strobeState)
            viewBinding.flashlightVisualView.setTorchState(strobeState, animated = false)
            flashHandler.postDelayed(this, 85)
        }
    }

    // Runnable tự động tắt đèn pin khi hết thời gian hẹn giờ
    private val autoOffRunnable = Runnable {
        if (isTorchActive) {
            turnOffTorch()
        }
    }

    // Nạp layout view binding cho Fragment đèn pin.
    override fun provideViewBinding(container: ViewGroup?): FragmentFlashlightBinding {
        return FragmentFlashlightBinding.inflate(layoutInflater, container, false)
    }

    // Khởi tạo các thành phần giao diện, camera hardware và cấu hình lưu trữ.
    override fun initViews() {
        prefs = requireContext().getSharedPreferences("flashlight_settings", Context.MODE_PRIVATE)
        autoOffMinutes = prefs.getInt("pref_auto_off_minutes", 0)
        isHapticEnabled = prefs.getBoolean("pref_haptic_feedback", true)

        initCameraHardware()
        setupTopBar()
        setupVisualView()
        setupBrightnessSlider()
        setupModeCards()
        updateModeCardsUi(FlashMode.CONTINUOUS)
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

    // Khởi tạo phần cứng máy ảnh và phát hiện khả năng chỉnh cường độ đèn flash.
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            maxStrengthLevel = characteristics.get(
                                CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL
                            ) ?: 1
                        }
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

    // Thiết lập thanh tiêu đề với nút quay lại và nút mở hộp thoại cài đặt.
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

    // Thiết lập thanh trượt chỉnh độ sáng đèn pin và cập nhật phần trăm hiển thị.
    private fun setupBrightnessSlider() {
        viewBinding.seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val safeProgress = progress.coerceIn(1, 100)
                currentBrightness = safeProgress
                viewBinding.tvBrightnessPercent.text = "$safeProgress%"
                viewBinding.flashlightVisualView.setBeamBrightness(safeProgress)

                if (isTorchActive && currentMode == FlashMode.CONTINUOUS) {
                    setHardwareTorch(true)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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

        scheduleAutoOffTimer()
    }

    // Tắt toàn bộ đèn pin phần cứng và ngừng các chu kỳ nhấp nháy.
    private fun turnOffTorch() {
        isTorchActive = false
        stopAllLoops()
        setHardwareTorch(false)
        viewBinding.flashlightVisualView.setTorchState(false, animated = true)
        autoOffHandler.removeCallbacks(autoOffRunnable)
    }

    // Dừng các luồng Handler đang chạy cho hiệu ứng nhấp nháy và phát mã SOS.
    private fun stopAllLoops() {
        flashHandler.removeCallbacks(strobeRunnable)
        flashHandler.removeCallbacks(sosRunnable)
    }

    // Khởi động chu kỳ chớp nháy tần số cao (Strobe).
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

    // Điều khiển bật tắt và cấp độ sáng của đèn flash camera phần cứng.
    private fun setHardwareTorch(enable: Boolean) {
        val id = cameraId ?: return
        try {
            if (enable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxStrengthLevel > 1) {
                    val strength = ((currentBrightness / 100f) * maxStrengthLevel).toInt().coerceIn(1, maxStrengthLevel)
                    cameraManager?.turnOnTorchWithStrengthLevel(id, strength)
                } else {
                    cameraManager?.setTorchMode(id, true)
                }
            } else {
                cameraManager?.setTorchMode(id, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Lập lịch tự động tắt đèn pin dựa theo cấu hình số phút hẹn giờ.
    private fun scheduleAutoOffTimer() {
        autoOffHandler.removeCallbacks(autoOffRunnable)
        if (autoOffMinutes > 0) {
            val timeoutMs = autoOffMinutes * 60 * 1000L
            autoOffHandler.postDelayed(autoOffRunnable, timeoutMs)
        }
    }

    // Phát rung phản hồi xúc giác nhẹ khi tương tác với các phím bấm.
    private fun triggerHapticFeedback(view: View) {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Hiển thị hộp thoại cài đặt hẹn giờ tự tắt và rung phản hồi của đèn pin.
    private fun showSettingsDialog() {
        val timeOptions = arrayOf(
            getString(R.string.flashlight_auto_off_never),
            getString(R.string.flashlight_auto_off_1m),
            getString(R.string.flashlight_auto_off_5m),
            getString(R.string.flashlight_auto_off_10m)
        )
        val minuteValues = intArrayOf(0, 1, 5, 10)
        var selectedIndex = minuteValues.indexOf(autoOffMinutes).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.flashlight_settings_title)
            .setSingleChoiceItems(timeOptions, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                autoOffMinutes = minuteValues[selectedIndex]
                prefs.edit().putInt("pref_auto_off_minutes", autoOffMinutes).apply()
                if (isTorchActive) {
                    scheduleAutoOffTimer()
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
