package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.DailyCheckinDao
import com.example.myfitnessapp.data.entity.DailyCheckin
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DailyCheckinRepository(private val checkinDao: DailyCheckinDao) {

    val allCheckins: LiveData<List<DailyCheckin>> = checkinDao.getAllCheckins()
    val totalCheckinCount: LiveData<Int> = checkinDao.getTotalCheckinCount()

    suspend fun checkinToday(): Boolean {
        val todayStr = getTodayString()
        val existingCheckin = checkinDao.getCheckinByDate(todayStr)
        
        if (existingCheckin != null) {
            return false // 今天已经签到过了
        }

        val latestCheckin = checkinDao.getLatestCheckin()
        var newStreak = 1

        if (latestCheckin != null) {
            val yesterdayStr = getYesterdayString()
            if (latestCheckin.date == yesterdayStr) {
                // 昨天签到了，连续签到天数 +1
                newStreak = latestCheckin.streakCount + 1
            }
        }

        val newCheckin = DailyCheckin(
            date = todayStr,
            streakCount = newStreak
        )
        checkinDao.insertCheckin(newCheckin)
        return true
    }

    suspend fun isCheckedInToday(): Boolean {
        val todayStr = getTodayString()
        return checkinDao.getCheckinByDate(todayStr) != null
    }

    suspend fun getCurrentStreak(): Int {
        val todayStr = getTodayString()
        val todayCheckin = checkinDao.getCheckinByDate(todayStr)
        if (todayCheckin != null) {
            return todayCheckin.streakCount
        }

        val yesterdayStr = getYesterdayString()
        val yesterdayCheckin = checkinDao.getCheckinByDate(yesterdayStr)
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
