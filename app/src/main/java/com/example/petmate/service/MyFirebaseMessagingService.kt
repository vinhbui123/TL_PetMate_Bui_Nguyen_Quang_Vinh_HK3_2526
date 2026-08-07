package com.example.petmate.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.petmate.MainActivity
import com.example.petmate.util.NotificationStorage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM"
        private const val CHANNEL_ID = "petmate_notifications"
        private const val CHANNEL_NAME = "PetMate Thông báo"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "=== FCM MESSAGE RECEIVED ===")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Notification: title=${remoteMessage.notification?.title}, body=${remoteMessage.notification?.body}")
        Log.d(TAG, "Data: ${remoteMessage.data}")

        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: remoteMessage.data["senderName"]
            ?: "PetMate"
        val body = remoteMessage.data["body"]
            ?: remoteMessage.notification?.body
            ?: remoteMessage.data["content"]
            ?: "Bạn có thông báo mới"
        val roomId = remoteMessage.data["roomId"]?.toLongOrNull()
        val type = remoteMessage.data["type"] ?: "general"

        // Save to local storage
        val storage = NotificationStorage(applicationContext)
        storage.saveNotification(title, body, type, remoteMessage.data)
        
        // Trigger global UI refresh for unread count
        com.example.petmate.util.AppEventBus.triggerRefresh()

        // Always show notification (foreground + background)
        showNotification(title, body, roomId)
    }

    private fun showNotification(title: String, messageBody: String, roomId: Long?) {
        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted, cannot show notification")
                return
            }
        }

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (roomId != null) {
                putExtra("roomId", roomId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()

        Log.d(TAG, "Showing notification #$notificationId: $title - $messageBody")
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo từ PetMate"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
