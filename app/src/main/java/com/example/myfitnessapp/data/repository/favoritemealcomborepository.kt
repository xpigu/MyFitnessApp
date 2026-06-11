package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.FavoriteMealComboDao
import com.example.myfitnessapp.data.entity.FavoriteMealCombo

class FavoriteMealComboRepository(
    private val dao: FavoriteMealComboDao,
    private val ownerUsername: String
) {

    val allCombos: LiveData<List<FavoriteMealCombo>> = dao.getAllCombos(ownerUsername)

    suspend fun insert(combo: FavoriteMealCombo): Long = dao.insert(combo.copy(ownerUsername = ownerUsername))

    suspend fun delete(combo: FavoriteMealCombo) = dao.delete(combo)
}
