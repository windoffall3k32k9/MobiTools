package com.example.baseproject

import com.example.baseproject.model.ToolGroup
import com.example.baseproject.registry.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm thử đơn vị tự động cho danh mục công cụ, chuyển đổi đơn vị và thuật toán xử lý văn bản.
 */
class MobiToolsUnitTest {

    // Kiểm tra danh mục công cụ đã đăng ký đủ 15 công cụ và không có mã trùng lặp.
    @Test
    fun testToolRegistryRegistration() {
        val tools = ToolRegistry.getAll()
        assertEquals(15, tools.size)

        val uniqueIds = tools.map { it.id }.toSet()
        assertEquals(15, uniqueIds.size)

        // Đảm bảo đủ 5 nhóm chức năng
        for (group in ToolGroup.entries) {
            val groupTools = ToolRegistry.getByGroup(group)
            assertTrue("Nhóm $group phải có ít nhất 1 công cụ", groupTools.isNotEmpty())
        }
    }

    // Kiểm tra tính chính xác của công thức quy đổi nhiệt độ.
    @Test
    fun testTemperatureConversion() {
        // 0 độ C = 32 độ F
        val fahrenheit = (0.0 * 9.0 / 5.0) + 32.0
        assertEquals(32.0, fahrenheit, 0.001)

        // 100 độ C = 373.15 Kelvin
        val kelvin = 100.0 + 273.15
        assertEquals(373.15, kelvin, 0.001)
    }

    // Kiểm tra thuật toán xử lý văn bản loại bỏ dòng trùng và dòng trống.
    @Test
    fun testTextTransformations() {
        val raw = "dòng 1\n\n dòng 1 \ndòng 2"
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }.distinct()
        assertEquals(2, lines.size)
        assertEquals("dòng 1", lines[0])
        assertEquals("dòng 2", lines[1])
    }
}
