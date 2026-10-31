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
 * Công cụ hiển thị toàn bộ cảm biến phần cứng của thiết bị và các thông số đo thời gian thực.
 */
class SensorViewerFragment : BaseToolFragment<FragmentSensorViewerBinding>(), SensorEventListener {

    override val toolId: String = "sensor_viewer"
    override val toolTitleResId: Int = R.string.tool_sensor_viewer_title

    private var sensorManager: SensorManager? = null
    private var sensorList: List<Sensor> = emptyList()
    private val sensorValueMap = mutableMapOf<Int, FloatArray>()
    private lateinit var sensorAdapter: SensorViewerAdapter

    override fun provideViewBinding(container: ViewGroup?): FragmentSensorViewerBinding {
        return FragmentSensorViewerBinding.inflate(layoutInflater, container, false)
    }

    override fun initViews() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorList = sensorManager?.getSensorList(Sensor.TYPE_ALL) ?: emptyList()

        viewBinding.tvTotalSensorsCount.text = "Phát hiện ${sensorList.size} cảm biến phần cứng"

        sensorAdapter = SensorViewerAdapter(sensorList, sensorValueMap)
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
}

/**
 * Adapter danh sách cảm biến hiển thị chi tiết tên, nhà sản xuất và chỉ số đo theo thời gian thực.
 */
class SensorViewerAdapter(
    private val sensors: List<Sensor>,
    private val valueMap: Map<Int, FloatArray>
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
        holder.binding.tvSensorName.text = sensor.name
        holder.binding.tvSensorType.text = getSensorTypeName(sensor.type)
        holder.binding.tvSensorVendor.text = "${sensor.vendor} • v${sensor.version}"

        val values = valueMap[sensor.type]
        if (values != null && values.isNotEmpty()) {
            val textBuilder = StringBuilder()
            when (values.size) {
                1 -> textBuilder.append(String.format(Locale.US, "Giá trị: %.2f", values[0]))
                2 -> textBuilder.append(String.format(Locale.US, "X: %.2f, Y: %.2f", values[0], values[1]))
                else -> textBuilder.append(
                    String.format(Locale.US, "X: %.2f, Y: %.2f, Z: %.2f", values[0], values[1], values[2])
                )
            }
            holder.binding.tvSensorLiveValues.text = textBuilder.toString()
        } else {
            holder.binding.tvSensorLiveValues.text = "Đang chờ dữ liệu đo…"
        }

        holder.binding.tvSensorSpecs.text = String.format(
            Locale.US,
            "Tầm đo tối đa: %.1f • Độ phân giải: %.4f • Năng lượng: %.2f mA",
            sensor.maximumRange,
            sensor.resolution,
            sensor.power
        )
    }

    // Chuyển mã loại cảm biến số nguyên thành tên trực quan.
    private fun getSensorTypeName(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> "GIA TỐC (ACCELEROMETER)"
            Sensor.TYPE_MAGNETIC_FIELD -> "TỪ KẾ (MAGNETOMETER)"
            Sensor.TYPE_GYROSCOPE -> "CON QUAY (GYROSCOPE)"
            Sensor.TYPE_LIGHT -> "ÁNH SÁNG (LIGHT)"
            Sensor.TYPE_PRESSURE -> "ÁP SUẤT (BAROMETER)"
            Sensor.TYPE_PROXIMITY -> "TIỆM CẬN (PROXIMITY)"
            Sensor.TYPE_GRAVITY -> "TRỌNG LỰC (GRAVITY)"
            Sensor.TYPE_ROTATION_VECTOR -> "VECTƠ XOAY (ROTATION)"
            Sensor.TYPE_STEP_COUNTER -> "ĐẾM BƯỚC (STEP COUNTER)"
            else -> "CẢM BIẾN #$type"
        }
    }
}
