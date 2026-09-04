package com.example.baseproject.utils

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import java.util.Locale

object LocaleHelper {

    fun onAttach(context: Context?, defaultLanguage: String): Context? {
        return setLocale(context, defaultLanguage)
    }



    fun setLocale(context: Context?, language: String): Context? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, language)
        } else updateResourcesLegacy(context, language)

    }


    @TargetApi(Build.VERSION_CODES.N)
    private fun updateResources(context: Context?, language: String): Context? {
        val locale = createLocale(language)
        Locale.setDefault(locale)
        val configuration = context?.resources?.configuration
        configuration?.setLocale(locale)
        configuration?.setLayoutDirection(locale)
        return configuration?.let { context.createConfigurationContext(it) }
    }

    private fun updateResourcesLegacy(context: Context?, language: String): Context? {
        val locale = createLocale(language)
        Locale.setDefault(locale)
        val resources = context?.resources
        val configuration = resources?.configuration
        configuration?.locale = locale
        configuration?.setLayoutDirection(locale)
        resources?.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }

    private fun createLocale(language: String): Locale {
        return Locale.forLanguageTag(language.ifBlank { "en" }.replace("_", "-"))
    }
}
