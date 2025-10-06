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
import android.widget.Toast
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var store: PrefStore
    private lateinit var rv: RecyclerView
    private lateinit var adapter: HabitAdapter
    private lateinit var tvStreak: TextView
    private lateinit var tvTodayCount: TextView
    private lateinit var habitCompletionChart: LineChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = PrefStore(requireContext())

        tvStreak = view.findViewById(R.id.tvStreak)
        tvTodayCount = view.findViewById(R.id.tvTodayCount)
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
                    Toast.makeText(requireContext(), "Habit Added!", Toast.LENGTH_SHORT).show()
                    refresh()
                }
            }
        }

        //  Hydration button
        view.findViewById<Button>(R.id.btnWaterReminder).setOnClickListener {
            Toast.makeText(requireContext(), "Hydration reminder set!", Toast.LENGTH_SHORT).show()
        }

        //  Mood button
        view.findViewById<Button>(R.id.btnQuickMood).setOnClickListener {
            Toast.makeText(requireContext(), "Open mood dialog here!", Toast.LENGTH_SHORT).show()
        }

        setupCompletionChart()
        refresh()
    }

    //  Refresh habit list + stats
    private fun refresh() {
        val all = store.getHabits()
        val doneToday = store.getCompleted(todayKey())
        adapter.submit(all, doneToday)

        val habitsDone = doneToday.size
        val streak = calcStreak()

        tvTodayCount.text = "$habitsDone Habits"
        tvStreak.text = "$streak Days"
    }

    //  Calculate streak
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

    //  Setup chart
    private fun setupCompletionChart() {
        habitCompletionChart.description.isEnabled = false
        habitCompletionChart.axisRight.isEnabled = false
        habitCompletionChart.axisLeft.axisMinimum = 0f
        habitCompletionChart.axisLeft.axisMaximum = 100f
        habitCompletionChart.legend.isEnabled = false
        habitCompletionChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        habitCompletionChart.xAxis.setDrawGridLines(false)

        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val entries = mutableListOf<Entry>()
        for (i in labels.indices) {
            entries.add(Entry(i.toFloat(), (60..100).random().toFloat()))
        }

        val set = LineDataSet(entries, "Completion").apply {
            color = resources.getColor(R.color.colorAccent, null)
            setDrawCircles(true)
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            lineWidth = 2f
            fillAlpha = 80
            setDrawFilled(true)
            fillColor = resources.getColor(R.color.chartFill, null)
        }

        habitCompletionChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return labels.getOrElse(idx) { "" }
            }
        }

        habitCompletionChart.data = LineData(set)
        habitCompletionChart.invalidate()
    }

    //  Utility
    private fun todayKey(date: Date = Date()): String {
        val c = Calendar.getInstance()
        c.time = date
        return String.format("%04d-%02d-%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
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
        AddHabitDialog.show(requireContext()) {
            Toast.makeText(requireContext(), "Habit updated!", Toast.LENGTH_SHORT).show()
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
        refresh()
    }
}
