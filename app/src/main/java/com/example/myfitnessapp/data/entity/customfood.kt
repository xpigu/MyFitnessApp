package com.example.myfitnessapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_foods")
data class CustomFood(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "calories_per_100g")
    val caloriesPer100g: Int,

    @ColumnInfo(name = "protein_per_100g")
    val proteinPer100g: Double,

    @ColumnInfo(name = "carbs_per_100g")
    val carbsPer100g: Double,

    @ColumnInfo(name = "fat_per_100g")
    val fatPer100g: Double,

    @ColumnInfo(name = "meal_type")
    val mealType: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
