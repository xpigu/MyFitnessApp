package com.example.myfitnessapp.course.data

import com.example.myfitnessapp.course.domain.CoursePlan
import com.example.myfitnessapp.course.domain.CourseStep
import com.example.myfitnessapp.course.domain.CourseStepType
import com.example.myfitnessapp.course.domain.TrainingCourse
import com.example.myfitnessapp.data.model.CourseRepository as LegacyCourseRepository
import com.example.myfitnessapp.data.model.WorkoutCourse as LegacyWorkoutCourse

class TrainingCourseRepository {

    fun getCoursesBySportType(sportType: String): List<TrainingCourse> {
        return LegacyCourseRepository.getCoursesBySportType(sportType)
            .map { it.toTrainingCourse() }
    }

    fun getCourseById(courseId: String): TrainingCourse? {
        return LegacyCourseRepository.getCourseById(courseId)?.toTrainingCourse()
    }

    private fun LegacyWorkoutCourse.toTrainingCourse(): TrainingCourse {
        return TrainingCourse(
            id = id,
            title = title,
            sportType = sportType,
            level = level,
            durationMinutes = durationMinutes,
            estimatedCalories = estimatedCalories,
            description = description,
            iconResId = iconResId,
            goal = buildGoal(),
            equipment = buildEquipment(),
            tags = buildTags(),
            plan = buildPlan()
        )
    }

    private fun LegacyWorkoutCourse.buildGoal(): String {
        return when (sportType) {
            "RUN" -> "提升跑步耐力与节奏控制"
            "CYCLING" -> "增强有氧能力和腿部耐力"
            "YOGA" -> "改善柔韧性与身体控制"
            "STRENGTH" -> "完成力量动作并稳定提升训练容量"
            "SWIMMING" -> "建立持续游进能力并优化泳姿"
            "JUMP_ROPE" -> "提高心肺耐力和节奏协调性"
            else -> "完成一节结构化课程训练"
        }
    }

    private fun LegacyWorkoutCourse.buildEquipment(): List<String> {
        return when (sportType) {
            "RUN" -> listOf("跑鞋", "运动手表")
            "CYCLING" -> listOf("自行车", "头盔")
            "YOGA" -> listOf("瑜伽垫")
            "STRENGTH" -> listOf("训练凳", "哑铃/杠铃")
            "SWIMMING" -> listOf("泳衣", "泳镜")
            "JUMP_ROPE" -> listOf("跳绳")
            else -> emptyList()
        }
    }

    private fun LegacyWorkoutCourse.buildTags(): List<String> {
        val baseTag = when (sportType) {
            "RUN", "CYCLING", "SWIMMING", "JUMP_ROPE" -> "有氧"
            "YOGA" -> "恢复"
            "STRENGTH" -> "力量"
            else -> "训练"
        }
        return listOf(baseTag, level, "${durationMinutes}分钟")
    }

    private fun LegacyWorkoutCourse.buildPlan(): CoursePlan {
        return when (sportType) {
            "RUN" -> buildRunningPlan()
            "CYCLING" -> buildCyclingPlan()
            "YOGA" -> buildYogaPlan()
            "STRENGTH" -> buildStrengthPlan()
            "SWIMMING" -> CoursePlan(
                steps = listOf(
                    CourseStep("岸上热身", CourseStepType.WARMUP, 180, "活动肩关节和髋部", "关节热开"),
                    CourseStep("泳池主训练", CourseStepType.MAIN, durationMinutes * 60 - 300, description, title),
                    CourseStep("放松游", CourseStepType.COOLDOWN, 120, "低强度划水并整理呼吸", "轻松节奏")
                )
            )
            "JUMP_ROPE" -> buildJumpRopePlan()
            else -> CoursePlan(
                steps = listOf(
                    CourseStep("热身", CourseStepType.WARMUP, 180, "完成通用热身"),
                    CourseStep("主训练", CourseStepType.MAIN, durationMinutes * 60 - 300, description, title),
                    CourseStep("放松", CourseStepType.COOLDOWN, 120, "完成拉伸和呼吸恢复")
                )
            )
        }
    }

