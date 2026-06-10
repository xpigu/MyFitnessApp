package com.example.myfitnessapp.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.entity.DailyCheckin
import com.example.myfitnessapp.data.entity.UserProfile
import com.example.myfitnessapp.data.entity.WorkoutRecord
import com.example.myfitnessapp.data.repository.DailyCheckinRepository
import com.example.myfitnessapp.data.repository.UserProfileRepository
import com.example.myfitnessapp.data.repository.WorkoutRecordRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserProfileRepository
    private val workoutRepository: WorkoutRecordRepository
    private val checkinRepository: DailyCheckinRepository

    val userProfile: LiveData<UserProfile>

    private val _editingProfile = MutableLiveData<UserProfile>()
    val editingProfile: LiveData<UserProfile> = _editingProfile

    private var currentProfile: UserProfile? = null
    private var currentWorkoutRecords: List<WorkoutRecord> = emptyList()
    private var currentCheckins: List<DailyCheckin> = emptyList()

    private val userProfileObserver = Observer<UserProfile> { profile ->
        currentProfile = profile
        syncStatsToProfileIfNeeded()
    }

    private val workoutObserver = Observer<List<WorkoutRecord>> { records ->
        currentWorkoutRecords = records ?: emptyList()
        syncStatsToProfileIfNeeded()
    }

    private val checkinObserver = Observer<List<DailyCheckin>> { checkins ->
        currentCheckins = checkins ?: emptyList()
        syncStatsToProfileIfNeeded()
    }

    init {
        val db = AppDatabase.getInstance(application)
        repository = UserProfileRepository(db.userProfileDao())
        workoutRepository = WorkoutRecordRepository(db.workoutRecordDao())
        checkinRepository = DailyCheckinRepository(db.dailyCheckinDao())
        userProfile = repository.getUserProfile()

        userProfile.observeForever(userProfileObserver)
        workoutRepository.allRecords.observeForever(workoutObserver)
        checkinRepository.allCheckins.observeForever(checkinObserver)

        // 初始化：如果数据库中没有用户资料，则插入默认值
        viewModelScope.launch {
            val existing = repository.getUserProfileSync()
            if (existing == null) {
                val defaultProfile = UserProfile()
                repository.insertUserProfile(defaultProfile)
            }
        }
    }

    /** 更新用户资料 */
    fun updateUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            val updated = profile.copy(lastUpdated = System.currentTimeMillis())
            repository.updateUserProfile(updated)
        }
    }

    /** 设置编辑中的资料 */
    fun setEditingProfile(profile: UserProfile) {
        _editingProfile.value = profile
    }

    /** 提交编辑 */
    fun submitEdit(profile: UserProfile) {
        updateUserProfile(profile)
    }

    private fun syncStatsToProfileIfNeeded() {
        val profile = currentProfile ?: return
        val totalWorkouts = currentWorkoutRecords.size
        val activeDays = buildSet {
            currentWorkoutRecords.forEach { add(it.date) }
            currentCheckins.forEach { add(it.date) }
        }.size
        val currentStreak = calculateCurrentStreak(currentCheckins)
        val level = calculateLevel(
            totalWorkouts = totalWorkouts,
            activeDays = activeDays,
            currentStreak = currentStreak
        )

        if (
            profile.totalWorkouts == totalWorkouts &&
            profile.activeDays == activeDays &&
            profile.level == level
        ) {
            return
        }

        updateUserProfile(
            profile.copy(
                totalWorkouts = totalWorkouts,
                activeDays = activeDays,
                level = level
            )
        )
    }

    private fun calculateCurrentStreak(checkins: List<DailyCheckin>): Int {
        if (checkins.isEmpty()) return 0

        val today = currentDateString()
        val yesterday = offsetDateString(-1)

        return when {
            checkins.firstOrNull { it.date == today } != null ->
                checkins.first { it.date == today }.streakCount
            checkins.firstOrNull { it.date == yesterday } != null ->
                checkins.first { it.date == yesterday }.streakCount
            else -> 0
        }
    }

    // 经验由训练量、活跃天数和连续签到共同组成，等级随经验线性成长。
    private fun calculateLevel(totalWorkouts: Int, activeDays: Int, currentStreak: Int): Int {
        val experience =
            totalWorkouts * 12 +
                activeDays * 8 +
                currentStreak * 15
        return (experience / 120 + 1).coerceIn(1, 30)
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
        userProfile.removeObserver(userProfileObserver)
        workoutRepository.allRecords.removeObserver(workoutObserver)
        checkinRepository.allCheckins.removeObserver(checkinObserver)
        super.onCleared()
    }
}
