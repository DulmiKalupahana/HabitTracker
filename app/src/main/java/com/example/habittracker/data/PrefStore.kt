package com.example.habittracker.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// SharedPreferences wrapper for persisting app data without a database
class PrefStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("lifetrack_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Save habits list to SharedPreferences as JSON
    fun saveHabits(list: List<Habit>) {
        sp.edit().putString("habits", gson.toJson(list)).apply()
    }
    // Retrieve habits list from SharedPreferences
    fun getHabits(): MutableList<Habit> {
        val json = sp.getString("habits", "[]")
        val type = object : TypeToken<MutableList<Habit>>(){}.type
        return gson.fromJson(json, type)
    }

    // Store completed habit IDs for a specific date
    fun setCompleted(dateKey: String, set: Set<String>) {
        sp.edit().putString("done_$dateKey", gson.toJson(set)).apply()
    }
    // Retrieve completed habit IDs for a specific date
    fun getCompleted(dateKey: String): MutableSet<String> {
        val json = sp.getString("done_$dateKey", "[]")
        val type = object : TypeToken<MutableSet<String>>(){}.type
        return gson.fromJson(json, type)
    }

    // Save mood entries list to SharedPreferences
    fun saveMoods(list: List<MoodEntry>) {
        sp.edit().putString("moods", gson.toJson(list)).apply()
    }
    // Retrieve mood entries from SharedPreferences
    fun getMoods(): MutableList<MoodEntry> {
        val json = sp.getString("moods", "[]")
        val type = object : TypeToken<MutableList<MoodEntry>>(){}.type
        return gson.fromJson(json, type)
    }

    // Store hydration reminder interval in minutes
    fun setInterval(mins: Int) = sp.edit().putInt("hydration_mins", mins).apply()
    fun getInterval(): Int = sp.getInt("hydration_mins", 0)
    fun setHydrationOn(on: Boolean) = sp.edit().putBoolean("hydration_on", on).apply()
    fun isHydrationOn(): Boolean = sp.getBoolean("hydration_on", false)
}