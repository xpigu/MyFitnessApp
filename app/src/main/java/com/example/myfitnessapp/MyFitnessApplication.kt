package com.example.myfitnessapp

import android.app.Application

class MyFitnessApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsPrefs.applySavedTheme(this)
    }
}
