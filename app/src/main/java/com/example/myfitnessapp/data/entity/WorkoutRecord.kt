package com.example.myfitnessapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sport_type")
    val sportType: String,

    @ColumnInfo(name = "sport_icon_res_id")
    val sportIconResId: Int,

    @ColumnInfo(name = "elapsed_seconds")
    val elapsedSeconds: Int,

    @ColumnInfo(name = "total_distance")
    val totalDistance: Double,

    @ColumnInfo(name = "total_calories")
    val totalCalories: Int,

    @ColumnInfo(name = "heart_rate")
    val heartRate: Int,

    @ColumnInfo(name = "pace")
    val pace: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "date")
    val date: String
)