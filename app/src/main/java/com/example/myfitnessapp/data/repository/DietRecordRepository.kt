package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.DietRecordDao
import com.example.myfitnessapp.data.dao.MealCalorieStats
import com.example.myfitnessapp.data.entity.DietRecord

class DietRecordRepository(
    private val dao: DietRecordDao,
    private val ownerUsername: String
) {

    val allRecords: LiveData<List<DietRecord>> = dao.getAllRecords(ownerUsername)

    suspend fun insert(record: DietRecord): Long = dao.insert(record.copy(ownerUsername = ownerUsername))

    suspend fun delete(record: DietRecord) = dao.delete(record)

    suspend fun deleteById(id: Long) = dao.deleteById(ownerUsername, id)

    fun getRecordsByDate(date: String): LiveData<List<DietRecord>> = dao.getRecordsByDate(ownerUsername, date)

    suspend fun getTotalCaloriesByDate(date: String): Int = dao.getTotalCaloriesByDate(ownerUsername, date)

    suspend fun getTotalCaloriesByMonth(monthPattern: String): Int =
        dao.getTotalCaloriesByMonth(ownerUsername, monthPattern)

    suspend fun getTotalRecordCount(): Int = dao.getTotalRecordCount(ownerUsername)

    suspend fun getDailyMealStats(date: String): List<MealCalorieStats> =
        dao.getDailyMealStats(ownerUsername, date)
}
