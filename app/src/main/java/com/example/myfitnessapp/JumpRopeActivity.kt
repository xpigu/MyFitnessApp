package com.example.myfitnessapp

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.course.data.ActiveCourseSessionStore
import com.example.myfitnessapp.course.data.TrainingCourseRepository
import com.example.myfitnessapp.course.domain.CourseSessionStatus
import com.example.myfitnessapp.course.domain.CourseStep
import com.example.myfitnessapp.course.domain.TrainingCourse
import com.example.myfitnessapp.course.navigation.CourseNavigator
import com.example.myfitnessapp.data.WorkoutRecordHelper
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

class JumpRopeActivity : AppCompatActivity() {

    private lateinit var tvCount: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvFrequency: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnStart: TextView
    private lateinit var btnStop: TextView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvCourseInfo: TextView
    private lateinit var tvRoundDetail: TextView

    private var count = 0
    private var elapsedSeconds = 0
    private var calories = 0
    private var isActive = false
    private var isPaused = false
    private var currentRoundIndex = 0
    private var currentRoundElapsedSeconds = 0
    private var shouldPersistCourseSession = true
    private lateinit var viewModel: WorkoutRecordViewModel
    private val sessionStore by lazy { ActiveCourseSessionStore(this) }
    private val courseRepository by lazy { TrainingCourseRepository() }
    private var activeCourse: TrainingCourse? = null
    private val handler = Handler(Looper.getMainLooper())
    private val countAnimatorInterval = 800L

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isActive && !isPaused) {
                elapsedSeconds++
                currentRoundElapsedSeconds++
                count += currentJumpIncrement()
                syncRoundProgress()
                updateTimerDisplay()
                updateCountDisplay()
                updateCalories()
                updateFrequency()
                persistCourseSessionProgress()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jump_rope)

        viewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)

        tvCount = findViewById(R.id.tv_jump_count)
        tvTimer = findViewById(R.id.tv_jump_timer)
        tvFrequency = findViewById(R.id.tv_jump_frequency)
        tvCalories = findViewById(R.id.tv_jump_calories)
        tvStatus = findViewById(R.id.tv_jump_status)
        btnStart = findViewById(R.id.btn_jump_start)
        btnStop = findViewById(R.id.btn_jump_stop)
        tvHeaderTitle = findViewById(R.id.tv_jump_header_title)
        tvCourseInfo = findViewById(R.id.tv_jump_course_info)
        tvRoundDetail = findViewById(R.id.tv_jump_round_detail)

        loadCourseRuntime()

        findViewById<View>(R.id.btn_jump_back).setOnClickListener {
            handleExitNavigation()
        }

        btnStart.setOnClickListener {
            if (!isActive) {
                startWorkout()
            } else if (!isPaused) {
                pauseWorkout()
            } else {
                resumeWorkout()
            }
        }

        btnStop.setOnClickListener {
            if (isActive) {
                showSummary()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleExitNavigation()
            }
        })
    }

    private fun handleExitNavigation() {
        if (isActive) {
            showSummary()
        } else {
            finish()
        }
    }

    private fun startWorkout() {
        shouldPersistCourseSession = true
        isActive = true
        isPaused = false
        btnStart.text = "暂停"
        updateJumpStatus()
        handler.post(timerRunnable)
    }

    private fun pauseWorkout() {
        isPaused = true
        btnStart.text = "继续"
        updateJumpStatus()
        handler.removeCallbacks(timerRunnable)
        persistCourseSessionProgress()
    }

    private fun resumeWorkout() {
        isPaused = false
        btnStart.text = "暂停"
        updateJumpStatus()
        handler.post(timerRunnable)
    }

    private fun updateCountDisplay() {
        val anim = ValueAnimator.ofInt(tvCount.text.toString().toIntOrNull() ?: 0, count)
        anim.duration = 300
        anim.addUpdateListener { tvCount.text = it.animatedValue.toString() }
        anim.start()
    }

    private fun updateTimerDisplay() {
        val min = elapsedSeconds / 60
        val sec = elapsedSeconds % 60
        tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }

    private fun updateCalories() {
        calories = (elapsedSeconds * 0.15).toInt()
        tvCalories.text = calories.toString()
    }

    private fun updateFrequency() {
        if (elapsedSeconds > 0) {
            val freq = (count * 60) / elapsedSeconds
            tvFrequency.text = freq.toString()
        }
    }

    private fun showSummary() {
        isActive = false
        isPaused = false
        handler.removeCallbacks(timerRunnable)
        btnStart.text = "开始"
        tvStatus.text = "已完成"

        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_workout_summary, null)
        dialog.setContentView(sheetView)

        sheetView.findViewById<TextView>(R.id.summary_tv_duration).text = tvTimer.text
        sheetView.findViewById<TextView>(R.id.summary_tv_distance).text = "$count 个"
        sheetView.findViewById<TextView>(R.id.summary_tv_calories).text = "$calories kcal"
        sheetView.findViewById<TextView>(R.id.summary_tv_avg_pace).text = "频率: ${tvFrequency.text} 个/分钟"
        bindCourseSummary(sheetView)

        sheetView.findViewById<View>(R.id.summary_btn_discard).setOnClickListener {
            dialog.dismiss()
            resetWorkout()
        }

        sheetView.findViewById<View>(R.id.summary_btn_save).setOnClickListener {
            val freq = if (elapsedSeconds > 0) (count * 60) / elapsedSeconds else 0
            val record = com.example.myfitnessapp.data.entity.WorkoutRecord(
                sportType = "JUMP_ROPE",
                sportIconResId = WorkoutRecordHelper.getIconRes("JUMP_ROPE"),
                elapsedSeconds = elapsedSeconds,
                totalDistance = 0.0,
                totalCalories = calories,
                pace = activeCourse?.title ?: "$freq 个/分钟",
                timestamp = WorkoutRecordHelper.nowTimestamp(),
                date = WorkoutRecordHelper.todayDate(),
                jumpCount = count,
                jumpFrequency = freq
            )
            viewModel.saveRecord(record)
            Toast.makeText(this, "记录已保存", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            resetWorkout()
        }

        dialog.show()
    }

    private fun resetWorkout() {
        shouldPersistCourseSession = false
        sessionStore.clear(CourseNavigator.courseIdOf(intent))
        count = 0
        elapsedSeconds = 0
        calories = 0
        isActive = false
        isPaused = false
        currentRoundIndex = 0
        currentRoundElapsedSeconds = 0
        tvCount.text = "0"
        tvTimer.text = "00:00"
        tvFrequency.text = "0"
        tvCalories.text = "0"
        updateJumpStatus()
        btnStart.text = "开始"
        updateRoundDetail()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (shouldPersistCourseSession) {
            persistCourseSessionProgress()
        }
        handler.removeCallbacks(timerRunnable)
    }

    private fun loadCourseRuntime() {
        val courseId = CourseNavigator.courseIdOf(intent) ?: return
        val course = courseRepository.getCourseById(courseId) ?: return
        if (course.sportType != "JUMP_ROPE") return

        activeCourse = course
        tvHeaderTitle.text = course.title
        tvCourseInfo.visibility = View.VISIBLE
        tvCourseInfo.text = "${course.goal} · ${course.plan.steps.size} 个回合"
        tvRoundDetail.visibility = View.VISIBLE

        sessionStore.getActiveFor(course.id)?.let { session ->
            elapsedSeconds = session.totalElapsedSeconds
            currentRoundIndex = session.currentStepIndex.coerceIn(0, course.plan.steps.lastIndex)
            currentRoundElapsedSeconds = session.currentStepElapsedSeconds
            count = session.metricPrimary
            calories = session.metricSecondary
            updateTimerDisplay()
            tvCount.text = count.toString()
            tvCalories.text = calories.toString()
            updateFrequency()
        }
        updateRoundDetail()
        updateJumpStatus()
    }

    private fun currentJumpIncrement(): Int {
        val step = currentCourseStep()
        val target = step?.target ?: return 1
        val perMinute = Regex("""(\d+)""").find(target)?.groupValues?.get(1)?.toIntOrNull() ?: return 1
        return (perMinute / 60f).coerceAtLeast(1f).toInt()
    }

    private fun syncRoundProgress() {
        val course = activeCourse ?: return
        val steps = course.plan.steps
        while (currentRoundIndex < steps.size) {
            val step = steps[currentRoundIndex]
            if (currentRoundElapsedSeconds < step.durationSeconds) {
                break
            }
            currentRoundElapsedSeconds -= step.durationSeconds
            currentRoundIndex++
            if (currentRoundIndex < steps.size) {
                Toast.makeText(this, "进入回合：${steps[currentRoundIndex].title}", Toast.LENGTH_SHORT).show()
            }
        }
        updateRoundDetail()
        updateJumpStatus()
    }

    private fun updateRoundDetail() {
        val step = currentCourseStep()
        if (step == null) {
            tvRoundDetail.visibility = View.GONE
            return
        }
        tvRoundDetail.visibility = View.VISIBLE
        val target = step.target ?: step.instruction
        tvRoundDetail.text = "当前回合：${step.title} · 目标 $target"
    }

    private fun updateJumpStatus() {
        val course = activeCourse
        if (course == null) {
            tvStatus.text = when {
                isPaused -> "已暂停"
                isActive -> "运动中"
                else -> "准备开始"
            }
            return
        }
        if (!isActive && !isPaused && elapsedSeconds == 0) {
            tvStatus.text = "准备开始"
            return
        }
        if (currentRoundIndex >= course.plan.steps.size) {
            tvStatus.text = "课程已完成"
            return
        }
        val state = if (isPaused) "已暂停" else "课程进行中"
        tvStatus.text = "$state ${currentRoundIndex + 1}/${course.plan.steps.size}"
    }

    private fun currentCourseStep(): CourseStep? {
        val course = activeCourse ?: return null
        return course.plan.steps.getOrNull(currentRoundIndex)
    }

    private fun persistCourseSessionProgress() {
        val course = activeCourse ?: return
        val existing = sessionStore.getActiveFor(course.id)
        sessionStore.save(
            (existing ?: com.example.myfitnessapp.course.domain.ActiveCourseSession(
                courseId = course.id,
                courseTitle = course.title,
                sportType = course.sportType,
                status = CourseSessionStatus.IN_PROGRESS,
                startedAt = System.currentTimeMillis()
            )).copy(
                currentStepIndex = currentRoundIndex.coerceAtMost(course.plan.steps.lastIndex.coerceAtLeast(0)),
                currentStepElapsedSeconds = currentRoundElapsedSeconds,
                totalElapsedSeconds = elapsedSeconds,
                metricPrimary = count,
                metricSecondary = calories
            )
        )
    }

    private fun bindCourseSummary(sheetView: View) {
        val course = activeCourse ?: return
        val titleView = sheetView.findViewById<TextView>(R.id.summary_tv_title)
        val subtitleView = sheetView.findViewById<TextView>(R.id.summary_tv_subtitle)
        val container = sheetView.findViewById<View>(R.id.summary_course_container)
        container.visibility = View.VISIBLE
        titleView.text = "课程完成"
        subtitleView.text = "${course.title} · 本次课程结果"
        val completedRounds = currentRoundIndex.coerceAtMost(course.plan.steps.size)
        val completionRate = ((completedRounds * 100f) / course.plan.steps.size.coerceAtLeast(1)).toInt()
        sheetView.findViewById<TextView>(R.id.summary_tv_course_title).text = "课程：${course.title}"
        sheetView.findViewById<TextView>(R.id.summary_tv_course_progress).text =
            "课程进度：$completedRounds/${course.plan.steps.size} 步，完成度 $completionRate%"
        sheetView.findViewById<TextView>(R.id.summary_tv_course_goal).text =
            if (completedRounds >= course.plan.steps.size) "目标达成：已完成全部跳绳回合" else "目标达成：已完成部分回合，下次可继续挑战"
    }
}
