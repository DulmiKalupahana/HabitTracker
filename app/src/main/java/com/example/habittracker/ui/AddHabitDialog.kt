package com.example.habittracker.ui

import android.app.*
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.example.habittracker.R
import com.example.habittracker.data.Habit
import com.example.habittracker.data.PrefStore
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.*

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
        val btnSave = v.findViewById<View>(R.id.btnSave)
        val btnCancel = v.findViewById<View>(R.id.btnCancel)
        val cbAllDay = v.findViewById<CheckBox>(R.id.cbAllDay)

        // Default values
        var repeat = "Daily"
        var dateStr = tvHabitDate.text.toString()
        var timeStr = tvTime.text.toString()

        // --- EDIT MODE PREFILL ---
        if (existingHabit != null) {
            etTitle.setText(existingHabit.title ?: "")
            tvHabitDate.text = existingHabit.date ?: ""
            swReminder.isChecked = existingHabit.reminder != null
            tvTime.text = existingHabit.reminder ?: ""
            repeat = existingHabit.repeat ?: "Daily"
            v.findViewById<TextView>(R.id.tvDialogTitle).text = "Edit Habit"

            (btnSave as Button).text = "Update"

            when (repeat) {
                "Daily" -> tgRepeat.check(R.id.btnRepeatDaily)
                "Weekly" -> tgRepeat.check(R.id.btnRepeatWeekly)
                "Monthly" -> tgRepeat.check(R.id.btnRepeatMonthly)
            }

            layoutDaily.visibility = if (repeat == "Daily") View.VISIBLE else View.GONE
            layoutWeekly.visibility = if (repeat == "Weekly") View.VISIBLE else View.GONE
            layoutMonthly.visibility = if (repeat == "Monthly") View.VISIBLE else View.GONE
        }

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
        val dlg = AlertDialog.Builder(context)
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
                color = existingHabit?.color ?: "#8B88F8",
                repeat = repeat,
                date = dateStr,
                reminder = if (swReminder.isChecked) timeStr else null
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
            dlg.dismiss()
            onAddedOrUpdated?.invoke()
        }

        dlg.show()
    }
}
