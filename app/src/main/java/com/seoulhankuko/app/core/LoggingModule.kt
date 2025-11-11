package com.seoulhankuko.app.core

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.seoulhankuko.app.BuildConfig
import com.seoulhankuko.app.R
import com.seoulhankuko.app.core.messaging.SeoulHankukoFirebaseMessagingService
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
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channelName = getString(R.string.notification_channel_name)
        val channelDescription = getString(R.string.notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(
            SeoulHankukoFirebaseMessagingService.DEFAULT_CHANNEL_ID,
            channelName,
            importance
        ).apply {
            description = channelDescription
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
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
