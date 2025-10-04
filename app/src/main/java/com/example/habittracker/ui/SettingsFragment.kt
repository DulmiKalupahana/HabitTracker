package com.example.habittracker.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.habittracker.R
import com.example.habittracker.data.PrefStore
import com.example.habittracker.notify.HydrationScheduler

class SettingsFragment : Fragment() {
    private lateinit var store: PrefStore
    private lateinit var tv: TextView
    private lateinit var et: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = PrefStore(requireContext())
        tv = view.findViewById(R.id.tvStatus)
        et = view.findViewById(R.id.etInterval)
        et.setText(store.getInterval().takeIf { it>0 }?.toString() ?: "")

        view.findViewById<View>(R.id.btnStart).setOnClickListener {
            val mins = et.text.toString().toIntOrNull()
            if (mins == null || mins <= 0) { et.error = "Enter minutes"; return@setOnClickListener }
            store.setInterval(mins)
            HydrationScheduler.start(requireContext(), mins)
            store.setHydrationOn(true)
            tv.text = "Status: On ($mins min)"
        }
        view.findViewById<View>(R.id.btnStop).setOnClickListener {
            HydrationScheduler.stop(requireContext())
            store.setHydrationOn(false)
            tv.text = "Status: Off"
        }

        view.findViewById<View>(R.id.btnShareMoodSummary).setOnClickListener {
            val moods = store.getMoods().take(10) // brief
            val text = buildString {
                append("My recent moods on LifeTrack:\n")
                moods.forEach { append("• ${it.emoji} @ ${java.text.SimpleDateFormat("MM-dd HH:mm").format(java.util.Date(it.timestamp))}\n") }
            }
            val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
            startActivity(Intent.createChooser(share, "Share via"))
        }

        tv.text = if (store.isHydrationOn()) "Status: On (${store.getInterval()} min)" else "Status: Off"
    }
}
