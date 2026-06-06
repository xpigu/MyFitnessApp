package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.DailyAggregation
import com.example.myfitnessapp.data.dao.TypeCount
import com.example.myfitnessapp.data.dao.TypeDistribution
import com.example.myfitnessapp.data.dao.WorkoutRecordDao
import com.example.myfitnessapp.data.entity.WorkoutRecord
import com.example.myfitnessapp.data.viewmodel.PBCardRecord
import com.example.myfitnessapp.data.viewmodel.PBMetricType
import com.example.myfitnessapp.data.viewmodel.PBRecords
import com.example.myfitnessapp.data.viewmodel.TypeStats

class WorkoutRecordRepository(private val dao: WorkoutRecordDao) {

    val allRecords: LiveData<List<WorkoutRecord>> = dao.getAllRecords()
    val allDates: LiveData<List<String>> = dao.getAllDates()

    suspend fun insert(record: WorkoutRecord): Long = dao.insert(record)

    suspend fun delete(record: WorkoutRecord) = dao.delete(record)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    fun getRecordsByDate(date: String): LiveData<List<WorkoutRecord>> = dao.getRecordsByDate(date)

    suspend fun getTotalCaloriesByDate(date: String): Int = dao.getTotalCaloriesByDate(date)

    suspend fun getTotalDurationByDate(date: String): Int = dao.getTotalDurationByDate(date)

    suspend fun getWorkoutCountByDate(date: String): Int = dao.getWorkoutCountByDate(date)

    suspend fun getTotalCaloriesByMonth(monthPattern: String): Int = dao.getTotalCaloriesByMonth(monthPattern)

    suspend fun getTotalDurationByMonth(monthPattern: String): Int = dao.getTotalDurationByMonth(monthPattern)

    suspend fun getWorkoutCountByMonth(monthPattern: String): Int = dao.getWorkoutCountByMonth(monthPattern)

    suspend fun getMonthlyTypeStats(monthPattern: String): List<TypeCount> = dao.getMonthlyTypeStats(monthPattern)

    suspend fun getDailyTypeStats(date: String): List<TypeCount> = dao.getDailyTypeStats(date)

    suspend fun getTotalRecordCount(): Int = dao.getTotalRecordCount()

    // ============================================================
    // 统计方法 — Phase 2 增强
    // ============================================================

    /** 获取日期范围的日期统计 */
    suspend fun getDailyAggregation(startDate: String, endDate: String): List<DailyAggregation> =
        dao.getDailyAggregation(startDate, endDate)

    /** 获取指定日期的按类型分布 */
    suspend fun getTypeDistributionByDate(date: String): List<TypeStats> =
        dao.getTypeDistributionByDate(date).map { it.toTypeStats() }

    /** 获取指定月份的按类型分布 */
    suspend fun getTypeDistributionByMonth(monthPattern: String): List<TypeStats> =
        dao.getTypeDistributionByMonth(monthPattern).map { it.toTypeStats() }

    /** 获取最佳成绩 */
    suspend fun getPBRecords(): PBRecords {
        val cards = buildList {
            dao.getRunRecordWithLongestDistance()?.let {
                add(PBCardRecord(PBMetricType.RUN_LONGEST_DISTANCE, it))
            }
            dao.getCyclingRecordWithLongestDistance()?.let {
                add(PBCardRecord(PBMetricType.CYCLING_LONGEST_DISTANCE, it))
            }
            dao.getStrengthRecordWithMaxWeight()?.let {
                add(PBCardRecord(PBMetricType.STRENGTH_MAX_WEIGHT, it))
            }
            dao.getSwimmingRecordWithLongestDistance()?.let {
                add(PBCardRecord(PBMetricType.SWIMMING_LONGEST_DISTANCE, it))
            }
            dao.getJumpRopeRecordWithMaxCount()?.let {
                add(PBCardRecord(PBMetricType.JUMP_ROPE_MAX_COUNT, it))
            }
            dao.getJumpRopeRecordWithMaxFrequency()?.let {
                add(PBCardRecord(PBMetricType.JUMP_ROPE_MAX_FREQUENCY, it))
            }
        }

        return PBRecords(cards = cards)
    }

    /** 获取最近30天的趋势数据 */
    suspend fun get30DaysTrend(startDate: String): List<DailyAggregation> =
        dao.get30DaysTrend(startDate)
}

/** TypeDistribution 转换为 TypeStats */
private fun TypeDistribution.toTypeStats(): TypeStats = TypeStats(
    sportType = this.sport_type,
    count = this.count,
    totalCalories = this.calories,
    totalDistance = this.distance,
    totalDuration = this.duration
)
