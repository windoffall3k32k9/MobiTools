package com.example.baseproject.ui.texttools

import java.util.Locale

/**
 * Các chế độ biến đổi định dạng văn bản được hỗ trợ.
 */
enum class TextTransformMode {
    UPPERCASE,
    LOWERCASE,
    TITLE_CASE,
    SENTENCE_CASE,
    TRIM_SPACES,
    REMOVE_LINES
}

/**
 * Động cơ xử lý và chuyển đổi cấu trúc định dạng văn bản.
 */
object TextTransformEngine {

    // Chuyển toàn bộ văn bản sang chữ in hoa.
    fun toUpperCase(text: String, locale: Locale = Locale.getDefault()): String {
        return text.uppercase(locale)
    }

    // Chuyển toàn bộ văn bản sang chữ thường.
    fun toLowerCase(text: String, locale: Locale = Locale.getDefault()): String {
        return text.lowercase(locale)
    }

    // Viết hoa chữ cái đầu tiên của từng từ và giữ nguyên các dấu phân tách.
    fun toTitleCase(text: String, locale: Locale = Locale.getDefault()): String {
        if (text.isEmpty()) return ""
        val sb = StringBuilder(text.length)
        var capitalizeNext = true
        for (char in text) {
            if (char.isLetter()) {
                if (capitalizeNext) {
                    sb.append(char.titlecase(locale))
                    capitalizeNext = false
                } else {
                    sb.append(char.lowercaseChar())
                }
            } else {
                sb.append(char)
                capitalizeNext = char.isWhitespace()
            }
        }
        return sb.toString()
    }

    // Viết hoa chữ cái đầu tiên ở đầu đoạn hoặc sau các dấu ngắt câu (chấm, hỏi, than, xuống dòng).
    fun toSentenceCase(text: String, locale: Locale = Locale.getDefault()): String {
        if (text.isEmpty()) return ""
        val sb = StringBuilder(text.length)
        var capitalizeNext = true
        for (char in text) {
            if (char.isLetter()) {
                if (capitalizeNext) {
                    sb.append(char.titlecase(locale))
                    capitalizeNext = false
                } else {
                    sb.append(char.lowercaseChar())
                }
            } else {
                sb.append(char)
                if (char == '.' || char == '!' || char == '?' || char == '\n') {
                    capitalizeNext = true
                }
            }
        }
        return sb.toString()
    }

    // Xóa khoảng trắng thừa ở từng dòng và thu gọn nhiều khoảng trắng liên tiếp.
    fun trimSpaces(text: String): String {
        if (text.isEmpty()) return ""
        return text.lines().joinToString("\n") { line ->
            line.trim().replace(Regex("[ \\t]+"), " ")
        }.trim()
    }

    // Loại bỏ toàn bộ các ký tự ngắt dòng và gom thành một đoạn văn bản liền mạch.
    fun removeLineBreaks(text: String): String {
        if (text.isEmpty()) return ""
        return text.replace(Regex("\\r?\\n+"), " ")
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }

    // Đếm tổng số từ hợp lệ trong chuỗi văn bản.
    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    // Đếm tổng số dòng văn bản.
    fun countLines(text: String): Int {
        if (text.isEmpty()) return 0
        return text.lines().size
    }

    // Thực hiện biến đổi chuỗi văn bản theo chế độ được chỉ định.
    fun transform(text: String, mode: TextTransformMode, locale: Locale = Locale.getDefault()): String {
        return when (mode) {
            TextTransformMode.UPPERCASE -> toUpperCase(text, locale)
            TextTransformMode.LOWERCASE -> toLowerCase(text, locale)
            TextTransformMode.TITLE_CASE -> toTitleCase(text, locale)
            TextTransformMode.SENTENCE_CASE -> toSentenceCase(text, locale)
            TextTransformMode.TRIM_SPACES -> trimSpaces(text)
            TextTransformMode.REMOVE_LINES -> removeLineBreaks(text)
        }
    }
}
