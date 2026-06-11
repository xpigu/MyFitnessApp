package com.example.myfitnessapp.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.myfitnessapp.CurrentAccount
import com.example.myfitnessapp.R
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.entity.AchievementBadge
import com.example.myfitnessapp.data.entity.DailyCheckin
import com.example.myfitnessapp.data.entity.UserProfile
import com.example.myfitnessapp.data.entity.WorkoutRecord
import com.example.myfitnessapp.data.repository.AchievementBadgeRepository
import com.example.myfitnessapp.data.repository.DailyCheckinRepository
import com.example.myfitnessapp.data.repository.UserProfileRepository
import com.example.myfitnessapp.data.repository.WorkoutRecordRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AchievementBadgeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AchievementBadgeRepository
    private val profileRepository: UserProfileRepository
    private val workoutRepository: WorkoutRecordRepository
    private val checkinRepository: DailyCheckinRepository
    private val profileLiveData: LiveData<UserProfile>
    private val currentUsername = CurrentAccount.requireUsername(application)

    val allBadges: LiveData<List<AchievementBadge>>
    val unlockedBadges: LiveData<List<AchievementBadge>>
    val unlockedCount: LiveData<Int>

    private var currentProfile: UserProfile? = null
    private var currentWorkouts: List<WorkoutRecord> = emptyList()
    private var currentCheckins: List<DailyCheckin> = emptyList()

    private val profileObserver = Observer<UserProfile> {
        currentProfile = it
        syncBadges()
    }

    private val workoutObserver = Observer<List<WorkoutRecord>> {
        currentWorkouts = it ?: emptyList()
        syncBadges()
    }

    private val checkinObserver = Observer<List<DailyCheckin>> {
        currentCheckins = it ?: emptyList()
        syncBadges()
    }

    init {
        val db = AppDatabase.getInstance(application)
        repository = AchievementBadgeRepository(db.achievementBadgeDao(), currentUsername)
        profileRepository = UserProfileRepository(db.userProfileDao(), currentUsername)
        workoutRepository = WorkoutRecordRepository(db.workoutRecordDao(), currentUsername)
        checkinRepository = DailyCheckinRepository(db.dailyCheckinDao(), currentUsername)
        profileLiveData = profileRepository.getUserProfile()
        allBadges = repository.allBadges
        unlockedBadges = repository.unlockedBadges
        unlockedCount = repository.unlockedCount

        initializeBadges()
        profileLiveData.observeForever(profileObserver)
        workoutRepository.allRecords.observeForever(workoutObserver)
        checkinRepository.allCheckins.observeForever(checkinObserver)
    }

    private fun initializeBadges() {
        viewModelScope.launch {
            val defaultBadges = listOf(
                AchievementBadge(id = "first_workout", name = "初出茅庐", description = "完成第一次运动", iconResId = R.drawable.ic_achievement, category = "WORKOUT"),
                AchievementBadge(id = "workout_3_days", name = "坚持不懈", description = "连续运动3天", iconResId = R.drawable.ic_achievement, category = "STREAK"),
                AchievementBadge(id = "workout_7_days", name = "运动达人", description = "连续运动7天", iconResId = R.drawable.ic_achievement, category = "STREAK"),
                AchievementBadge(id = "calories_1000", name = "燃烧卡路里", description = "单次运动消耗1000千卡", iconResId = R.drawable.ic_achievement, category = "WORKOUT"),
                AchievementBadge(id = "water_master", name = "水之源", description = "连续3天完成饮水目标", iconResId = R.drawable.ic_achievement, category = "DIET"),
                AchievementBadge(id = "early_bird", name = "早起鸟", description = "早上6点前完成一次运动", iconResId = R.drawable.ic_achievement, category = "WORKOUT"),
                AchievementBadge(id = "active_7_days", name = "活跃新星", description = "累计活跃 7 天", iconResId = R.drawable.ic_achievement, category = "ACTIVE"),
                AchievementBadge(id = "active_30_days", name = "稳定高光", description = "累计活跃 30 天", iconResId = R.drawable.ic_achievement, category = "ACTIVE"),
                AchievementBadge(id = "checkin_streak_3", name = "签到开局", description = "连续签到 3 天", iconResId = R.drawable.ic_achievement, category = "CHECKIN"),
                AchievementBadge(id = "checkin_streak_7", name = "自律连击", description = "连续签到 7 天", iconResId = R.drawable.ic_achievement, category = "CHECKIN"),
                AchievementBadge(id = "level_5", name = "成长进阶", description = "运动等级达到 Lv.5", iconResId = R.drawable.ic_achievement, category = "LEVEL"),
                AchievementBadge(id = "level_10", name = "高能觉醒", description = "运动等级达到 Lv.10", iconResId = R.drawable.ic_achievement, category = "LEVEL"),
                AchievementBadge(id = "workout_20_sessions", name = "训练积累者", description = "累计完成 20 次训练", iconResId = R.drawable.ic_achievement, category = "WORKOUT")
            )
            repository.initializeDefaultBadges(defaultBadges)
            syncBadges()
        }
    }

    fun checkAndUnlockBadge(badgeId: String) {
        viewModelScope.launch {
            repository.unlockBadge(badgeId)
        }
    }

    private fun syncBadges() {
        val profile = currentProfile ?: return
        val workouts = currentWorkouts
        val checkins = currentCheckins

        val activeDays = buildSet {
            workouts.forEach { add(it.date) }
            checkins.forEach { add(it.date) }
        }.size
        val workoutStreak = calculateLongestWorkoutStreak(workouts)
        val currentCheckinStreak = calculateCurrentCheckinStreak(checkins)

        if (workouts.isNotEmpty()) checkAndUnlockBadge("first_workout")
        if (workoutStreak >= 3) checkAndUnlockBadge("workout_3_days")
        if (workoutStreak >= 7) checkAndUnlockBadge("workout_7_days")
        if (workouts.any { it.totalCalories >= 1000 }) checkAndUnlockBadge("calories_1000")
        if (workouts.any { hourOf(it.timestamp) < 6 }) checkAndUnlockBadge("early_bird")
        if (activeDays >= 7) checkAndUnlockBadge("active_7_days")
        if (activeDays >= 30) checkAndUnlockBadge("active_30_days")
        if (currentCheckinStreak >= 3) checkAndUnlockBadge("checkin_streak_3")
        if (currentCheckinStreak >= 7) checkAndUnlockBadge("checkin_streak_7")
        if (profile.level >= 5) checkAndUnlockBadge("level_5")
        if (profile.level >= 10) checkAndUnlockBadge("level_10")
        if (workouts.size >= 20) checkAndUnlockBadge("workout_20_sessions")
    }

    private fun calculateLongestWorkoutStreak(workouts: List<WorkoutRecord>): Int {
        val dates = workouts.map { it.date }.distinct().sorted()
        if (dates.isEmpty()) return 0

        var longest = 1
        var current = 1
        for (index in 1 until dates.size) {
            if (isNextDay(dates[index - 1], dates[index])) {
                current += 1
                longest = maxOf(longest, current)
            } else {
                current = 1
            }
        }
        return longest
    }

    private fun calculateCurrentCheckinStreak(checkins: List<DailyCheckin>): Int {
        if (checkins.isEmpty()) return 0
        val today = currentDateString()
        val yesterday = offsetDateString(-1)
        return when {
            checkins.firstOrNull { it.date == today } != null -> checkins.first { it.date == today }.streakCount
            checkins.firstOrNull { it.date == yesterday } != null -> checkins.first { it.date == yesterday }.streakCount
            else -> 0
        }
    }

    private fun isNextDay(previous: String, current: String): Boolean {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val previousDate = formatter.parse(previous) ?: return false
        val currentDate = formatter.parse(current) ?: return false
        val calendar = Calendar.getInstance().apply { time = previousDate; add(Calendar.DAY_OF_YEAR, 1) }
        return formatter.format(calendar.time) == formatter.format(currentDate)
    }

    private fun hourOf(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    private fun currentDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun offsetDateString(offsetDays: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    override fun onCleared() {
        profileLiveData.removeObserver(profileObserver)
        workoutRepository.allRecords.removeObserver(workoutObserver)
        checkinRepository.allCheckins.removeObserver(checkinObserver)
        super.onCleared()
    }
}
