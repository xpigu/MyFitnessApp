package com.example.myfitnessapp.data.viewmodel

import com.example.myfitnessapp.data.entity.WorkoutRecord

// ============================================================
// 统计相关数据类
// ============================================================

/**
 * 运动类型统计数据
 */
data class TypeStats(
    val sportType: String,      // 运动类型（RUN、CYCLING 等）
    val count: Int,             // 该类型的运动次数
    val totalCalories: Int,     // 该类型消耗的总卡路里
    val totalDistance: Double,  // 该类型的总距离（公里）
    val totalDuration: Int      // 该类型的总时长（秒）
)

/**
 * 周统计数据
 */
data class WeeklyStats(
    val weekStart: String,      // 周开始日期（YYYY-MM-dd）
    val weekEnd: String,        // 周结束日期（YYYY-MM-dd）
    val workoutCount: Int,      // 本周运动次数
    val totalCalories: Int,     // 本周消耗卡路里
    val totalDuration: Int,     // 本周总时长（秒）
    val typeDistribution: List<TypeStats> // 各类型分布
)

/**
 * 月统计数据
 */
data class MonthlyStats(
    val month: String,          // 月份（YYYY-MM）
    val workoutCount: Int,      // 本月运动次数
    val totalCalories: Int,     // 本月消耗卡路里
    val totalDuration: Int,     // 本月总时长（秒）
    val typeDistribution: List<TypeStats> // 各类型分布
)

/**
 * 最佳成绩（Personal Record）
 */
data class PBRecords(
    val cards: List<PBCardRecord> = emptyList()
) {
    fun hasAnyData(): Boolean = cards.isNotEmpty()
}

data class PBCardRecord(
    val metricType: PBMetricType,
    val record: WorkoutRecord
)

enum class PBMetricType {
    RUN_LONGEST_DISTANCE,
    CYCLING_LONGEST_DISTANCE,
    STRENGTH_MAX_WEIGHT,
    SWIMMING_LONGEST_DISTANCE,
    JUMP_ROPE_MAX_COUNT,
    JUMP_ROPE_MAX_FREQUENCY
}

/**
 * 日期趋势数据（用于图表显示）
 */
data class DailyTrendData(
    val date: String,           // 日期（YYYY-MM-dd）
    val workoutCount: Int,      // 该日运动次数
    val totalCalories: Int,     // 该日消耗卡路里
    val totalDuration: Int      // 该日总时长（秒）
)

/**
 * 每日的原始统计数据（从数据库查询返回）
 */
data class DailyAggregation(
    val date: String,
    val count: Int,
    val calories: Int,
    val duration: Int
)
