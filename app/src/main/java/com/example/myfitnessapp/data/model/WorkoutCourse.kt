package com.example.myfitnessapp.data.model

/**
 * 课程数据模型
 */
data class WorkoutCourse(
    val id: String,                     // 课程唯一标识
    val title: String,                  // 课程名称
    val sportType: String,              // 运动类型：RUN, CYCLING, YOGA, STRENGTH, SWIMMING, JUMP_ROPE
    val level: String,                  // 难度等级：初学者, 中级, 高级
    val durationMinutes: Int,           // 课程时长（分钟）
    val estimatedCalories: Int,         // 预估消耗卡路里
    val description: String,            // 课程描述
    val iconResId: Int                  // 运动图标资源ID
) {
    /**
     * 格式化课程信息显示
     */
    fun formatInfo(): String {
        return "$level · ${durationMinutes}分钟 · 约${estimatedCalories} kcal"
    }
}

/**
 * 课程数据仓库 - 为每种运动类型提供专属课程
 */
object CourseRepository {

    // ============================================================
    // 跑步课程
    // ============================================================
    private val runningCourses = listOf(
        WorkoutCourse(
            id = "run_001",
            title = "5公里入门跑",
            sportType = "RUN",
            level = "初学者",
            durationMinutes = 30,
            estimatedCalories = 300,
            description = "适合刚开始跑步的人群，循序渐进完成5公里目标",
            iconResId = com.example.myfitnessapp.R.drawable.ic_running
        ),
        WorkoutCourse(
            id = "run_002",
            title = "燃脂间歇跑",
            sportType = "RUN",
            level = "中级",
            durationMinutes = 45,
            estimatedCalories = 450,
            description = "高强度间歇训练，快速燃烧脂肪，提升心肺功能",
            iconResId = com.example.myfitnessapp.R.drawable.ic_running
        ),
        WorkoutCourse(
            id = "run_003",
            title = "10公里耐力跑",
            sportType = "RUN",
            level = "高级",
            durationMinutes = 60,
            estimatedCalories = 600,
            description = "挑战10公里长跑，提升耐力和意志力",
            iconResId = com.example.myfitnessapp.R.drawable.ic_running
        )
    )

    // ============================================================
    // 骑行课程
    // ============================================================
    private val cyclingCourses = listOf(
        WorkoutCourse(
            id = "cycling_001",
            title = "城市休闲骑行",
            sportType = "CYCLING",
            level = "初学者",
            durationMinutes = 45,
            estimatedCalories = 350,
            description = "轻松骑行，适合日常通勤和休闲锻炼",
            iconResId = com.example.myfitnessapp.R.drawable.ic_cycling
        ),
        WorkoutCourse(
            id = "cycling_002",
            title = "山地爬坡训练",
            sportType = "CYCLING",
            level = "中级",
            durationMinutes = 60,
            estimatedCalories = 500,
            description = "模拟山地爬坡，增强腿部力量和耐力",
            iconResId = com.example.myfitnessapp.R.drawable.ic_cycling
        ),
        WorkoutCourse(
            id = "cycling_003",
            title = "公路竞速骑行",
            sportType = "CYCLING",
            level = "高级",
            durationMinutes = 90,
            estimatedCalories = 800,
            description = "高速骑行训练，挑战速度极限",
            iconResId = com.example.myfitnessapp.R.drawable.ic_cycling
        )
    )

    // ============================================================
    // 瑜伽课程
    // ============================================================
    private val yogaCourses = listOf(
        WorkoutCourse(
            id = "yoga_001",
            title = "晨间唤醒瑜伽",
            sportType = "YOGA",
            level = "初学者",
            durationMinutes = 20,
            estimatedCalories = 150,
            description = "温和的瑜伽体式，唤醒身体活力",
            iconResId = com.example.myfitnessapp.R.drawable.ic_yoga
        ),
        WorkoutCourse(
            id = "yoga_002",
            title = "核心力量瑜伽",
            sportType = "YOGA",
            level = "中级",
            durationMinutes = 40,
            estimatedCalories = 250,
            description = "强化核心肌群，提升身体稳定性",
            iconResId = com.example.myfitnessapp.R.drawable.ic_yoga
        ),
        WorkoutCourse(
            id = "yoga_003",
            title = "深度放松瑜伽",
            sportType = "YOGA",
            level = "高级",
            durationMinutes = 60,
            estimatedCalories = 300,
            description = "深度拉伸和冥想，缓解身心压力",
            iconResId = com.example.myfitnessapp.R.drawable.ic_yoga
        )
    )

