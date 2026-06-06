package com.example.myfitnessapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.course.data.ActiveCourseSessionStore
import com.example.myfitnessapp.course.data.TrainingCourseRepository
import com.example.myfitnessapp.course.domain.CourseStepType
import com.example.myfitnessapp.course.domain.TrainingCourse
import com.example.myfitnessapp.course.navigation.CourseNavigator
import com.example.myfitnessapp.data.WorkoutRecordHelper
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import java.util.Locale

class StrengthActivity : AppCompatActivity() {

    private lateinit var tvExerciseName: TextView
    private lateinit var tvSetProgress: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvReps: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvTotalSets: TextView
    private lateinit var tvTotalVolume: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvRestTimer: TextView
    private lateinit var restOverlay: View
    private lateinit var llCompletedSets: LinearLayout
    private lateinit var tvEmptyHistory: TextView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvCourseInfo: TextView
    private lateinit var tvPhaseDetail: TextView

    private val sessionStore by lazy { ActiveCourseSessionStore(this) }
    private val courseRepository by lazy { TrainingCourseRepository() }
    private var activeCourse: TrainingCourse? = null
    private var exercises: List<StrengthExercise> = defaultStrengthExercises()

    private var currentExerciseIndex = 0
    private var currentSet = 1
    private var totalSets = 0
    private var totalVolume = 0.0
    private var maxCompletedWeight = 0.0
    private var elapsedSeconds = 0
    private var restSeconds = 0
    private var isResting = false
    private var shouldPersistCourseSession = true
    private lateinit var viewModel: WorkoutRecordViewModel
    private val handler = Handler(Looper.getMainLooper())

    private val mainTimer = object : Runnable {
        override fun run() {
            if (!isResting) {
                elapsedSeconds++
                updateTimer()
            }
            persistCourseSessionProgress()
            handler.postDelayed(this, 1000)
        }
    }

    private val restTimer = object : Runnable {
        override fun run() {
            if (restSeconds > 0) {
                restSeconds--
                updateRestDisplay()
                persistCourseSessionProgress()
                handler.postDelayed(this, 1000)
            } else {
                endRest()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strength)

        viewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)

        tvExerciseName = findViewById(R.id.tv_exercise_name)
        tvSetProgress = findViewById(R.id.tv_set_progress)
        tvWeight = findViewById(R.id.tv_weight)
        tvReps = findViewById(R.id.tv_reps)
        tvTimer = findViewById(R.id.tv_strength_timer)
        tvTotalSets = findViewById(R.id.tv_total_sets)
        tvTotalVolume = findViewById(R.id.tv_total_volume)
        tvCalories = findViewById(R.id.tv_strength_calories)
        tvRestTimer = findViewById(R.id.tv_rest_timer)
        restOverlay = findViewById(R.id.rest_overlay)
        llCompletedSets = findViewById(R.id.ll_completed_sets)
        tvEmptyHistory = findViewById(R.id.tv_empty_history)
        tvHeaderTitle = findViewById(R.id.tv_strength_header_title)
        tvCourseInfo = findViewById(R.id.tv_strength_course_info)
        tvPhaseDetail = findViewById(R.id.tv_strength_phase_detail)

        loadCourseRuntime()

        findViewById<View>(R.id.btn_strength_back).setOnClickListener { finish() }

        loadExercise(currentExerciseIndex, resetSet = false)
        updateTimer()
        updateSummary()
        applyRestState()

        findViewById<View>(R.id.btn_weight_minus).setOnClickListener { adjustWeight(-2.5) }
        findViewById<View>(R.id.btn_weight_plus).setOnClickListener { adjustWeight(2.5) }
        findViewById<View>(R.id.btn_reps_minus).setOnClickListener { adjustReps(-1) }
        findViewById<View>(R.id.btn_reps_plus).setOnClickListener { adjustReps(1) }

        findViewById<View>(R.id.btn_complete_set).setOnClickListener { completeSet() }
        findViewById<View>(R.id.btn_skip_rest).setOnClickListener { skipRest() }
        findViewById<View>(R.id.btn_finish_strength).setOnClickListener { finishWorkout() }

        if (isResting) {
            handler.post(restTimer)
        }
        handler.post(mainTimer)
    }

    private fun loadExercise(index: Int, resetSet: Boolean = true) {
        if (index >= exercises.size) {
            finishWorkout()
            return
        }
        val ex = exercises[index]
        tvExerciseName.text = ex.name
        tvWeight.text = ex.targetWeight.toInt().toString()
        tvReps.text = ex.targetReps.toString()
        if (ex.targetLabel.isNotBlank()) {
            tvPhaseDetail.visibility = View.VISIBLE
            tvPhaseDetail.text = "目标：${ex.targetLabel}"
        } else {
            tvPhaseDetail.visibility = View.GONE
        }
        if (resetSet) {
            currentSet = 1
        } else {
            currentSet = currentSet.coerceIn(1, ex.sets)
        }
        updateSetProgress()
    }

