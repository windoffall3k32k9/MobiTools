package com.example.baseproject.model

import com.example.baseproject.R

/**
 * Danh mục nhóm công cụ theo 5 nhóm chức năng được xác định trong tài liệu MobiTools.
 */
enum class ToolGroup(val titleResId: Int) {
    MEASUREMENT_SENSOR(R.string.group_measurement_sensor),
    CALCULATION_TIME(R.string.group_calculation_time),
    DEVICE_UTILITY(R.string.group_device_utility),
    TEXT_CONVERSION(R.string.group_text_conversion),
    OBSERVATION_DATA(R.string.group_observation_data)
}
