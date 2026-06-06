package com.example.myfitnessapp

import android.content.Context
import java.util.Locale

data class ReminderTime(
    val hour: Int,
    val minute: Int
)

enum class ReminderRepeatMode {
    DAILY,
    WORKDAY
}

data class ReminderItem(
    val enabled: Boolean,
    val times: List<ReminderTime>,
    val repeatMode: ReminderRepeatMode
)

data class ReminderSettings(
    val workout: ReminderItem,
    val water: ReminderItem,
    val checkin: ReminderItem
)

object ReminderPrefs {
    private const val PREFS_NAME = "reminder_prefs"

    private const val KEY_WORKOUT_ENABLED = "workout_enabled"
    private const val KEY_WORKOUT_HOUR = "workout_hour"
    private const val KEY_WORKOUT_MINUTE = "workout_minute"
    private const val KEY_WORKOUT_TIMES = "workout_times"
    private const val KEY_WORKOUT_REPEAT = "workout_repeat"

    private const val KEY_WATER_ENABLED = "water_enabled"
    private const val KEY_WATER_HOUR = "water_hour"
    private const val KEY_WATER_MINUTE = "water_minute"
    private const val KEY_WATER_TIMES = "water_times"
    private const val KEY_WATER_REPEAT = "water_repeat"

    private const val KEY_CHECKIN_ENABLED = "checkin_enabled"
    private const val KEY_CHECKIN_HOUR = "checkin_hour"
    private const val KEY_CHECKIN_MINUTE = "checkin_minute"
    private const val KEY_CHECKIN_TIMES = "checkin_times"
    private const val KEY_CHECKIN_REPEAT = "checkin_repeat"

    fun getSettings(context: Context): ReminderSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ReminderSettings(
            workout = ReminderItem(
                enabled = prefs.getBoolean(KEY_WORKOUT_ENABLED, true),
                times = getTimes(
                    prefs = prefs,
                    listKey = KEY_WORKOUT_TIMES,
                    legacyHourKey = KEY_WORKOUT_HOUR,
                    legacyMinuteKey = KEY_WORKOUT_MINUTE,
                    defaultTime = ReminderTime(19, 30)
                ),
                repeatMode = getRepeatMode(prefs, KEY_WORKOUT_REPEAT)
            ),
            water = ReminderItem(
                enabled = prefs.getBoolean(KEY_WATER_ENABLED, false),
                times = getTimes(
                    prefs = prefs,
                    listKey = KEY_WATER_TIMES,
                    legacyHourKey = KEY_WATER_HOUR,
                    legacyMinuteKey = KEY_WATER_MINUTE,
                    defaultTime = ReminderTime(15, 0)
                ),
                repeatMode = getRepeatMode(prefs, KEY_WATER_REPEAT)
            ),
            checkin = ReminderItem(
                enabled = prefs.getBoolean(KEY_CHECKIN_ENABLED, true),
                times = getTimes(
                    prefs = prefs,
                    listKey = KEY_CHECKIN_TIMES,
                    legacyHourKey = KEY_CHECKIN_HOUR,
                    legacyMinuteKey = KEY_CHECKIN_MINUTE,
                    defaultTime = ReminderTime(21, 0)
                ),
                repeatMode = getRepeatMode(prefs, KEY_CHECKIN_REPEAT)
            )
        )
    }

    fun updateWorkout(
        context: Context,
        enabled: Boolean,
        times: List<ReminderTime>,
        repeatMode: ReminderRepeatMode
    ) {
        updateReminder(context, KEY_WORKOUT_ENABLED, KEY_WORKOUT_TIMES, KEY_WORKOUT_REPEAT, enabled, times, repeatMode)
    }

    fun updateWater(
        context: Context,
        enabled: Boolean,
        times: List<ReminderTime>,
        repeatMode: ReminderRepeatMode
    ) {
        updateReminder(context, KEY_WATER_ENABLED, KEY_WATER_TIMES, KEY_WATER_REPEAT, enabled, times, repeatMode)
    }

    fun updateCheckin(
        context: Context,
        enabled: Boolean,
        times: List<ReminderTime>,
        repeatMode: ReminderRepeatMode
    ) {
        updateReminder(context, KEY_CHECKIN_ENABLED, KEY_CHECKIN_TIMES, KEY_CHECKIN_REPEAT, enabled, times, repeatMode)
    }

    private fun updateReminder(
        context: Context,
        enabledKey: String,
        timesKey: String,
        repeatKey: String,
        enabled: Boolean,
        times: List<ReminderTime>,
        repeatMode: ReminderRepeatMode
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(enabledKey, enabled)
            .putString(timesKey, encodeTimes(times))
            .putString(repeatKey, repeatMode.name)
            .apply()
    }

    fun hasAnyEnabledReminder(context: Context): Boolean {
        val settings = getSettings(context)
        return (settings.workout.enabled && settings.workout.times.isNotEmpty()) ||
            (settings.water.enabled && settings.water.times.isNotEmpty()) ||
            (settings.checkin.enabled && settings.checkin.times.isNotEmpty())
    }

    private fun getTimes(
        prefs: android.content.SharedPreferences,
        listKey: String,
        legacyHourKey: String,
        legacyMinuteKey: String,
        defaultTime: ReminderTime
    ): List<ReminderTime> {
        val saved = prefs.getString(listKey, null)
        if (!saved.isNullOrBlank()) {
            return decodeTimes(saved).ifEmpty { listOf(defaultTime) }
        }

        val hasLegacy = prefs.contains(legacyHourKey) || prefs.contains(legacyMinuteKey)
        if (hasLegacy) {
            return listOf(
                ReminderTime(
                    hour = prefs.getInt(legacyHourKey, defaultTime.hour),
                    minute = prefs.getInt(legacyMinuteKey, defaultTime.minute)
                )
            )
        }

        return listOf(defaultTime)
    }

    private fun encodeTimes(times: List<ReminderTime>): String {
        return times.distinct()
            .sortedWith(compareBy({ it.hour }, { it.minute }))
            .joinToString(",") { time ->
                String.format(Locale.getDefault(), "%02d:%02d", time.hour, time.minute)
            }
    }

    private fun decodeTimes(value: String): List<ReminderTime> {
        return value.split(",")
            .mapNotNull { item ->
                val parts = item.split(":")
                if (parts.size != 2) return@mapNotNull null
                val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
                val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
                ReminderTime(hour, minute)
            }
    }

    private fun getRepeatMode(
        prefs: android.content.SharedPreferences,
        key: String
    ): ReminderRepeatMode {
        return prefs.getString(key, ReminderRepeatMode.DAILY.name)
            ?.let { raw -> ReminderRepeatMode.entries.firstOrNull { it.name == raw } }
            ?: ReminderRepeatMode.DAILY
    }
}