    private fun LegacyWorkoutCourse.buildRunningPlan(): CoursePlan {
        val steps = when (id) {
            "run_001" -> listOf(
                CourseStep("热身步行", CourseStepType.WARMUP, 300, "快走并活动踝膝髋关节", "舒适配速"),
                CourseStep("轻松慢跑", CourseStepType.MAIN, 600, "用能顺畅说话的节奏建立跑感", "6'40''-7'00''/km"),
                CourseStep("稳定推进", CourseStepType.OUTDOOR_INTERVAL, 600, "把呼吸和步频稳定下来", "6'10''-6'30''/km"),
                CourseStep("放松慢走", CourseStepType.COOLDOWN, 300, "逐步降低心率并拉伸小腿", "轻松走")
            )
            "run_002" -> listOf(
                CourseStep("动态热身", CourseStepType.WARMUP, 300, "快走 + 开合跳唤醒身体", "热开身体"),
                CourseStep("进入节奏", CourseStepType.MAIN, 240, "先建立基础呼吸和步频", "6'20''-6'40''/km"),
                CourseStep("间歇冲刺 1", CourseStepType.OUTDOOR_INTERVAL, 120, "提升步频，注意摆臂", "5'10''-5'30''/km"),
                CourseStep("恢复慢跑", CourseStepType.REST, 60, "放慢到恢复配速", "6'50''/km 左右"),
                CourseStep("间歇冲刺 2", CourseStepType.OUTDOOR_INTERVAL, 120, "保持髋部稳定和落地节奏", "5'10''-5'30''/km"),
                CourseStep("恢复慢跑", CourseStepType.REST, 60, "恢复呼吸准备下组", "6'50''/km 左右"),
                CourseStep("间歇冲刺 3", CourseStepType.OUTDOOR_INTERVAL, 120, "维持冲刺节奏", "5'10''-5'30''/km"),
                CourseStep("恢复慢跑", CourseStepType.REST, 60, "让心率略微回落", "6'50''/km 左右"),
                CourseStep("间歇冲刺 4", CourseStepType.OUTDOOR_INTERVAL, 120, "完成最后一组加速", "5'10''-5'30''/km"),
                CourseStep("冷却整理", CourseStepType.COOLDOWN, 600, "慢跑转步行，整理呼吸", "轻松走")
            )
            "run_003" -> listOf(
                CourseStep("热身步行", CourseStepType.WARMUP, 300, "激活踝膝髋并调整呼吸", "舒适配速"),
                CourseStep("耐力建立", CourseStepType.MAIN, 900, "均匀配速跑，避免前快后慢", "6'20''-6'40''/km"),
                CourseStep("稳态推进", CourseStepType.OUTDOOR_INTERVAL, 900, "控制在可持续但有压力的节奏", "5'50''-6'10''/km"),
                CourseStep("节奏收尾", CourseStepType.MAIN, 300, "保持稳定跑姿完成最后推进", "6'00''/km 左右"),
                CourseStep("放松慢走", CourseStepType.COOLDOWN, 300, "拉长呼吸，降低心率", "轻松走")
            )
            else -> listOf(
                CourseStep("热身步行", CourseStepType.WARMUP, 300, "先快走并活动髋膝踝关节", "舒适配速"),
                CourseStep("主训练", CourseStepType.OUTDOOR_INTERVAL, durationMinutes * 60 - 600, description, title),
                CourseStep("放松慢走", CourseStepType.COOLDOWN, 300, "逐步降低心率并放松小腿", "轻松走")
            )
        }
        return CoursePlan(steps = steps)
    }

