package com.example.myfitnessapp.course.domain

data class TrainingCourse(
    val id: String,
    val title: String,
    val sportType: String,
    val level: String,
    val durationMinutes: Int,
    val estimatedCalories: Int,
    val description: String,
    val iconResId: Int,
    val goal: String,
    val equipment: List<String>,
    val tags: List<String>,
    val plan: CoursePlan
) {
    fun formatCardInfo(): String {
        return "$level · ${durationMinutes}分钟 · 约${estimatedCalories} kcal"
    }

    fun formatEquipment(): String {
        return if (equipment.isEmpty()) "无需器械" else equipment.joinToString("、")
    }
}

data class CoursePlan(
    val steps: List<CourseStep>
)

data class CourseStep(
    val title: String,
    val stepType: CourseStepType,
    val durationSeconds: Int,
    val instruction: String,
    val target: String? = null
)

enum class CourseStepType {
    WARMUP,
    MAIN,
    REST,
    COOLDOWN,
    POSE,
    STRENGTH_SET,
    FREE_TRAIN,
    OUTDOOR_INTERVAL
}

enum class CourseSessionStatus {
    IN_PROGRESS
}

data class ActiveCourseSession(
    val courseId: String,
    val courseTitle: String,
    val sportType: String,
    val status: CourseSessionStatus,
    val startedAt: Long,
    val currentStepIndex: Int = 0,
    val currentStepElapsedSeconds: Int = 0,
    val totalElapsedSeconds: Int = 0,
    val metricPrimary: Int = 0,
    val metricSecondary: Int = 0,
    val metricTertiary: Int = 0,
    val decimalPrimary: Float = 0f,
    val decimalSecondary: Float = 0f
)
