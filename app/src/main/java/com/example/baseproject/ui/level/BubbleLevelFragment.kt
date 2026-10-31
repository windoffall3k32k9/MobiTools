package com.example.baseproject.ui.level

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentLevelBinding
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Công cụ thước thủy bọt nước 2 chiều kiểm tra mức cân bằng mặt phẳng qua gia tốc kế.
 */
class BubbleLevelFragment : BaseToolFragment<FragmentLevelBinding>(), SensorEventListener {

    override val toolId: String = "bubble_level"
    override val toolTitleResId: Int = R.string.tool_bubble_level_title

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private var pitchOffset = 0f
    private var rollOffset = 0f
    private var filteredPitch = 0f
    private var filteredRoll = 0f
    private var wasBalanced = false

    private val prefs by lazy {
        requireContext().getSharedPreferences("bubble_level_prefs", Context.MODE_PRIVATE)
    }

    override fun provideViewBinding(container: ViewGroup?): FragmentLevelBinding {
        return FragmentLevelBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        pitchOffset = prefs.getFloat("pitch_offset", 0f)
        rollOffset = prefs.getFloat("roll_offset", 0f)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            viewBinding.tvLevelSensorWarning.visibility = View.VISIBLE
        }

        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    // Thiết lập sự kiện cho nút đặt mốc 0 chuẩn và khôi phục mốc gốc.
    private fun setupButtons() {
        viewBinding.btnCalibrate.setOnClickListener {
            pitchOffset = filteredPitch + pitchOffset
            rollOffset = filteredRoll + rollOffset
            prefs.edit().putFloat("pitch_offset", pitchOffset).putFloat("roll_offset", rollOffset).apply()
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }

        viewBinding.btnResetCalibrate.setOnClickListener {
            pitchOffset = 0f
            rollOffset = 0f
            prefs.edit().remove("pitch_offset").remove("roll_offset").apply()
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val rawPitch = Math.toDegrees(atan2(-y.toDouble(), sqrt((x * x + z * z).toDouble()))).toFloat()
        val rawRoll = Math.toDegrees(atan2(x.toDouble(), z.toDouble())).toFloat()

        val currentPitch = rawPitch - pitchOffset
        val currentRoll = rawRoll - rollOffset

        // Bộ lọc thông thấp (low-pass filter) khử rung nhẹ
        val alpha = 0.85f
        filteredPitch = alpha * filteredPitch + (1 - alpha) * currentPitch
        filteredRoll = alpha * filteredRoll + (1 - alpha) * currentRoll

        viewBinding.bubbleLevelView.setAngles(filteredPitch, filteredRoll)
        viewBinding.tvRollValue.text = String.format(Locale.US, "%.1f°", filteredRoll)
        viewBinding.tvPitchValue.text = String.format(Locale.US, "%.1f°", filteredPitch)

        val isBalanced = abs(filteredPitch) < 1.0f && abs(filteredRoll) < 1.0f
        if (isBalanced && !wasBalanced) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
        wasBalanced = isBalanced
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
