package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.WorkoutRecordDao
import com.example.myfitnessapp.data.dao.TypeCount
import com.example.myfitnessapp.data.entity.WorkoutRecord

class WorkoutRecordRepository(private val dao: WorkoutRecordDao) {

    val allRecords: LiveData<List<WorkoutRecord>> = dao.getAllRecords()
    val allDates: LiveData<List<String>> = dao.getAllDates()

    suspend fun insert(record: WorkoutRecord): Long = dao.insert(record)

    suspend fun delete(record: WorkoutRecord) = dao.delete(record)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    fun getRecordsByDate(date: String): LiveData<List<WorkoutRecord>> = dao.getRecordsByDate(date)

    suspend fun getTotalCaloriesByDate(date: String): Int = dao.getTotalCaloriesByDate(date)

    suspend fun getTotalDurationByDate(date: String): Int = dao.getTotalDurationByDate(date)

    suspend fun getWorkoutCountByDate(date: String): Int = dao.getWorkoutCountByDate(date)

    suspend fun getTotalCaloriesByMonth(monthPattern: String): Int = dao.getTotalCaloriesByMonth(monthPattern)

    suspend fun getTotalDurationByMonth(monthPattern: String): Int = dao.getTotalDurationByMonth(monthPattern)

    suspend fun getWorkoutCountByMonth(monthPattern: String): Int = dao.getWorkoutCountByMonth(monthPattern)

    suspend fun getMonthlyTypeStats(monthPattern: String): List<TypeCount> = dao.getMonthlyTypeStats(monthPattern)

    suspend fun getDailyTypeStats(date: String): List<TypeCount> = dao.getDailyTypeStats(date)

    suspend fun getTotalRecordCount(): Int = dao.getTotalRecordCount()
}
