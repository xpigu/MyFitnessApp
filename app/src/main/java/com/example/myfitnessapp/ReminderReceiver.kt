package com.example.myfitnessapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ReminderScheduler.shouldDeliverToday(intent.getStringExtra(ReminderScheduler.EXTRA_REPEAT_MODE))) {
            return
        }

        val notificationId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 0)
        val reminderType = ReminderType.from(
            intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TYPE)
        )

        ReminderScheduler.showReminderNotification(context, notificationId, reminderType)
    }
}
