package com.example.baseproject.model

import android.content.Context
import androidx.fragment.app.Fragment

/**
 * Mô tả siêu dữ liệu đầy đủ của một công cụ trong hệ thống MobiTools.
 */
data class ToolDescriptor(
    val id: String,
    val titleResId: Int,
    val descriptionResId: Int,
    val iconResId: Int,
    val group: ToolGroup,
    val requiredPermissions: List<String> = emptyList(),
    val isHardwareSupported: (Context) -> Boolean = { true },
    val unsupportedMessageResId: Int? = null,
    val createFragment: () -> Fragment
)
