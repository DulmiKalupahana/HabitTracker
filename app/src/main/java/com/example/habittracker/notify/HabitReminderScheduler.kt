package com.example.habittracker.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import com.example.habittracker.data.Habit
import java.util.Calendar

// Schedules/cancels per-habit reminder notifications
object HabitReminderScheduler {
    private const val REQUEST_OFFSET = 5000

    fun schedule(context: Context, habit: Habit) {
        val reminder = habit.reminder
        if (!habit.reminderEnabled || reminder.isNullOrBlank()) {
            cancel(context, habit.id)
            return
        }

        val parts = reminder.split(":")
        if (parts.size != 2) {
            cancel(context, habit.id)
            return
        }

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val alarmTime = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(habit.id),
            Intent(context, HabitReminderReceiver::class.java).apply {
                putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habit.id)
                putExtra(HabitReminderReceiver.EXTRA_HABIT_TITLE, habit.title)
                putExtra(HabitReminderReceiver.EXTRA_HABIT_TIME, formatTime(context, hour, minute))
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            alarmTime.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        NotifyUtils.ensureChannel(context)
    }

    fun cancel(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(habitId),
            Intent(context, HabitReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun requestCodeFor(habitId: String): Int {
        return REQUEST_OFFSET + (habitId.hashCode() and 0x0FFFFFFF)
    }

    private fun formatTime(context: Context, hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }
}