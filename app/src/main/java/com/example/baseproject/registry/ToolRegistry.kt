package com.example.baseproject.registry

import android.content.Context
import com.example.baseproject.R
import com.example.baseproject.model.ToolDescriptor
import com.example.baseproject.model.ToolGroup
import com.example.baseproject.ui.calculator.CalculatorFragment
import com.example.baseproject.ui.counter.TallyCounterFragment
import com.example.baseproject.ui.stopwatch.StopwatchFragment
import com.example.baseproject.ui.timer.CountdownTimerFragment

/**
 * Đăng ký và quản trị danh mục công cụ tập trung cho MobiTools.
 */
object ToolRegistry {

    private val tools = mutableListOf<ToolDescriptor>()

    init {
        initDefaultTools()
    }

    // Đăng ký toàn bộ các công cụ mặc định vào hệ thống.
    private fun initDefaultTools() {
        // 1. Máy tính
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

        // 2. Bấm giờ
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

        // 3. Đếm ngược
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

        // 4. Bộ đếm
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
