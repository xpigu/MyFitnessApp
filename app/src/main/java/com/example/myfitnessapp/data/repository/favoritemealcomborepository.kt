package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.FavoriteMealComboDao
import com.example.myfitnessapp.data.entity.FavoriteMealCombo

class FavoriteMealComboRepository(private val dao: FavoriteMealComboDao) {

    val allCombos: LiveData<List<FavoriteMealCombo>> = dao.getAllCombos()

    suspend fun insert(combo: FavoriteMealCombo): Long = dao.insert(combo)

    suspend fun delete(combo: FavoriteMealCombo) = dao.delete(combo)
}