    // ============================================================
    // 力量训练课程
    // ============================================================
    private val strengthCourses = listOf(
        WorkoutCourse(
            id = "strength_001",
            title = "上肢力量入门",
            sportType = "STRENGTH",
            level = "初学者",
            durationMinutes = 30,
            estimatedCalories = 200,
            description = "基础手臂和肩部训练，建立力量基础",
            iconResId = com.example.myfitnessapp.R.drawable.ic_strength_white
        ),
        WorkoutCourse(
            id = "strength_002",
            title = "全身综合训练",
            sportType = "STRENGTH",
            level = "中级",
            durationMinutes = 45,
            estimatedCalories = 350,
            description = "全身多部位力量训练，均衡发展肌肉",
            iconResId = com.example.myfitnessapp.R.drawable.ic_strength_white
        ),
        WorkoutCourse(
            id = "strength_003",
            title = "高强度力量挑战",
            sportType = "STRENGTH",
            level = "高级",
            durationMinutes = 60,
            estimatedCalories = 500,
            description = "高强度力量训练，突破极限",
            iconResId = com.example.myfitnessapp.R.drawable.ic_strength_white
        )
    )

    // ============================================================
    // 游泳课程
    // ============================================================
    private val swimmingCourses = listOf(
        WorkoutCourse(
            id = "swimming_001",
            title = "自由泳基础",
            sportType = "SWIMMING",
            level = "初学者",
            durationMinutes = 45,
            estimatedCalories = 400,
            description = "学习自由泳基本技巧，提升水性",
            iconResId = com.example.myfitnessapp.R.drawable.ic_swimming
        ),
        WorkoutCourse(
            id = "swimming_002",
            title = "蛙泳进阶训练",
            sportType = "SWIMMING",
            level = "中级",
            durationMinutes = 60,
            estimatedCalories = 550,
            description = "优化蛙泳技术，增强游泳耐力",
            iconResId = com.example.myfitnessapp.R.drawable.ic_swimming
        ),
        WorkoutCourse(
            id = "swimming_003",
            title = "混合泳姿挑战",
            sportType = "SWIMMING",
            level = "高级",
            durationMinutes = 90,
            estimatedCalories = 800,
            description = "多种泳姿切换训练，全面提升游泳能力",
            iconResId = com.example.myfitnessapp.R.drawable.ic_swimming
        )
    )

    // ============================================================
    // 跳绳课程
    // ============================================================
    private val jumpRopeCourses = listOf(
        WorkoutCourse(
            id = "jump_rope_001",
            title = "跳绳基础入门",
            sportType = "JUMP_ROPE",
            level = "初学者",
            durationMinutes = 15,
            estimatedCalories = 200,
            description = "掌握跳绳基本节奏，建立协调性",
            iconResId = com.example.myfitnessapp.R.drawable.ic_jump_rope
        ),
        WorkoutCourse(
            id = "jump_rope_002",
            title = "花样跳绳训练",
            sportType = "JUMP_ROPE",
            level = "中级",
            durationMinutes = 25,
            estimatedCalories = 350,
            description = "学习多种跳绳技巧，增加趣味性",
            iconResId = com.example.myfitnessapp.R.drawable.ic_jump_rope
        ),
        WorkoutCourse(
            id = "jump_rope_003",
            title = "高强度跳绳挑战",
            sportType = "JUMP_ROPE",
            level = "高级",
            durationMinutes = 30,
            estimatedCalories = 450,
            description = "高强度间歇跳绳，快速燃脂",
            iconResId = com.example.myfitnessapp.R.drawable.ic_jump_rope
        )
    )

    // ============================================================
    // 根据运动类型获取课程列表
    // ============================================================
    fun getCoursesBySportType(sportType: String): List<WorkoutCourse> {
        return when (sportType.uppercase()) {
            "RUN" -> runningCourses
            "CYCLING" -> cyclingCourses
            "YOGA" -> yogaCourses
            "STRENGTH" -> strengthCourses
            "SWIMMING" -> swimmingCourses
            "JUMP_ROPE" -> jumpRopeCourses
            else -> emptyList()
        }
    }

    // ============================================================
    // 根据课程ID获取课程详情
    // ============================================================
    fun getCourseById(courseId: String): WorkoutCourse? {
        val allCourses = runningCourses + cyclingCourses + yogaCourses +
                         strengthCourses + swimmingCourses + jumpRopeCourses
        return allCourses.find { it.id == courseId }
    }
}