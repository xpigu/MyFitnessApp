package com.example.myfitnessapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myfitnessapp.data.entity.AchievementBadge

@Dao
interface AchievementBadgeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadges(badges: List<AchievementBadge>)

    @Update
    suspend fun updateBadge(badge: AchievementBadge)

    @Query("SELECT * FROM achievement_badges")
    fun getAllBadges(): LiveData<List<AchievementBadge>>

    @Query("SELECT * FROM achievement_badges WHERE is_unlocked = 1 ORDER BY unlocked_date DESC")
    fun getUnlockedBadges(): LiveData<List<AchievementBadge>>

    @Query("SELECT * FROM achievement_badges WHERE id = :id")
    suspend fun getBadgeById(id: String): AchievementBadge?

    @Query("SELECT COUNT(*) FROM achievement_badges WHERE is_unlocked = 1")
    fun getUnlockedCount(): LiveData<Int>
}
