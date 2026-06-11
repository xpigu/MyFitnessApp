package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.DailyCheckinDao
import com.example.myfitnessapp.data.entity.DailyCheckin
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DailyCheckinRepository(
    private val checkinDao: DailyCheckinDao,
    private val ownerUsername: String
) {

    val allCheckins: LiveData<List<DailyCheckin>> = checkinDao.getAllCheckins(ownerUsername)
    val totalCheckinCount: LiveData<Int> = checkinDao.getTotalCheckinCount(ownerUsername)

    suspend fun checkinToday(): Boolean {
        val todayStr = getTodayString()
        val existingCheckin = checkinDao.getCheckinByDate(ownerUsername, todayStr)
        
        if (existingCheckin != null) {
            return false // 今天已经签到过了
        }

        val latestCheckin = checkinDao.getLatestCheckin(ownerUsername)
        var newStreak = 1

        if (latestCheckin != null) {
            val yesterdayStr = getYesterdayString()
            if (latestCheckin.date == yesterdayStr) {
                // 昨天签到了，连续签到天数 +1
                newStreak = latestCheckin.streakCount + 1
            }
        }

        val newCheckin = DailyCheckin(
            ownerUsername = ownerUsername,
            date = todayStr,
            streakCount = newStreak
        )
        checkinDao.insertCheckin(newCheckin)
        return true
    }

    suspend fun isCheckedInToday(): Boolean {
        val todayStr = getTodayString()
        return checkinDao.getCheckinByDate(ownerUsername, todayStr) != null
    }

    suspend fun getCurrentStreak(): Int {
        val todayStr = getTodayString()
        val todayCheckin = checkinDao.getCheckinByDate(ownerUsername, todayStr)
        if (todayCheckin != null) {
            return todayCheckin.streakCount
        }

        val yesterdayStr = getYesterdayString()
        val yesterdayCheckin = checkinDao.getCheckinByDate(ownerUsername, yesterdayStr)
        if (yesterdayCheckin != null) {
            return yesterdayCheckin.streakCount
        }

        return 0
    }

    private fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getYesterdayString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }
}
