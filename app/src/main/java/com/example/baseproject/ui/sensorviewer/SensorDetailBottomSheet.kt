package com.example.baseproject.ui.sensorviewer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.baseproject.databinding.FragmentSensorDetailSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale
import kotlin.math.sqrt

/**
 * BottomSheet hiển thị toàn bộ thông số kỹ thuật chi tiết và dữ liệu đo thời gian thực của cảm biến được chọn.
 */
class SensorDetailBottomSheet : BottomSheetDialogFragment(), SensorEventListener {

    private var _binding: FragmentSensorDetailSheetBinding? = null
    private val binding get() = _binding!!

    private var sensorManager: SensorManager? = null
    private var targetSensor: Sensor? = null
    private var sensorType: Int = 0

    // Khởi tạo và nạp layout giao diện BottomSheet thông số cảm biến.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorDetailSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập dữ liệu thông số và đăng ký lắng nghe sự kiện đo lường thời gian thực.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sensorType = arguments?.getInt(ARG_SENSOR_TYPE) ?: 0
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as? SensorManager

        targetSensor = sensorManager?.getSensorList(Sensor.TYPE_ALL)?.find { it.type == sensorType }
            ?: sensorManager?.getDefaultSensor(sensorType)

        val sensor = targetSensor
        if (sensor == null) {
            binding.tvDetailSensorName.text = "Không tìm thấy cảm biến"
            binding.tvDetailLiveValues.text = "Cảm biến này hiện không khả dụng trên phần cứng thiết bị."
            binding.btnCloseDetailSheet.setOnClickListener { dismiss() }
            binding.btnCloseDetailButton.setOnClickListener { dismiss() }
            return
        }

