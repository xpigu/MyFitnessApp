package com.example.myfitnessapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_meal_combos")
data class FavoriteMealCombo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "meal_type")
    val mealType: String,

    @ColumnInfo(name = "items_payload")
    val itemsPayload: String,

    @ColumnInfo(name = "total_calories")
    val totalCalories: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
