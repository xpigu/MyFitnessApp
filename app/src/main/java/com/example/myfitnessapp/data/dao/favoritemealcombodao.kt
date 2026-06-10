package com.example.myfitnessapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myfitnessapp.data.entity.FavoriteMealCombo

@Dao
interface FavoriteMealComboDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(combo: FavoriteMealCombo): Long

    @Delete
    suspend fun delete(combo: FavoriteMealCombo)

    @Query("SELECT * FROM favorite_meal_combos ORDER BY created_at DESC")
    fun getAllCombos(): LiveData<List<FavoriteMealCombo>>
}
