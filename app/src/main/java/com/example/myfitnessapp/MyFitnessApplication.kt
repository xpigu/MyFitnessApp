package com.example.myfitnessapp

import android.app.Application
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer

class MyFitnessApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsPrefs.applySavedTheme(this)
        ThemeRuntime.init(this)
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
    }
}
