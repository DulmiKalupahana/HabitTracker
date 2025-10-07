package com.example.habittracker.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.habittracker.R
import com.example.habittracker.data.PrefStore
import com.example.habittracker.notify.HydrationScheduler

class ProfileFragment : Fragment() {

    private lateinit var store: PrefStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = PrefStore(requireContext())

        val etName = view.findViewById<EditText>(R.id.etName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etGoal = view.findViewById<EditText>(R.id.etGoal)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val etInterval = view.findViewById<EditText>(R.id.etInterval)

        etName.setText(store.getProfileName())
        etEmail.setText(store.getProfileEmail())
        etGoal.setText(store.getProfileGoal())
        etInterval.setText(store.getInterval().takeIf { it > 0 }?.toString() ?: "")
        tvStatus.text = if (store.isHydrationOn()) {
            getString(R.string.status_on) + " (${store.getInterval()} min)"
        } else {
            getString(R.string.status_off)
        }

        view.findViewById<View>(R.id.btnSaveProfile).setOnClickListener {
            store.setProfileName(etName.text.toString().trim())
            store.setProfileEmail(etEmail.text.toString().trim())
            store.setProfileGoal(etGoal.text.toString().trim())
            Toast.makeText(requireContext(), R.string.profile_saved_message, Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnStart).setOnClickListener {
            val mins = etInterval.text.toString().toIntOrNull()
            if (mins == null || mins <= 0) {
                etInterval.error = getString(R.string.hint_minutes)
                return@setOnClickListener
            }
            store.setInterval(mins)
            HydrationScheduler.start(requireContext(), mins)
            store.setHydrationOn(true)
            tvStatus.text = getString(R.string.status_on) + " (${mins} min)"
            Toast.makeText(requireContext(), getString(R.string.hydration_started_toast, mins), Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnStop).setOnClickListener {
            HydrationScheduler.stop(requireContext())
            store.setHydrationOn(false)
            tvStatus.text = getString(R.string.status_off)
            Toast.makeText(requireContext(), R.string.hydration_disabled_toast, Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnShareMoodSummary).setOnClickListener {
            val moods = store.getMoods().take(10)
            val text = buildString {
                append("My recent moods on LifeTrack:\n")
                moods.forEach {
                    val formatted = java.text.SimpleDateFormat("MM-dd HH:mm").format(java.util.Date(it.timestamp))
                    append("• ${it.emoji} @ $formatted\n")
                }
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.profile_share_moods)))
        }
    }
}