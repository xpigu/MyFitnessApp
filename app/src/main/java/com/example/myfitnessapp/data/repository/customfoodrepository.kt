package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.CustomFoodDao
import com.example.myfitnessapp.data.entity.CustomFood

class CustomFoodRepository(
    private val dao: CustomFoodDao,
    private val ownerUsername: String
) {

    val allFoods: LiveData<List<CustomFood>> = dao.getAllFoods(ownerUsername)

    suspend fun insert(food: CustomFood): Long = dao.insert(food.copy(ownerUsername = ownerUsername))

    suspend fun delete(food: CustomFood) = dao.delete(food)
}
