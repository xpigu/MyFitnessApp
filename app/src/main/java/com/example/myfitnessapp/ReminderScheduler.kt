package com.example.myfitnessapp

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

object ReminderScheduler {
    const val CHANNEL_ID = "fitness_reminders"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_REPEAT_MODE = "extra_repeat_mode"
    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
    const val EXTRA_FROM_REMINDER = "extra_from_reminder"

    private const val BASE_REQ_WORKOUT = 2000
    private const val BASE_REQ_WATER = 3000
    private const val BASE_REQ_CHECKIN = 4000
    private const val MAX_REMINDER_SLOTS = 8
    private const val NOTIFY_TEST = 2999

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.reminder_channel_desc)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun rescheduleAll(context: Context) {
        createChannel(context)
        val settings = ReminderPrefs.getSettings(context)

        scheduleReminder(
            context = context,
            baseRequestCode = BASE_REQ_WORKOUT,
            enabled = settings.workout.enabled,
            times = settings.workout.times,
            repeatMode = settings.workout.repeatMode,
            type = ReminderType.WORKOUT
        )
        scheduleReminder(
            context = context,
            baseRequestCode = BASE_REQ_WATER,
            enabled = settings.water.enabled,
            times = settings.water.times,
            repeatMode = settings.water.repeatMode,
            type = ReminderType.WATER
        )
        scheduleReminder(
            context = context,
            baseRequestCode = BASE_REQ_CHECKIN,
            enabled = settings.checkin.enabled,
            times = settings.checkin.times,
            repeatMode = settings.checkin.repeatMode,
            type = ReminderType.CHECKIN
        )
    }

    fun showTestNotification(context: Context) {
        createChannel(context)
        if (!canPostNotifications(context)) return

        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFY_TEST,
            Intent(context, ReminderSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_test_title))
            .setContentText(context.getString(R.string.reminder_test_message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFY_TEST, notification)
    }

    fun showReminderNotification(
        context: Context,
        notificationId: Int,
        type: ReminderType
    ) {
        createChannel(context)
        if (!canPostNotifications(context)) return

        val titleResId = when (type) {
            ReminderType.WORKOUT -> R.string.reminder_workout_title
            ReminderType.WATER -> R.string.reminder_water_title
            ReminderType.CHECKIN -> R.string.reminder_checkin_title
        }
        val messageResId = when (type) {
            ReminderType.WORKOUT -> R.string.reminder_workout_message
            ReminderType.WATER -> R.string.reminder_water_message
            ReminderType.CHECKIN -> R.string.reminder_checkin_message
        }
        val title = context.getString(titleResId)
        val message = context.getString(messageResId)

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            buildTargetIntent(context, type).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun scheduleReminder(
        context: Context,
        baseRequestCode: Int,
        enabled: Boolean,
        times: List<ReminderTime>,
        repeatMode: ReminderRepeatMode,
        type: ReminderType
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        clearReminderGroup(alarmManager, context, baseRequestCode)
        if (!enabled) return

        times.distinct()
            .sortedWith(compareBy({ it.hour }, { it.minute }))
            .take(MAX_REMINDER_SLOTS)
            .forEachIndexed { index, time ->
                val requestCode = baseRequestCode + index
                val pendingIntent = buildReminderPendingIntent(
                    context = context,
                    requestCode = requestCode,
                    repeatMode = repeatMode,
                    type = type
                )
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, time.hour)
                    set(Calendar.MINUTE, time.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
    }

    private fun clearReminderGroup(
        alarmManager: AlarmManager,
        context: Context,
        baseRequestCode: Int
    ) {
        repeat(MAX_REMINDER_SLOTS) { index ->
            val requestCode = baseRequestCode + index
            val pendingIntent = buildReminderPendingIntent(
                context = context,
                requestCode = requestCode,
                repeatMode = ReminderRepeatMode.DAILY,
                type = ReminderType.WORKOUT
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun buildReminderPendingIntent(
        context: Context,
        requestCode: Int,
        repeatMode: ReminderRepeatMode,
        type: ReminderType
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EXTRA_REPEAT_MODE, repeatMode.name)
            putExtra(EXTRA_REMINDER_TYPE, type.name)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun shouldDeliverToday(repeatModeRaw: String?): Boolean {
        val repeatMode = repeatModeRaw
            ?.let { raw -> ReminderRepeatMode.entries.firstOrNull { it.name == raw } }
            ?: ReminderRepeatMode.DAILY
        if (repeatMode == ReminderRepeatMode.DAILY) return true

        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
    }

    private fun buildTargetIntent(context: Context, type: ReminderType): Intent {
        return when (type) {
            ReminderType.WORKOUT -> Intent(context, TrainingActivity::class.java)
            ReminderType.WATER -> Intent(context, MainActivity::class.java)
            ReminderType.CHECKIN -> Intent(context, DailyCheckinActivity::class.java)
        }.apply {
            putExtra(EXTRA_FROM_REMINDER, true)
            putExtra(EXTRA_REMINDER_TYPE, type.name)
        }
    }
}
