package com.example.myfitnessapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myfitnessapp.data.entity.CustomFood

@Dao
interface CustomFoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: CustomFood): Long

    @Delete
    suspend fun delete(food: CustomFood)

    @Query("SELECT * FROM custom_foods WHERE owner_username = :ownerUsername ORDER BY created_at DESC")
    fun getAllFoods(ownerUsername: String): LiveData<List<CustomFood>>

    @Query("DELETE FROM custom_foods WHERE owner_username = :ownerUsername")
    suspend fun deleteByOwnerUsername(ownerUsername: String)
}
