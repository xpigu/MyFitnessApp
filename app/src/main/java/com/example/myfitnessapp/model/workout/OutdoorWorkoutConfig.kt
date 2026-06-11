package com.example.myfitnessapp.model.workout

import com.example.myfitnessapp.R

/**
 * 户外运动配置
 * 通过配置类实现跑步/骑行的差异化，避免代码中散落硬编码类型判断
 */
data class OutdoorWorkoutConfig(
    val type: WorkoutType,
    val primaryLabel: String,           // 一级数据标签：配速 / 速度
    val primaryUnit: String,            // 一级数据单位：/km / km/h
    val primaryFormat: (Double) -> String, // 格式化函数
    val primaryColorRes: Int,           // 主色调
    val mapHeightRatio: Float,          // 地图占屏比例
    val mapCollapsible: Boolean,        // 地图是否可折叠
    val mapTrailWidth: Float,           // 轨迹线宽 dp
    val distanceMultiplier: Double,     // 每秒距离增量
    val secondaryCards: List<SecondaryCard>, // 二级数据卡
    val showLapButton: Boolean,         // 是否显示分段计时
    val showMetronome: Boolean,         // 是否显示节拍器
    val showMaxSpeed: Boolean,          // 是否显示最高速度
    val showLockScreen: Boolean,        // 是否显示锁屏
    val showAutoPause: Boolean,         // 是否显示自动暂停
    val paceTarget: Double,             // 目标配速/速度，用于动态变色
    val paceFast: Double,               // 比目标快多少算"快"
    val paceSlow: Double                // 比目标慢多少算"慢"
) {
    fun isRunning(): Boolean = type == WorkoutType.RUN
    fun isCycling(): Boolean = type == WorkoutType.CYCLING

    companion object {
        fun forRunning(): OutdoorWorkoutConfig = OutdoorWorkoutConfig(
            type = WorkoutType.RUN,
            primaryLabel = "配速",
            primaryUnit = "/km",
            primaryFormat = { secPerKm ->
                val min = secPerKm.toInt() / 60
                val sec = secPerKm.toInt() % 60
                "${min}'${String.format("%02d", sec)}\""
            },
            primaryColorRes = R.color.tracking_metric_running,
            mapHeightRatio = 0.40f,
            mapCollapsible = true,
            mapTrailWidth = 2f,
            distanceMultiplier = 0.0033,
            secondaryCards = listOf(
                SecondaryCard("步频", "步/分钟", R.color.tracking_card_running),
                SecondaryCard("卡路里", "kcal", R.color.tracking_card_calories),
                SecondaryCard("总步数", "步", R.color.tracking_card_steps)
            ),
            showLapButton = true,
            showMetronome = true,
            showMaxSpeed = false,
            showLockScreen = true,
            showAutoPause = false,
            paceTarget = 360.0,  // 6'00" = 360秒/公里
            paceFast = 330.0,     // 5'30" = 330秒
            paceSlow = 390.0      // 6'30" = 390秒
        )

        fun forCycling(): OutdoorWorkoutConfig = OutdoorWorkoutConfig(
            type = WorkoutType.CYCLING,
            primaryLabel = "速度",
            primaryUnit = "km/h",
            primaryFormat = { speedKmh ->
                String.format("%.1f", speedKmh)
            },
            primaryColorRes = R.color.tracking_metric_cycling,
            mapHeightRatio = 0.55f,
            mapCollapsible = true,
            mapTrailWidth = 3f,
            distanceMultiplier = 0.0069,
            secondaryCards = listOf(
                SecondaryCard("海拔爬升", "m", R.color.tracking_card_elevation),
                SecondaryCard("平均坡度", "%", R.color.tracking_card_grade),
                SecondaryCard("卡路里", "kcal", R.color.tracking_card_calories),
                SecondaryCard("最高速度", "km/h", R.color.tracking_card_max_speed)
            ),
            showLapButton = false,
            showMetronome = false,
            showMaxSpeed = true,
            showLockScreen = false,
            showAutoPause = true,
            paceTarget = 25.0,   // 25 km/h
            paceFast = 28.0,     // 28 km/h
            paceSlow = 22.0      // 22 km/h
        )
    }
}

data class SecondaryCard(
    val label: String,
    val unit: String,
    val colorRes: Int
)
