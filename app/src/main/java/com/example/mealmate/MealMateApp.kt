package com.example.mealmate

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

class MealMateApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Subscribe to the "all_users" topic
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Successfully subscribed to topic: all_users")
                } else {
                    Log.e("FCM", "Failed to subscribe to topic: all_users", task.exception)
                }
            }
    }
}
