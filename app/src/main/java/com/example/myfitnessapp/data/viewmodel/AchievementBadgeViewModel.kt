package com.example.myfitnessapp.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.myfitnessapp.R
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.entity.AchievementBadge
import com.example.myfitnessapp.data.repository.AchievementBadgeRepository
import kotlinx.coroutines.launch

class AchievementBadgeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AchievementBadgeRepository

    val allBadges: LiveData<List<AchievementBadge>>
    val unlockedBadges: LiveData<List<AchievementBadge>>
    val unlockedCount: LiveData<Int>

    init {
        val dao = AppDatabase.getInstance(application).achievementBadgeDao()
        repository = AchievementBadgeRepository(dao)
        allBadges = repository.allBadges
        unlockedBadges = repository.unlockedBadges
        unlockedCount = repository.unlockedCount

        initializeBadges()
    }

    private fun initializeBadges() {
        viewModelScope.launch {
            val defaultBadges = listOf(
                AchievementBadge("first_workout", "初出茅庐", "完成第一次运动", R.drawable.ic_achievement, category = "WORKOUT"),
                AchievementBadge("workout_3_days", "坚持不懈", "连续运动3天", R.drawable.ic_achievement, category = "STREAK"),
                AchievementBadge("workout_7_days", "运动达人", "连续运动7天", R.drawable.ic_achievement, category = "STREAK"),
                AchievementBadge("calories_1000", "燃烧卡路里", "单次运动消耗1000千卡", R.drawable.ic_achievement, category = "WORKOUT"),
                AchievementBadge("water_master", "水之源", "连续3天完成饮水目标", R.drawable.ic_achievement, category = "DIET"),
                AchievementBadge("early_bird", "早起鸟", "早上6点前完成一次运动", R.drawable.ic_achievement, category = "WORKOUT")
            )
            repository.initializeDefaultBadges(defaultBadges)
        }
    }

    fun checkAndUnlockBadge(badgeId: String) {
        viewModelScope.launch {
            repository.unlockBadge(badgeId)
        }
    }
}
