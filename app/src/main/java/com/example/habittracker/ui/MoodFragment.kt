package com.example.habittracker.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.example.habittracker.R
import com.example.habittracker.data.MoodEntry
import com.example.habittracker.data.PrefStore
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*

// Fragment for mood journal with calendar view and emoji selector
class MoodFragment : Fragment() {

    private lateinit var store: PrefStore
    private var currentMonth: Calendar = Calendar.getInstance()
    private lateinit var calendarGrid: GridView
    private lateinit var monthTitle: TextView
    private lateinit var infoText: TextView

    private data class DayCell(
        val day: Int?,
        val calendar: Calendar?,
        val emoji: String?,
        val label: String?
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_mood, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = PrefStore(requireContext())
        calendarGrid = view.findViewById(R.id.calendarGrid)
        monthTitle = view.findViewById(R.id.tvMonth)
        infoText = view.findViewById(R.id.tvMoodInfo)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextMonth)

        loadMonth()

        btnPrev.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            loadMonth()
        }
        btnNext.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            loadMonth()
        }
    }

    // Retrieve mood entries from SharedPreferences
    private fun getMoodList(): MutableList<MoodEntry> {
        return store.getMoods()
    }

    private fun saveMoodList(list: List<MoodEntry>) {
        store.saveMoods(list)
    }

    // Load and display mood calendar for the current month
    private fun loadMonth() {
        currentMonth.set(Calendar.DAY_OF_MONTH, 1)
        val list = getMoodList()
        val month = currentMonth.get(Calendar.MONTH)
        val year = currentMonth.get(Calendar.YEAR)
        monthTitle.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time)

        val cells = buildCells(list, year, month)

        calendarGrid.adapter = object : BaseAdapter() {
            override fun getCount() = cells.size
            override fun getItem(pos: Int) = cells[pos]
            override fun getItemId(pos: Int) = pos.toLong()
            override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.item_day, parent, false)
                val cell = cells[pos]

                val dayNum = view.findViewById<TextView>(R.id.tvDayNum)
                val emojiView = view.findViewById<TextView>(R.id.tvEmoji)
                val labelView = view.findViewById<TextView>(R.id.tvMoodLabel)

                if (cell.calendar == null || cell.day == null) {
                    dayNum.text = ""
                    emojiView.text = ""
                    labelView.text = ""
                    view.background = null
                    view.isSelected = false
                    view.isEnabled = false
                    view.isClickable = false
                    view.visibility = View.INVISIBLE
                    view.setOnClickListener(null)
                } else {
                    dayNum.text = cell.day.toString()
                    emojiView.text = cell.emoji ?: ""
                    labelView.text = cell.label ?: ""
                    view.visibility = View.VISIBLE
                    view.isEnabled = true
                    view.isClickable = true
                    view.background = ContextCompat.getDrawable(requireContext(), R.drawable.day_bg_selector)
                    val isToday = isSameDay(cell.calendar.timeInMillis, System.currentTimeMillis())
                    view.isSelected = isToday
                    view.setOnClickListener {
                        showMoodPicker(cell.calendar, infoText)
                    }
                }

                return view
            }
        }

        updateTodayMessage(list)
    }

    // Show bottom sheet dialog for adding/editing mood with emoji selector
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
            selectedEmoji = existing.emoji to existing.label
        } else {
            saveBtn.visibility = View.VISIBLE
            actionRow.visibility = View.GONE
        }

        val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(selectedDate.time)
        tvInfo.text = existing?.let {
            getString(R.string.mood_selected_summary, formattedDate, it.emoji, it.label)
        } ?: getString(R.string.no_mood_recorded, formattedDate)

        val unselectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.mood_card_unselected)
        val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.mood_card_selected)
        val labelDefaultColor = ContextCompat.getColor(requireContext(), R.color.textPrimary)
        val labelSelectedColor = ContextCompat.getColor(requireContext(), android.R.color.white)

        // Create emoji cards
        moods.forEach { (emoji, label) ->
            val emojiCard = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
                background = unselectedBg?.constantState?.newDrawable()

                val emojiText = TextView(context).apply {
                    text = emoji
                    textSize = 36f
                    gravity = Gravity.CENTER
                }

                val labelText = TextView(context).apply {
                    text = label
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTextColor(labelDefaultColor)
                }

                addView(emojiText)
                addView(labelText)

                // Preselect current mood
                if (existing != null && existing.emoji == emoji) {
                    background = selectedBg?.constantState?.newDrawable()
                    labelText.setTextColor(labelSelectedColor)
                    selectedEmoji = emoji to label
                }

                setOnClickListener {
                    selectedEmoji = emoji to label
                    for (child in emojiGrid.children) {
                        child.background = unselectedBg?.constantState?.newDrawable()
                        val lbl = (child as LinearLayout).getChildAt(1) as TextView
                        lbl.setTextColor(labelDefaultColor)
                    }
                    background = selectedBg?.constantState?.newDrawable()
                    labelText.setTextColor(labelSelectedColor)
                }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(12, 12, 12, 12)
            }
            emojiCard.layoutParams = params
            emojiGrid.addView(emojiCard)
        }

        // Add Mood
        saveBtn.setOnClickListener {
            selectedEmoji?.let { (emoji, label) ->
                val list = getMoodList()
                list.removeAll { isSameDay(it.timestamp, selectedDate.timeInMillis) }
                list.add(MoodEntry(selectedDate.timeInMillis, emoji, label))
                saveMoodList(list)
                loadMonth()
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
                loadMonth()
                dialog.dismiss()
                Toast.makeText(requireContext(), "Mood updated", Toast.LENGTH_SHORT).show()
            }
        }

        // Delete Mood
        deleteBtn.setOnClickListener {
            val list = getMoodList()
            list.removeAll { isSameDay(it.timestamp, selectedDate.timeInMillis) }
            saveMoodList(list)
            loadMonth()
            dialog.dismiss()
            Toast.makeText(requireContext(), "Mood deleted", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(Date(t1)) == fmt.format(Date(t2))
    }

    private fun buildCells(list: List<MoodEntry>, year: Int, month: Int): List<DayCell> {
        val firstDay = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOffset = (firstDay.get(Calendar.DAY_OF_WEEK) + 5) % 7

        val cells = mutableListOf<DayCell>()
        repeat(startOffset) {
            cells.add(DayCell(null, null, null, null))
        }

        val totalDays = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..totalDays) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val entry = list.find { isSameDay(it.timestamp, cal.timeInMillis) }
            cells.add(DayCell(day, cal, entry?.emoji, entry?.label))
        }

        val trailing = (7 - (cells.size % 7)) % 7
        repeat(trailing) {
            cells.add(DayCell(null, null, null, null))
        }

        return cells
    }

    private fun updateTodayMessage(list: List<MoodEntry>) {
        val today = list.find { isSameDay(it.timestamp, System.currentTimeMillis()) }
        infoText.text = today?.let {
            getString(R.string.mood_today_summary_text, it.emoji, it.label)
        } ?: getString(R.string.mood_today_empty_text)
    }
}
