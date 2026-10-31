package com.example.baseproject.ui.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentCompassBinding
import kotlin.math.roundToInt

/**
 * Công cụ la bàn điện tử sử dụng cảm biến định hướng và từ kế với kim xoay trực quan.
 */
class CompassFragment : BaseToolFragment<FragmentCompassBinding>(), SensorEventListener {

    override val toolId: String = "compass"
    override val toolTitleResId: Int = R.string.tool_compass_title

    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var hasAccelerometer = false
    private var hasMagnetometer = false
    private var currentAzimuth = 0f

    override fun provideViewBinding(container: ViewGroup?): FragmentCompassBinding {
        return FragmentCompassBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationVectorSensor == null && (accelerometer == null || magnetometer == null)) {
            viewBinding.tvCompassSensorWarning.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        rotationVectorSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        } ?: run {
            accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            if (azimuth < 0) azimuth += 360f
            updateCompass(azimuth)
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                hasAccelerometer = true
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                hasMagnetometer = true
            }

            if (hasAccelerometer && hasMagnetometer) {
                val success = SensorManager.getRotationMatrix(
                    rotationMatrix,
                    null,
                    accelerometerReading,
                    magnetometerReading
                )
                if (success) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    if (azimuth < 0) azimuth += 360f
                    updateCompass(azimuth)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // Cập nhật góc quay của kim la bàn và hướng phương vị theo độ đo.
    private fun updateCompass(azimuth: Float) {
        currentAzimuth = azimuth
        val roundedDegree = azimuth.roundToInt() % 360

        viewBinding.tvAzimuthDegree.text = "$roundedDegree°"
        viewBinding.tvDirectionName.text = getDirectionText(roundedDegree)
        viewBinding.ivCompassNeedle.rotation = -azimuth
    }

    // Xác định tên hướng địa lý dựa trên số đo góc phương vị.
    private fun getDirectionText(degree: Int): String {
        return when (degree) {
            in 0..22, in 338..360 -> "BẮC (N)"
            in 23..67 -> "ĐÔNG BẮC (NE)"
            in 68..112 -> "ĐÔNG (E)"
            in 113..157 -> "ĐÔNG NAM (SE)"
            in 158..202 -> "NAM (S)"
            in 203..247 -> "TÂY NAM (SW)"
            in 248..292 -> "TÂY (W)"
            in 293..337 -> "TÂY BẮC (NW)"
            else -> "BẮC (N)"
        }
    }
}
