package com.example.myfitnessapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 运动类型: RUN, CYCLING, JUMP_ROPE, STRENGTH, SWIMMING, YOGA */
    @ColumnInfo(name = "sport_type")
    val sportType: String,

    /** 运动图标资源 ID */
    @ColumnInfo(name = "sport_icon_res_id")
    val sportIconResId: Int,

    /** 运动时长（秒） */
    @ColumnInfo(name = "elapsed_seconds")
    val elapsedSeconds: Int,

    /** 总距离（公里），非距离类运动为 0 */
    @ColumnInfo(name = "total_distance")
    val totalDistance: Double,

    /** 总消耗卡路里 */
    @ColumnInfo(name = "total_calories")
    val totalCalories: Int,

    /** 配速或速度文本，如 "6'12\"" 或 "25.3" */
    @ColumnInfo(name = "pace")
    val pace: String,

    /** 记录创建时间戳 */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /** 日期字符串 yyyy-MM-dd，用于按日分组 */
    @ColumnInfo(name = "date")
    val date: String,

    // ===== 运动类型专属字段 =====

    /** 跳绳次数 */
    @ColumnInfo(name = "jump_count")
    val jumpCount: Int = 0,

    /** 跳绳频率（个/分钟） */
    @ColumnInfo(name = "jump_frequency")
    val jumpFrequency: Int = 0,

    /** 力量训练总组数 */
    @ColumnInfo(name = "strength_sets")
    val strengthSets: Int = 0,

    /** 力量训练总容量（kg） */
    @ColumnInfo(name = "strength_volume")
    val strengthVolume: Double = 0.0,

    /** 游泳距离（米） */
    @ColumnInfo(name = "swim_distance_m")
    val swimDistanceM: Int = 0,

    /** 泳姿 */
    @ColumnInfo(name = "swim_stroke")
    val swimStroke: String = "",

    /** 瑜伽完成体式数 */
    @ColumnInfo(name = "yoga_poses_completed")
    val yogaPosesCompleted: Int = 0,

    /** 瑜伽难度评价 1-4 */
    @ColumnInfo(name = "yoga_difficulty")
    val yogaDifficulty: Int = 0,

    /** 跑步总步数 */
    @ColumnInfo(name = "run_steps")
    val runSteps: Int = 0,

    /** 跑步步频（步/分钟） */
    @ColumnInfo(name = "run_cadence")
    val runCadence: Int = 0,

    /** 骑行海拔爬升（米） */
    @ColumnInfo(name = "cycling_elevation")
    val cyclingElevation: Int = 0,

    /** 骑行最高速度（km/h） */
    @ColumnInfo(name = "cycling_max_speed")
    val cyclingMaxSpeed: Double = 0.0
)
