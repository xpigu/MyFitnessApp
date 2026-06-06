package com.example.myfitnessapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_checkins")
data class DailyCheckin(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String, // 签到日期，格式：YYYY-MM-DD

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(), // 签到时间戳

    @ColumnInfo(name = "streak_count")
    val streakCount: Int // 当前连续签到天数
)
