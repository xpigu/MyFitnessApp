package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.CustomFoodDao
import com.example.myfitnessapp.data.entity.CustomFood

class CustomFoodRepository(private val dao: CustomFoodDao) {

    val allFoods: LiveData<List<CustomFood>> = dao.getAllFoods()

    suspend fun insert(food: CustomFood): Long = dao.insert(food)

    suspend fun delete(food: CustomFood) = dao.delete(food)
}
