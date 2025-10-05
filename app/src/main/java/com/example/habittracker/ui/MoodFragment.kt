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
import androidx.core.view.children

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

    private fun getMoodList(): MutableList<MoodEntry> {
        val json = prefs.getString("moodList", "[]")
        val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveMoodList(list: List<MoodEntry>) {
        prefs.edit().putString("moodList", gson.toJson(list)).apply()
    }

    private fun loadMonth(grid: GridView, tvMonth: TextView, tvInfo: TextView) {
        val list = getMoodList()
        val days = mutableListOf<Triple<Int, String?, String?>>()
        val month = currentMonth.get(Calendar.MONTH)
        val year = currentMonth.get(Calendar.YEAR)
        tvMonth.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time)

        val totalDays = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..totalDays) {
            val key = "$year-${String.format("%02d", month + 1)}-${String.format("%02d", i)}"
            val entry = list.find { dateFormat.format(Date(it.timestamp)) == key }
            days.add(Triple(i, entry?.emoji, entry?.label))
        }

        grid.adapter = object : BaseAdapter() {
            override fun getCount() = days.size
            override fun getItem(pos: Int) = days[pos]
            override fun getItemId(pos: Int) = pos.toLong()
            override fun getView(pos: Int, cv: View?, parent: ViewGroup?): View {
                val v = layoutInflater.inflate(R.layout.item_day, parent, false)
                val (day, emoji, label) = days[pos]
                v.findViewById<TextView>(R.id.tvDayNum).text = day.toString()
                v.findViewById<TextView>(R.id.tvEmoji).text = emoji ?: ""
                v.findViewById<TextView>(R.id.tvMoodLabel).text = label ?: ""

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

    private fun showMoodPicker(selectedDate: Calendar, tvInfo: TextView) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_mood, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        val emojiGrid = dialogView.findViewById<GridLayout>(R.id.moodEmojiGrid)
        val saveBtn = dialogView.findViewById<Button>(R.id.btnSaveMood)
        val updateBtn = dialogView.findViewById<Button>(R.id.btnUpdateMood)
        val deleteBtn = dialogView.findViewById<Button>(R.id.btnDeleteMood)
        val actionRow = dialogView.findViewById<LinearLayout>(R.id.actionRow)

        val moods = listOf(
            "😎" to "Great",
            "😊" to "Good",
            "😐" to "Okay",
            "😢" to "Not Good",
            "😡" to "Bad"
        )
        var selectedEmoji: Pair<String, String>? = null

        // Check if mood already exists for that date
        val existing = getMoodList().find { isSameDay(it.timestamp, selectedDate.timeInMillis) }

        // Toggle visibility
        if (existing != null) {
            saveBtn.visibility = View.GONE
            actionRow.visibility = View.VISIBLE
        } else {
            saveBtn.visibility = View.VISIBLE
            actionRow.visibility = View.GONE
        }

        // Create emoji cards
        moods.forEach { (emoji, label) ->
            val emojiCard = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
                background = resources.getDrawable(R.drawable.mood_card_unselected, null)

                val emojiText = TextView(context).apply {
                    text = emoji
                    textSize = 36f
                    gravity = Gravity.CENTER
                }

                val labelText = TextView(context).apply {
                    text = label
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTextColor(resources.getColor(R.color.textPrimary, null))
                }

                addView(emojiText)
                addView(labelText)

                // Preselect current mood
                if (existing != null && existing.emoji == emoji) {
                    setBackgroundResource(R.drawable.mood_card_selected)
                    labelText.setTextColor(resources.getColor(android.R.color.white, null))
                    selectedEmoji = emoji to label
                }

                setOnClickListener {
                    selectedEmoji = emoji to label
                    for (child in emojiGrid.children) {
                        child.setBackgroundResource(R.drawable.mood_card_unselected)
                        val lbl = (child as LinearLayout).getChildAt(1) as TextView
                        lbl.setTextColor(resources.getColor(R.color.textPrimary, null))
                    }
                    setBackgroundResource(R.drawable.mood_card_selected)
                    labelText.setTextColor(resources.getColor(android.R.color.white, null))
                }
            }
            emojiGrid.addView(emojiCard)
        }

        // Add Mood
        saveBtn.setOnClickListener {
            selectedEmoji?.let { (emoji, label) ->
                val list = getMoodList()
                list.removeAll { isSameDay(it.timestamp, selectedDate.timeInMillis) }
                list.add(MoodEntry(selectedDate.timeInMillis, emoji, label))
                saveMoodList(list)
                loadMonth(
                    requireView().findViewById(R.id.calendarGrid),
                    requireView().findViewById(R.id.tvMonth),
                    tvInfo
                )
                dialog.dismiss()
                Toast.makeText(requireContext(), "Mood added", Toast.LENGTH_SHORT).show()
            }
        }

        // Update Mood
        updateBtn.setOnClickListener {
            selectedEmoji?.let { (emoji, label) ->
                val list = getMoodList()
                list.removeAll { isSameDay(it.timestamp, selectedDate.timeInMillis) }
                list.add(MoodEntry(selectedDate.timeInMillis, emoji, label))
                saveMoodList(list)
                loadMonth(
                    requireView().findViewById(R.id.calendarGrid),
                    requireView().findViewById(R.id.tvMonth),
                    tvInfo
                )
                dialog.dismiss()
                Toast.makeText(requireContext(), "Mood updated", Toast.LENGTH_SHORT).show()
            }
        }

        // Delete Mood
        deleteBtn.setOnClickListener {
            val list = getMoodList()
            list.removeAll { isSameDay(it.timestamp, selectedDate.timeInMillis) }
            saveMoodList(list)
            loadMonth(
                requireView().findViewById(R.id.calendarGrid),
                requireView().findViewById(R.id.tvMonth),
                tvInfo
            )
            dialog.dismiss()
            Toast.makeText(requireContext(), "Mood deleted", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(Date(t1)) == fmt.format(Date(t2))
    }
}
