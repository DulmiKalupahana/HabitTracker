package com.example.habittracker.ui

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.habittracker.R
import com.example.habittracker.data.PrefStore
import com.example.habittracker.data.todayKey
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import java.util.*

class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val store = PrefStore(requireContext())
        val dateTv = view.findViewById<TextView>(R.id.tvDate)
        val progTv = view.findViewById<TextView>(R.id.tvProgress)
        val habits = store.getHabits()
        val done = store.getCompleted(todayKey())

        // show today's date
        dateTv.text = LocalDate.now().toString()
        progTv.text = "Today's habits: ${done.size}/${habits.size}"

        // Weekly mood trend: map emoji to score (😀=5 ... 😢=1)
        val score = mapOf("🤩" to 5f, "🥳" to 5f, "😀" to 5f, "🙂" to 4f, "😌" to 4f,
            "😐" to 3f, "😕" to 2f, "😡" to 1f, "😢" to 1f, "😴" to 2f)

        val moods = store.getMoods()
        val today = LocalDate.now()
        val last7 = (6 downTo 0).map { d -> today.minusDays(d.toLong()) }

        val entries = mutableListOf<Entry>()
        last7.forEachIndexed { idx, day ->
            val start = day.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            val end = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            val dayMoods = moods.filter { it.timestamp in start until end }
            val avg = if (dayMoods.isEmpty()) 0f else dayMoods.map { score[it.emoji] ?: 3f }.average().toFloat()
            entries.add(Entry(idx.toFloat(), avg))
        }

        val chart = view.findViewById<LineChart>(R.id.lineChart)
        val ds = LineDataSet(entries, "Mood (7 days)")
        ds.setDrawValues(false)
        ds.setDrawCircles(true)

        val data = LineData(ds)
        chart.data = data

        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.axisMaximum = 5f
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.description.isEnabled = false

        chart.invalidate()

    }
}
