package com.example.baseproject.registry

import android.content.Context
import com.example.baseproject.R
import com.example.baseproject.model.ToolDescriptor
import com.example.baseproject.model.ToolGroup
import com.example.baseproject.ui.calculator.CalculatorFragment
import com.example.baseproject.ui.compass.CompassFragment
import com.example.baseproject.ui.converter.UnitConverterFragment
import com.example.baseproject.ui.counter.TallyCounterFragment
import com.example.baseproject.ui.dailymath.DailyMathFragment
import com.example.baseproject.ui.flashlight.FlashlightFragment
import com.example.baseproject.ui.inclinometer.InclinometerFragment
import com.example.baseproject.ui.level.BubbleLevelFragment
import com.example.baseproject.ui.qr.QrGeneratorFragment
import com.example.baseproject.ui.scanner.BarcodeScannerFragment
import com.example.baseproject.ui.sensorviewer.SensorViewerFragment
import com.example.baseproject.ui.soundmeter.SoundMeterFragment
import com.example.baseproject.ui.stopwatch.StopwatchFragment
import com.example.baseproject.ui.texttools.TextToolsFragment
import com.example.baseproject.ui.timer.CountdownTimerFragment

/**
 * Đăng ký và quản trị danh mục công cụ tập trung cho toàn bộ hệ thống MobiTools.
 */
object ToolRegistry {

    private val tools = mutableListOf<ToolDescriptor>()

    init {
        initDefaultTools()
    }

