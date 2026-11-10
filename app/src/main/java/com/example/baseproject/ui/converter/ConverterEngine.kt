package com.example.baseproject.ui.converter

import com.example.baseproject.R
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Danh sách 12 chức năng chuyển đổi và tính toán chuyên dụng theo thứ tự lưới 3x4.
 */
enum class ConverterCategory(
    val id: String,
    val titleResId: Int,
    val iconResId: Int,
    val subtitle: String
) {
    CURRENCY("currency", R.string.conv_cat_currency, R.drawable.ic_conv_currency, "Quy đổi tỷ giá tiền tệ quốc tế"),
    LENGTH("length", R.string.conv_cat_length, R.drawable.ic_conv_length, "Chuyển đổi đơn vị đo chiều dài"),
    WEIGHT("weight", R.string.conv_cat_weight, R.drawable.ic_conv_weight, "Chuyển đổi khối lượng và cân nặng"),
    AREA("area", R.string.conv_cat_area, R.drawable.ic_conv_area, "Chuyển đổi diện tích đất và bề mặt"),
    TIME("time", R.string.conv_cat_time, R.drawable.ic_conv_time, "Chuyển đổi năm, tháng, ngày, giờ, giây"),
    DATA("data", R.string.conv_cat_data, R.drawable.ic_conv_data, "Chuyển đổi dung lượng lưu trữ số"),
    DISCOUNT("discount", R.string.conv_cat_discount, R.drawable.ic_conv_discount, "Tính giá sau giảm và số tiền tiết kiệm"),
    VOLUME("volume", R.string.conv_cat_volume, R.drawable.ic_conv_volume, "Chuyển đổi thể tích chất lỏng và khối"),
    NUMERAL_SYSTEM("numeral", R.string.conv_cat_numeral, R.drawable.ic_conv_numeral, "Chuyển đổi Nhị phân, Bát phân, Thập phân, Hex"),
    SPEED("speed", R.string.conv_cat_speed, R.drawable.ic_conv_speed, "Chuyển đổi vận tốc chuyển động"),
    TEMPERATURE("temperature", R.string.conv_cat_temp, R.drawable.ic_conv_temp, "Quy đổi thang đo Độ C, Độ F, Kelvin"),
    BMI("bmi", R.string.conv_cat_bmi, R.drawable.ic_conv_bmi, "Đánh giá chỉ số khối cơ thể và cân nặng chuẩn")
}

/**
 * Mô tả thông tin một đơn vị đo lường kèm hệ số quy đổi về đơn vị cơ sở.
 */
data class UnitOption(
    val code: String,
    val name: String,
    val symbol: String,
    val factorToBase: Double
) {
    // Trả về nhãn hiển thị đầy đủ gồm tên đơn vị và ký hiệu viết tắt.
    fun displayLabel(): String = "$name ($symbol)"
}

/**
 * Kết quả tính toán giảm giá thương mại kèm trạng thái hợp lệ.
 */
data class DiscountCalculationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val originalPrice: Double = 0.0,
    val finalPrice: Double = 0.0,
    val savedAmount: Double = 0.0,
    val effectiveDiscountPercent: Double = 0.0
)

/**
 * Kết quả chuyển đổi giữa 4 hệ cơ số: Thập phân, Thập lục phân, Bát phân và Nhị phân.
 */
data class NumeralConversionResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val decValue: String = "0",
    val hexValue: String = "0",
    val octValue: String = "0",
    val binValue: String = "0"
)

/**
 * Phân loại thể trạng sức khỏe theo chỉ số BMI.
 */
enum class BmiStatus(
    val label: String,
    val advice: String,
    val colorHex: String
) {
    UNDERWEIGHT("Thiếu cân", "Bạn nên bổ sung dinh dưỡng và tập luyện để tăng cân lành mạnh.", "#0284C7"),
    NORMAL("Bình thường", "Cơ thể cân đối! Hãy duy trì chế độ ăn uống và vận động hiện tại.", "#059669"),
    OVERWEIGHT("Thừa cân", "Nên kết hợp vận động đều đặn và điều chỉnh khẩu phần ăn hợp lý.", "#D97706"),
    OBESE("Béo phì", "Khuyến nghị giảm cân khoa học và tham khảo ý kiến chuyên gia y tế.", "#DC2626")
}

