package com.example.myfitnessapp

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        bindVersionInfo()
        setupActions()
    }

    private fun bindVersionInfo() {
        findViewById<TextView>(R.id.tv_about_version).text =
            getString(R.string.about_version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        findViewById<TextView>(R.id.tv_about_theme_status).text =
            getString(R.string.about_theme_status_format, currentThemeStatusLabel())
    }

    private fun setupActions() {
        findViewById<View>(R.id.btn_about_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_about_check_update).setOnClickListener {
            showAppFeedback(getString(R.string.about_latest_toast), FeedbackType.INFO)
        }
    }

    private fun currentThemeStatusLabel(): String {
        return when (SettingsPrefs.getThemeMode(this)) {
            AppThemeMode.SYSTEM -> {
                val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
                getString(if (isDark) R.string.about_theme_system_dark else R.string.about_theme_system_light)
            }
            AppThemeMode.LIGHT -> getString(R.string.about_theme_light_active)
            AppThemeMode.DARK -> getString(R.string.about_theme_dark_active)
        }
    }
}
