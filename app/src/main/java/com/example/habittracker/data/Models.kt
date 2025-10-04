package com.example.habittracker.data
import org.threeten.bp.LocalDate

data class Habit(val id: String, var title: String)
data class MoodEntry(val timestamp: Long, val emoji: String, val note: String?)

fun todayKey(): String = LocalDate.now().toString()