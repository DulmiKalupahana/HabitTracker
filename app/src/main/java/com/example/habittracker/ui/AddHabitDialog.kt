package com.example.habittracker.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.habittracker.R
import com.example.habittracker.data.Habit
import com.example.habittracker.data.PrefStore
import com.example.habittracker.notify.HabitReminderScheduler
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar
import java.util.Locale
import java.util.UUID

object AddHabitDialog {

    fun show(
        context: Context,
        existingHabit: Habit? = null,
        onAddedOrUpdated: (() -> Unit)? = null
    ) {
        val store = PrefStore(context)
        val v: View = LayoutInflater.from(context).inflate(R.layout.dialog_add_habit, null)

        // UI
        val etTitle = v.findViewById<EditText>(R.id.etTitle)
        val tgRepeat = v.findViewById<MaterialButtonToggleGroup>(R.id.tgRepeat)
        val layoutDaily = v.findViewById<LinearLayout>(R.id.layoutDaily)
        val layoutWeekly = v.findViewById<LinearLayout>(R.id.layoutWeekly)
        val layoutMonthly = v.findViewById<LinearLayout>(R.id.layoutMonthly)
        val tvHabitDate = v.findViewById<TextView>(R.id.tvHabitDate)
        val tvTime = v.findViewById<TextView>(R.id.tvTime)
        val swReminder = v.findViewById<SwitchMaterial>(R.id.swReminder)
        val btnSave = v.findViewById<Button>(R.id.btnSave)
        val btnCancel = v.findViewById<Button>(R.id.btnCancel)

        // Default values
        var repeat = "Daily"
        var dateStr = tvHabitDate.text.toString()
        var timeStr = tvTime.text.toString()

        swReminder.setOnCheckedChangeListener { _, isChecked ->
            tvTime.isEnabled = isChecked
            tvTime.alpha = if (isChecked) 1f else 0.5f
        }

        // --- EDIT MODE PREFILL ---
        if (existingHabit != null) {
            etTitle.setText(existingHabit.title ?: "")
            tvHabitDate.text = existingHabit.date ?: ""
            val hasReminder = existingHabit.reminderEnabled || !existingHabit.reminder.isNullOrEmpty()
            swReminder.isChecked = hasReminder
            tvTime.text = existingHabit.reminder ?: ""
            repeat = existingHabit.repeat ?: "Daily"
            v.findViewById<TextView>(R.id.tvDialogTitle).text = "Edit Habit"

            btnSave.text = "Update"

            when (repeat) {
                "Daily" -> tgRepeat.check(R.id.btnRepeatDaily)
                "Weekly" -> tgRepeat.check(R.id.btnRepeatWeekly)
                "Monthly" -> tgRepeat.check(R.id.btnRepeatMonthly)
            }

            layoutDaily.visibility = if (repeat == "Daily") View.VISIBLE else View.GONE
            layoutWeekly.visibility = if (repeat == "Weekly") View.VISIBLE else View.GONE
            layoutMonthly.visibility = if (repeat == "Monthly") View.VISIBLE else View.GONE

            timeStr = existingHabit.reminder ?: timeStr
            dateStr = existingHabit.date ?: dateStr
        }

        tvTime.isEnabled = swReminder.isChecked
        tvTime.alpha = if (swReminder.isChecked) 1f else 0.5f

        // --- SWITCH LAYOUTS ---
        tgRepeat.addOnButtonCheckedListener { _, checkedId, _ ->
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

        // --- DATE PICKER ---
        v.findViewById<View>(R.id.btnPickHabitDate).setOnClickListener {
            val c = Calendar.getInstance()
            val dp = DatePickerDialog(
                context,
                { _, y, m, d ->
                    dateStr = "$d/${m + 1}/$y"
                    tvHabitDate.text = dateStr
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            )
            dp.show()
        }

        // --- TIME PICKER ---
        tvTime.setOnClickListener {
            val c = Calendar.getInstance()
            val tp = TimePickerDialog(
                context,
                { _, h, m ->
                    timeStr = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                    tvTime.text = timeStr
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
            )
            tp.show()
        }

        // --- DIALOG ---
        val dlg = MaterialAlertDialogBuilder(context)
            .setView(v)
            .create()

        // Cancel
        btnCancel.setOnClickListener { dlg.dismiss() }

        // --- SAVE / UPDATE ---
        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                etTitle.error = "Required"
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

            val list = store.getHabits()

            if (existingHabit != null) {
                val index = list.indexOfFirst { it.id == existingHabit.id }
                if (index != -1) list[index] = newHabit
                Toast.makeText(context, "Habit Updated!", Toast.LENGTH_SHORT).show()
            } else {
                list.add(newHabit)
                Toast.makeText(context, "Habit Added!", Toast.LENGTH_SHORT).show()
            }

            store.saveHabits(list)

            if (newHabit.reminderEnabled && !newHabit.reminder.isNullOrEmpty()) {
                HabitReminderScheduler.schedule(context, newHabit)
            } else {
                HabitReminderScheduler.cancel(context, newHabit.id)
            }

            dlg.dismiss()
            onAddedOrUpdated?.invoke()
        }

        dlg.show()
    }
}
