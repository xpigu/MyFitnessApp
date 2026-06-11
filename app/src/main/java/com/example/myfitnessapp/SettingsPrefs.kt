package com.example.myfitnessapp

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import java.util.WeakHashMap

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
    private const val KEY_LAST_RENDERED_LEVEL = "last_rendered_level"
    private const val KEY_RENDERED_BADGE_IDS = "rendered_badge_ids"

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
        val nightMode = if (themeMode == AppThemeMode.SYSTEM && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        } else {
            themeMode.nightMode
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
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

    fun getLastRenderedLevel(context: Context, username: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("${KEY_LAST_RENDERED_LEVEL}_$username", -1)
    }

    fun updateLastRenderedLevel(context: Context, username: String, level: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("${KEY_LAST_RENDERED_LEVEL}_$username", level)
            .apply()
    }

    fun shouldShowLevelUp(context: Context, username: String, level: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_LAST_RENDERED_LEVEL}_$username"
        val previousLevel = prefs.getInt(key, -1)

        if (previousLevel == -1) {
            prefs.edit().putInt(key, level).apply()
            return false
        }

        if (level <= previousLevel) {
            return false
        }

        prefs.edit().putInt(key, level).apply()
        return true
    }

    fun consumeNewUnlockedBadgeIds(
        context: Context,
        username: String,
        unlockedBadgeIds: Set<String>
    ): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_RENDERED_BADGE_IDS}_$username"
        val renderedIds = prefs.getStringSet(key, emptySet()).orEmpty().toSet()

        if (renderedIds.isEmpty()) {
            prefs.edit().putStringSet(key, unlockedBadgeIds).apply()
            return emptySet()
        }

        val newUnlockedIds = unlockedBadgeIds - renderedIds
        if (newUnlockedIds.isNotEmpty()) {
            prefs.edit().putStringSet(key, renderedIds + unlockedBadgeIds).apply()
        }
        return newUnlockedIds
    }
}

object ThemeRuntime {
    private val handler = Handler(Looper.getMainLooper())
    private val activityThemeVersions = WeakHashMap<Activity, Int>()
    private var themeVersion: Int = 0

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activityThemeVersions[activity] = themeVersion
            }

            override fun onActivityResumed(activity: Activity) {
                if (activity is ThemeSettingsActivity) return
                val lastVersion = activityThemeVersions[activity] ?: -1
                if (lastVersion != themeVersion) {
                    activityThemeVersions[activity] = themeVersion
                    handler.post {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            activity.recreate()
                        }
                    }
                }
            }

            override fun onActivityDestroyed(activity: Activity) {
                activityThemeVersions.remove(activity)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })
    }

    fun notifyThemeChanged(exclude: Activity? = null) {
        themeVersion += 1
        val snapshot = activityThemeVersions.keys.toList()
        handler.post {
            snapshot.forEach { activity ->
                if (activity == exclude) return@forEach
                if (activity.isFinishing || activity.isDestroyed) return@forEach
                activityThemeVersions[activity] = themeVersion
                activity.recreate()
            }
        }
    }
}
