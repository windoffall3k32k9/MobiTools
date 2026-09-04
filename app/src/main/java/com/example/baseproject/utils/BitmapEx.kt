package com.example.baseproject.utils

import android.content.Context
import android.graphics.Bitmap

object BitmapEx{
    fun Bitmap.saveToFilePng(path : String){
        val outputStream = java.io.FileOutputStream(path)
        compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    }
    fun Bitmap.saveToFileJpg(path : String){
        val outputStream = java.io.FileOutputStream(path)
        compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    }
}