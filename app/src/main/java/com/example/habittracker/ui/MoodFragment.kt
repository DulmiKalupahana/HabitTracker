package com.example.habittracker.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.habittracker.R
import com.example.habittracker.data.MoodEntry
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MoodFragment : Fragment() {

    private lateinit var prefs: android.content.SharedPreferences
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentMonth: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_mood, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        prefs = requireContext().getSharedPreferences("moods", 0)

        val grid = view.findViewById<GridView>(R.id.calendarGrid)
        val monthTv = view.findViewById<TextView>(R.id.tvMonth)
        val tvInfo = view.findViewById<TextView>(R.id.tvMoodInfo)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextMonth)

        loadMonth(grid, monthTv, tvInfo)

        btnPrev.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            loadMonth(grid, monthTv, tvInfo)
        }

        btnNext.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            loadMonth(grid, monthTv, tvInfo)
        }
    }

    // ----------------- Mood storage -----------------
    private fun getMoodList(): MutableList<MoodEntry> {
        val json = prefs.getString("moodList", "[]")
        val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveMoodList(list: List<MoodEntry>) {
        prefs.edit().putString("moodList", gson.toJson(list)).apply()
    }

    // ----------------- Calendar load -----------------
    private fun loadMonth(grid: GridView, tvMonth: TextView, tvInfo: TextView) {
        val list = getMoodList()
        val days = mutableListOf<Pair<Int, String?>>()
        val month = currentMonth.get(Calendar.MONTH)
        val year = currentMonth.get(Calendar.YEAR)
        tvMonth.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time)

        val totalDays = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..totalDays) {
            val key = "$year-${String.format("%02d", month + 1)}-${String.format("%02d", i)}"
            val emoji = list.find { dateFormat.format(Date(it.timestamp)) == key }?.emoji
            days.add(i to emoji)
        }

        grid.adapter = object : BaseAdapter() {
            override fun getCount() = days.size
            override fun getItem(pos: Int) = days[pos]
            override fun getItemId(pos: Int) = pos.toLong()
            override fun getView(pos: Int, cv: View?, parent: ViewGroup?): View {
                val v = layoutInflater.inflate(R.layout.item_day, parent, false)
                val (day, emoji) = days[pos]
                v.findViewById<TextView>(R.id.tvDayNum).text = day.toString()
                v.findViewById<TextView>(R.id.tvEmoji).text = emoji ?: ""

                // click to add/edit mood
                v.setOnClickListener {
                    val selectedDate = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    showMoodPicker(selectedDate, tvInfo)
                }
                return v
            }
        }
    }

    // ----------------- Mood Picker -----------------
    private fun showMoodPicker(selectedDate: Calendar, tvInfo: TextView) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_mood, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        val emojiGrid = dialogView.findViewById<GridLayout>(R.id.moodEmojiGrid)
        val saveBtn = dialogView.findViewById<Button>(R.id.btnSaveMood)

        val moods = listOf(
            "😎" to "Great",
            "😊" to "Good",
            "😐" to "Okay",
            "😢" to "Not Good",
            "😡" to "Bad"
        )
        var selected = moods[0]

        moods.forEach { (emoji, label) ->
            val emojiView = TextView(requireContext()).apply {
                text = emoji
                textSize = 36f
                gravity = Gravity.CENTER
                setPadding(20, 20, 20, 20)
                setOnClickListener {
                    selected = emoji to label
                    setBackgroundResource(R.drawable.selected_emoji_bg)
                }
            }
            emojiGrid.addView(emojiView)
        }

        saveBtn.setOnClickListener {
            val list = getMoodList()
            list.removeAll { isSameDay(it.timestamp, selectedDate.timeInMillis) }
            list.add(MoodEntry(selectedDate.timeInMillis, selected.first, selected.second))
            saveMoodList(list)

            // refresh calendar UI
            loadMonth(
                requireView().findViewById(R.id.calendarGrid),
                requireView().findViewById(R.id.tvMonth),
                tvInfo
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    // ----------------- Utility -----------------
    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(Date(t1)) == fmt.format(Date(t2))
    }
}
