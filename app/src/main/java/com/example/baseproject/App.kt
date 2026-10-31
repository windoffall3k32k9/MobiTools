package com.example.baseproject

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.example.baseproject.localization.AppLanguageManager
import dagger.hilt.android.HiltAndroidApp

/**
 * Lớp ứng dụng toàn cục quản lý vòng đời ứng dụng và đa ngôn ngữ.
 */
@HiltAndroidApp
class App : Application() {

    // Bọc context khởi tạo với cấu hình Locale đã lưu của ứng dụng.
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguageManager.wrapContext(base))
    }

    // Khởi tạo Singleton toàn cục và áp dụng cấu hình ngôn ngữ khi ứng dụng khởi chạy.
    override fun onCreate() {
        super.onCreate()
        instanceRef = this
        AppLanguageManager.applySavedMode(this)
    }

    // Đảm bảo ngôn ngữ được giữ vững hoặc cập nhật chính xác khi cấu hình thiết bị thay đổi.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppLanguageManager.applySavedMode(this)
    }

    companion object {
        @Volatile
        private var instanceRef: App? = null

        /** Tham chiếu Singleton của App sau khi khởi tạo. */
        val instance: App
            get() = checkNotNull(instanceRef) { "App chưa được khởi tạo." }

        /** Context ứng dụng an toàn cho toàn tiến trình. */
        val context: Context
            get() = instance.applicationContext
    }
}
