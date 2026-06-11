package com.example.myfitnessapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 0)
        val repeatMode = intent.getStringExtra(ReminderScheduler.EXTRA_REPEAT_MODE)
        val reminderType = ReminderType.from(
            intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TYPE)
        )
        val hour = intent.getIntExtra(ReminderScheduler.EXTRA_REMINDER_HOUR, 0)
        val minute = intent.getIntExtra(ReminderScheduler.EXTRA_REMINDER_MINUTE, 0)

        ReminderScheduler.handleReminderTrigger(
            context = context,
            notificationId = notificationId,
            repeatModeRaw = repeatMode,
            type = reminderType,
            hour = hour,
            minute = minute
        )
    }
}
