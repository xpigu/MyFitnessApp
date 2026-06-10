package com.example.myfitnessapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ThemeSettingsActivity : AppCompatActivity() {

    private lateinit var optionSystem: View
    private lateinit var optionLight: View
    private lateinit var optionDark: View
    private lateinit var currentSelectionView: TextView

    private var selectedMode: AppThemeMode = AppThemeMode.SYSTEM

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
    }

    private fun bindCurrentTheme() {
        selectedMode = SettingsPrefs.getThemeMode(this)
        renderSelectedMode()
    }

    private fun setupActions() {
        findViewById<View>(R.id.btn_theme_back).setOnClickListener { finish() }
        optionSystem.setOnClickListener {
            selectedMode = AppThemeMode.SYSTEM
            renderSelectedMode()
        }
        optionLight.setOnClickListener {
            selectedMode = AppThemeMode.LIGHT
            renderSelectedMode()
        }
        optionDark.setOnClickListener {
            selectedMode = AppThemeMode.DARK
            renderSelectedMode()
        }
        findViewById<View>(R.id.btn_apply_theme).setOnClickListener {
            SettingsPrefs.updateThemeMode(this, selectedMode)
            Toast.makeText(this, R.string.theme_settings_applied, Toast.LENGTH_SHORT).show()
            recreate()
        }
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
