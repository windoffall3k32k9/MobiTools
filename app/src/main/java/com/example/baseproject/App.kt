package com.example.baseproject

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import com.example.baseproject.utils.AppTheme
import com.example.baseproject.utils.DataStatic
import com.example.baseproject.utils.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class App : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    companion object {
        var instance: App? = null
        private set
    }
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super<Application>.onCreate()
        instance = this
        //DataStatic.context = applicationContext
        //AppTheme.applySaveMode()
    }

    override fun attachBaseContext(base: Context?) {
        //val context = LocaleHelper.onAttach(base, Locale.getDefault().language)
        super.attachBaseContext(base)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityStopped(activity: Activity) {
    }


}