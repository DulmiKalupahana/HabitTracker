package com.example.habittracker.ui

import android.app.*
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.example.habittracker.R
import com.example.habittracker.data.Habit
import com.example.habittracker.data.PrefStore
import java.util.*

// Dialog for adding new habits with title input
object AddHabitDialog {

    fun show(context: Context, onAdded: (() -> Unit)? = null) {
        val store = PrefStore(context)
        val v: View = LayoutInflater.from(context).inflate(R.layout.dialog_add_habit, null)

        val etTitle = v.findViewById<EditText>(R.id.etTitle)
        val dateTv = v.findViewById<TextView>(R.id.tvDate)
        val timeTv = v.findViewById<TextView>(R.id.tvTime)
        val swReminder = v.findViewById<Switch>(R.id.swReminder)

        var selectedColor = "#8B88F8"
        var repeat = "Daily"
        var dateStr = dateTv.text.toString()
        var timeStr = timeTv.text.toString()

        // Color selection
        val colorGrid = v.findViewById<GridLayout>(R.id.colorGrid)
        for (i in 0 until colorGrid.childCount) {
            val swatch = colorGrid.getChildAt(i)
            swatch.setOnClickListener {
                selectedColor = String.format(
                    "#%06X",
                    0xFFFFFF and (swatch.backgroundTintList?.defaultColor ?: 0)
                )
                for (j in 0 until colorGrid.childCount)
                    colorGrid.getChildAt(j).scaleX = 1f
                swatch.scaleX = 1.2f
                swatch.scaleY = 1.2f
            }
        }

        // Repeat selection
        val tgRepeat = v.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.tgRepeat)
        tgRepeat.addOnButtonCheckedListener { _, checkedId, _ ->
            repeat = when (checkedId) {
                R.id.btnDaily -> "Daily"
                R.id.btnWeekly -> "Weekly"
                R.id.btnMonthly -> "Monthly"
                else -> "Daily"
            }
        }

        // Date picker
        v.findViewById<View>(R.id.btnPickDate).setOnClickListener {
            val c = Calendar.getInstance()
            val dp = DatePickerDialog(
                context,
                { _, y, m, d -> dateStr = "$d/${m + 1}/$y"; dateTv.text = dateStr },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            )
            dp.show()
        }

        // Time picker
        timeTv.setOnClickListener {
            val c = Calendar.getInstance()
            val tp = TimePickerDialog(
                context,
                { _, h, m ->
                    timeStr = String.format("%02d:%02d", h, m)
                    timeTv.text = timeStr
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
            )
            tp.show()
        }

        // Dialog
        val dlg = AlertDialog.Builder(context)
            .setView(v)
            .create()

        v.findViewById<View>(R.id.btnCancel).setOnClickListener { dlg.dismiss() }

        v.findViewById<View>(R.id.btnSave).setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                etTitle.error = "Required"
                return@setOnClickListener
            }

            val habit = Habit(
                id = UUID.randomUUID().toString(),
                title = title,
                color = selectedColor,
                repeat = repeat,
                date = dateStr,
                reminder = if (swReminder.isChecked) timeStr else null
            )

            val list = store.getHabits()
            list.add(habit)
            store.saveHabits(list)

            Toast.makeText(context, "Habit Added!", Toast.LENGTH_SHORT).show()
            dlg.dismiss()
            onAdded?.invoke()
        }

        dlg.show()
    }
}
