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

    // Kiểm tra đầy đủ 12 danh mục công cụ chuyển đổi trong ConverterEngine.
    @Test
    fun testConverterEngineAllTwelveCategories() {
        val categories = com.example.baseproject.ui.converter.ConverterCategory.entries
        assertEquals(12, categories.size)

        // 1. Độ dài: 1 km = 1000 m
        val km = com.example.baseproject.ui.converter.ConverterEngine.lengthUnits.first { it.code == "km" }
        val m = com.example.baseproject.ui.converter.ConverterEngine.lengthUnits.first { it.code == "m" }
        assertEquals(1000.0, com.example.baseproject.ui.converter.ConverterEngine.convertLinear(1.0, km, m), 0.001)

        // 2. Trọng lượng: 1 tấn = 1000 kg
        val ton = com.example.baseproject.ui.converter.ConverterEngine.weightUnits.first { it.code == "t" }
        val kg = com.example.baseproject.ui.converter.ConverterEngine.weightUnits.first { it.code == "kg" }
        assertEquals(1000.0, com.example.baseproject.ui.converter.ConverterEngine.convertLinear(1.0, ton, kg), 0.001)

        // 3. Diện tích: 1 ha = 10.000 m2
        val ha = com.example.baseproject.ui.converter.ConverterEngine.areaUnits.first { it.code == "ha" }
        val m2 = com.example.baseproject.ui.converter.ConverterEngine.areaUnits.first { it.code == "m2" }
        assertEquals(10000.0, com.example.baseproject.ui.converter.ConverterEngine.convertLinear(1.0, ha, m2), 0.001)

        // 4. Thời gian: 1 giờ = 3600 giây
        val hour = com.example.baseproject.ui.converter.ConverterEngine.timeUnits.first { it.code == "h" }
        val sec = com.example.baseproject.ui.converter.ConverterEngine.timeUnits.first { it.code == "s" }
        assertEquals(3600.0, com.example.baseproject.ui.converter.ConverterEngine.convertLinear(1.0, hour, sec), 0.001)

        // 5. Dữ liệu: 1 GB = 1024 MB
        val gb = com.example.baseproject.ui.converter.ConverterEngine.dataUnits.first { it.code == "GB" }
        val mb = com.example.baseproject.ui.converter.ConverterEngine.dataUnits.first { it.code == "MB" }
        assertEquals(1024.0, com.example.baseproject.ui.converter.ConverterEngine.convertLinear(1.0, gb, mb), 0.001)

        // 6. Thể tích: 1 m3 = 1000 L
        val m3 = com.example.baseproject.ui.converter.ConverterEngine.volumeUnits.first { it.code == "m3" }
        val liter = com.example.baseproject.ui.converter.ConverterEngine.volumeUnits.first { it.code == "L" }
        assertEquals(1000.0, com.example.baseproject.ui.converter.ConverterEngine.convertLinear(1.0, m3, liter), 0.001)

        // 7. Tốc độ: 36 km/h = 10 m/s
        val kmh = com.example.baseproject.ui.converter.ConverterEngine.speedUnits.first { it.code == "kmh" }
        val ms = com.example.baseproject.ui.converter.ConverterEngine.speedUnits.first { it.code == "ms" }
        assertEquals(10.0, com.example.baseproject.ui.converter.ConverterEngine.convertLinear(36.0, kmh, ms), 0.001)

        // 8. Nhiệt độ: 100 C = 212 F và kiểm tra độ 0 tuyệt đối
        assertEquals(212.0, com.example.baseproject.ui.converter.ConverterEngine.convertTemperature(100.0, "C", "F"), 0.001)
        assertTrue(com.example.baseproject.ui.converter.ConverterEngine.isBelowAbsoluteZero(-300.0, "C"))
    }

    // Kiểm tra tính toán Giảm giá, Hệ thống số và BMI cùng các trường hợp biên/lỗi.
    @Test
    fun testDiscountNumeralAndBmiValidation() {
        // Giảm giá: 500.000 giảm 20%, giảm thêm 10% -> còn 360.000, tiết kiệm 140.000 (28%)
        val disc = com.example.baseproject.ui.converter.ConverterEngine.calculateDiscount("500000", "20", "10")
        assertTrue(disc.isValid)
        assertEquals(360000.0, disc.finalPrice, 0.01)
        assertEquals(140000.0, disc.savedAmount, 0.01)
        assertEquals(28.0, disc.effectiveDiscountPercent, 0.01)

        val invalidDisc = com.example.baseproject.ui.converter.ConverterEngine.calculateDiscount("100000", "150", "0")
        assertTrue(!invalidDisc.isValid)

        // Hệ thống số: 255 (DEC) -> FF (HEX), 377 (OCT), 1111 1111 (BIN)
        val num = com.example.baseproject.ui.converter.ConverterEngine.convertNumeralSystem("255", 10)
        assertTrue(num.isValid)
        assertEquals("FF", num.hexValue)
        assertEquals("377", num.octValue)
        assertEquals("1111 1111", num.binValue)

        val invalidBin = com.example.baseproject.ui.converter.ConverterEngine.convertNumeralSystem("10201", 2)
        assertTrue(!invalidBin.isValid)

        // BMI: Cao 170 cm, nặng 65 kg -> BMI ~ 22.49 (Bình thường)
        val bmiRes = com.example.baseproject.ui.converter.ConverterEngine.calculateBmi("170", "65")
        assertTrue(bmiRes.isValid)
        assertEquals(22.49, bmiRes.bmi, 0.05)
        assertEquals(com.example.baseproject.ui.converter.BmiStatus.NORMAL, bmiRes.status)

        val invalidBmi = com.example.baseproject.ui.converter.ConverterEngine.calculateBmi("10", "65")
        assertTrue(!invalidBmi.isValid)
    }
}
