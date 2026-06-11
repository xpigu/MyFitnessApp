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

    @Query("SELECT * FROM achievement_badges WHERE owner_username = :ownerUsername ORDER BY is_unlocked DESC, unlocked_date DESC, category ASC, id ASC")
    fun getAllBadges(ownerUsername: String): LiveData<List<AchievementBadge>>

    @Query("SELECT * FROM achievement_badges WHERE owner_username = :ownerUsername AND is_unlocked = 1 ORDER BY unlocked_date DESC")
    fun getUnlockedBadges(ownerUsername: String): LiveData<List<AchievementBadge>>

    @Query("SELECT * FROM achievement_badges WHERE owner_username = :ownerUsername AND id = :id")
    suspend fun getBadgeById(ownerUsername: String, id: String): AchievementBadge?

    @Query("SELECT COUNT(*) FROM achievement_badges WHERE owner_username = :ownerUsername AND is_unlocked = 1")
    fun getUnlockedCount(ownerUsername: String): LiveData<Int>

    @Query("DELETE FROM achievement_badges WHERE owner_username = :ownerUsername")
    suspend fun deleteByOwnerUsername(ownerUsername: String)
}
