package com.example.habittracker.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.example.habittracker.R
import com.example.habittracker.data.Habit
import com.example.habittracker.data.PrefStore
import java.util.*

object AddHabitDialog {
    fun show(context: Context, onAdded: (() -> Unit)? = null) {
        val store = PrefStore(context)
        val v: View = LayoutInflater.from(context).inflate(R.layout.dialog_add_habit, null)

        val etTitle = v.findViewById<EditText>(R.id.etTitle)
        val datePicker = v.findViewById<TextView>(R.id.tvDate)
        val timePicker = v.findViewById<TextView>(R.id.tvTime)

        val dlg = AlertDialog.Builder(context)
            .setView(v)
            .create()

        // Pick Date
        datePicker.setOnClickListener {
            val c = Calendar.getInstance()
            val dp = DatePickerDialog(
                context,
                { _, y, m, d -> datePicker.text = "$d/${m + 1}/$y" },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            )
            dp.show()
        }

        // Pick Time
        timePicker.setOnClickListener {
            val c = Calendar.getInstance()
            val tp = TimePickerDialog(
                context,
                { _, h, m ->
                    val formatted = String.format("%02d:%02d", h, m)
                    timePicker.text = formatted
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
            )
            tp.show()
        }

        // Cancel button
        v.findViewById<View>(R.id.btnCancel).setOnClickListener { dlg.dismiss() }

        // Save button
        v.findViewById<View>(R.id.btnSave).setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                etTitle.error = "Required"
                return@setOnClickListener
            }

            val habit = Habit(
                id = UUID.randomUUID().toString(),
                title = title
            )
            val list = store.getHabits()
            list.add(habit)
            store.saveHabits(list)

            Toast.makeText(context, "Habit added!", Toast.LENGTH_SHORT).show()
            dlg.dismiss()
            onAdded?.invoke()
        }

        dlg.show()
    }
}