/**
 * Kết quả phân tích chỉ số khối cơ thể BMI.
 */
data class BmiCalculationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val bmi: Double = 0.0,
    val status: BmiStatus = BmiStatus.NORMAL,
    val minIdealWeight: Double = 0.0,
    val maxIdealWeight: Double = 0.0,
    val weightDifference: Double = 0.0,
    val gaugeFraction: Float = 0.5f
)

/**
 * Động cơ tính toán và xác thực dữ liệu trung tâm cho toàn bộ 12 công cụ Chuyển đổi.
 */
object ConverterEngine {

    private val formatSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val standardFormat = DecimalFormat("#,##0.######", formatSymbols)
    private val currencyFormat = DecimalFormat("#,##0.##", formatSymbols)
    private val integerFormat = DecimalFormat("#,##0", formatSymbols)
    private val oneDecimalFormat = DecimalFormat("#,##0.0", formatSymbols)

    // Danh sách tiền tệ quốc tế với hệ số quy đổi mặc định sang VND (1 đơn vị ngoại tệ = X VND)
    val currencyUnits = listOf(
        UnitOption("USD", "Đô la Mỹ", "USD", 25450.0),
        UnitOption("VND", "Đồng Việt Nam", "VND", 1.0),
        UnitOption("EUR", "Đồng Euro", "EUR", 27650.0),
        UnitOption("JPY", "Yên Nhật", "JPY", 168.5),
        UnitOption("GBP", "Bảng Anh", "GBP", 32400.0),
        UnitOption("CNY", "Nhân dân tệ", "CNY", 3520.0),
        UnitOption("KRW", "Won Hàn Quốc", "KRW", 18.4),
        UnitOption("SGD", "Đô la Singapore", "SGD", 18950.0),
        UnitOption("THB", "Baht Thái", "THB", 745.0),
        UnitOption("AUD", "Đô la Úc", "AUD", 16750.0),
        UnitOption("CAD", "Đô la Canada", "CAD", 18300.0),
        UnitOption("TWD", "Đô la Đài Loan", "TWD", 790.0)
    )

    // Danh sách đơn vị Độ dài (Cơ sở: Mét - m)
    val lengthUnits = listOf(
        UnitOption("km", "Kilômét", "km", 1000.0),
        UnitOption("m", "Mét", "m", 1.0),
        UnitOption("dm", "Đềximét", "dm", 0.1),
        UnitOption("cm", "Centimét", "cm", 0.01),
        UnitOption("mm", "Milimét", "mm", 0.001),
        UnitOption("um", "Micrômét", "µm", 1e-6),
        UnitOption("nm", "Nanômét", "nm", 1e-9),
        UnitOption("mi", "Dặm", "mi", 1609.344),
        UnitOption("yd", "Yard", "yd", 0.9144),
        UnitOption("ft", "Foot", "ft", 0.3048),
        UnitOption("in", "Inch", "in", 0.0254),
        UnitOption("nmi", "Hải lý", "nmi", 1852.0)
    )

    // Danh sách đơn vị Trọng lượng (Cơ sở: Kilôgam - kg)
    val weightUnits = listOf(
        UnitOption("t", "Tấn", "t", 1000.0),
        UnitOption("ta", "Tạ", "tạ", 100.0),
        UnitOption("yen", "Yến", "yến", 10.0),
        UnitOption("kg", "Kilôgam", "kg", 1.0),
        UnitOption("g", "Gam", "g", 0.001),
        UnitOption("mg", "Miligam", "mg", 1e-6),
        UnitOption("lb", "Pound", "lb", 0.45359237),
        UnitOption("oz", "Ounce", "oz", 0.028349523125),
        UnitOption("luong", "Lạng / Cây (VN)", "lượng", 0.0375),
        UnitOption("chi", "Chỉ (VN)", "chỉ", 0.00375),
        UnitOption("ct", "Carat", "ct", 0.0002)
    )