    // Đăng ký toàn bộ 15 công cụ của MobiTools vào hệ thống.
    private fun initDefaultTools() {
        // --- 1. Nhóm Tính toán & Thời gian ---
        register(
            ToolDescriptor(
                id = "calculator",
                titleResId = R.string.tool_calculator_title,
                descriptionResId = R.string.tool_calculator_desc,
                iconResId = R.drawable.ic_calculator,
                group = ToolGroup.CALCULATION_TIME,
                createFragment = { CalculatorFragment() }
            )
        )

        register(
            ToolDescriptor(
                id = "stopwatch",
                titleResId = R.string.tool_stopwatch_title,
                descriptionResId = R.string.tool_stopwatch_desc,
                iconResId = R.drawable.ic_stopwatch,
                group = ToolGroup.CALCULATION_TIME,
                createFragment = { StopwatchFragment() }
            )
        )

        register(
            ToolDescriptor(
                id = "timer",
                titleResId = R.string.tool_timer_title,
                descriptionResId = R.string.tool_timer_desc,
                iconResId = R.drawable.ic_timer,
                group = ToolGroup.CALCULATION_TIME,
                createFragment = { CountdownTimerFragment() }
            )
        )

        register(
            ToolDescriptor(
                id = "counter",
                titleResId = R.string.tool_counter_title,
                descriptionResId = R.string.tool_counter_desc,
                iconResId = R.drawable.ic_counter,
                group = ToolGroup.CALCULATION_TIME,
                createFragment = { TallyCounterFragment() }
            )
        )

        register(
            ToolDescriptor(
                id = "daily_math",
                titleResId = R.string.tool_daily_math_title,
                descriptionResId = R.string.tool_daily_math_desc,
                iconResId = R.drawable.ic_math,
                group = ToolGroup.CALCULATION_TIME,
                createFragment = { DailyMathFragment() }
            )
        )

        // --- 2. Nhóm Tiện ích thiết bị ---
        register(
            ToolDescriptor(
                id = "flashlight",
                titleResId = R.string.tool_flashlight_title,
                descriptionResId = R.string.tool_flashlight_desc,
                iconResId = R.drawable.ic_flashlight,
                group = ToolGroup.DEVICE_UTILITY,
                createFragment = { FlashlightFragment() }
            )
        )

        register(
            ToolDescriptor(
                id = "qr_generator",
                titleResId = R.string.tool_qr_generator_title,
                descriptionResId = R.string.tool_qr_generator_desc,
                iconResId = R.drawable.ic_qr_code,
                group = ToolGroup.DEVICE_UTILITY,
                createFragment = { QrGeneratorFragment() }
            )
        )

        register(
            ToolDescriptor(
                id = "barcode_scanner",
                titleResId = R.string.tool_barcode_scanner_title,
                descriptionResId = R.string.tool_barcode_scanner_desc,
                iconResId = R.drawable.ic_barcode_scanner,
                group = ToolGroup.DEVICE_UTILITY,
                createFragment = { BarcodeScannerFragment() }
            )
        )

        // --- 3. Nhóm Văn bản & Chuyển đổi ---
        register(
            ToolDescriptor(
                id = "unit_converter",
                titleResId = R.string.tool_unit_converter_title,
                descriptionResId = R.string.tool_unit_converter_desc,
                iconResId = R.drawable.ic_converter,
                group = ToolGroup.TEXT_CONVERSION,
                createFragment = { UnitConverterFragment() }
            )
        )

        register(
            ToolDescriptor(
                id = "text_tools",
                titleResId = R.string.tool_text_tools_title,
                descriptionResId = R.string.tool_text_tools_desc,
                iconResId = R.drawable.ic_text_format,
                group = ToolGroup.TEXT_CONVERSION,
                createFragment = { TextToolsFragment() }
            )
        )

        // --- 4. Nhóm Đo đạc & Cảm biến ---
        register(
            ToolDescriptor(
                id = "compass",
                titleResId = R.string.tool_compass_title,
                descriptionResId = R.string.tool_compass_desc,
                iconResId = R.drawable.ic_compass,
                group = ToolGroup.MEASUREMENT_SENSOR,
                createFragment = { CompassFragment() }
            )
        )

        register(
            ToolDescriptor(
                id = "bubble_level",
                titleResId = R.string.tool_bubble_level_title,
                descriptionResId = R.string.tool_bubble_level_desc,
                iconResId = R.drawable.ic_level,
                group = ToolGroup.MEASUREMENT_SENSOR,
                createFragment = { BubbleLevelFragment() }
            )
        )

        register(
            ToolDescriptor(
                id = "inclinometer",
                titleResId = R.string.tool_inclinometer_title,
                descriptionResId = R.string.tool_inclinometer_desc,
                iconResId = R.drawable.ic_inclinometer,
                group = ToolGroup.MEASUREMENT_SENSOR,
                createFragment = { InclinometerFragment() }
            )
        )

        register(
            ToolDescriptor(
                id = "sound_meter",
                titleResId = R.string.tool_sound_meter_title,
                descriptionResId = R.string.tool_sound_meter_desc,
                iconResId = R.drawable.ic_sound_meter,
                group = ToolGroup.MEASUREMENT_SENSOR,
                createFragment = { SoundMeterFragment() }
            )
        )

        // --- 5. Nhóm Quan sát & Dữ liệu ---
        register(
            ToolDescriptor(
                id = "sensor_viewer",
                titleResId = R.string.tool_sensor_viewer_title,
                descriptionResId = R.string.tool_sensor_viewer_desc,
                iconResId = R.drawable.ic_sensors,
                group = ToolGroup.OBSERVATION_DATA,
                createFragment = { SensorViewerFragment() }
            )
        )
    }

    // Đăng ký một công cụ vào danh mục hệ thống nếu chưa tồn tại.
    @Synchronized
    fun register(descriptor: ToolDescriptor) {
        if (tools.none { it.id == descriptor.id }) {
            tools.add(descriptor)
        }
    }

    // Đăng ký danh sách công cụ theo khối.
    @Synchronized
    fun registerAll(descriptors: List<ToolDescriptor>) {
        descriptors.forEach { register(it) }
    }

    // Lấy toàn bộ danh sách công cụ đã đăng ký.
    fun getAll(): List<ToolDescriptor> = tools.toList()

    // Lấy công cụ theo mã định danh.
    fun getById(id: String): ToolDescriptor? = tools.find { it.id == id }

    // Lấy danh sách công cụ thuộc một nhóm chức năng.
    fun getByGroup(group: ToolGroup): List<ToolDescriptor> = tools.filter { it.group == group }

    // Tìm kiếm công cụ cục bộ theo từ khóa trong tên hoặc mô tả.
    fun search(context: Context, query: String): List<ToolDescriptor> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return getAll()
        return tools.filter { descriptor ->
            val title = context.getString(descriptor.titleResId).lowercase()
            val desc = context.getString(descriptor.descriptionResId).lowercase()
            title.contains(trimmed) || desc.contains(trimmed)
        }
    }
}
