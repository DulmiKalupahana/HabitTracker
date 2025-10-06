package com.example.habittracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.example.habittracker.R
import com.example.habittracker.data.Habit
import com.example.habittracker.data.MoodEntry
import com.example.habittracker.data.PrefStore
import com.example.habittracker.data.todayKey
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.habittracker.notify.HabitReminderScheduler
import com.example.habittracker.notify.HydrationScheduler
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var store: PrefStore
    private lateinit var rv: RecyclerView
    private lateinit var adapter: HabitAdapter
    private lateinit var tvStreak: TextView
    private lateinit var tvTodayCount: TextView
    private lateinit var tvTodayMood: TextView
    private lateinit var tvWaterStatus: TextView
    private lateinit var btnWaterReminder: Button
    private lateinit var habitCompletionChart: LineChart
    private val moodOptions = listOf(
        MoodOption("😎", "Great"),
        MoodOption("😊", "Good"),
        MoodOption("😐", "Okay"),
        MoodOption("😢", "Not Good"),
        MoodOption("😡", "Bad")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = PrefStore(requireContext())

        tvStreak = view.findViewById(R.id.tvStreak)
        tvTodayCount = view.findViewById(R.id.tvTodayCount)
        tvTodayMood = view.findViewById(R.id.tvTodayMood)
        tvWaterStatus = view.findViewById(R.id.tvWaterStatus)
        habitCompletionChart = view.findViewById(R.id.habitCompletionChart)

        rv = view.findViewById(R.id.rvTodayHabits)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = HabitAdapter(::toggleDone, ::editHabit, ::deleteHabit)
        rv.adapter = adapter

        //  Floating button animation + habit dialog
        val fabAddHabit = view.findViewById<FloatingActionButton>(R.id.fabAddHabit)
        fabAddHabit.setOnClickListener {
            fabAddHabit.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                fabAddHabit.animate().scaleX(1f).scaleY(1f).setDuration(100)
                AddHabitDialog.show(requireContext()) {
                    refresh()
                }
            }
        }

        //  Hydration button
        btnWaterReminder = view.findViewById(R.id.btnWaterReminder)
        btnWaterReminder.setOnClickListener { showHydrationDialog() }

        //  Mood button
        view.findViewById<Button>(R.id.btnQuickMood).setOnClickListener {
            showQuickMoodDialog()
        }

        view.findViewById<Button>(R.id.btnManageHabits).setOnClickListener {
            val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottomNav)
            if (bottomNav != null) {
                bottomNav.selectedItemId = R.id.habitsFragment
            } else {
                findNavController().navigate(R.id.habitsFragment)
            }
        }

        setupCompletionChart()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (this::store.isInitialized) {
            refresh()
        }
    }

    private fun showQuickMoodDialog() {
        if (moodOptions.isEmpty()) return

        val todayKeyValue = todayKey()
        val existing = store.getMoods().firstOrNull { entry ->
            sameDay(entry.timestamp, todayKeyValue)
        }

        val optionLabels = moodOptions.map { "${it.emoji} ${it.label}" }.toTypedArray()
        var selectedIndex = existing?.let { entry ->
            moodOptions.indexOfFirst { it.matches(entry) }
        } ?: 0
        if (selectedIndex < 0) selectedIndex = 0
        var currentSelection = selectedIndex

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.mood_prompt))
            .setSingleChoiceItems(optionLabels, selectedIndex) { _, which ->
                currentSelection = which
            }
            .setPositiveButton(getString(R.string.save)) { dialog, _ ->
                val choice = moodOptions.getOrNull(currentSelection) ?: return@setPositiveButton
                val updated = store.getMoods().filterNot { entry ->
                    sameDay(entry.timestamp, todayKeyValue)
                }.toMutableList()
                updated.add(MoodEntry(System.currentTimeMillis(), choice.emoji, choice.label))
                store.saveMoods(updated)
                Toast.makeText(requireContext(), getString(R.string.mood_saved_today), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                refresh()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun sameDay(timestamp: Long, key: String): Boolean {
        return todayKey(Date(timestamp)) == key
    }

    private data class MoodOption(val emoji: String, val label: String) {
        fun matches(entry: MoodEntry): Boolean = entry.emoji == emoji && entry.label == label
    }

    //  Refresh habit list + stats
    private fun refresh() {
        val todayKeyValue = todayKey()
        val all = store.getHabits()
        val doneToday = store.getCompleted(todayKeyValue)
        adapter.submit(all, doneToday)

        store.setHabitTotalForDay(todayKeyValue, all.size)

        val habitsDone = doneToday.size
        val streak = calcStreak(all.size)

        tvTodayCount.text = getString(R.string.home_habits_done, habitsDone)
        tvStreak.text = getString(R.string.home_streak_days, streak)

        updateTodayMood()
        updateCompletionChart(all.size)
        updateHydrationStatus()
    }

    //  Calculate streak
    private fun calcStreak(todayHabitCount: Int): Int {
        var streak = 0
        val cal = Calendar.getInstance()
        while (true) {
            val key = todayKey(cal.time)
            val total = store.getHabitTotalForDay(key, todayHabitCount)
            if (total <= 0) break
            val done = store.getCompleted(key)
            if (done.size >= total) {
                streak++
            } else {
                break
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun showHydrationDialog() {
        val intervals = listOf(30, 60, 90, 120)
        val stored = store.getInterval().takeIf { it > 0 }
        var selectedIndex = stored?.let { intervals.indexOf(it) } ?: 1
        if (selectedIndex < 0) selectedIndex = 1
        var currentSelection = selectedIndex

        val labels = intervals.map { getString(R.string.hydration_interval_option, it) }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.hydration_manage_title))
            .setSingleChoiceItems(labels, selectedIndex) { _, which ->
                currentSelection = which
            }
            .setPositiveButton(getString(R.string.start)) { dialog, _ ->
                val minutes = intervals.getOrNull(currentSelection) ?: intervals[1]
                store.setInterval(minutes)
                store.setHydrationOn(true)
                HydrationScheduler.start(requireContext(), minutes)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.hydration_started_toast, minutes),
                    Toast.LENGTH_SHORT
                ).show()
                updateHydrationStatus()
                dialog.dismiss()
            }
            .setNeutralButton(getString(R.string.hydration_stop)) { dialog, _ ->
                HydrationScheduler.stop(requireContext())
                store.setHydrationOn(false)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.hydration_disabled_toast),
                    Toast.LENGTH_SHORT
                ).show()
                updateHydrationStatus()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateHydrationStatus() {
        val isOn = store.isHydrationOn()
        val interval = store.getInterval()
        if (isOn && interval > 0) {
            tvWaterStatus.text = getString(R.string.home_hydration_status_on, interval)
            btnWaterReminder.text = getString(R.string.hydration_button_manage)
        } else {
            tvWaterStatus.text = getString(R.string.home_hydration_status_off)
            btnWaterReminder.text = getString(R.string.hydration_button_set)
        }
    }

    //  Setup chart
    private fun setupCompletionChart() {
        habitCompletionChart.description.isEnabled = false
        habitCompletionChart.axisRight.isEnabled = false
        habitCompletionChart.axisLeft.axisMinimum = 0f
        habitCompletionChart.axisLeft.axisMaximum = 100f
        habitCompletionChart.legend.isEnabled = false
        habitCompletionChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        habitCompletionChart.xAxis.setDrawGridLines(false)
        habitCompletionChart.xAxis.setDrawAxisLine(false)
        habitCompletionChart.axisLeft.setDrawAxisLine(false)
        habitCompletionChart.axisLeft.granularity = 20f
        habitCompletionChart.axisLeft.setDrawGridLines(true)
        habitCompletionChart.axisLeft.gridColor = ContextCompat.getColor(requireContext(), R.color.chartGrid)
        val axisTextColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
        habitCompletionChart.axisLeft.textColor = axisTextColor
        habitCompletionChart.xAxis.textColor = axisTextColor
        habitCompletionChart.xAxis.granularity = 1f
        habitCompletionChart.setScaleEnabled(false)
        habitCompletionChart.setNoDataText(getString(R.string.home_chart_empty))
    }

    private fun updateCompletionChart(todayHabitCount: Int) {
        val formatter = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)

        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        for (index in 0..6) {
            val date = cal.time
            val key = todayKey(date)
            val total = store.getHabitTotalForDay(key, todayHabitCount)
            val done = store.getCompleted(key).size
            val percentage = if (total <= 0) 0f else (minOf(done, total) * 100f) / total
            entries.add(Entry(index.toFloat(), percentage))
            labels.add(formatter.format(date))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dataSet = LineDataSet(entries, getString(R.string.home_chart_label)).apply {
            color = ContextCompat.getColor(requireContext(), R.color.chartLine)
            setDrawCircles(true)
            circleRadius = 5f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            lineWidth = 2.5f
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.chartFill)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.chartPoint))
            circleHoleColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        }

        habitCompletionChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return labels.getOrElse(idx) { "" }
            }
        }

        habitCompletionChart.data = LineData(dataSet)
        habitCompletionChart.animateY(600)
        habitCompletionChart.invalidate()
    }

    private fun updateTodayMood() {
        val todayKeyValue = todayKey()
        val todayMood = store.getMoods().firstOrNull { entry ->
            sameDay(entry.timestamp, todayKeyValue)
        }

        if (todayMood != null) {
            tvTodayMood.text = getString(R.string.home_today_mood, todayMood.emoji, todayMood.label)
            tvTodayMood.isActivated = true
            tvTodayMood.setTextColor(ContextCompat.getColor(requireContext(), R.color.textOnAccent))
        } else {
            tvTodayMood.text = getString(R.string.home_today_mood_empty)
            tvTodayMood.isActivated = false
            tvTodayMood.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        }
    }

    //  Habit operations
    private fun toggleDone(h: Habit, done: Boolean) {
        val set = store.getCompleted(todayKey())
        if (done) set.add(h.id) else set.remove(h.id)
        store.setCompleted(todayKey(), set)
        refresh()
    }

    private fun editHabit(h: Habit) {
        // previously you had fragment.showAddDialog(h) which won't work
        AddHabitDialog.show(requireContext(), h) {
        refresh()
        }
    }

    private fun deleteHabit(h: Habit) {
        val list = store.getHabits()
        list.removeAll { it.id == h.id }
        store.saveHabits(list)
        val set = store.getCompleted(todayKey())
        set.remove(h.id)
        store.setCompleted(todayKey(), set)
        HabitReminderScheduler.cancel(requireContext(), h.id)
        Toast.makeText(requireContext(), getString(R.string.habit_deleted_success), Toast.LENGTH_SHORT).show()
        refresh()
    }
}