    private fun updateSetProgress() {
        val ex = exercises[currentExerciseIndex]
        val actionProgress = if (activeCourse != null) {
            "动作 ${currentExerciseIndex + 1}/${exercises.size} · "
        } else {
            ""
        }
        tvSetProgress.text = "${actionProgress}第 $currentSet 组 / 共 ${ex.sets} 组"
    }

    private fun adjustWeight(delta: Double) {
        val current = tvWeight.text.toString().toDoubleOrNull() ?: 0.0
        val newVal = (current + delta).coerceAtLeast(0.0)
        tvWeight.text = if (newVal == newVal.toInt().toDouble()) newVal.toInt().toString() else newVal.toString()
    }

    private fun adjustReps(delta: Int) {
        val current = tvReps.text.toString().toIntOrNull() ?: 0
        val newVal = (current + delta).coerceAtLeast(1)
        tvReps.text = newVal.toString()
    }

    private fun completeSet() {
        val weight = tvWeight.text.toString().toDoubleOrNull() ?: 0.0
        val reps = tvReps.text.toString().toIntOrNull() ?: 0
        totalSets++
        totalVolume += weight * reps
        maxCompletedWeight = maxOf(maxCompletedWeight, weight)
        updateSummary()

        // 添加历史记录
        addHistoryEntry(weight, reps)

        val ex = exercises[currentExerciseIndex]
        if (currentSet >= ex.sets) {
            // 当前动作完成，进入下一个动作
            currentExerciseIndex++
            if (currentExerciseIndex >= exercises.size) {
                finishWorkout()
                return
            }
            currentSet = 1
            loadExercise(currentExerciseIndex)
            startRest()
        } else {
            currentSet++
            updateSetProgress()
            startRest()
        }
        persistCourseSessionProgress()
    }

    private fun addHistoryEntry(weight: Double, reps: Int) {
        tvEmptyHistory.visibility = View.GONE
        val item = layoutInflater.inflate(R.layout.workout_set_item, llCompletedSets, false)
        val ex = exercises[currentExerciseIndex]
        item.findViewById<TextView>(R.id.tv_set_item_name).text = ex.name
        item.findViewById<TextView>(R.id.tv_set_item_detail).text = "第${currentSet}组  ${weight.toInt()}kg × $reps"
        llCompletedSets.addView(item, 0)
    }

    private fun startRest() {
        isResting = true
        restSeconds = exercises.getOrNull(currentExerciseIndex)?.restSeconds ?: 60
        applyRestState()
        persistCourseSessionProgress()
        handler.post(restTimer)
    }

    private fun skipRest() {
        handler.removeCallbacks(restTimer)
        endRest()
    }

    private fun endRest() {
        isResting = false
        restSeconds = 0
        handler.removeCallbacks(restTimer)
        applyRestState()
        persistCourseSessionProgress()
        Toast.makeText(this, "休息结束，开始下一组！", Toast.LENGTH_SHORT).show()
    }

    private fun updateRestDisplay() {
        tvRestTimer.text = restSeconds.toString()
    }

    private fun applyRestState() {
        restOverlay.visibility = if (isResting) View.VISIBLE else View.GONE
        updateRestDisplay()
    }

    private fun updateSummary() {
        tvTotalSets.text = totalSets.toString()
        tvTotalVolume.text = totalVolume.toInt().toString()
        tvCalories.text = (elapsedSeconds * 0.12).toInt().toString()
    }

    private fun updateTimer() {
        val min = elapsedSeconds / 60
        val sec = elapsedSeconds % 60
        tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", min, sec)
        updateSummary()
    }

