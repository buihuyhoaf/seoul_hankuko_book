package com.seoulhankuko.app.core.messaging

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.seoulhankuko.app.MainActivity
import com.seoulhankuko.app.R
import com.seoulhankuko.app.data.local.UserPreferencesManager
import com.seoulhankuko.app.data.repository.PushTokenRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SeoulHankukoFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushTokenRepository: PushTokenRepository

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("Received new FCM token (length=%d)", token.length)

        serviceScope.launch {
            pushTokenRepository.cacheFcmToken(token)

            val userId = userPreferencesManager.getSavedUserId()
            if (!userId.isNullOrBlank()) {
                pushTokenRepository.registerCachedToken(userId).onFailure {
                    Timber.w(it, "Failed to register FCM token from FirebaseMessagingService")
                }
            } else {
                Timber.d("No logged in user found when new FCM token arrived")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: ""

        Timber.d("FCM message received. Title=%s, BodyLength=%d", title, body.length)

        if (body.isEmpty() && remoteMessage.data.isEmpty()) {
            Timber.w("Skipping notification because payload is empty")
            return
        }

        showNotification(title, body, remoteMessage)
    }

    private fun showNotification(title: String, body: String, remoteMessage: RemoteMessage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.w("Notification permission not granted. Dropping push notification display.")
            return
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtras(Intent().apply {
                remoteMessage.data.forEach { (key, value) ->
                    putExtra(key, value)
                }
            })
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )

        val builder = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(this).notify(
            NOTIFICATION_BASE_ID + (System.currentTimeMillis() % 1000).toInt(),
            builder.build()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    companion object {
        const val DEFAULT_CHANNEL_ID = "seoul_hankuko_default"
        private const val NOTIFICATION_REQUEST_CODE = 1010
        private const val NOTIFICATION_BASE_ID = 7000
    }
}