    private fun LegacyWorkoutCourse.buildYogaPlan(): CoursePlan {
        val steps = when (id) {
            "yoga_001" -> listOf(
                CourseStep("呼吸进入", CourseStepType.WARMUP, 90, "盘坐调整呼吸，放松肩颈", "吸气"),
                CourseStep("猫牛式", CourseStepType.POSE, 90, "吸气弓背，呼气塌腰，唤醒脊柱", "吸气"),
                CourseStep("下犬式", CourseStepType.POSE, 120, "双手双脚撑地，延展后侧链", "呼气"),
                CourseStep("战士一式", CourseStepType.POSE, 150, "稳定核心，向上延展躯干", "吸气"),
                CourseStep("战士二式", CourseStepType.POSE, 150, "打开髋部，手臂向两侧延展", "呼气"),
                CourseStep("三角式", CourseStepType.POSE, 150, "保持侧腰延展和胸腔打开", "吸气"),
                CourseStep("坐姿前屈", CourseStepType.POSE, 150, "从髋部折叠身体，拉伸后侧链", "呼气"),
                CourseStep("婴儿式", CourseStepType.COOLDOWN, 120, "完全放松背部和肩颈", "呼气"),
                CourseStep("挺尸式", CourseStepType.COOLDOWN, 180, "自然呼吸，完成身心放松", "缓慢呼吸")
            )
            "yoga_002" -> listOf(
                CourseStep("呼吸唤醒", CourseStepType.WARMUP, 90, "找到核心收紧和骨盆中立", "吸气"),
                CourseStep("平板支撑", CourseStepType.POSE, 120, "肩胛稳定，腹部持续发力", "呼气"),
                CourseStep("船式", CourseStepType.POSE, 120, "保持脊柱延展和核心发力", "吸气"),
                CourseStep("战士三式", CourseStepType.POSE, 150, "核心收紧，保持身体平衡", "呼气"),
                CourseStep("侧板支撑", CourseStepType.POSE, 120, "感受侧腰和肩部稳定", "吸气"),
                CourseStep("桥式", CourseStepType.POSE, 150, "臀腿发力，带动骨盆上提", "呼气"),
                CourseStep("仰卧扭转", CourseStepType.COOLDOWN, 150, "释放腰背紧张", "呼气"),
                CourseStep("挺尸式", CourseStepType.COOLDOWN, 180, "恢复呼吸，结束训练", "缓慢呼吸")
            )
            "yoga_003" -> listOf(
                CourseStep("静坐呼吸", CourseStepType.WARMUP, 120, "慢慢放松肩颈和下颌", "缓慢呼吸"),
                CourseStep("婴儿式", CourseStepType.POSE, 150, "放松背阔肌与腰背", "呼气"),
                CourseStep("坐姿侧屈", CourseStepType.POSE, 150, "延展身体两侧", "吸气"),
                CourseStep("蝴蝶式", CourseStepType.POSE, 180, "打开髋部，放松腿内侧", "呼气"),
                CourseStep("鸽子式", CourseStepType.POSE, 180, "温和释放臀部和髋部", "呼气"),
                CourseStep("仰卧扭转", CourseStepType.POSE, 150, "让脊柱逐步放松", "吸气"),
                CourseStep("靠墙抬腿", CourseStepType.COOLDOWN, 150, "恢复双腿，缓解疲劳", "缓慢呼吸"),
                CourseStep("挺尸式", CourseStepType.COOLDOWN, 270, "停留在平静呼吸中结束课程", "缓慢呼吸")
            )
            else -> listOf(
                CourseStep("呼吸进入", CourseStepType.WARMUP, 120, "先调整呼吸，放松肩颈", "匀速呼吸"),
                CourseStep("核心体式练习", CourseStepType.POSE, durationMinutes * 60 - 240, description, title),
                CourseStep("舒缓收尾", CourseStepType.COOLDOWN, 120, "保持身体稳定，完成放松", "缓慢呼吸")
            )
        }
        return CoursePlan(steps = steps)
    }

