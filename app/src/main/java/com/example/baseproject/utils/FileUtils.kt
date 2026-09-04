package com.example.baseproject.utils

import android.content.Context
import java.io.File

class FileUtils {
    companion object {
        fun readStringFromAsset(context: Context, fileName: String): String {
            return try {
                context.assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }

        fun saveStringAsFile(text: String, path: String) {
            val file = File(path)
            file.parentFile?.let {
                if (!it.exists()) it.mkdirs()
            }
            file.writeText(text, Charsets.UTF_8)
        }

        fun readStringFromFile(path: String): String? {
            val file = File(path)
            if (!file.exists()) return null
            return file.readText(Charsets.UTF_8)
        }
    }
}