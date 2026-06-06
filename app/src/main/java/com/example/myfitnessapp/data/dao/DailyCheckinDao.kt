package com.example.myfitnessapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myfitnessapp.data.entity.DailyCheckin

@Dao
interface DailyCheckinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckin(checkin: DailyCheckin)

    @Query("SELECT * FROM daily_checkins ORDER BY date DESC")
    fun getAllCheckins(): LiveData<List<DailyCheckin>>

    @Query("SELECT * FROM daily_checkins WHERE date = :date LIMIT 1")
    suspend fun getCheckinByDate(date: String): DailyCheckin?

    @Query("SELECT * FROM daily_checkins ORDER BY date DESC LIMIT 1")
    suspend fun getLatestCheckin(): DailyCheckin?

    @Query("SELECT COUNT(*) FROM daily_checkins")
    fun getTotalCheckinCount(): LiveData<Int>
}
