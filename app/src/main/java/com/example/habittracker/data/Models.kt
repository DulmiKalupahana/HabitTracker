package com.example.habittracker.data

import java.util.*

//  Habits
data class Habit(
    val id: String,
    var title: String,
    var icon: String? = null,
    var color: String? = null,
    var repeat: String? = "Daily",
    var date: String? = null,
    var reminder: String? = null
)

//  Moods
data class MoodEntry(
    val timestamp: Long,
    val emoji: String,
    val label: String
)

//  Utility
fun todayKey(date: Date = Date()): String {
    val cal = Calendar.getInstance()
    cal.time = date
    val year = cal.get(Calendar.YEAR)
    val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)
    val day = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
    return "$year-$month-$day"
}