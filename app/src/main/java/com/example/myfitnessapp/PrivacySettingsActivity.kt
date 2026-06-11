package com.example.myfitnessapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class PrivacySettingsActivity : AppCompatActivity() {

    private lateinit var switchLocalAvatar: SwitchCompat
    private lateinit var switchBodyMetrics: SwitchCompat
    private lateinit var switchInsights: SwitchCompat
    private lateinit var switchAnalytics: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_settings)

        initializeViews()
        bindSettings()
        setupActions()
    }

    private fun initializeViews() {
        switchLocalAvatar = findViewById(R.id.switch_privacy_local_avatar)
        switchBodyMetrics = findViewById(R.id.switch_privacy_body_metrics)
        switchInsights = findViewById(R.id.switch_privacy_insights)
        switchAnalytics = findViewById(R.id.switch_privacy_analytics)
    }

    private fun bindSettings() {
        val settings = SettingsPrefs.getPrivacySettings(this)
        switchLocalAvatar.isChecked = settings.allowLocalAvatarAccess
        switchBodyMetrics.isChecked = settings.showBodyMetrics
        switchInsights.isChecked = settings.enablePersonalizedInsights
        switchAnalytics.isChecked = settings.allowAnonymousUsageStats
    }

    private fun setupActions() {
        findViewById<View>(R.id.btn_privacy_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_save_privacy_settings).setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        SettingsPrefs.updatePrivacySettings(
            context = this,
            settings = PrivacySettings(
                allowLocalAvatarAccess = switchLocalAvatar.isChecked,
                showBodyMetrics = switchBodyMetrics.isChecked,
                enablePersonalizedInsights = switchInsights.isChecked,
                allowAnonymousUsageStats = switchAnalytics.isChecked
            )
        )
        showAppFeedback(getString(R.string.privacy_settings_saved), FeedbackType.SUCCESS)
    }
}
