package com.example.baseproject.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Thực thể lưu trữ trạng thái yêu thích và lịch sử sử dụng của từng công cụ.
 */
@Entity(tableName = "tool_usage")
data class ToolUsageEntity(
    @PrimaryKey
    val toolId: String,
    val isFavorite: Boolean = false,
    val lastUsedTimestamp: Long = 0L
)
