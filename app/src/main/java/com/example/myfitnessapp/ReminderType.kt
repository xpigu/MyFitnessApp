package com.example.myfitnessapp

enum class ReminderType {
    WORKOUT,
    WATER,
    CHECKIN;

    companion object {
        fun from(raw: String?): ReminderType {
            return entries.firstOrNull { it.name == raw } ?: WORKOUT
        }
    }
}
