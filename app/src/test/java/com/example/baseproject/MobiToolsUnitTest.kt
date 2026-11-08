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

    // Kiểm tra danh mục công cụ đã đăng ký và không có mã trùng lặp.
    @Test
    fun testToolRegistryRegistration() {
        val tools = ToolRegistry.getAll()
        assertTrue("Tổng số công cụ phải từ 15 trở lên", tools.size >= 15)

        val uniqueIds = tools.map { it.id }.toSet()
        assertEquals(tools.size, uniqueIds.size)

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

    // Kiểm tra động cơ xử lý văn bản TextTransformEngine trên tất cả các chế độ chuyển đổi.
    @Test
    fun testTextTransformEngineAllModes() {
        val input = "mobiTools là ứng dụng. tiện ích khác!"
        val upper = com.example.baseproject.ui.texttools.TextTransformEngine.toUpperCase(input)
        assertEquals("MOBITOOLS LÀ ỨNG DỤNG. TIỆN ÍCH KHÁC!", upper)

        val lower = com.example.baseproject.ui.texttools.TextTransformEngine.toLowerCase(upper)
        assertEquals("mobitools là ứng dụng. tiện ích khác!", lower)

        val title = com.example.baseproject.ui.texttools.TextTransformEngine.toTitleCase("mobiTools là ứng dụng tiện ích")
        assertEquals("Mobitools Là Ứng Dụng Tiện Ích", title)

        val sentence = com.example.baseproject.ui.texttools.TextTransformEngine.toSentenceCase("mobiTools là ứng dụng. tiện ích khác! tuyệt vời?")
        assertEquals("Mobitools là ứng dụng. Tiện ích khác! Tuyệt vời?", sentence)

        val spaced = "  đoạn 1   từ 2  \n  đoạn 2   từ 3  "
        val trimmed = com.example.baseproject.ui.texttools.TextTransformEngine.trimSpaces(spaced)
        assertEquals("đoạn 1 từ 2\nđoạn 2 từ 3", trimmed)

        val brokenLines = "Dòng 1\nDòng 2\r\nDòng 3"
        val singleLine = com.example.baseproject.ui.texttools.TextTransformEngine.removeLineBreaks(brokenLines)
        assertEquals("Dòng 1 Dòng 2 Dòng 3", singleLine)

        val wordCount = com.example.baseproject.ui.texttools.TextTransformEngine.countWords("MobiTools là ứng dụng tiện ích")
        assertEquals(6, wordCount)

        val lineCount = com.example.baseproject.ui.texttools.TextTransformEngine.countLines("1\n2\n3")
        assertEquals(3, lineCount)
    }
}