        bindSensorStaticSpecs(sensor)
        setupActions()
    }

    override fun onResume() {
        super.onResume()
        targetSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Gắn sự kiện đóng BottomSheet cho các nút thao tác.
    private fun setupActions() {
        binding.btnCloseDetailSheet.setOnClickListener {
            dismiss()
        }
        binding.btnCloseDetailButton.setOnClickListener {
            dismiss()
        }
    }

    // Hiển thị toàn bộ thông số kỹ thuật phần cứng của cảm biến vào giao diện.
    private fun bindSensorStaticSpecs(sensor: Sensor) {
        val viName = SensorViewerFragment.getVietnameseSensorName(sensor.type) ?: sensor.name

        binding.tvDetailSensorName.text = viName
        binding.tvDetailSensorVendor.text = "${sensor.vendor} • ${sensor.name}"

        binding.tvSpecVendor.text = sensor.vendor.ifEmpty { "Không rõ" }
        binding.tvSpecModel.text = sensor.name
        binding.tvSpecTypeId.text = "Loại #${sensor.type}"
        binding.tvSpecStringType.text = sensor.stringType.ifEmpty { "android.sensor.custom" }
        binding.tvSpecVersion.text = "Phiên bản ${sensor.version}"

        val unit = getSensorUnit(sensor.type)
        binding.tvSpecMaxRange.text = String.format(Locale.US, "%.2f %s", sensor.maximumRange, unit)
        binding.tvSpecResolution.text = String.format(Locale.US, "%.5f %s", sensor.resolution, unit)
        binding.tvSpecPower.text = String.format(Locale.US, "%.3f mA", sensor.power)

        // Tần số lấy mẫu tối đa (dựa trên khoảng trễ tối thiểu minDelay tính bằng microsecond)
        if (sensor.minDelay > 0) {
            val maxRateHz = 1_000_000.0 / sensor.minDelay
            binding.tvSpecMinDelay.text = String.format(
                Locale.US,
                "%.1f Hz (%d µs)",
                maxRateHz,
                sensor.minDelay
            )
        } else {
            binding.tvSpecMinDelay.text = "Chế độ kích hoạt tức thì (0 µs)"
        }

        // Tần số lấy mẫu tối thiểu (dựa trên maxDelay)
        if (sensor.maxDelay > 0) {
            val minRateHz = 1_000_000.0 / sensor.maxDelay
            binding.tvSpecMaxDelay.text = String.format(
                Locale.US,
                "%.2f Hz (%d µs)",
                minRateHz,
                sensor.maxDelay
            )
        } else {
            binding.tvSpecMaxDelay.text = "Không quy định ngưỡng dưới"
        }

        // Chế độ báo cáo dữ liệu
        binding.tvSpecReportingMode.text = when (sensor.reportingMode) {
            Sensor.REPORTING_MODE_CONTINUOUS -> "Liên tục (Continuous)"
            Sensor.REPORTING_MODE_ON_CHANGE -> "Khi có thay đổi (On Change)"
            Sensor.REPORTING_MODE_ONE_SHOT -> "Một lần kích hoạt (One Shot)"
            Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> "Kích hoạt đặc biệt (Special Trigger)"
            else -> "Tiêu chuẩn hệ thống"
        }

        // Khả năng đánh thức CPU
        binding.tvSpecWakeUp.text = if (sensor.isWakeUpSensor) "Có hỗ trợ đánh thức thiết bị" else "Không (Non wake-up)"

        // Bộ đệm sự kiện FIFO
        binding.tvSpecFifoMax.text = "${sensor.fifoMaxEventCount} sự kiện"
    }

    // Nhận dữ liệu đo thời gian thực từ cảm biến và hiển thị chi tiết các trục.
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (_binding == null) return

        val values = event.values
        val unit = getSensorUnit(event.sensor.type)
        val sb = StringBuilder()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                sb.append(String.format(Locale.US, "• Trái / Phải (Trục X):  %+.3f %s\n", values[0], unit))
                sb.append(String.format(Locale.US, "• Trước / Sau (Trục Y):  %+.3f %s\n", values[1], unit))
                sb.append(String.format(Locale.US, "• Lên / Xuống (Trục Z):  %+.3f %s", values[2], unit))

                val magnitude = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
                binding.tvDetailLiveExtra.visibility = View.VISIBLE
                binding.tvDetailLiveExtra.text = String.format(Locale.US, "Độ lớn véc-tơ tổng hợp: %.3f %s", magnitude, unit)
            }
            Sensor.TYPE_GYROSCOPE -> {
                sb.append(String.format(Locale.US, "• Trục nghiêng X (Pitch):  %+.3f rad/s\n", values[0]))
                sb.append(String.format(Locale.US, "• Trục quay Y (Roll):      %+.3f rad/s\n", values[1]))
                sb.append(String.format(Locale.US, "• Trục xoay Z (Yaw):       %+.3f rad/s", values[2]))
                binding.tvDetailLiveExtra.visibility = View.GONE
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                sb.append(String.format(Locale.US, "• Trái / Phải (Trục X):  %+.2f µT\n", values[0]))
                sb.append(String.format(Locale.US, "• Trước / Sau (Trục Y):  %+.2f µT\n", values[1]))
                sb.append(String.format(Locale.US, "• Lên / Xuống (Trục Z):  %+.2f µT", values[2]))

                val totalField = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
                binding.tvDetailLiveExtra.visibility = View.VISIBLE
                binding.tvDetailLiveExtra.text = String.format(Locale.US, "Tổng cường độ từ trường: %.2f µT", totalField)
            }
            Sensor.TYPE_PROXIMITY -> {
                sb.append(String.format(Locale.US, "Khoảng cách phát hiện vật cản: %.1f cm", values[0]))
                val isNear = values[0] < event.sensor.maximumRange
                binding.tvDetailLiveExtra.visibility = View.VISIBLE
                binding.tvDetailLiveExtra.text = if (isNear) "Trạng thái: GẦN (Phát hiện vật cản)" else "Trạng thái: XA (Không có vật cản)"
            }
            Sensor.TYPE_LIGHT -> {
                sb.append(String.format(Locale.US, "Cường độ ánh sáng môi trường: %.1f lux", values[0]))
                binding.tvDetailLiveExtra.visibility = View.GONE
            }
            Sensor.TYPE_PRESSURE -> {
                sb.append(String.format(Locale.US, "Áp suất khí quyển: %.2f hPa", values[0]))
                binding.tvDetailLiveExtra.visibility = View.GONE
            }
            Sensor.TYPE_STEP_COUNTER -> {
                sb.append(String.format(Locale.US, "Tổng số bước chân đã đếm: %.0f bước", values[0]))
                binding.tvDetailLiveExtra.visibility = View.GONE
            }
            else -> {
                when (values.size) {
                    1 -> sb.append(String.format(Locale.US, "Giá trị đo được: %.3f %s", values[0], unit))
                    2 -> {
                        sb.append(String.format(Locale.US, "• Trái / Phải:  %+.3f %s\n", values[0], unit))
                        sb.append(String.format(Locale.US, "• Trước / Sau:  %+.3f %s", values[1], unit))
                    }
                    else -> {
                        sb.append(String.format(Locale.US, "• Trái / Phải:  %+.3f %s\n", values[0], unit))
                        sb.append(String.format(Locale.US, "• Trước / Sau:  %+.3f %s\n", values[1], unit))
                        sb.append(String.format(Locale.US, "• Lên / Xuống:  %+.3f %s", values[2], unit))
                    }
                }
                binding.tvDetailLiveExtra.visibility = View.GONE
            }
        }

        binding.tvDetailLiveValues.text = sb.toString()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // Lấy đơn vị đo lường tương ứng của loại cảm biến.
    private fun getSensorUnit(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION -> "m/s²"
            Sensor.TYPE_GYROSCOPE -> "rad/s"
            Sensor.TYPE_MAGNETIC_FIELD -> "µT"
            Sensor.TYPE_LIGHT -> "lux"
            Sensor.TYPE_PRESSURE -> "hPa"
            Sensor.TYPE_PROXIMITY -> "cm"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "°C"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "%"
            Sensor.TYPE_STEP_COUNTER -> "bước"
            else -> ""
        }
    }

    companion object {
        private const val ARG_SENSOR_TYPE = "arg_sensor_type"

        // Khởi tạo một phiên bản BottomSheet chi tiết cảm biến với mã định danh loại cảm biến.
        fun newInstance(sensorType: Int): SensorDetailBottomSheet {
            return SensorDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SENSOR_TYPE, sensorType)
                }
            }
        }
    }
}
