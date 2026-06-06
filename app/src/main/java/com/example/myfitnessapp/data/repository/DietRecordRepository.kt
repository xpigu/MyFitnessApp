package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.DietRecordDao
import com.example.myfitnessapp.data.dao.MealCalorieStats
import com.example.myfitnessapp.data.entity.DietRecord

class DietRecordRepository(private val dao: DietRecordDao) {

    val allRecords: LiveData<List<DietRecord>> = dao.getAllRecords()

    suspend fun insert(record: DietRecord): Long = dao.insert(record)

    suspend fun delete(record: DietRecord) = dao.delete(record)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    fun getRecordsByDate(date: String): LiveData<List<DietRecord>> = dao.getRecordsByDate(date)

    suspend fun getTotalCaloriesByDate(date: String): Int = dao.getTotalCaloriesByDate(date)

    suspend fun getTotalCaloriesByMonth(monthPattern: String): Int = dao.getTotalCaloriesByMonth(monthPattern)

    suspend fun getTotalRecordCount(): Int = dao.getTotalRecordCount()

    suspend fun getDailyMealStats(date: String): List<MealCalorieStats> = dao.getDailyMealStats(date)
}
