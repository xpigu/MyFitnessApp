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

    @Query("SELECT * FROM daily_checkins WHERE owner_username = :ownerUsername ORDER BY date DESC")
    fun getAllCheckins(ownerUsername: String): LiveData<List<DailyCheckin>>

    @Query("SELECT * FROM daily_checkins WHERE owner_username = :ownerUsername AND date = :date LIMIT 1")
    suspend fun getCheckinByDate(ownerUsername: String, date: String): DailyCheckin?

    @Query("SELECT * FROM daily_checkins WHERE owner_username = :ownerUsername ORDER BY date DESC LIMIT 1")
    suspend fun getLatestCheckin(ownerUsername: String): DailyCheckin?

    @Query("SELECT COUNT(*) FROM daily_checkins WHERE owner_username = :ownerUsername")
    fun getTotalCheckinCount(ownerUsername: String): LiveData<Int>

    @Query("DELETE FROM daily_checkins WHERE owner_username = :ownerUsername")
    suspend fun deleteByOwnerUsername(ownerUsername: String)
}
