package com.example.baseproject.ui.flashlight

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentFlashlightBinding

/**
 * Công cụ đèn pin điều khiển đèn flash camera và chế độ phát tín hiệu khẩn cấp SOS.
 */
class FlashlightFragment : BaseToolFragment<FragmentFlashlightBinding>() {

    override val toolId: String = "flashlight"
    override val toolTitleResId: Int = R.string.tool_flashlight_title

    private var isTorchOn: Boolean = false
    private var isSosRunning: Boolean = false
    private var isScreenLightOn: Boolean = false

    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var hasFlashHardware: Boolean = false

    // Chuỗi mẫu SOS theo mã Morse (... --- ...) tính bằng mili-giây.
    private val sosPattern = longArrayOf(
        200, 200, 200, 200, 200, 500, // S (3 ngắn)
        600, 200, 600, 200, 600, 500, // O (3 dài)
        200, 200, 200, 200, 200, 1200 // S (3 ngắn) + nghỉ chu kỳ
    )
    private var sosIndex = 0

    private val sosRunnable = object : Runnable {
        override fun run() {
            if (isSosRunning) {
                val turnOn = (sosIndex % 2 == 0)
                setTorchModeInternal(turnOn)
                val delay = sosPattern[sosIndex]
                sosIndex = (sosIndex + 1) % sosPattern.size
                handler.postDelayed(this, delay)
            }
        }
    }

    override fun provideViewBinding(container: ViewGroup?): FragmentFlashlightBinding {
        return FragmentFlashlightBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        val context = requireContext()
        hasFlashHardware = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!hasFlashHardware || cameraId == null) {
            viewBinding.tvFlashlightWarning.visibility = View.VISIBLE
            viewBinding.tvFlashlightWarning.setText(R.string.flash_not_available)
        }

        setupButtons()
    }

    override fun onPause() {
        super.onPause()
        turnOffAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        turnOffAll()
    }

    // Thiết lập sự kiện cho các nút bật tắt đèn pin, SOS và màn hình sáng.
    private fun setupButtons() {
        viewBinding.btnToggleFlashlight.setOnClickListener {
            if (isSosRunning) stopSos()
            toggleTorch()
        }

        viewBinding.btnSosMode.setOnClickListener {
            if (isTorchOn) turnOffTorch()
            if (!isSosRunning) {
                startSos()
            } else {
                stopSos()
            }
        }

        viewBinding.btnScreenLight.setOnClickListener {
            toggleScreenLight()
        }
    }

    // Đảo trạng thái bật tắt của đèn pin camera.
    private fun toggleTorch() {
        if (!hasFlashHardware || cameraId == null) {
            toggleScreenLight()
            return
        }
        val targetState = !isTorchOn
        setTorchModeInternal(targetState)
        isTorchOn = targetState
        updateTorchUi(isTorchOn)
    }

    // Gửi lệnh điều khiển bật tắt đèn pin tới phần cứng camera.
    private fun setTorchModeInternal(enable: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager?.setTorchMode(id, enable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Cập nhật màu sắc giao diện và biểu tượng đèn pin.
    private fun updateTorchUi(isOn: Boolean) {
        if (isOn) {
            viewBinding.btnToggleFlashlight.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.colorWarning)
            )
            viewBinding.ivFlashlightIcon.setColorFilter(Color.WHITE)
            viewBinding.tvFlashlightStatus.text = "Đèn pin đang BẬT"
            viewBinding.tvFlashlightStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.textPrimary)
            )
        } else {
            viewBinding.btnToggleFlashlight.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.colorCardBackground)
            )
            viewBinding.ivFlashlightIcon.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.textSecondary)
            )
            viewBinding.tvFlashlightStatus.text = "Đèn pin đang TẮT"
            viewBinding.tvFlashlightStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.textSecondary)
            )
        }
    }

    // Bắt đầu chu kỳ nhấp nháy phát tín hiệu SOS.
    private fun startSos() {
        if (!hasFlashHardware || cameraId == null) return
        isSosRunning = true
        sosIndex = 0
        viewBinding.btnSosMode.setText("Dừng SOS")
        viewBinding.btnSosMode.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorDanger))
        handler.post(sosRunnable)
    }

    // Dừng chu kỳ nhấp nháy SOS.
    private fun stopSos() {
        isSosRunning = false
        handler.removeCallbacks(sosRunnable)
        setTorchModeInternal(false)
        viewBinding.btnSosMode.setText("Tín hiệu SOS")
        viewBinding.btnSosMode.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
        updateTorchUi(false)
    }

    // Bật tắt chế độ tăng tối đa độ sáng màn hình làm nguồn sáng thay thế.
    private fun toggleScreenLight() {
        isScreenLightOn = !isScreenLightOn
        val window = requireActivity().window
        val layoutParams = window.attributes

        if (isScreenLightOn) {
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            window.attributes = layoutParams
            viewBinding.root.setBackgroundColor(Color.WHITE)
            viewBinding.btnScreenLight.setText("Tắt sáng màn hình")
        } else {
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = layoutParams
            viewBinding.root.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.colorBackground)
            )
            viewBinding.btnScreenLight.setText("Sáng màn hình")
        }
    }

    // Tắt toàn bộ đèn pin, SOS và phục hồi độ sáng màn hình khi thoát hoặc dừng màn hình.
    private fun turnOffAll() {
        if (isSosRunning) stopSos()
        if (isTorchOn) {
            setTorchModeInternal(false)
            isTorchOn = false
            updateTorchUi(false)
        }
        if (isScreenLightOn) {
            toggleScreenLight()
        }
    }

    // Tắt nhanh đèn pin camera.
    private fun turnOffTorch() {
        if (isTorchOn) {
            setTorchModeInternal(false)
            isTorchOn = false
            updateTorchUi(false)
        }
    }
}
