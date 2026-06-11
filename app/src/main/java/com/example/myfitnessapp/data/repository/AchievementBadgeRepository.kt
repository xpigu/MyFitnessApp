package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.AchievementBadgeDao
import com.example.myfitnessapp.data.entity.AchievementBadge

class AchievementBadgeRepository(
    private val badgeDao: AchievementBadgeDao,
    private val ownerUsername: String
) {

    val allBadges: LiveData<List<AchievementBadge>> = badgeDao.getAllBadges(ownerUsername)
    val unlockedBadges: LiveData<List<AchievementBadge>> = badgeDao.getUnlockedBadges(ownerUsername)
    val unlockedCount: LiveData<Int> = badgeDao.getUnlockedCount(ownerUsername)

    suspend fun initializeDefaultBadges(defaultBadges: List<AchievementBadge>) {
        badgeDao.insertBadges(defaultBadges.map { it.copy(ownerUsername = ownerUsername) })
    }

    suspend fun unlockBadge(badgeId: String) {
        val badge = badgeDao.getBadgeById(ownerUsername, badgeId)
        if (badge != null && !badge.isUnlocked) {
            val unlockedBadge = badge.copy(
                isUnlocked = true,
                unlockedDate = System.currentTimeMillis()
            )
            badgeDao.updateBadge(unlockedBadge)
        }
    }
}
