package com.example.mealmate.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mealmate.login.LoginPage
import com.example.mealmate.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Message received: ${remoteMessage.data}")

        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "MealMate"
            val body = remoteMessage.data["body"] ?: "Time to cook something delicious!"
            Log.d("FCM", "Data Notification -> Title: $title, Body: $body")
            showNotification(title, body)
        } else if (remoteMessage.notification != null) {
            val title = remoteMessage.notification?.title ?: "MealMate"
            val body = remoteMessage.notification?.body ?: "Time to cook something delicious!"
            Log.d("FCM", "Notification Payload -> Title: $title, Body: $body")
            showNotification(title, body)
        }
    }



    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM Token", token)
        // Send the token to your server if needed
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        // Send the token to your backend server (if applicable)
        // Example: ApiClient.sendTokenToServer(token)
    }

    private fun showNotification(title: String, body: String) {
        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(this, LoginPage::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "mealMate_notifications"

        // Create a notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MealMate Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo_small)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }
}