    private fun finishWorkout() {
        handler.removeCallbacks(mainTimer)
        handler.removeCallbacks(restTimer)

        val cal = (elapsedSeconds * 0.12).toInt()
        val courseTitle = activeCourse?.title
        val completedActions = currentExerciseIndex.coerceAtMost(exercises.size)
        val completionRate = ((completedActions * 100f) / exercises.size.coerceAtLeast(1)).toInt()
        val record = com.example.myfitnessapp.data.entity.WorkoutRecord(
            sportType = "STRENGTH",
            sportIconResId = WorkoutRecordHelper.getIconRes("STRENGTH"),
            elapsedSeconds = elapsedSeconds,
            totalDistance = 0.0,
            totalCalories = cal,
            pace = courseTitle ?: "--",
            timestamp = WorkoutRecordHelper.nowTimestamp(),
            date = WorkoutRecordHelper.todayDate(),
            strengthSets = totalSets,
            strengthVolume = totalVolume,
            strengthMaxWeight = maxCompletedWeight
        )
        viewModel.saveRecord(record)
        shouldPersistCourseSession = false
        sessionStore.clear(CourseNavigator.courseIdOf(intent))

        Toast.makeText(
            this,
            if (courseTitle != null) {
                "课程完成：$courseTitle，动作完成度 $completionRate%，共完成 $totalSets 组"
            } else {
                "力量训练完成！总组数: $totalSets, 最大重量: ${maxCompletedWeight.toInt()}kg"
            },
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun loadCourseRuntime() {
        val courseId = CourseNavigator.courseIdOf(intent) ?: return
        val course = courseRepository.getCourseById(courseId) ?: return
        if (course.sportType != "STRENGTH") return

        activeCourse = course
        tvHeaderTitle.text = course.title
        tvCourseInfo.visibility = View.VISIBLE

        val courseExercises = course.plan.steps
            .filter { it.stepType == CourseStepType.STRENGTH_SET }
            .map { step -> step.toStrengthExercise() }

        if (courseExercises.isNotEmpty()) {
            exercises = courseExercises
            tvCourseInfo.text = "${course.goal} · ${courseExercises.size} 个动作"
        } else {
            tvCourseInfo.text = "${course.goal} · 使用默认动作模板"
        }

        sessionStore.getActiveFor(course.id)?.let { session ->
            elapsedSeconds = session.totalElapsedSeconds
            totalSets = session.metricPrimary
            restSeconds = session.metricSecondary.coerceAtLeast(0)
            isResting = session.metricTertiary == 1 && restSeconds > 0
            totalVolume = session.decimalPrimary.toDouble()
            maxCompletedWeight = session.decimalSecondary.toDouble()
            currentExerciseIndex = session.currentStepIndex.coerceIn(0, exercises.lastIndex)
            val restoredExercise = exercises.getOrNull(currentExerciseIndex)
            currentSet = session.currentStepElapsedSeconds
                .coerceAtLeast(1)
                .coerceAtMost(restoredExercise?.sets ?: 1)
        }
    }

    private fun defaultStrengthExercises(): List<StrengthExercise> {
        return listOf(
            StrengthExercise("杠铃卧推", 4, 10, 60.0, "4组 x 10次 x 60kg"),
            StrengthExercise("哑铃飞鸟", 3, 12, 14.0, "3组 x 12次 x 14kg"),
            StrengthExercise("上斜卧推", 3, 10, 50.0, "3组 x 10次 x 50kg"),
            StrengthExercise("绳索夹胸", 3, 15, 20.0, "3组 x 15次 x 20kg"),
            StrengthExercise("三头下压", 3, 12, 25.0, "3组 x 12次 x 25kg")
        )
    }

    private fun com.example.myfitnessapp.course.domain.CourseStep.toStrengthExercise(): StrengthExercise {
        val parsed = parseStrengthTarget(target)
        return StrengthExercise(
            name = title,
            sets = parsed?.sets ?: 3,
            targetReps = parsed?.reps ?: 10,
            targetWeight = parsed?.weightKg ?: 0.0,
            targetLabel = target ?: instruction,
            restSeconds = parsed?.restSeconds ?: 60
        )
    }

    private fun parseStrengthTarget(target: String?): StrengthTarget? {
        if (target.isNullOrBlank()) return null
        val match = Regex("""(\d+)组\s*x\s*(\d+)次\s*x\s*([0-9.]+|自重)""").find(target)
            ?: return null
        val sets = match.groupValues[1].toIntOrNull() ?: return null
        val reps = match.groupValues[2].toIntOrNull() ?: return null
        val weightToken = match.groupValues[3]
        val weight = if (weightToken == "自重") 0.0 else weightToken.toDoubleOrNull() ?: 0.0
        val rest = when {
            sets >= 5 -> 90
            weight >= 40 -> 75
            else -> 60
        }
        return StrengthTarget(sets, reps, weight, rest)
    }

    private fun persistCourseSessionProgress() {
        val course = activeCourse ?: return
        val existing = sessionStore.getActiveFor(course.id)
        sessionStore.save(
            (existing ?: defaultCourseSession(course)).copy(
                currentStepIndex = currentExerciseIndex.coerceAtMost(exercises.lastIndex.coerceAtLeast(0)),
                currentStepElapsedSeconds = currentSet,
                totalElapsedSeconds = elapsedSeconds,
                metricPrimary = totalSets,
                metricSecondary = restSeconds,
                metricTertiary = if (isResting) 1 else 0,
                decimalPrimary = totalVolume.toFloat(),
                decimalSecondary = maxCompletedWeight.toFloat()
            )
        )
    }

    private fun defaultCourseSession(course: TrainingCourse) =
        com.example.myfitnessapp.course.domain.ActiveCourseSession(
            courseId = course.id,
            courseTitle = course.title,
            sportType = course.sportType,
            status = com.example.myfitnessapp.course.domain.CourseSessionStatus.IN_PROGRESS,
            startedAt = System.currentTimeMillis()
        )

    override fun onDestroy() {
        super.onDestroy()
        if (shouldPersistCourseSession) {
            persistCourseSessionProgress()
        }
        handler.removeCallbacks(mainTimer)
        handler.removeCallbacks(restTimer)
    }
}

data class StrengthExercise(
    val name: String,
    val sets: Int,
    val targetReps: Int,
    val targetWeight: Double,
    val targetLabel: String = "",
    val restSeconds: Int = 60
)

data class StrengthTarget(
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val restSeconds: Int
)
