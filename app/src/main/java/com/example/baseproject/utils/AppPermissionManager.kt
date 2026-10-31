package com.example.baseproject.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Mục thông tin mô tả chi tiết từng quyền ứng dụng để hiển thị trong màn hình Quyền.
 */
data class PermissionItem(
    val title: String,
    val description: String,
    val permissionKey: String,
    val isGranted: Boolean
)

/**
 * Quản lý tập trung toàn bộ quyền cần thiết của ứng dụng MobiTools.
 */
object AppPermissionManager {

    // Danh sách toàn bộ các quyền mà MobiTools yêu cầu.
    fun getAllRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.toTypedArray()
    }

    // Kiểm tra các quyền còn thiếu chưa được người dùng cấp.
    fun getMissingPermissions(context: Context): List<String> {
        return getAllRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    // Kiểm tra xem đã cấp đủ tất cả các quyền cần thiết chưa.
    fun hasAllPermissions(context: Context): Boolean {
        return getMissingPermissions(context).isEmpty()
    }

    // Lấy danh sách trạng thái chi tiết của từng quyền phục vụ giao diện cài đặt.
    fun getPermissionDisplayList(context: Context): List<PermissionItem> {
        val list = mutableListOf(
            PermissionItem(
                title = "Máy ảnh (Camera)",
                description = "Dùng cho công cụ quét mã vạch, mã QR và điều khiển đèn pin",
                permissionKey = Manifest.permission.CAMERA,
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            ),
            PermissionItem(
                title = "Ghi âm (Microphone)",
                description = "Dùng cho công cụ đo độ ồn âm thanh tương đối qua micro",
                permissionKey = Manifest.permission.RECORD_AUDIO,
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            ),
            PermissionItem(
                title = "Vị trí địa lý (Location)",
                description = "Dùng để xác định tọa độ và địa chỉ kinh độ/vĩ độ trên la bàn số",
                permissionKey = Manifest.permission.ACCESS_FINE_LOCATION,
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionItem(
                    title = "Thông báo (Notifications)",
                    description = "Dùng để phát thông báo chuông báo khi đồng hồ đếm ngược kết thúc",
                    permissionKey = Manifest.permission.POST_NOTIFICATIONS,
                    isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                )
            )
        }

        return list
    }
}