    // Danh sách đơn vị Diện tích (Cơ sở: Mét vuông - m²)
    val areaUnits = listOf(
        UnitOption("km2", "Kilômét vuông", "km²", 1e6),
        UnitOption("ha", "Hecta", "ha", 10000.0),
        UnitOption("m2", "Mét vuông", "m²", 1.0),
        UnitOption("dm2", "Đềximét vuông", "dm²", 0.01),
        UnitOption("cm2", "Centimét vuông", "cm²", 0.0001),
        UnitOption("mm2", "Milimét vuông", "mm²", 1e-6),
        UnitOption("acre", "Mẫu Anh", "acre", 4046.8564224),
        UnitOption("mi2", "Dặm vuông", "mi²", 2589988.110336),
        UnitOption("yd2", "Yard vuông", "yd²", 0.83612736),
        UnitOption("ft2", "Foot vuông", "ft²", 0.09290304),
        UnitOption("in2", "Inch vuông", "in²", 0.00064516)
    )

    // Danh sách đơn vị Thời gian (Cơ sở: Giây - s)
    val timeUnits = listOf(
        UnitOption("yr", "Năm (365 ngày)", "năm", 31536000.0),
        UnitOption("mo", "Tháng (30 ngày)", "tháng", 2592000.0),
        UnitOption("wk", "Tuần", "tuần", 604800.0),
        UnitOption("d", "Ngày", "ngày", 86400.0),
        UnitOption("h", "Giờ", "h", 3600.0),
        UnitOption("min", "Phút", "phút", 60.0),
        UnitOption("s", "Giây", "s", 1.0),
        UnitOption("ms", "Miligiây", "ms", 0.001),
        UnitOption("us", "Microgiây", "µs", 1e-6)
    )

    // Danh sách đơn vị Dữ liệu số (Cơ sở: Byte - B)
    val dataUnits = listOf(
        UnitOption("bit", "Bit", "b", 0.125),
        UnitOption("B", "Byte", "B", 1.0),
        UnitOption("KB", "Kilobyte", "KB", 1024.0),
        UnitOption("MB", "Megabyte", "MB", 1048576.0),
        UnitOption("GB", "Gigabyte", "GB", 1073741824.0),
        UnitOption("TB", "Terabyte", "TB", 1099511627776.0),
        UnitOption("PB", "Petabyte", "PB", 1125899906842624.0)
    )

    // Danh sách đơn vị Thể tích (Cơ sở: Lít - L)
    val volumeUnits = listOf(
        UnitOption("m3", "Mét khối", "m³", 1000.0),
        UnitOption("L", "Lít / Đềximét khối", "L", 1.0),
        UnitOption("mL", "Mililít / Centimét khối", "mL", 0.001),
        UnitOption("mm3", "Milimét khối", "mm³", 1e-6),
        UnitOption("gal", "Gallon Mỹ", "gal", 3.785411784),
        UnitOption("qt", "Quart Mỹ", "qt", 0.946352946),
        UnitOption("pt", "Pint Mỹ", "pt", 0.473176473),
        UnitOption("cup", "Cup Mỹ", "cup", 0.2365882365),
        UnitOption("floz", "Ounce lỏng Mỹ", "fl oz", 0.02957352956),
        UnitOption("ft3", "Foot khối", "ft³", 28.316846592),
        UnitOption("in3", "Inch khối", "in³", 0.016387064)
    )

    // Danh sách đơn vị Tốc độ (Cơ sở: Mét/giây - m/s)
    val speedUnits = listOf(
        UnitOption("kmh", "Kilômét/giờ", "km/h", 1.0 / 3.6),
        UnitOption("ms", "Mét/giây", "m/s", 1.0),
        UnitOption("mph", "Dặm/giờ", "mph", 0.44704),
        UnitOption("knot", "Hải lý/giờ (Knot)", "kn", 0.5144444444),
        UnitOption("fts", "Foot/giây", "ft/s", 0.3048),
        UnitOption("mach", "Mach (Âm thanh)", "Mach", 340.3),
        UnitOption("light", "Tốc độ ánh sáng", "c", 299792458.0)
    )

