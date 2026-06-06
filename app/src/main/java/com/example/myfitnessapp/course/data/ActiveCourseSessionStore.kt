package com.example.myfitnessapp.course.data

import android.content.Context
import com.example.myfitnessapp.course.domain.ActiveCourseSession
import com.example.myfitnessapp.course.domain.CourseSessionStatus

class ActiveCourseSessionStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(session: ActiveCourseSession) {
        prefs.edit()
            .putString(KEY_COURSE_ID, session.courseId)
            .putString(KEY_COURSE_TITLE, session.courseTitle)
            .putString(KEY_SPORT_TYPE, session.sportType)
            .putString(KEY_STATUS, session.status.name)
            .putLong(KEY_STARTED_AT, session.startedAt)
            .putInt(KEY_CURRENT_STEP_INDEX, session.currentStepIndex)
            .putInt(KEY_CURRENT_STEP_ELAPSED_SECONDS, session.currentStepElapsedSeconds)
            .putInt(KEY_TOTAL_ELAPSED_SECONDS, session.totalElapsedSeconds)
            .putInt(KEY_METRIC_PRIMARY, session.metricPrimary)
            .putInt(KEY_METRIC_SECONDARY, session.metricSecondary)
            .putInt(KEY_METRIC_TERTIARY, session.metricTertiary)
            .putFloat(KEY_DECIMAL_PRIMARY, session.decimalPrimary)
            .putFloat(KEY_DECIMAL_SECONDARY, session.decimalSecondary)
            .apply()
    }

    fun getActive(): ActiveCourseSession? {
        val courseId = prefs.getString(KEY_COURSE_ID, null) ?: return null
        val title = prefs.getString(KEY_COURSE_TITLE, null) ?: return null
        val sportType = prefs.getString(KEY_SPORT_TYPE, null) ?: return null
        val status = prefs.getString(KEY_STATUS, null)
            ?.let { CourseSessionStatus.valueOf(it) }
            ?: CourseSessionStatus.IN_PROGRESS
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        return ActiveCourseSession(
            courseId = courseId,
            courseTitle = title,
            sportType = sportType,
            status = status,
            startedAt = startedAt,
            currentStepIndex = prefs.getInt(KEY_CURRENT_STEP_INDEX, 0),
            currentStepElapsedSeconds = prefs.getInt(KEY_CURRENT_STEP_ELAPSED_SECONDS, 0),
            totalElapsedSeconds = prefs.getInt(KEY_TOTAL_ELAPSED_SECONDS, 0),
            metricPrimary = prefs.getInt(KEY_METRIC_PRIMARY, 0),
            metricSecondary = prefs.getInt(KEY_METRIC_SECONDARY, 0),
            metricTertiary = prefs.getInt(KEY_METRIC_TERTIARY, 0),
            decimalPrimary = prefs.getFloat(KEY_DECIMAL_PRIMARY, 0f),
            decimalSecondary = prefs.getFloat(KEY_DECIMAL_SECONDARY, 0f)
        )
    }

    fun getActiveFor(courseId: String?): ActiveCourseSession? {
        val active = getActive() ?: return null
        return if (courseId != null && active.courseId == courseId) active else null
    }

    fun isCourseActive(courseId: String): Boolean {
        return getActive()?.courseId == courseId
    }

    fun clear(courseId: String? = null) {
        if (courseId != null && !isCourseActive(courseId)) return
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "active_course_session"
        private const val KEY_COURSE_ID = "course_id"
        private const val KEY_COURSE_TITLE = "course_title"
        private const val KEY_SPORT_TYPE = "sport_type"
        private const val KEY_STATUS = "status"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_CURRENT_STEP_INDEX = "current_step_index"
        private const val KEY_CURRENT_STEP_ELAPSED_SECONDS = "current_step_elapsed_seconds"
        private const val KEY_TOTAL_ELAPSED_SECONDS = "total_elapsed_seconds"
        private const val KEY_METRIC_PRIMARY = "metric_primary"
        private const val KEY_METRIC_SECONDARY = "metric_secondary"
        private const val KEY_METRIC_TERTIARY = "metric_tertiary"
        private const val KEY_DECIMAL_PRIMARY = "decimal_primary"
        private const val KEY_DECIMAL_SECONDARY = "decimal_secondary"
    }
}
