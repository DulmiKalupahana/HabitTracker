package com.example.habittracker.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.concurrent.TimeUnit

// Manages repeating hydration reminders using AlarmManager
object HydrationScheduler {
    // Schedule repeating alarm for hydration notifications
    fun start(context: Context, minutes: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HydrationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 101, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val intervalMs = TimeUnit.MINUTES.toMillis(minutes.toLong())
        val first = System.currentTimeMillis() + intervalMs
        am.setRepeating(AlarmManager.RTC_WAKEUP, first, intervalMs, pi)
    }

    // Cancel scheduled hydration reminders
    fun stop(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HydrationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 101, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }
}
