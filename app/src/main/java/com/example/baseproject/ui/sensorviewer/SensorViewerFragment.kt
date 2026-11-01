package com.example.baseproject.ui.sensorviewer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.baseproject.R
import com.example.baseproject.base.BaseToolFragment
import com.example.baseproject.databinding.FragmentSensorViewerBinding
import com.example.baseproject.databinding.ItemSensorCardBinding
import java.util.Locale

/**
 * Công cụ hiển thị toàn bộ cảm biến phần cứng có thể nhận biết của thiết bị với tên tiếng Việt rõ ràng.
 */
class SensorViewerFragment : BaseToolFragment<FragmentSensorViewerBinding>(), SensorEventListener {

    override val toolId: String = "sensor_viewer"
    override val toolTitleResId: Int = R.string.tool_sensor_viewer_title

    private var sensorManager: SensorManager? = null
    private var sensorList: List<Sensor> = emptyList()
    private val sensorValueMap = mutableMapOf<Int, FloatArray>()
    private lateinit var sensorAdapter: SensorViewerAdapter

    // Khởi tạo ViewBinding liên kết với giao diện trình xem cảm biến.
    override fun provideViewBinding(container: ViewGroup?): FragmentSensorViewerBinding {
        return FragmentSensorViewerBinding.inflate(layoutInflater, container, false)
    }

    // Lọc danh sách cảm biến chuẩn có tên tiếng Việt và khởi tạo danh sách hiển thị.
    override fun initViews() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rawSensors = sensorManager?.getSensorList(Sensor.TYPE_ALL) ?: emptyList()

        // Lọc các cảm biến có tên rõ ràng và loại bỏ cảm biến kiểm thử nội bộ không xác định
        sensorList = rawSensors
            .filter { getVietnameseSensorName(it.type) != null }
            .distinctBy { it.type }

        viewBinding.tvTotalSensorsCount.text = "Phát hiện ${sensorList.size} cảm biến tiêu chuẩn"

