package com.example.baseproject.registry

import android.content.Context
import com.example.baseproject.model.ToolDescriptor
import com.example.baseproject.model.ToolGroup

/**
 * Đăng ký và quản trị danh mục công cụ tập trung cho MobiTools.
 */
object ToolRegistry {

    private val tools = mutableListOf<ToolDescriptor>()

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
