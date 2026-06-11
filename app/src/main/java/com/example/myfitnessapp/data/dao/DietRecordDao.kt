package com.example.myfitnessapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myfitnessapp.data.entity.DietRecord

@Dao
interface DietRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DietRecord): Long

    @Delete
    suspend fun delete(record: DietRecord)

    @Query("DELETE FROM diet_records WHERE owner_username = :ownerUsername AND id = :id")
    suspend fun deleteById(ownerUsername: String, id: Long)

    @Query("DELETE FROM diet_records WHERE owner_username = :ownerUsername")
    suspend fun deleteByOwnerUsername(ownerUsername: String)

    /** 获取所有记录，按时间倒序 */
    @Query("SELECT * FROM diet_records WHERE owner_username = :ownerUsername ORDER BY timestamp DESC")
    fun getAllRecords(ownerUsername: String): LiveData<List<DietRecord>>

    /** 获取指定日期的记录 */
    @Query("SELECT * FROM diet_records WHERE owner_username = :ownerUsername AND date = :date ORDER BY timestamp DESC")
    fun getRecordsByDate(ownerUsername: String, date: String): LiveData<List<DietRecord>>

    /** 指定日期的总卡路里 */
    @Query("SELECT COALESCE(SUM(calories), 0) FROM diet_records WHERE owner_username = :ownerUsername AND date = :date")
    suspend fun getTotalCaloriesByDate(ownerUsername: String, date: String): Int

    /** 指定月份的总卡路里 */
    @Query("SELECT COALESCE(SUM(calories), 0) FROM diet_records WHERE owner_username = :ownerUsername AND date LIKE :monthPattern")
    suspend fun getTotalCaloriesByMonth(ownerUsername: String, monthPattern: String): Int

    /** 获取所有记录总数 */
    @Query("SELECT COUNT(*) FROM diet_records WHERE owner_username = :ownerUsername")
    suspend fun getTotalRecordCount(ownerUsername: String): Int

    /** 按膳食类型统计指定日期内的卡路里 */
    @Query("""
        SELECT meal_type, COALESCE(SUM(calories), 0) as total
        FROM diet_records
        WHERE owner_username = :ownerUsername AND date = :date
        GROUP BY meal_type
        ORDER BY meal_type
    """)
    suspend fun getDailyMealStats(ownerUsername: String, date: String): List<MealCalorieStats>
}

/** 膳食类型的卡路里统计 */
data class MealCalorieStats(
    val meal_type: String,
    val total: Int
)
