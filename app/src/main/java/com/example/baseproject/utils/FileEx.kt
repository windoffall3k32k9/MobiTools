package com.example.baseproject.utils

import java.io.File

object FileEx {

    fun File.deleteRecursively(): Boolean {
        if (isDirectory) {
            listFiles()?.forEach { f ->
                f.deleteRecursively()
            }
        }
        return delete()
    }
}