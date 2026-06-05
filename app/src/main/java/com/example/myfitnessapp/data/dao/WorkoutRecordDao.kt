package com.example.myfitnessapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myfitnessapp.data.entity.WorkoutRecord

@Dao
interface WorkoutRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WorkoutRecord): Long

    @Delete
    suspend fun delete(record: WorkoutRecord)

    @Query("DELETE FROM workout_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 获取所有记录，按时间倒序 */
    @Query("SELECT * FROM workout_records ORDER BY timestamp DESC")
    fun getAllRecords(): LiveData<List<WorkoutRecord>>

    /** 按日期分组获取所有日期，倒序 */
    @Query("SELECT DISTINCT date FROM workout_records ORDER BY date DESC")
    fun getAllDates(): LiveData<List<String>>

    /** 获取指定日期的记录 */
    @Query("SELECT * FROM workout_records WHERE date = :date ORDER BY timestamp DESC")
    fun getRecordsByDate(date: String): LiveData<List<WorkoutRecord>>

    /** 指定日期的总卡路里 */
    @Query("SELECT COALESCE(SUM(total_calories), 0) FROM workout_records WHERE date = :date")
    suspend fun getTotalCaloriesByDate(date: String): Int

    /** 指定日期的总时长（秒） */
    @Query("SELECT COALESCE(SUM(elapsed_seconds), 0) FROM workout_records WHERE date = :date")
    suspend fun getTotalDurationByDate(date: String): Int

    /** 指定日期的运动次数 */
    @Query("SELECT COUNT(*) FROM workout_records WHERE date = :date")
    suspend fun getWorkoutCountByDate(date: String): Int

    /** 指定月份的总卡路里 */
    @Query("SELECT COALESCE(SUM(total_calories), 0) FROM workout_records WHERE date LIKE :monthPattern")
    suspend fun getTotalCaloriesByMonth(monthPattern: String): Int

    /** 指定月份的总时长 */
    @Query("SELECT COALESCE(SUM(elapsed_seconds), 0) FROM workout_records WHERE date LIKE :monthPattern")
    suspend fun getTotalDurationByMonth(monthPattern: String): Int

    /** 指定月份的运动次数 */
    @Query("SELECT COUNT(*) FROM workout_records WHERE date LIKE :monthPattern")
    suspend fun getWorkoutCountByMonth(monthPattern: String): Int

    /** 按运动类型统计月份内次数 */
    @Query("""
        SELECT sport_type, COUNT(*) as count FROM workout_records 
        WHERE date LIKE :monthPattern 
        GROUP BY sport_type ORDER BY count DESC
    """)
    suspend fun getMonthlyTypeStats(monthPattern: String): List<TypeCount>

    /** 按运动类型统计日期内次数 */
    @Query("""
        SELECT sport_type, COUNT(*) as count FROM workout_records 
        WHERE date = :date 
        GROUP BY sport_type ORDER BY count DESC
    """)
    suspend fun getDailyTypeStats(date: String): List<TypeCount>

    /** 获取所有记录总数 */
    @Query("SELECT COUNT(*) FROM workout_records")
    suspend fun getTotalRecordCount(): Int
}

/** 按类型统计的结果 */
data class TypeCount(
    val sport_type: String,
    val count: Int
)