    // Danh sách đơn vị Nhiệt độ
    val temperatureUnits = listOf(
        UnitOption("C", "Độ C (Celsius)", "°C", 1.0),
        UnitOption("F", "Độ F (Fahrenheit)", "°F", 1.0),
        UnitOption("K", "Kelvin", "K", 1.0),
        UnitOption("R", "Rankine", "°R", 1.0),
        UnitOption("Re", "Réaumur", "°Ré", 1.0)
    )

    // Trả về danh sách các đơn vị tương ứng với danh mục chuyển đổi.
    fun getUnitsForCategory(category: ConverterCategory): List<UnitOption> {
        return when (category) {
            ConverterCategory.CURRENCY -> currencyUnits
            ConverterCategory.LENGTH -> lengthUnits
            ConverterCategory.WEIGHT -> weightUnits
            ConverterCategory.AREA -> areaUnits
            ConverterCategory.TIME -> timeUnits
            ConverterCategory.DATA -> dataUnits
            ConverterCategory.VOLUME -> volumeUnits
            ConverterCategory.SPEED -> speedUnits
            ConverterCategory.TEMPERATURE -> temperatureUnits
            else -> emptyList()
        }
    }

    // Chuẩn hóa chuỗi số người dùng nhập (hỗ trợ cả dấu phẩy và dấu chấm thập phân) sang số thực Double.
    fun parseNormalizedDouble(rawInput: String?): Double? {
        if (rawInput.isNullOrBlank()) return null
        val cleaned = rawInput.trim().replace(" ", "")
        val normalized = if (cleaned.contains(",") && cleaned.contains(".")) {
            cleaned.replace(".", "").replace(",", ".")
        } else {
            cleaned.replace(",", ".")
        }
        val parsed = normalized.toDoubleOrNull() ?: return null
        return if (parsed.isNaN() || parsed.isInfinite()) null else parsed
    }

    // Thực hiện quy đổi giữa hai đơn vị tuyến tính hoặc tiền tệ theo hệ số cơ sở.
    fun convertLinear(
        value: Double,
        fromUnit: UnitOption,
        toUnit: UnitOption,
        customRateFromTo: Double? = null
    ): Double {
        if (fromUnit.code == toUnit.code) return value
        if (customRateFromTo != null && customRateFromTo > 0.0) {
            return value * customRateFromTo
        }
        val inBase = value * fromUnit.factorToBase
        return inBase / toUnit.factorToBase
    }

    // Tính tỷ giá hoặc hệ số quy đổi của 1 đơn vị nguồn sang đơn vị đích.
    fun getUnitRatio(
        category: ConverterCategory,
        fromUnit: UnitOption,
        toUnit: UnitOption,
        customRateFromTo: Double? = null
    ): Double {
        if (category == ConverterCategory.TEMPERATURE) {
            return convertTemperature(1.0, fromUnit.code, toUnit.code)
        }
        return convertLinear(1.0, fromUnit, toUnit, customRateFromTo)
    }

    // Thực hiện chuyển đổi nhiệt độ giữa các thang đo C, F, K, Rankine và Réaumur.
    fun convertTemperature(value: Double, fromCode: String, toCode: String): Double {
        if (fromCode == toCode) return value
        val celsius = when (fromCode) {
            "C" -> value
            "F" -> (value - 32.0) * 5.0 / 9.0
            "K" -> value - 273.15
            "R" -> (value - 491.67) * 5.0 / 9.0
            "Re" -> value * 1.25
            else -> value
        }
        return when (toCode) {
            "C" -> celsius
            "F" -> (celsius * 9.0 / 5.0) + 32.0
            "K" -> celsius + 273.15
            "R" -> (celsius + 273.15) * 9.0 / 5.0
            "Re" -> celsius * 0.8
            else -> celsius
        }
    }

    // Kiểm tra giá trị nhiệt độ có vượt dưới độ không tuyệt đối (-273.15 °C) hay không.
    fun isBelowAbsoluteZero(value: Double, unitCode: String): Boolean {
        val kelvin = convertTemperature(value, unitCode, "K")
        return kelvin < -1e-6
    }

