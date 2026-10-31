package com.example.baseproject.ui.inclinometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentInclinometerBinding
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Công cụ đo góc nghiêng bề mặt với tính năng đóng băng số đo (Hold) và hiệu chuẩn mốc 0.
 */
class InclinometerFragment : BaseToolFragment<FragmentInclinometerBinding>(), SensorEventListener {

    override val toolId: String = "inclinometer"
    override val toolTitleResId: Int = R.string.tool_inclinometer_title

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private var isHolding = false
    private var zeroOffset = 0.0
    private var currentAngle = 0.0

    override fun provideViewBinding(container: ViewGroup?): FragmentInclinometerBinding {
        return FragmentInclinometerBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    // Thiết lập sự kiện nút giữ số đo và hiệu chuẩn mốc chuẩn 0.
    private fun setupButtons() {
        viewBinding.btnHoldValue.setOnClickListener {
            isHolding = !isHolding
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            if (isHolding) {
                viewBinding.btnHoldValue.text = "Tiếp tục đo"
                viewBinding.btnHoldValue.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorWarning)
                viewBinding.tvHoldStatus.text = "Đã giữ giá trị"
                viewBinding.tvHoldStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorWarning)
                )
            } else {
                viewBinding.btnHoldValue.text = "Giữ giá trị"
                viewBinding.btnHoldValue.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)
                viewBinding.tvHoldStatus.text = "Đang đo trực tiếp"
                viewBinding.tvHoldStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
            }
        }

        viewBinding.btnCalibrateZero.setOnClickListener {
            zeroOffset = currentAngle + zeroOffset
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isHolding) return
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0].toDouble()
        val y = event.values[1].toDouble()
        val z = event.values[2].toDouble()

        // Tính góc nghiêng so với mặt phẳng ngang
        val norm = sqrt(x * x + y * y)
        val rawAngle = Math.toDegrees(atan2(norm, abs(z)))
        currentAngle = rawAngle - zeroOffset

        val displayAngle = abs(currentAngle)
        val rad = Math.toRadians(displayAngle)
        val slope = if (displayAngle < 89.9) abs(tan(rad)) * 100.0 else 999.9

        viewBinding.tvInclineAngle.text = String.format(Locale.US, "%.1f°", displayAngle)
        viewBinding.tvSlopePercent.text = String.format(Locale.US, "Độ dốc: %.1f%%", slope)
        viewBinding.tvHorizontalAngle.text = String.format(Locale.US, "%.1f°", displayAngle)
        viewBinding.tvVerticalAngle.text = String.format(Locale.US, "%.1f°", abs(90.0 - displayAngle))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
