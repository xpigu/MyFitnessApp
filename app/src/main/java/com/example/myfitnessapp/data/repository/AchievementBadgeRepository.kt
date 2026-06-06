package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.AchievementBadgeDao
import com.example.myfitnessapp.data.entity.AchievementBadge

class AchievementBadgeRepository(private val badgeDao: AchievementBadgeDao) {

    val allBadges: LiveData<List<AchievementBadge>> = badgeDao.getAllBadges()
    val unlockedBadges: LiveData<List<AchievementBadge>> = badgeDao.getUnlockedBadges()
    val unlockedCount: LiveData<Int> = badgeDao.getUnlockedCount()

    suspend fun initializeDefaultBadges(defaultBadges: List<AchievementBadge>) {
        badgeDao.insertBadges(defaultBadges)
    }

    suspend fun unlockBadge(badgeId: String) {
        val badge = badgeDao.getBadgeById(badgeId)
        if (badge != null && !badge.isUnlocked) {
            val unlockedBadge = badge.copy(
                isUnlocked = true,
                unlockedDate = System.currentTimeMillis()
            )
            badgeDao.updateBadge(unlockedBadge)
        }
    }
}
