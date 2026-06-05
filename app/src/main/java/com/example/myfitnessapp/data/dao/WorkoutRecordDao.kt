package com.example.myfitnessapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myfitnessapp.data.entity.WorkoutRecord

@Dao
interface WorkoutRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WorkoutRecord): Long

    @Delete
    suspend fun delete(record: WorkoutRecord)

    @Query("SELECT * FROM workout_records ORDER BY timestamp DESC")
    fun getAllRecords(): LiveData<List<WorkoutRecord>>

    @Query("SELECT * FROM workout_records WHERE date = :date ORDER BY timestamp DESC")
    fun getRecordsByDate(date: String): LiveData<List<WorkoutRecord>>

    @Query("SELECT SUM(total_calories) FROM workout_records WHERE date = :date")
    suspend fun getTotalCaloriesByDate(date: String): Int?

    @Query("SELECT SUM(elapsed_seconds) FROM workout_records WHERE date = :date")
    suspend fun getTotalDurationByDate(date: String): Int?

    @Query("SELECT COUNT(*) FROM workout_records WHERE date = :date")
    suspend fun getWorkoutCountByDate(date: String): Int

    @Query("SELECT * FROM workout_records WHERE date = :date ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecordByDate(date: String): WorkoutRecord?
}