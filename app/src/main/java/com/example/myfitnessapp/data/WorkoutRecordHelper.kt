package com.example.myfitnessapp.data

import com.example.myfitnessapp.R
import com.example.myfitnessapp.data.entity.WorkoutRecord
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 运动记录保存辅助工具
 * 统一处理时间戳、日期、图标等公共字段
 */
object WorkoutRecordHelper {

    /** 获取运动类型对应的图标资源 */
    fun getIconRes(sportType: String): Int {
        return when (sportType.uppercase()) {
            "RUN" -> R.drawable.ic_running
            "CYCLING" -> R.drawable.ic_cycling
            "JUMP_ROPE" -> R.drawable.ic_jump_rope
            "STRENGTH" -> R.drawable.ic_strength_white
            "SWIMMING" -> R.drawable.ic_swimming
            "YOGA" -> R.drawable.ic_yoga
            else -> R.drawable.ic_running
        }
    }

    /** 获取运动类型的中文名称 */
    fun getSportLabel(sportType: String): String {
        return when (sportType.uppercase()) {
            "RUN" -> "户外跑步"
            "CYCLING" -> "户外骑行"
            "JUMP_ROPE" -> "跳绳"
            "STRENGTH" -> "力量训练"
            "SWIMMING" -> "游泳"
            "YOGA" -> "瑜伽"
            else -> "运动"
        }
    }

    /** 当前时间戳 */
    fun nowTimestamp(): Long = System.currentTimeMillis()

    /** 当前日期字符串 yyyy-MM-dd */
    fun todayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    /** 时间戳转时间字符串 HH:mm */
    fun timestampToTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /** 时间戳转完整日期时间 */
    fun timestampToDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /** 日期字符串转显示格式 "6月5日 周四" */
    fun dateToDisplay(dateStr: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return dateStr
        val displaySdf = SimpleDateFormat("M月d日 E", Locale.CHINESE)
        return displaySdf.format(date)
    }

    /** 月份字符串转显示格式 "2026年6月" */
    fun monthToDisplay(monthStr: String): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdf.parse(monthStr) ?: return monthStr
        val displaySdf = SimpleDateFormat("yyyy年M月", Locale.CHINESE)
        return displaySdf.format(date)
    }

    /** 格式化时长 */
    fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    /** 格式化时长（简短版） */
    fun formatDurationShort(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /** 生成运动记录的摘要文本 */
    fun buildSummary(record: WorkoutRecord): String {
        return when (record.sportType.uppercase()) {
            "RUN" -> {
                val dist = String.format("%.2f", record.totalDistance)
                "$dist 公里 / ${record.totalCalories} 千卡"
            }
            "CYCLING" -> {
                val dist = String.format("%.2f", record.totalDistance)
                "$dist 公里 / ${record.totalCalories} 千卡"
            }
            "JUMP_ROPE" -> "${record.jumpCount} 个 / ${record.totalCalories} 千卡"
            "STRENGTH" -> "${record.strengthSets} 组 / ${record.strengthVolume.toInt()} kg"
            "SWIMMING" -> "${record.swimDistanceM} 米 / ${record.swimStroke}"
            "YOGA" -> "${record.yogaPosesCompleted} 个体式"
            else -> "${record.totalCalories} 千卡"
        }
    }
}