    private fun LegacyWorkoutCourse.buildStrengthPlan(): CoursePlan {
        val steps = when (id) {
            "strength_001" -> listOf(
                CourseStep("肩背热身", CourseStepType.WARMUP, 180, "弹力带拉伸和肩部环绕唤醒上肢", "2 轮"),
                CourseStep("哑铃肩推", CourseStepType.STRENGTH_SET, 360, "保持核心稳定，向上推起后缓慢下放", "4组 x 10次 x 12kg"),
                CourseStep("俯身划船", CourseStepType.STRENGTH_SET, 300, "夹紧肩胛骨，手肘沿身体两侧发力", "3组 x 12次 x 16kg"),
                CourseStep("侧平举", CourseStepType.STRENGTH_SET, 240, "控制离心阶段，避免耸肩代偿", "3组 x 15次 x 6kg"),
                CourseStep("整理拉伸", CourseStepType.COOLDOWN, 180, "拉伸肩胸和上背，记录今日体感", "3 分钟")
            )
            "strength_002" -> listOf(
                CourseStep("全身激活", CourseStepType.WARMUP, 240, "开合跳、深蹲和动态箭步蹲组合热身", "2 轮"),
                CourseStep("高脚杯深蹲", CourseStepType.STRENGTH_SET, 360, "脚掌踩稳，膝盖朝脚尖方向发力", "4组 x 12次 x 18kg"),
                CourseStep("罗马尼亚硬拉", CourseStepType.STRENGTH_SET, 360, "髋主导发力，感受臀腿后侧拉伸", "4组 x 10次 x 24kg"),
                CourseStep("俯卧撑", CourseStepType.STRENGTH_SET, 240, "身体保持一条直线，胸部主动下放", "3组 x 12次 x 自重"),
                CourseStep("平板支撑", CourseStepType.STRENGTH_SET, 180, "核心收紧，避免塌腰", "3组 x 45次 x 自重"),
                CourseStep("放松整理", CourseStepType.COOLDOWN, 180, "拉伸股四头肌、腘绳肌与胸肩", "3 分钟")
            )
            "strength_003" -> listOf(
                CourseStep("高强度热身", CourseStepType.WARMUP, 240, "动态热身并提高心率，准备进入重训练", "3 轮"),
                CourseStep("杠铃深蹲", CourseStepType.STRENGTH_SET, 420, "下蹲时保持核心和躯干稳定", "5组 x 8次 x 50kg"),
                CourseStep("杠铃卧推", CourseStepType.STRENGTH_SET, 360, "控制下放速度，顶峰主动发力", "4组 x 8次 x 45kg"),
                CourseStep("硬拉", CourseStepType.STRENGTH_SET, 360, "发力前锁紧背部，杠铃贴腿上提", "4组 x 6次 x 70kg"),
                CourseStep("波比跳冲刺", CourseStepType.STRENGTH_SET, 180, "控制节奏完成爆发性收尾", "3组 x 12次 x 自重"),
                CourseStep("冷却拉伸", CourseStepType.COOLDOWN, 180, "重点放松下背、臀腿和胸肩", "3 分钟")
            )
            else -> listOf(
                CourseStep("动态热身", CourseStepType.WARMUP, 180, "活动肩背、核心和关节", "8-10 次"),
                CourseStep("动作组训练", CourseStepType.STRENGTH_SET, durationMinutes * 60 - 300, description, title),
                CourseStep("整理放松", CourseStepType.COOLDOWN, 120, "拉伸目标肌群并记录反馈", "5 分钟内完成")
            )
        }
        return CoursePlan(steps = steps)
    }

    private fun LegacyWorkoutCourse.buildCyclingPlan(): CoursePlan {
        val steps = when (id) {
            "cycling_001" -> listOf(
                CourseStep("轻松热身骑行", CourseStepType.WARMUP, 300, "低阻力骑行，让腿部逐步进入状态", "80-90 rpm"),
                CourseStep("匀速巡航", CourseStepType.MAIN, 900, "保持均匀踏频，专注呼吸与坐姿稳定", "20-23 km/h"),
                CourseStep("节奏推进", CourseStepType.MAIN, 600, "略微提高输出，维持稳定踩踏", "24-26 km/h"),
                CourseStep("低强度冷却", CourseStepType.COOLDOWN, 300, "逐步降低阻力恢复呼吸", "轻松踩踏")
            )
            "cycling_002" -> listOf(
                CourseStep("平路热身", CourseStepType.WARMUP, 300, "低阻力热身并激活臀腿发力", "80 rpm"),
                CourseStep("坡度推进 1", CourseStepType.MAIN, 360, "提高阻力模拟爬坡，保持坐姿稳定", "70-75 rpm"),
                CourseStep("坡顶恢复", CourseStepType.REST, 120, "降低阻力恢复呼吸和腿部状态", "轻松踩踏"),
                CourseStep("坡度推进 2", CourseStepType.MAIN, 360, "第二段持续发力，保持核心收紧", "70-75 rpm"),
                CourseStep("坡顶恢复", CourseStepType.REST, 120, "控制节奏准备最后一段", "轻松踩踏"),
                CourseStep("坡度冲刺", CourseStepType.MAIN, 300, "最后一段爬坡输出，完成强度刺激", "26-28 km/h"),
                CourseStep("冷却整理", CourseStepType.COOLDOWN, 300, "放松双腿并恢复心率", "轻松踩踏")
            )
            "cycling_003" -> listOf(
                CourseStep("竞速热身", CourseStepType.WARMUP, 300, "逐步提升踏频，准备高速度输出", "85-95 rpm"),
                CourseStep("高速巡航", CourseStepType.MAIN, 900, "维持低风阻姿态和稳定输出", "28-30 km/h"),
                CourseStep("加速推进", CourseStepType.OUTDOOR_INTERVAL, 480, "短时间提高功率，建立竞速感", "32-35 km/h"),
                CourseStep("恢复骑行", CourseStepType.REST, 180, "降低速度让腿部恢复", "22-24 km/h"),
                CourseStep("冲线收尾", CourseStepType.OUTDOOR_INTERVAL, 360, "最后一段高节奏输出", "32-36 km/h"),
                CourseStep("低阻力冷却", CourseStepType.COOLDOWN, 300, "慢慢恢复呼吸并整理腿部状态", "轻松踩踏")
            )
            else -> listOf(
                CourseStep("轻松骑行热身", CourseStepType.WARMUP, 300, "低阻力热身并检查踏频", "90 rpm 左右"),
                CourseStep("主骑行阶段", CourseStepType.MAIN, durationMinutes * 60 - 600, description, title),
                CourseStep("低强度冷却", CourseStepType.COOLDOWN, 300, "降低阻力恢复呼吸", "轻松踩踏")
            )
        }
        return CoursePlan(steps = steps)
    }

