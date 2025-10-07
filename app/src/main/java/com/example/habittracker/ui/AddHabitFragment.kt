package com.example.habittracker.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.habittracker.R
import com.example.habittracker.data.Habit
import com.example.habittracker.data.PrefStore
import com.example.habittracker.notify.HabitReminderScheduler
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddHabitFragment : Fragment() {

    private lateinit var store: PrefStore
    private var existingHabit: Habit? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = PrefStore(requireContext())
        val habitId = arguments?.getString(ARG_HABIT_ID)
        existingHabit = habitId?.let { id ->
            store.getHabits().firstOrNull { it.id == id }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_habit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        toolbar.title = if (existingHabit == null) {
            getString(R.string.add_habit_heading)
        } else {
            getString(R.string.edit_habit)
        }

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val tgRepeat = view.findViewById<MaterialButtonToggleGroup>(R.id.tgRepeat)
        val layoutDaily = view.findViewById<LinearLayout>(R.id.layoutDaily)
        val layoutWeekly = view.findViewById<LinearLayout>(R.id.layoutWeekly)
        val layoutMonthly = view.findViewById<LinearLayout>(R.id.layoutMonthly)
        val tvHabitDate = view.findViewById<TextView>(R.id.tvHabitDate)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val swReminder = view.findViewById<SwitchMaterial>(R.id.swReminder)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        var repeat = "Daily"
        var dateStr = tvHabitDate.text.toString()
        var timeStr = tvTime.text.toString()

        swReminder.setOnCheckedChangeListener { _, isChecked ->
            tvTime.isEnabled = isChecked
            tvTime.alpha = if (isChecked) 1f else 0.5f
        }

        existingHabit?.let { habit ->
            view.findViewById<TextView>(R.id.tvDialogTitle).text = getString(R.string.edit_habit)
            btnSave.text = getString(R.string.add_habit_save)
            etTitle.setText(habit.title)
            tvHabitDate.text = habit.date ?: getString(R.string.add_habit_date_none)
            repeat = habit.repeat ?: "Daily"
            val hasReminder = habit.reminderEnabled && !habit.reminder.isNullOrEmpty()
            swReminder.isChecked = hasReminder
            tvTime.text = habit.reminder ?: timeStr
            timeStr = habit.reminder ?: timeStr
            dateStr = habit.date ?: dateStr

            when (repeat) {
                "Daily" -> tgRepeat.check(R.id.btnRepeatDaily)
                "Weekly" -> tgRepeat.check(R.id.btnRepeatWeekly)
                "Monthly" -> tgRepeat.check(R.id.btnRepeatMonthly)
            }

            layoutDaily.visibility = if (repeat == "Daily") View.VISIBLE else View.GONE
            layoutWeekly.visibility = if (repeat == "Weekly") View.VISIBLE else View.GONE
            layoutMonthly.visibility = if (repeat == "Monthly") View.VISIBLE else View.GONE
        }

        if (!swReminder.isChecked) {
            tvTime.isEnabled = false
            tvTime.alpha = 0.5f
        }

        tgRepeat.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            layoutDaily.visibility = View.GONE
            layoutWeekly.visibility = View.GONE
            layoutMonthly.visibility = View.GONE
            repeat = when (checkedId) {
                R.id.btnRepeatDaily -> {
                    layoutDaily.visibility = View.VISIBLE
                    "Daily"
                }
                R.id.btnRepeatWeekly -> {
                    layoutWeekly.visibility = View.VISIBLE
                    "Weekly"
                }
                R.id.btnRepeatMonthly -> {
                    layoutMonthly.visibility = View.VISIBLE
                    "Monthly"
                }
                else -> "Daily"
            }
        }

        view.findViewById<View>(R.id.btnPickHabitDate).setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    dateStr = "$dayOfMonth/${month + 1}/$year"
                    tvHabitDate.text = dateStr
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        tvTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                    tvTime.text = timeStr
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        btnCancel.setOnClickListener { findNavController().navigateUp() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                etTitle.error = getString(R.string.habit_title_required)
                return@setOnClickListener
            }

            val newHabit = Habit(
                id = existingHabit?.id ?: UUID.randomUUID().toString(),
                title = title,
                icon = existingHabit?.icon,
                color = existingHabit?.color ?: "#8B88F8",
                repeat = repeat,
                date = dateStr,
                reminder = if (swReminder.isChecked) timeStr else null,
                reminderEnabled = swReminder.isChecked,
                timeOfDay = existingHabit?.timeOfDay,
                weeklyDays = existingHabit?.weeklyDays ?: emptyList(),
                monthlyDays = existingHabit?.monthlyDays,
                endType = existingHabit?.endType,
                endValue = existingHabit?.endValue
            )

            val habits = store.getHabits()
            if (existingHabit != null) {
                val index = habits.indexOfFirst { it.id == existingHabit!!.id }
                if (index != -1) {
                    habits[index] = newHabit
                }
                Toast.makeText(requireContext(), R.string.habit_updated_success, Toast.LENGTH_SHORT).show()
            } else {
                habits.add(newHabit)
                Toast.makeText(requireContext(), R.string.habit_created_success, Toast.LENGTH_SHORT).show()
            }

            store.saveHabits(habits)

            if (newHabit.reminderEnabled && !newHabit.reminder.isNullOrEmpty()) {
                HabitReminderScheduler.schedule(requireContext(), newHabit)
            } else {
                HabitReminderScheduler.cancel(requireContext(), newHabit.id)
            }

            val resultBundle = bundleOf(RESULT_REFRESH to true)
            parentFragmentManager.setFragmentResult(REQUEST_KEY, resultBundle)
            findNavController().navigateUp()
        }
    }

    companion object {
        const val ARG_HABIT_ID = "habitId"
        const val REQUEST_KEY = "add_habit_result"
        const val RESULT_REFRESH = "refresh"
    }
}