package com.example.myfitnessapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diet_records")
data class DietRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 所属账号用户名 */
    @ColumnInfo(name = "owner_username")
    val ownerUsername: String = "",

    /** 膳食类型: BREAKFAST, LUNCH, DINNER, SNACK */
    @ColumnInfo(name = "meal_type")
    val mealType: String,

    /** 食物名称 */
    @ColumnInfo(name = "food_name")
    val foodName: String,

    /** 卡路里数值 */
    @ColumnInfo(name = "calories")
    val calories: Int,

    /** 记录时间戳（毫秒） */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /** 日期字符串 yyyy-MM-dd */
    @ColumnInfo(name = "date")
    val date: String,

    /** 备注/描述 */
    @ColumnInfo(name = "notes")
    val notes: String = ""
)
