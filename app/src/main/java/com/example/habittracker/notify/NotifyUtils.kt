package com.example.habittracker.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

// Utility for creating notification channels (required for Android O+)
object NotifyUtils {
    const val CHANNEL_ID = "reminder_channel"

    // Create notification channel if it doesn't exist
    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "LifeTrack Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(ch)
        }
    }
}
