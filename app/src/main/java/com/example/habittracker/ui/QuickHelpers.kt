package com.example.habittracker.ui

import android.content.Context
import android.widget.Toast

object HydrationReminder {
    fun schedule(ctx: Context) {
        Toast.makeText(ctx, "Hydration reminder coming soon!", Toast.LENGTH_SHORT).show()
    }
}

object MoodQuickDialog {
    fun show(ctx: Context) {
        Toast.makeText(ctx, "Open mood dialog here!", Toast.LENGTH_SHORT).show()
    }
}