    // Tính toán giá sau giảm, số tiền tiết kiệm và tổng phần trăm giảm giá kèm kiểm tra hợp lệ.
    fun calculateDiscount(
        originalPriceInput: String?,
        discountPercentInput: String?,
        extraDiscountPercentInput: String? = null
    ): DiscountCalculationResult {
        if (originalPriceInput.isNullOrBlank()) {
            return DiscountCalculationResult(isValid = true)
        }
        val originalPrice = parseNormalizedDouble(originalPriceInput)
            ?: return DiscountCalculationResult(false, "Giá gốc không hợp lệ")

        if (originalPrice < 0.0) {
            return DiscountCalculationResult(false, "Giá gốc không được nhỏ hơn 0")
        }
        if (originalPrice > 1e15) {
            return DiscountCalculationResult(false, "Giá gốc vượt quá giới hạn cho phép")
        }

        val discount1 = if (discountPercentInput.isNullOrBlank()) 0.0 else {
            parseNormalizedDouble(discountPercentInput)
                ?: return DiscountCalculationResult(false, "Phần trăm giảm giá không hợp lệ")
        }
        if (discount1 < 0.0 || discount1 > 100.0) {
            return DiscountCalculationResult(false, "Mức giảm giá phải từ 0% đến 100%")
        }

        val discount2 = if (extraDiscountPercentInput.isNullOrBlank()) 0.0 else {
            parseNormalizedDouble(extraDiscountPercentInput)
                ?: return DiscountCalculationResult(false, "Phần trăm giảm thêm không hợp lệ")
        }
        if (discount2 < 0.0 || discount2 > 100.0) {
            return DiscountCalculationResult(false, "Mức giảm thêm phải từ 0% đến 100%")
        }

        val afterFirst = originalPrice * (1.0 - discount1 / 100.0)
        val finalPrice = afterFirst * (1.0 - discount2 / 100.0)
        val savedAmount = (originalPrice - finalPrice).coerceAtLeast(0.0)
        val effectivePercent = if (originalPrice > 0.0) (savedAmount / originalPrice) * 100.0 else 0.0

        return DiscountCalculationResult(
            isValid = true,
            originalPrice = originalPrice,
            finalPrice = finalPrice,
            savedAmount = savedAmount,
            effectiveDiscountPercent = effectivePercent
        )
    }