    private fun LegacyWorkoutCourse.buildJumpRopePlan(): CoursePlan {
        val steps = when (id) {
            "jump_rope_001" -> listOf(
                CourseStep("热身小跳", CourseStepType.WARMUP, 90, "低频率小跳，先找到节奏", "90 次/分钟"),
                CourseStep("基础跳 1", CourseStepType.MAIN, 120, "双脚并跳，保持手腕发力", "110 次/分钟"),
                CourseStep("恢复踏步", CourseStepType.REST, 45, "放慢频率调整呼吸", "轻松恢复"),
                CourseStep("基础跳 2", CourseStepType.MAIN, 120, "保持稳定弹跳和落地缓冲", "115 次/分钟"),
                CourseStep("恢复踏步", CourseStepType.REST, 45, "轻松恢复，准备最后一段", "轻松恢复"),
                CourseStep("稳定收尾", CourseStepType.MAIN, 120, "把节奏保持到课程结束", "120 次/分钟"),
                CourseStep("放松整理", CourseStepType.COOLDOWN, 90, "步行放松并拉伸小腿", "恢复心率")
            )
            "jump_rope_002" -> listOf(
                CourseStep("花样热身", CourseStepType.WARMUP, 90, "交替步和并脚跳热身", "100 次/分钟"),
                CourseStep("交替步", CourseStepType.MAIN, 120, "左右交替落地，保持肩颈放松", "125 次/分钟"),
                CourseStep("左右侧摆", CourseStepType.MAIN, 90, "用手腕控制绳路和节奏", "120 次/分钟"),
                CourseStep("恢复小跳", CourseStepType.REST, 45, "回到基础小跳恢复呼吸", "轻松恢复"),
                CourseStep("开合跳", CourseStepType.MAIN, 120, "控制节奏，注意核心稳定", "130 次/分钟"),
                CourseStep("花样串联", CourseStepType.OUTDOOR_INTERVAL, 120, "交替步、并脚跳和侧摆连贯完成", "135 次/分钟"),
                CourseStep("放松整理", CourseStepType.COOLDOWN, 90, "小跳转步行，完成放松", "恢复心率")
            )
            "jump_rope_003" -> listOf(
                CourseStep("高频热身", CourseStepType.WARMUP, 90, "逐步提高频率，让身体进入状态", "110 次/分钟"),
                CourseStep("冲刺回合 1", CourseStepType.OUTDOOR_INTERVAL, 90, "高频快速跳，保持小幅度离地", "150 次/分钟"),
                CourseStep("恢复回合", CourseStepType.REST, 45, "降低频率恢复呼吸", "100 次/分钟"),
                CourseStep("冲刺回合 2", CourseStepType.OUTDOOR_INTERVAL, 90, "继续保持高频输出", "155 次/分钟"),
                CourseStep("恢复回合", CourseStepType.REST, 45, "恢复心率准备下一轮", "100 次/分钟"),
                CourseStep("冲刺回合 3", CourseStepType.OUTDOOR_INTERVAL, 90, "最后一轮冲刺，保持节奏", "160 次/分钟"),
                CourseStep("稳定收尾", CourseStepType.MAIN, 120, "回到可控节奏完成收尾", "125 次/分钟"),
                CourseStep("步行冷却", CourseStepType.COOLDOWN, 90, "放松小腿和跟腱", "恢复心率")
            )
            else -> listOf(
                CourseStep("节奏热身", CourseStepType.WARMUP, 120, "先做低频率跳跃热身", "轻松频率"),
                CourseStep("跳绳主训练", CourseStepType.MAIN, durationMinutes * 60 - 240, description, title),
                CourseStep("步行放松", CourseStepType.COOLDOWN, 120, "整理呼吸并拉伸小腿", "恢复心率")
            )
        }
        return CoursePlan(steps = steps)
    }
}
