package com.example.myfitnessapp

import android.animation.ValueAnimator
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.course.data.ActiveCourseSessionStore
import com.example.myfitnessapp.course.data.TrainingCourseRepository
import com.example.myfitnessapp.course.domain.CourseSessionStatus
import com.example.myfitnessapp.course.domain.CourseStep
import com.example.myfitnessapp.course.domain.TrainingCourse
import com.example.myfitnessapp.course.navigation.CourseNavigator
import com.example.myfitnessapp.data.WorkoutRecordHelper
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class JumpRopeActivity : AppCompatActivity(), SensorEventListener {

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
    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val accelerometer by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    private var activeCourse: TrainingCourse? = null
    private val handler = Handler(Looper.getMainLooper())
    private var countAnimator: ValueAnimator? = null
    private var currentWeightKg = DEFAULT_WEIGHT_KG
    private var isSensorRegistered = false
    private var gravityEstimate = SensorManager.GRAVITY_EARTH
    private var smoothedMotion = 0f
    private var motionBaseline = 0.35f
    private var lastJumpTimestampMs = 0L
    private var jumpThresholdArmed = true
    private val recentJumpTimestamps = ArrayDeque<Long>()

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isActive && !isPaused) {
                elapsedSeconds++
                currentRoundElapsedSeconds++
                syncRoundProgress()
                updateTimerDisplay()
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
        loadUserWeight()

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
        if (accelerometer == null) {
            showAppFeedback("设备不支持加速度传感器，暂时无法进行真实跳绳计数", FeedbackType.WARNING)
            return
        }
        shouldPersistCourseSession = true
        isActive = true
        isPaused = false
        registerJumpSensor()
        btnStart.text = "暂停"
        updateJumpStatus()
        handler.post(timerRunnable)
    }

    private fun pauseWorkout() {
        isPaused = true
        btnStart.text = "继续"
        updateJumpStatus()
        handler.removeCallbacks(timerRunnable)
        unregisterJumpSensor()
        persistCourseSessionProgress()
    }

    private fun resumeWorkout() {
        if (accelerometer == null) {
            showAppFeedback("设备不支持加速度传感器，暂时无法继续真实计数", FeedbackType.WARNING)
            return
        }
        isPaused = false
        btnStart.text = "暂停"
        registerJumpSensor()
        updateJumpStatus()
        handler.post(timerRunnable)
    }

    private fun updateCountDisplay() {
        countAnimator?.cancel()
        val currentShown = tvCount.text.toString().toIntOrNull() ?: 0
        if (abs(count - currentShown) <= 1) {
            tvCount.text = count.toString()
            return
        }
        countAnimator = ValueAnimator.ofInt(currentShown, count).apply {
            duration = 180
            addUpdateListener { tvCount.text = it.animatedValue.toString() }
            start()
        }
    }

    private fun updateTimerDisplay() {
        val min = elapsedSeconds / 60
        val sec = elapsedSeconds % 60
        tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }

    private fun updateCalories() {
        if (elapsedSeconds <= 0 || count <= 0) {
            calories = 0
            tvCalories.text = "0"
            return
        }
        val averageFrequency = averageJumpFrequency()
        val met = when {
            averageFrequency >= 140 -> 12.8
            averageFrequency >= 120 -> 12.3
            averageFrequency >= 100 -> 11.8
            averageFrequency >= 80 -> 10.5
            else -> 8.8
        }
        val caloriesBurned = met * 3.5 * currentWeightKg / 200.0 * (elapsedSeconds / 60.0)
        calories = caloriesBurned.roundToInt()
        tvCalories.text = calories.toString()
    }

    private fun updateFrequency() {
        val currentFrequency = currentJumpFrequency()
        tvFrequency.text = currentFrequency.toString()
    }

    private fun showSummary() {
        isActive = false
        isPaused = false
        handler.removeCallbacks(timerRunnable)
        unregisterJumpSensor()
        btnStart.text = "开始"
        tvStatus.text = "已完成"

        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_workout_summary, null)
        dialog.setContentView(sheetView)
        val averageFrequency = averageJumpFrequency()
        val saveButton = sheetView.findViewById<TextView>(R.id.summary_btn_save)
        val saveGuardHint = sheetView.findViewById<TextView>(R.id.summary_tv_save_guard_hint)

        sheetView.findViewById<TextView>(R.id.summary_tv_duration).text = tvTimer.text
        sheetView.findViewById<TextView>(R.id.summary_tv_distance).text = "$count 个"
        sheetView.findViewById<TextView>(R.id.summary_tv_calories).text = "$calories kcal"
        sheetView.findViewById<TextView>(R.id.summary_tv_avg_pace).text = "平均频率: $averageFrequency 个/分钟"
        bindCourseSummary(sheetView)

        val saveGuardMessage = currentJumpRopeSaveGuardMessage()
        val canSaveRecord = saveGuardMessage == null
        saveButton.isEnabled = canSaveRecord
        saveButton.isClickable = canSaveRecord
        saveButton.isFocusable = canSaveRecord
        saveButton.alpha = if (canSaveRecord) 1f else 0.45f
        saveButton.text = if (canSaveRecord) "保存记录" else "暂不可保存"
        saveGuardHint.visibility = if (saveGuardMessage.isNullOrBlank()) View.GONE else View.VISIBLE
        saveGuardHint.text = saveGuardMessage

        sheetView.findViewById<View>(R.id.summary_btn_discard).setOnClickListener {
            dialog.dismiss()
            resetWorkout()
        }

        saveButton.setOnClickListener {
            if (!canSaveJumpRopeRecord()) {
                return@setOnClickListener
            }
            val record = com.example.myfitnessapp.data.entity.WorkoutRecord(
                sportType = "JUMP_ROPE",
                sportIconResId = WorkoutRecordHelper.getIconRes("JUMP_ROPE"),
                elapsedSeconds = elapsedSeconds,
                totalDistance = 0.0,
                totalCalories = calories,
                pace = activeCourse?.title ?: "$averageFrequency 个/分钟",
                timestamp = WorkoutRecordHelper.nowTimestamp(),
                date = WorkoutRecordHelper.todayDate(),
                jumpCount = count,
                jumpFrequency = averageFrequency
            )
            viewModel.saveRecord(record) {
                showAppFeedback("记录已保存", FeedbackType.SUCCESS)
                dialog.dismiss()
                resetWorkout()
            }
        }

        dialog.show()
    }

    private fun canSaveJumpRopeRecord(): Boolean {
        val guardMessage = currentJumpRopeSaveGuardMessage()
        if (guardMessage == null) {
            return true
        }
        showAppFeedback(guardMessage, FeedbackType.WARNING)
        return false
    }

    private fun currentJumpRopeSaveGuardMessage(): String? {
        if (count >= MIN_JUMP_ROPE_SAVE_COUNT && elapsedSeconds >= MIN_JUMP_ROPE_SAVE_DURATION_SECONDS) {
            return null
        }
        return "本次跳绳未达到保存条件，至少需要 ${MIN_JUMP_ROPE_SAVE_COUNT} 个，且时长不少于 ${MIN_JUMP_ROPE_SAVE_DURATION_SECONDS} 秒。"
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
        clearJumpSensorState()
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
        unregisterJumpSensor()
        countAnimator?.cancel()
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
                showAppFeedback("进入回合：${steps[currentRoundIndex].title}", FeedbackType.INFO)
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

    private fun loadUserWeight() {
        lifecycleScope.launch {
            val username = CurrentAccount.requireUsername(this@JumpRopeActivity)
            val profile = AppDatabase.getInstance(this@JumpRopeActivity)
                .userProfileDao()
                .getUserProfileSync(username)
            val weight = profile?.weightKg ?: 0.0
            if (weight > 0.0) {
                currentWeightKg = weight
                updateCalories()
            }
        }
    }

    private fun registerJumpSensor() {
        val sensor = accelerometer ?: return
        if (isSensorRegistered) return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        isSensorRegistered = true
    }

    private fun unregisterJumpSensor() {
        if (!isSensorRegistered) return
        sensorManager.unregisterListener(this, accelerometer)
        isSensorRegistered = false
    }

    private fun clearJumpSensorState() {
        unregisterJumpSensor()
        gravityEstimate = SensorManager.GRAVITY_EARTH
        smoothedMotion = 0f
        motionBaseline = 0.35f
        lastJumpTimestampMs = 0L
        jumpThresholdArmed = true
        recentJumpTimestamps.clear()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isActive || isPaused || event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)

        gravityEstimate = gravityEstimate * 0.92f + magnitude * 0.08f
        val linearAcceleration = magnitude - gravityEstimate
        val motionLevel = abs(linearAcceleration)
        smoothedMotion = smoothedMotion * 0.78f + motionLevel * 0.22f

        if (smoothedMotion < 2.2f) {
            motionBaseline = motionBaseline * 0.985f + smoothedMotion * 0.015f
        }
        val dynamicThreshold = maxOf(MIN_JUMP_THRESHOLD, motionBaseline * 2.7f)
        if (smoothedMotion < dynamicThreshold * 0.55f) {
            jumpThresholdArmed = true
        }

        val timestampMs = event.timestamp / 1_000_000L
        if (jumpThresholdArmed && smoothedMotion > dynamicThreshold && isValidJumpInterval(timestampMs)) {
            registerJump(timestampMs)
            jumpThresholdArmed = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }

    private fun isValidJumpInterval(timestampMs: Long): Boolean {
        if (lastJumpTimestampMs == 0L) return true
        return timestampMs - lastJumpTimestampMs >= MIN_JUMP_INTERVAL_MS
    }

    private fun registerJump(timestampMs: Long) {
        lastJumpTimestampMs = timestampMs
        count++
        recentJumpTimestamps.addLast(timestampMs)
        trimRecentJumpTimestamps(timestampMs)
        updateCountDisplay()
        updateFrequency()
        updateCalories()
    }

    private fun trimRecentJumpTimestamps(nowMs: Long) {
        while (recentJumpTimestamps.isNotEmpty() && nowMs - recentJumpTimestamps.first() > FREQUENCY_WINDOW_MS) {
            recentJumpTimestamps.removeFirst()
        }
    }

    private fun currentJumpFrequency(): Int {
        val nowMs = SystemClock.elapsedRealtime()
        trimRecentJumpTimestamps(nowMs)
        if (recentJumpTimestamps.size < 2) {
            return 0
        }
        val first = recentJumpTimestamps.first()
        val last = recentJumpTimestamps.last()
        val windowMs = (last - first).coerceAtLeast(1L)
        return (((recentJumpTimestamps.size - 1) * 60_000f) / windowMs).roundToInt()
    }

    private fun averageJumpFrequency(): Int {
        if (elapsedSeconds <= 0 || count <= 0) {
            return 0
        }
        return ((count * 60.0) / elapsedSeconds).roundToInt()
    }

    companion object {
        private const val MIN_JUMP_INTERVAL_MS = 280L
        private const val FREQUENCY_WINDOW_MS = 10_000L
        private const val MIN_JUMP_THRESHOLD = 1.15f
        private const val DEFAULT_WEIGHT_KG = 70.0
        private const val MIN_JUMP_ROPE_SAVE_COUNT = 20
        private const val MIN_JUMP_ROPE_SAVE_DURATION_SECONDS = 30
    }
}
