package com.example.habittracker

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import com.example.habittracker.notify.NotifyUtils

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        NotifyUtils.ensureChannel(this)
    }
}
