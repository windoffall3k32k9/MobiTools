package com.example.baseproject.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService

object StringEx {
    fun String.copyToClipboard(context : Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clip = ClipData.newPlainText("copied_text", this)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Copped", Toast.LENGTH_SHORT).show()
    }

}