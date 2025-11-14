package com.seoulhankuko.app.core.messaging

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.seoulhankuko.app.MainActivity
import com.seoulhankuko.app.R
import com.seoulhankuko.app.data.local.UserPreferencesManager
import com.seoulhankuko.app.data.repository.PushTokenRepository
import com.seoulhankuko.app.notifications.WritingNotificationCenter
import com.seoulhankuko.app.notifications.WritingNotificationEvent
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

        val payloadType = remoteMessage.data["type"]
        if (payloadType == WritingNotificationCenter.TYPE_WRITING_GRADED) {
            handleWritingGraded(remoteMessage)
            return
        }

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

        showNotification(title, body, remoteMessage, null)
    }

    private fun handleWritingGraded(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val lessonId = data["lesson_id"]
        val submissionId = data["submission_id"]
        val title = data["title"] ?: getString(R.string.writing_notification_title)
        val body = data["body"] ?: getString(R.string.writing_notification_body)

        if (lessonId.isNullOrBlank()) {
            Timber.w("writing_graded notification missing lesson_id: %s", data)
            return
        }

        WritingNotificationCenter.publish(
            WritingNotificationEvent.WritingGraded(
                lessonId = lessonId,
                submissionId = submissionId,
                title = title,
                body = body
            )
        )

        val enrichedData = mutableMapOf<String, String>()
        enrichedData.putAll(data)
        enrichedData[WritingNotificationCenter.EXTRA_TARGET_LESSON_ID] = lessonId
        submissionId?.let {
            enrichedData[WritingNotificationCenter.EXTRA_SUBMISSION_ID] = it
        }

        showNotification(title, body, remoteMessage, enrichedData)
    }

    private fun showNotification(
        title: String,
        body: String,
        remoteMessage: RemoteMessage,
        extraData: Map<String, String>?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.w("Notification permission not granted. Dropping push notification display.")
            return
        }

        val notificationExtras = mutableMapOf<String, String>()
        notificationExtras.putAll(remoteMessage.data)
        extraData?.let { notificationExtras.putAll(it) }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtras(Intent().apply {
                notificationExtras.forEach { (key, value) ->
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

        // Create custom notification layout
        val customView = RemoteViews(packageName, R.layout.custom_notification)
        customView.setTextViewText(R.id.notification_title, title)
        customView.setTextViewText(R.id.notification_body, body)
        customView.setImageViewResource(R.id.notification_logo, R.drawable.ic_launcher_foreground)

        // Create expanded layout for BigTextStyle
        val expandedView = RemoteViews(packageName, R.layout.custom_notification_expanded)
        expandedView.setTextViewText(R.id.notification_title, title)
        expandedView.setTextViewText(R.id.notification_body, body)
        expandedView.setImageViewResource(R.id.notification_logo, R.drawable.ic_launcher_foreground)

        // Create large icon bitmap from drawable (vector drawable)
        val largeIcon = try {
            val drawable = ContextCompat.getDrawable(this, R.drawable.ic_launcher_foreground)
            drawableToBitmap(drawable, 128, 128) // 128x128 for large icon
        } catch (e: Exception) {
            Timber.w(e, "Failed to create large icon bitmap")
            null
        }

        val builder = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Small icon (required)
            .setLargeIcon(largeIcon) // Large icon with app logo
            .setContentTitle(title) // Fallback title
            .setContentText(body) // Fallback text
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()) // Use decorated custom view style
            .setCustomContentView(customView) // Custom layout for collapsed state
            .setCustomBigContentView(expandedView) // Custom layout for expanded state
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setColor(getColor(R.color.primary_green)) // Accent color
            .setColorized(true) // Colorize notification
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            // Add subtle vibration pattern
            .setVibrate(longArrayOf(0, 200, 150, 200))
            // Use default notification sound
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))

        // Add action button for writing notifications
        if (extraData?.containsKey(WritingNotificationCenter.EXTRA_TARGET_LESSON_ID) == true) {
            val viewIntent = Intent(this, MainActivity::class.java).apply {
                putExtra(WritingNotificationCenter.EXTRA_TARGET_LESSON_ID, 
                    extraData[WritingNotificationCenter.EXTRA_TARGET_LESSON_ID])
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val viewPendingIntent = PendingIntent.getActivity(
                this,
                NOTIFICATION_REQUEST_CODE + 1,
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
            )
            
            builder.addAction(
                R.drawable.ic_launcher_foreground, // Icon for action
                "Xem bài viết", // Action text
                viewPendingIntent
            )
        }

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

    /**
     * Convert Drawable to Bitmap for use as large icon in notifications
     */
    private fun drawableToBitmap(drawable: Drawable?, width: Int, height: Int): Bitmap? {
        if (drawable == null) return null
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        const val DEFAULT_CHANNEL_ID = "seoul_hankuko_default"
        private const val NOTIFICATION_REQUEST_CODE = 1010
        private const val NOTIFICATION_BASE_ID = 7000
    }
}

