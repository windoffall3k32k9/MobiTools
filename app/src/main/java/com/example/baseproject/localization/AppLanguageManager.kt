package com.example.baseproject.localization

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.baseproject.R
import java.util.Locale

/**
 * Quản lý ngôn ngữ ứng dụng độc lập hoặc đồng bộ theo ngôn ngữ hệ thống của thiết bị.
 */
object AppLanguageManager {

    enum class Mode(val persistedValue: String) {
        SYSTEM("system"),
        VIETNAMESE("vi"),
        ENGLISH("en")
    }

    // Đọc chế độ ngôn ngữ đã lưu trong cấu hình SharedPreferences.
    fun currentMode(context: Context): Mode {
        val savedValue = preferences(context).getString(KEY_LANGUAGE_MODE, Mode.SYSTEM.persistedValue)
        return Mode.entries.firstOrNull { it.persistedValue == savedValue } ?: Mode.SYSTEM
    }

    // Trả về resource ID nhãn hiển thị tương ứng với chế độ ngôn ngữ hiện tại.
    @StringRes
    fun currentMenuLabel(context: Context): Int = when (currentMode(context)) {
        Mode.SYSTEM -> R.string.language_system
        Mode.VIETNAMESE -> R.string.language_vietnamese
        Mode.ENGLISH -> R.string.language_english
    }

    // Bọc context với Locale đã lưu để các Activity luôn tải đúng resource đa ngôn ngữ.
    fun wrapContext(context: Context): Context {
        val mode = currentMode(context)
        val tag = languageTagsFor(mode) ?: return context
        val targetLocale = Locale.forLanguageTag(tag)
        Locale.setDefault(targetLocale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)
        return context.createConfigurationContext(config)
    }

    // Lưu chế độ ngôn ngữ mới và áp dụng ngay lập tức cho toàn bộ ứng dụng.
    fun setMode(context: Context, mode: Mode) {
        preferences(context)
            .edit()
            .putString(KEY_LANGUAGE_MODE, mode.persistedValue)
            .apply()
        applyMode(context, mode)
    }

    // Áp dụng lại chế độ ngôn ngữ đã lưu khi khởi động ứng dụng hoặc khi cấu hình hệ thống thay đổi.
    fun applySavedMode(context: Context) {
        applyMode(context, currentMode(context))
    }

    // Trả về language tag chuẩn ISO tương ứng với từng chế độ.
    internal fun languageTagsFor(mode: Mode): String? = when (mode) {
        Mode.SYSTEM -> null
        Mode.VIETNAMESE -> Mode.VIETNAMESE.persistedValue
        Mode.ENGLISH -> Mode.ENGLISH.persistedValue
    }

    // Áp dụng Locale cho hệ điều hành thông qua platform LocaleManager hoặc AppCompatDelegate.
    private fun applyMode(context: Context, mode: Mode) {
        val languageTags = languageTagsFor(mode).orEmpty()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = LocaleList.forLanguageTags(languageTags)
            }
        } else {
            val locales = if (languageTags.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(languageTags)
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private const val PREFERENCES_NAME = "mobitools_preferences"
    private const val KEY_LANGUAGE_MODE = "language_mode"
}
