package com.example.myfitnessapp.model.workout

enum class WorkoutType(val label: String) {
    RUN("跑步"),
    CYCLING("骑行"),
    JUMP_ROPE("跳绳"),
    STRENGTH("力量训练"),
    SWIMMING("游泳"),
    YOGA("瑜伽");

    companion object {
        fun fromLabel(label: String): WorkoutType {
            return entries.firstOrNull { it.label == label } ?: RUN
        }
    }
}

data class WorkoutState(
    val type: WorkoutType,
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
    val calories: Int = 0
)

data class OutdoorWorkoutState(
    val base: WorkoutState = WorkoutState(type = WorkoutType.RUN),
    val distance: Double = 0.0,
    val speed: Double = 0.0,
    val pace: String = "--"
)

data class JumpRopeState(
    val base: WorkoutState = WorkoutState(type = WorkoutType.JUMP_ROPE),
    val count: Int = 0,
    val frequency: Int = 0,
    val tripCount: Int = 0,
    val targetMode: String = "free",
    val targetValue: Int = 0
)

data class StrengthExercise(
    val name: String,
    val sets: Int,
    val targetReps: Int,
    val targetWeight: Double
)

data class StrengthState(
    val base: WorkoutState = WorkoutState(type = WorkoutType.STRENGTH),
    val exercises: List<StrengthExercise> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val completedSets: Int = 0,
    val restSeconds: Int = 0,
    val isResting: Boolean = false,
    val totalVolume: Double = 0.0
)

data class YogaPose(
    val name: String,
    val durationSeconds: Int,
    val instruction: String,
    val breathIn: Boolean = true
)

data class YogaState(
    val base: WorkoutState = WorkoutState(type = WorkoutType.YOGA),
    val poses: List<YogaPose> = emptyList(),
    val currentPoseIndex: Int = 0,
    val poseRemainingSeconds: Int = 0,
    val isBreathIn: Boolean = true,
    val difficulty: Int = 0
)