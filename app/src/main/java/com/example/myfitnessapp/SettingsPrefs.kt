package com.example.myfitnessapp

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

enum class AppThemeMode(val storageValue: String, val nightMode: Int) {
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        fun fromStorage(value: String?): AppThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}

data class PrivacySettings(
    val allowLocalAvatarAccess: Boolean,
    val showBodyMetrics: Boolean,
    val enablePersonalizedInsights: Boolean,
    val allowAnonymousUsageStats: Boolean
)

object SettingsPrefs {
    private const val PREFS_NAME = "app_settings_prefs"

    private const val KEY_THEME_MODE = "theme_mode"

    private const val KEY_ALLOW_LOCAL_AVATAR_ACCESS = "allow_local_avatar_access"
    private const val KEY_SHOW_BODY_METRICS = "show_body_metrics"
    private const val KEY_ENABLE_PERSONALIZED_INSIGHTS = "enable_personalized_insights"
    private const val KEY_ALLOW_ANONYMOUS_USAGE_STATS = "allow_anonymous_usage_stats"

    fun getThemeMode(context: Context): AppThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppThemeMode.fromStorage(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.storageValue))
    }

    fun updateThemeMode(context: Context, themeMode: AppThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, themeMode.storageValue)
            .apply()
        applyTheme(themeMode)
    }

    fun applySavedTheme(context: Context) {
        applyTheme(getThemeMode(context))
    }

    private fun applyTheme(themeMode: AppThemeMode) {
        AppCompatDelegate.setDefaultNightMode(themeMode.nightMode)
    }

    fun getPrivacySettings(context: Context): PrivacySettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return PrivacySettings(
            allowLocalAvatarAccess = prefs.getBoolean(KEY_ALLOW_LOCAL_AVATAR_ACCESS, true),
            showBodyMetrics = prefs.getBoolean(KEY_SHOW_BODY_METRICS, true),
            enablePersonalizedInsights = prefs.getBoolean(KEY_ENABLE_PERSONALIZED_INSIGHTS, true),
            allowAnonymousUsageStats = prefs.getBoolean(KEY_ALLOW_ANONYMOUS_USAGE_STATS, false)
        )
    }

    fun updatePrivacySettings(context: Context, settings: PrivacySettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ALLOW_LOCAL_AVATAR_ACCESS, settings.allowLocalAvatarAccess)
            .putBoolean(KEY_SHOW_BODY_METRICS, settings.showBodyMetrics)
            .putBoolean(KEY_ENABLE_PERSONALIZED_INSIGHTS, settings.enablePersonalizedInsights)
            .putBoolean(KEY_ALLOW_ANONYMOUS_USAGE_STATS, settings.allowAnonymousUsageStats)
            .apply()
    }
}
