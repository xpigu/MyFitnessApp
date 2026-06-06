package com.example.myfitnessapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievement_badges")
data class AchievementBadge(
    @PrimaryKey
    val id: String, // 徽章唯一标识，如 "first_workout", "10k_steps"

    @ColumnInfo(name = "name")
    val name: String, // 徽章名称

    @ColumnInfo(name = "description")
    val description: String, // 徽章描述

    @ColumnInfo(name = "icon_res_id")
    val iconResId: Int, // 徽章图标资源 ID

    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean = false, // 是否已解锁

    @ColumnInfo(name = "unlocked_date")
    val unlockedDate: Long? = null, // 解锁时间戳

    @ColumnInfo(name = "category")
    val category: String // 徽章分类：WORKOUT, DIET, STREAK
)
