package com.seoulhankuko.app.core

import android.app.Application
import com.seoulhankuko.app.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SeoulHankukoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In production, you might want to plant a custom tree for crash reporting
            Timber.plant(ReleaseTree())
        }
        
        Logger.appStarted()
    }
}

// Custom tree for release builds
class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // In production, you might want to send logs to crash reporting service
        // For now, we'll just use the default behavior
        super.log(priority, tag, message, t)
    }
}