        sensorAdapter = SensorViewerAdapter(sensorList, sensorValueMap) { sensor ->
            val sheet = SensorDetailBottomSheet.newInstance(sensor.type)
            sheet.show(childFragmentManager, "sensor_detail_${sensor.type}")
        }
        viewBinding.rvSensors.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sensorAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        sensorList.forEach { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    // Nhận sự kiện thay đổi dữ liệu đo và cập nhật mục hiển thị tương ứng trong adapter.
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val type = event.sensor.type
        sensorValueMap[type] = event.values.clone()

        val index = sensorList.indexOfFirst { it.type == type }
        if (index != -1) {
            sensorAdapter.notifyItemChanged(index, Unit)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        // Ánh xạ mã định danh cảm biến Android thành tên tiếng Việt rõ ràng, trả về null nếu không xác định.
        fun getVietnameseSensorName(type: Int): String? {
            return when (type) {
                Sensor.TYPE_PROXIMITY -> "Cảm biến tiệm cận"
                Sensor.TYPE_ACCELEROMETER -> "Cảm biến gia tốc"
                Sensor.TYPE_MAGNETIC_FIELD -> "Cảm biến từ kế"
                Sensor.TYPE_GYROSCOPE -> "Cảm biến con quay hồi chuyển"
                Sensor.TYPE_LIGHT -> "Cảm biến ánh sáng"
                Sensor.TYPE_PRESSURE -> "Cảm biến áp suất khí quyển"
                Sensor.TYPE_GRAVITY -> "Cảm biến trọng lực"
                Sensor.TYPE_LINEAR_ACCELERATION -> "Cảm biến gia tốc tuyến tính"
                Sensor.TYPE_ROTATION_VECTOR -> "Cảm biến vectơ xoay"
                Sensor.TYPE_STEP_COUNTER -> "Cảm biến đếm bước chân"
                Sensor.TYPE_STEP_DETECTOR -> "Cảm biến phát hiện bước"
                Sensor.TYPE_SIGNIFICANT_MOTION -> "Cảm biến chuyển động tức thời"
                Sensor.TYPE_AMBIENT_TEMPERATURE -> "Cảm biến nhiệt độ môi trường"
                Sensor.TYPE_RELATIVE_HUMIDITY -> "Cảm biến độ ẩm không khí"
                Sensor.TYPE_GAME_ROTATION_VECTOR -> "Cảm biến xoay định hướng"
                Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "Cảm biến vectơ địa từ"
                else -> null
            }
        }

        // Lấy đơn vị đo lường tương ứng của loại cảm biến.
        fun getSensorUnit(type: Int): String {
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
    }
}

/**
 * Adapter danh sách cảm biến hiển thị tên tiếng Việt màu xanh và thông số trước sau trái phải.
 */
class SensorViewerAdapter(
    private val sensors: List<Sensor>,
    private val valueMap: Map<Int, FloatArray>,
    private val onSensorClick: (Sensor) -> Unit
) : RecyclerView.Adapter<SensorViewerAdapter.SensorViewHolder>() {

    inner class SensorViewHolder(val binding: ItemSensorCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val binding = ItemSensorCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SensorViewHolder(binding)
    }

    override fun getItemCount(): Int = sensors.size

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        val sensor = sensors[position]
        val viName = SensorViewerFragment.getVietnameseSensorName(sensor.type) ?: sensor.name

        holder.binding.root.setOnClickListener {
            onSensorClick(sensor)
        }

        holder.binding.tvSensorName.text = viName
        holder.binding.tvSensorVendor.text = "Nhà sản xuất: ${sensor.vendor}"

        val values = valueMap[sensor.type]
        val unit = SensorViewerFragment.getSensorUnit(sensor.type)
        if (values != null && values.isNotEmpty()) {
            val textBuilder = StringBuilder()
            when (sensor.type) {
                Sensor.TYPE_PROXIMITY -> {
                    val isNear = values[0] < sensor.maximumRange
                    textBuilder.append(String.format(Locale.US, "Khoảng cách đo: %.1f cm (%s)", values[0], if (isNear) "GẦN" else "XA"))
                }
                Sensor.TYPE_LIGHT -> {
                    textBuilder.append(String.format(Locale.US, "Cường độ ánh sáng: %.1f lux", values[0]))
                }
                Sensor.TYPE_PRESSURE -> {
                    textBuilder.append(String.format(Locale.US, "Áp suất khí quyển: %.1f hPa", values[0]))
                }
                Sensor.TYPE_STEP_COUNTER -> {
                    textBuilder.append(String.format(Locale.US, "Tổng số bước chân: %.0f bước", values[0]))
                }
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_GRAVITY,
                Sensor.TYPE_LINEAR_ACCELERATION,
                Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_GYROSCOPE -> {
                    if (values.size >= 3) {
                        textBuilder.append(String.format(Locale.US, "Trái / Phải:  %+.2f %s\n", values[0], unit))
                        textBuilder.append(String.format(Locale.US, "Trước / Sau:  %+.2f %s\n", values[1], unit))
                        textBuilder.append(String.format(Locale.US, "Lên / Xuống:  %+.2f %s", values[2], unit))
                    } else {
                        textBuilder.append(String.format(Locale.US, "Trái / Phải: %+.2f %s", values[0], unit))
                    }
                }
                else -> {
                    when (values.size) {
                        1 -> textBuilder.append(String.format(Locale.US, "Giá trị: %.2f %s", values[0], unit).trim())
                        2 -> {
                            textBuilder.append(String.format(Locale.US, "Trái / Phải: %+.2f %s\n", values[0], unit))
                            textBuilder.append(String.format(Locale.US, "Trước / Sau: %+.2f %s", values[1], unit))
                        }
                        else -> {
                            textBuilder.append(String.format(Locale.US, "Trái / Phải:  %+.2f %s\n", values[0], unit))
                            textBuilder.append(String.format(Locale.US, "Trước / Sau:  %+.2f %s\n", values[1], unit))
                            textBuilder.append(String.format(Locale.US, "Lên / Xuống:  %+.2f %s", values[2], unit))
                        }
                    }
                }
            }
            holder.binding.tvSensorLiveValues.text = textBuilder.toString()
        } else {
            holder.binding.tvSensorLiveValues.text = "Đang chờ tín hiệu cảm biến…"
        }

        holder.binding.tvSensorSpecs.text = "Nhấn để xem thông số chi tiết →"
    }
}