    // Chuyển đổi qua lại giữa 4 hệ thống số: BIN (2), OCT (8), DEC (10), HEX (16) kèm kiểm tra ký tự.
    fun convertNumeralSystem(rawInput: String?, radix: Int): NumeralConversionResult {
        val cleaned = rawInput?.trim()?.replace(" ", "")?.uppercase(Locale.ROOT) ?: ""
        if (cleaned.isEmpty()) {
            return NumeralConversionResult(isValid = true)
        }

        val validRegex = when (radix) {
            2 -> Regex("^[01]+$")
            8 -> Regex("^[0-7]+$")
            10 -> Regex("^[0-9]+$")
            16 -> Regex("^[0-9A-F]+$")
            else -> return NumeralConversionResult(false, "Hệ cơ số không được hỗ trợ")
        }

        if (!validRegex.matches(cleaned)) {
            val msg = when (radix) {
                2 -> "Hệ nhị phân (BIN) chỉ chấp nhận chữ số 0 và 1"
                8 -> "Hệ bát phân (OCT) chỉ chấp nhận chữ số từ 0 đến 7"
                10 -> "Hệ thập phân (DEC) chỉ chấp nhận chữ số từ 0 đến 9"
                16 -> "Hệ thập lục phân (HEX) chỉ chấp nhận 0-9 và A-F"
                else -> "Dữ liệu không hợp lệ"
            }
            return NumeralConversionResult(false, msg)
        }

        return try {
            val bigInt = BigInteger(cleaned, radix)
            // Giới hạn tối đa 64-bit unsigned để hiển thị gọn gàng, chính xác
            val maxAllowed = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)
            if (bigInt > maxAllowed || bigInt < BigInteger.ZERO) {
                return NumeralConversionResult(false, "Giá trị vượt quá giới hạn 64-bit")
            }

            val decStr = bigInt.toString(10)
            val hexStr = bigInt.toString(16).uppercase(Locale.ROOT)
            val octStr = bigInt.toString(8)
            val rawBin = bigInt.toString(2)
            val groupedBin = formatBinaryNibbles(rawBin)

            NumeralConversionResult(
                isValid = true,
                decValue = decStr,
                hexValue = hexStr,
                octValue = octStr,
                binValue = groupedBin
            )
        } catch (e: Exception) {
            NumeralConversionResult(false, "Giá trị số không hợp lệ")
        }
    }

    // Nhóm chuỗi nhị phân thành từng cụm 4 bit giúp dễ đọc.
    fun formatBinaryNibbles(binary: String): String {
        if (binary.length <= 4) return binary
        val paddedLength = ((binary.length + 3) / 4) * 4
        val padded = binary.padStart(paddedLength, '0')
        return padded.chunked(4).joinToString(" ")
    }

    // Tính chỉ số khối cơ thể BMI, phân loại thể trạng và xác định khoảng cân nặng lý tưởng.
    fun calculateBmi(heightCmInput: String?, weightKgInput: String?): BmiCalculationResult {
        if (heightCmInput.isNullOrBlank() || weightKgInput.isNullOrBlank()) {
            return BmiCalculationResult(false, "Vui lòng nhập đầy đủ chiều cao và cân nặng")
        }

        val heightCm = parseNormalizedDouble(heightCmInput)
            ?: return BmiCalculationResult(false, "Chiều cao không hợp lệ")
        val weightKg = parseNormalizedDouble(weightKgInput)
            ?: return BmiCalculationResult(false, "Cân nặng không hợp lệ")

        if (heightCm < 40.0 || heightCm > 260.0) {
            return BmiCalculationResult(false, "Chiều cao hợp lệ nằm trong khoảng 40 cm – 260 cm")
        }
        if (weightKg < 2.0 || weightKg > 400.0) {
            return BmiCalculationResult(false, "Cân nặng hợp lệ nằm trong khoảng 2 kg – 400 kg")
        }

        val heightM = heightCm / 100.0
        val bmi = weightKg / (heightM * heightM)

        val status = when {
            bmi < 18.5 -> BmiStatus.UNDERWEIGHT
            bmi < 25.0 -> BmiStatus.NORMAL
            bmi < 30.0 -> BmiStatus.OVERWEIGHT
            else -> BmiStatus.OBESE
        }

        val minIdeal = 18.5 * heightM * heightM
        val maxIdeal = 24.9 * heightM * heightM

        val diff = when {
            weightKg < minIdeal -> weightKg - minIdeal // âm: cần tăng
            weightKg > maxIdeal -> weightKg - maxIdeal // dương: cần giảm
            else -> 0.0
        }

        // Chuẩn hóa vị trí thanh đo từ BMI 12 -> 38 sang dải [0f..1f]
        val fraction = ((bmi - 12.0) / (38.0 - 12.0)).toFloat().coerceIn(0.02f, 0.98f)

        return BmiCalculationResult(
            isValid = true,
            bmi = bmi,
            status = status,
            minIdealWeight = minIdeal,
            maxIdealWeight = maxIdeal,
            weightDifference = diff,
            gaugeFraction = fraction
        )
    }

    // Định dạng số thực sang chuỗi hiển thị thân thiện chuẩn Việt Nam.
    fun formatNumber(value: Double, isCurrency: Boolean = false): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val absVal = abs(value)
        if (absVal == 0.0) return "0"
        if (absVal >= 1e12 || (absVal < 1e-5 && !isCurrency)) {
            val sci = DecimalFormat("0.####E0", formatSymbols)
            return sci.format(value).replace("E", " × 10^")
        }
        return if (isCurrency && absVal >= 100.0) {
            currencyFormat.format(value)
        } else {
            standardFormat.format(value)
        }
    }

    // Định dạng số nguyên hoặc tiền tệ lớn không lấy phần lẻ thập phân.
    fun formatIntegerMoney(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        return integerFormat.format(value)
    }

    // Định dạng số thực với 1 chữ số thập phân (dùng cho BMI và % giảm giá).
    fun formatOneDecimal(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0,0"
        return oneDecimalFormat.format(value)
    }
}
