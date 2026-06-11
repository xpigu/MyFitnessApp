package com.example.myfitnessapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ThemeSettingsActivity : AppCompatActivity() {

    private lateinit var optionSystem: View
    private lateinit var optionLight: View
    private lateinit var optionDark: View
    private lateinit var currentSelectionView: TextView
    private lateinit var applyButton: TextView

    private var selectedMode: AppThemeMode = AppThemeMode.SYSTEM
    private var savedMode: AppThemeMode = AppThemeMode.SYSTEM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_settings)

        initializeViews()
        bindCurrentTheme()
        setupActions()
    }

    private fun initializeViews() {
        optionSystem = findViewById(R.id.option_theme_system)
        optionLight = findViewById(R.id.option_theme_light)
        optionDark = findViewById(R.id.option_theme_dark)
        currentSelectionView = findViewById(R.id.tv_theme_current_selection)
        applyButton = findViewById(R.id.btn_apply_theme)
    }

    private fun bindCurrentTheme() {
        savedMode = SettingsPrefs.getThemeMode(this)
        selectedMode = savedMode
        renderSelectedMode()
        updateApplyButtonState()
    }

    private fun setupActions() {
        findViewById<View>(R.id.btn_theme_back).setOnClickListener { finish() }
        optionSystem.setOnClickListener {
            updateSelectedMode(AppThemeMode.SYSTEM)
        }
        optionLight.setOnClickListener {
            updateSelectedMode(AppThemeMode.LIGHT)
        }
        optionDark.setOnClickListener {
            updateSelectedMode(AppThemeMode.DARK)
        }
        applyButton.setOnClickListener {
            if (selectedMode == savedMode) {
                finish()
                return@setOnClickListener
            }
            SettingsPrefs.updateThemeMode(this, selectedMode)
            ThemeRuntime.notifyThemeChanged(exclude = this)
            showAppFeedback(getString(R.string.theme_settings_applied), FeedbackType.SUCCESS)
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun updateSelectedMode(mode: AppThemeMode) {
        selectedMode = mode
        renderSelectedMode()
        updateApplyButtonState()
    }

    private fun renderSelectedMode() {
        bindOptionSelected(optionSystem, selectedMode == AppThemeMode.SYSTEM)
        bindOptionSelected(optionLight, selectedMode == AppThemeMode.LIGHT)
        bindOptionSelected(optionDark, selectedMode == AppThemeMode.DARK)
        currentSelectionView.text = getString(
            R.string.theme_settings_selected_format,
            themeModeLabel(selectedMode)
        )
    }

    private fun updateApplyButtonState() {
        val enabled = selectedMode != savedMode
        applyButton.isEnabled = enabled
        applyButton.isClickable = enabled
        applyButton.alpha = if (enabled) 1f else 0.6f
    }

    private fun bindOptionSelected(target: View, selected: Boolean) {
        target.setBackgroundResource(
            if (selected) R.drawable.dialog_goal_option_selected_bg else R.drawable.dialog_goal_option_bg
        )
    }

    private fun themeModeLabel(mode: AppThemeMode): String = when (mode) {
        AppThemeMode.SYSTEM -> getString(R.string.theme_mode_system)
        AppThemeMode.LIGHT -> getString(R.string.theme_mode_light)
        AppThemeMode.DARK -> getString(R.string.theme_mode_dark)
    }
}
