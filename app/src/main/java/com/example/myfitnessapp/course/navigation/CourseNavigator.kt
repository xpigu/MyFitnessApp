package com.example.myfitnessapp.course.navigation

import android.content.Context
import android.content.Intent
import com.example.myfitnessapp.JumpRopeActivity
import com.example.myfitnessapp.StrengthActivity
import com.example.myfitnessapp.SwimmingActivity
import com.example.myfitnessapp.WorkoutTrackingActivity
import com.example.myfitnessapp.YogaActivity
import com.example.myfitnessapp.course.data.ActiveCourseSessionStore
import com.example.myfitnessapp.course.domain.ActiveCourseSession
import com.example.myfitnessapp.course.domain.CourseSessionStatus
import com.example.myfitnessapp.course.domain.TrainingCourse

class CourseNavigator(
    private val sessionStore: ActiveCourseSessionStore
) {

    fun openCourse(context: Context, course: TrainingCourse) {
        val existingSession = sessionStore.getActiveFor(course.id)
        if (existingSession == null) {
            sessionStore.save(
                ActiveCourseSession(
                    courseId = course.id,
                    courseTitle = course.title,
                    sportType = course.sportType,
                    status = CourseSessionStatus.IN_PROGRESS,
                    startedAt = System.currentTimeMillis()
                )
            )
        }

        val intent = when (course.sportType) {
            "RUN", "CYCLING" -> Intent(context, WorkoutTrackingActivity::class.java).apply {
                putExtra(WorkoutTrackingActivity.EXTRA_SPORT_NAME, course.title)
                putExtra(WorkoutTrackingActivity.EXTRA_SPORT_ICON, course.iconResId)
                putExtra(WorkoutTrackingActivity.EXTRA_SPORT_TYPE, course.sportType)
            }
            "YOGA" -> Intent(context, YogaActivity::class.java)
            "STRENGTH" -> Intent(context, StrengthActivity::class.java)
            "SWIMMING" -> Intent(context, SwimmingActivity::class.java)
            "JUMP_ROPE" -> Intent(context, JumpRopeActivity::class.java)
            else -> Intent(context, WorkoutTrackingActivity::class.java).apply {
                putExtra(WorkoutTrackingActivity.EXTRA_SPORT_NAME, course.title)
                putExtra(WorkoutTrackingActivity.EXTRA_SPORT_ICON, course.iconResId)
                putExtra(WorkoutTrackingActivity.EXTRA_SPORT_TYPE, "RUN")
            }
        }.apply {
            putExtra(EXTRA_COURSE_ID, course.id)
            putExtra(EXTRA_COURSE_TITLE, course.title)
            putExtra(EXTRA_COURSE_DURATION, course.durationMinutes)
            putExtra(EXTRA_COURSE_CALORIES, course.estimatedCalories)
            putExtra(EXTRA_COURSE_GOAL, course.goal)
        }

        context.startActivity(intent)
    }

    companion object Contract {
        const val EXTRA_COURSE_ID = "extra_course_id"
        const val EXTRA_COURSE_TITLE = "extra_course_title"
        const val EXTRA_COURSE_DURATION = "extra_course_duration"
        const val EXTRA_COURSE_CALORIES = "extra_course_calories"
        const val EXTRA_COURSE_GOAL = "extra_course_goal"

        fun courseIdOf(intent: Intent?): String? {
            return intent?.getStringExtra(EXTRA_COURSE_ID)
        }
    }
}
