package com.example.habittracker.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.habittracker.R
import com.example.habittracker.data.Habit
import com.example.habittracker.data.PrefStore
import com.example.habittracker.data.todayKey
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.example.habittracker.ui.AddHabitDialog
import java.util.*

// Home dashboard showing habit completion stats and mood trend chart
class HomeFragment : Fragment() {

    private lateinit var store: PrefStore
    private lateinit var rv: RecyclerView
    private lateinit var adapter: HabitAdapter
    private lateinit var tvStreak: TextView
    private lateinit var tvTodayCount: TextView
    private lateinit var moodChart: LineChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = PrefStore(requireContext())

        // Initialize UI elements
        tvStreak = view.findViewById(R.id.tvStreak)
        tvTodayCount = view.findViewById(R.id.tvTodayCount)
        moodChart = view.findViewById(R.id.moodChart)

        rv = view.findViewById(R.id.rvTodayHabits)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = HabitAdapter(::toggleDone, ::editHabit, ::deleteHabit)
        rv.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fabAddHabitHome).setOnClickListener {
            AddHabitDialog.show(requireContext()) {
                Toast.makeText(requireContext(), "Habit Added!", Toast.LENGTH_SHORT).show()
                refresh()
            }
        }

        view.findViewById<Button>(R.id.btnWaterReminder).setOnClickListener {
            Toast.makeText(requireContext(), "Hydration reminder set!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnQuickMood).setOnClickListener {
            Toast.makeText(requireContext(), "Open mood dialog here!", Toast.LENGTH_SHORT).show()
        }

        setupMoodChart()
        refresh()
    }

    private fun refresh() {
        val all = store.getHabits()
        val doneToday = store.getCompleted(todayKey())
        adapter.submit(all, doneToday)

        val habitsDone = doneToday.size
        val streak = calcStreak()

        tvTodayCount.text = "$habitsDone Habits"
        tvStreak.text = "$streak Days"

        updateMoodChart()
    }

    // Calculate consecutive days where all habits were completed
    private fun calcStreak(): Int {
        var streak = 0
        val cal = Calendar.getInstance()
        while (true) {
            val key = todayKey(cal.time)
            val habits = store.getHabits()
            val done = store.getCompleted(key)
            val allDone = habits.isNotEmpty() && done.size == habits.size
            if (allDone) streak++ else break
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun setupMoodChart() {
        moodChart.description.isEnabled = false
        moodChart.axisRight.isEnabled = false
        moodChart.axisLeft.axisMinimum = 0f
        moodChart.axisLeft.axisMaximum = 100f
        moodChart.legend.isEnabled = false
        moodChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        moodChart.xAxis.setDrawGridLines(false)
    }

    private fun updateMoodChart() {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val entries = mutableListOf<Entry>()
        for (i in labels.indices) {
            entries.add(Entry(i.toFloat(), (50..100).random().toFloat()))
        }

        val set = LineDataSet(entries, "Mood Trend").apply {
            color = resources.getColor(R.color.taskBlue, null)
            setDrawCircles(true)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        moodChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return labels.getOrElse(idx) { "" }
            }
        }

        moodChart.data = LineData(set)
        moodChart.invalidate()
    }

    private fun todayKey(date: Date = Date()): String {
        val c = Calendar.getInstance()
        c.time = date
        return String.format("%04d-%02d-%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun toggleDone(h: Habit, done: Boolean) {
        val set = store.getCompleted(todayKey())
        if (done) set.add(h.id) else set.remove(h.id)
        store.setCompleted(todayKey(), set)
        refresh()
    }

    private fun editHabit(h: Habit) {
        val fragment = HabitsFragment()
        fragment.showAddDialog(h)
    }

    private fun deleteHabit(h: Habit) {
        val list = store.getHabits()
        list.removeAll { it.id == h.id }
        store.saveHabits(list)
        val set = store.getCompleted(todayKey())
        set.remove(h.id)
        store.setCompleted(todayKey(), set)
        refresh()
    }
}
