package com.example.myfitnessapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = false)
    val id: Long = 1L,

    /** 用户名 */
    @ColumnInfo(name = "username")
    val username: String = "健身达人",

    /** 个人签名/简介 */
    @ColumnInfo(name = "bio")
    val bio: String = "坚持运动，遇见更好的自己",

    /** 性别: MALE, FEMALE, OTHER */
    @ColumnInfo(name = "gender")
    val gender: String = "",

    /** 身高（厘米） */
    @ColumnInfo(name = "height_cm")
    val heightCm: Int = 0,

    /** 体重（公斤） */
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double = 0.0,

    /** 生日（YYYY-MM-DD） */
    @ColumnInfo(name = "birthday")
    val birthday: String = "",

    /** 用户等级 */
    @ColumnInfo(name = "level")
    val level: Int = 1,

    /** 总运动数 */
    @ColumnInfo(name = "total_workouts")
    val totalWorkouts: Int = 0,

    /** 活跃天数 */
    @ColumnInfo(name = "active_days")
    val activeDays: Int = 0,

    /** 头像 URI（本地路径或网址） */
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String = "",

    /** 最后更新时间戳 */
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
