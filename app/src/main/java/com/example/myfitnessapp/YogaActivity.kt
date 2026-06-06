package com.example.myfitnessapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.course.data.ActiveCourseSessionStore
import com.example.myfitnessapp.course.data.TrainingCourseRepository
import com.example.myfitnessapp.course.domain.TrainingCourse
import com.example.myfitnessapp.course.navigation.CourseNavigator
import com.example.myfitnessapp.data.WorkoutRecordHelper
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import java.util.Locale

class YogaActivity : AppCompatActivity() {

    private lateinit var tvPoseName: TextView
    private lateinit var tvPoseInstruction: TextView
    private lateinit var tvPoseTimer: TextView
    private lateinit var tvPoseProgress: TextView
    private lateinit var tvBreathText: TextView
    private lateinit var btnPause: TextView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvCourseInfo: TextView

    private val courseRepository by lazy { TrainingCourseRepository() }
    private val sessionStore by lazy { ActiveCourseSessionStore(this) }
    private var poses = defaultYogaPoses()
    private var activeCourse: TrainingCourse? = null
    private var activeCourseTitle: String? = null

    private var currentPoseIndex = 0
    private var poseRemainingSeconds = 0
    private var breathCycle = 0
    private var isPaused = false
    private var isActive = true
    private var totalElapsedSeconds = 0
    private var restoredPoseElapsedSeconds = 0
    private var shouldPersistCourseSession = true
    private lateinit var viewModel: WorkoutRecordViewModel
    private val handler = Handler(Looper.getMainLooper())

    private val poseTimer = object : Runnable {
        override fun run() {
            if (!isActive) return
            if (isPaused) {
                handler.postDelayed(this, 200)
                return
            }

            if (poseRemainingSeconds > 0) {
                poseRemainingSeconds--
                totalElapsedSeconds++
                updateTimerDisplay()
                updateBreath()
                persistCourseSessionProgress()
                handler.postDelayed(this, 1000)
            } else {
                nextPose()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yoga)

        viewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)

        tvPoseName = findViewById(R.id.tv_pose_name)
        tvPoseInstruction = findViewById(R.id.tv_pose_instruction)
        tvPoseTimer = findViewById(R.id.tv_pose_timer)
        tvPoseProgress = findViewById(R.id.tv_pose_progress)
        tvBreathText = findViewById(R.id.tv_breath_text)
        btnPause = findViewById(R.id.btn_yoga_pause)
        tvHeaderTitle = findViewById(R.id.tv_yoga_header_title)
        tvCourseInfo = findViewById(R.id.tv_yoga_course_info)

        loadCourseRuntime()

        findViewById<View>(R.id.btn_yoga_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_yoga_skip).setOnClickListener { nextPose() }
        findViewById<View>(R.id.btn_yoga_end).setOnClickListener { showFinishDialog() }

        btnPause.setOnClickListener {
            if (isPaused) {
                resumeSession()
            } else {
                pauseSession()
            }
        }

        loadPose(currentPoseIndex)
        handler.post(poseTimer)
    }

    private fun loadCourseRuntime() {
        val courseId = CourseNavigator.courseIdOf(intent) ?: return
        val course = courseRepository.getCourseById(courseId) ?: return
        if (course.sportType != "YOGA" || course.plan.steps.isEmpty()) return

        activeCourse = course
        activeCourseTitle = course.title
        tvHeaderTitle.text = course.title
        tvCourseInfo.visibility = View.VISIBLE
        tvCourseInfo.text = "${course.goal} · ${course.plan.steps.size} 个步骤"
        poses = course.plan.steps.map { step ->
            YogaPose(
                name = step.title,
                durationSeconds = step.durationSeconds,
                instruction = step.instruction,
                breathIn = step.target?.contains("吸") == true || step.target?.contains("缓慢") == true
            )
        }
        sessionStore.getActiveFor(course.id)?.let { session ->
            totalElapsedSeconds = session.totalElapsedSeconds
            currentPoseIndex = session.currentStepIndex.coerceIn(0, poses.lastIndex)
            restoredPoseElapsedSeconds = session.currentStepElapsedSeconds.coerceAtLeast(0)
        }
    }

    private fun loadPose(index: Int) {
        if (index >= poses.size) {
            finishSession()
            return
        }
        val pose = poses[index]
        tvPoseName.text = pose.name
        tvPoseInstruction.text = pose.instruction
        val restoredElapsed = restoredPoseElapsedSeconds.coerceAtMost(pose.durationSeconds - 1)
        poseRemainingSeconds = (pose.durationSeconds - restoredElapsed).coerceAtLeast(1)
        restoredPoseElapsedSeconds = 0
        tvPoseProgress.text = "${index + 1}/${poses.size}"
        updateTimerDisplay()
        updateBreath()
    }

    private fun updateTimerDisplay() {
        tvPoseTimer.text = poseRemainingSeconds.toString()
    }

    private fun updateBreath() {
        val pose = poses.getOrNull(currentPoseIndex)
        if (pose == null) return
        breathCycle++
        if (pose.breathIn) {
            tvBreathText.text = "吸气"
        } else {
            tvBreathText.text = "呼气"
        }
    }

    private fun nextPose() {
        currentPoseIndex++
        handler.removeCallbacks(poseTimer)
        if (currentPoseIndex >= poses.size) {
            finishSession()
        } else {
            loadPose(currentPoseIndex)
            Toast.makeText(this, "下一个体式: ${poses[currentPoseIndex].name}", Toast.LENGTH_SHORT).show()
            persistCourseSessionProgress()
            handler.post(poseTimer)
        }
    }

    private fun pauseSession() {
        isPaused = true
        btnPause.text = "继续"
        persistCourseSessionProgress()
    }

    private fun resumeSession() {
        isPaused = false
        btnPause.text = "暂停"
    }

    private fun showFinishDialog() {
        handler.removeCallbacks(poseTimer)
        isActive = false

        val completedPoses = (currentPoseIndex + 1).coerceAtMost(poses.size)
        val completionRate = ((completedPoses * 100f) / poses.size.coerceAtLeast(1)).toInt()
        val difficulties = arrayOf("轻松", "适中", "有挑战", "困难")
        AlertDialog.Builder(this)
            .setTitle(activeCourseTitle ?: "课程完成")
            .setMessage("已完成 $completedPoses/${poses.size} 个步骤，完成度 $completionRate%\n请评价本次瑜伽难度")
            .setItems(difficulties) { _, which ->
                val diff = which + 1
                val cal = (totalElapsedSeconds * 0.06).toInt()
                val record = com.example.myfitnessapp.data.entity.WorkoutRecord(
                    sportType = "YOGA",
                    sportIconResId = WorkoutRecordHelper.getIconRes("YOGA"),
                    elapsedSeconds = totalElapsedSeconds,
                    totalDistance = 0.0,
                    totalCalories = cal,
                    pace = activeCourse?.title ?: "$completedPoses 个体式",
                    timestamp = WorkoutRecordHelper.nowTimestamp(),
                    date = WorkoutRecordHelper.todayDate(),
                    yogaPosesCompleted = completedPoses,
                    yogaDifficulty = diff
                )
                viewModel.saveRecord(record)
                shouldPersistCourseSession = false
                sessionStore.clear(CourseNavigator.courseIdOf(intent))
                Toast.makeText(this, "难度评价: ${difficulties[which]}, 已完成 $completedPoses/${poses.size} 个步骤", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setOnCancelListener {
                shouldPersistCourseSession = false
                sessionStore.clear(CourseNavigator.courseIdOf(intent))
                finish()
            }
            .show()
    }

    private fun finishSession() {
        isActive = false
        handler.removeCallbacks(poseTimer)
        showFinishDialog()
    }

    private fun persistCourseSessionProgress() {
        val course = activeCourse ?: return
        val pose = poses.getOrNull(currentPoseIndex) ?: return
        val poseElapsedSeconds = (pose.durationSeconds - poseRemainingSeconds).coerceAtLeast(0)
        val existing = sessionStore.getActiveFor(course.id)
        sessionStore.save(
            (existing ?: defaultCourseSession(course)).copy(
                currentStepIndex = currentPoseIndex.coerceAtMost(poses.lastIndex.coerceAtLeast(0)),
                currentStepElapsedSeconds = poseElapsedSeconds,
                totalElapsedSeconds = totalElapsedSeconds
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
        handler.removeCallbacks(poseTimer)
    }

    private fun defaultYogaPoses(): List<YogaPose> {
        return listOf(
            YogaPose("山式", 30, "双脚并拢，身体直立，双臂自然下垂", true),
            YogaPose("前屈式", 30, "从髋部前屈，双手触地", false),
            YogaPose("战士一式", 45, "右腿前弓步，双臂上举，目视前方", true),
            YogaPose("战士二式", 45, "双臂侧平举，目视右手指尖", false),
            YogaPose("三角式", 30, "右手触右脚踝，左臂向上伸展", true),
            YogaPose("下犬式", 45, "双手双脚撑地，臀部向上推", false),
            YogaPose("上犬式", 30, "双臂伸直，胸部前推，目视上方", true),
            YogaPose("婴儿式", 30, "跪坐，前额贴地，双臂向前伸展", false),
            YogaPose("猫牛式", 30, "吸气弓背，呼气塌腰", true),
            YogaPose("桥式", 30, "仰卧屈膝，臀部向上推起", false),
            YogaPose("仰卧扭转", 30, "仰卧，双膝倒向一侧，转头看对侧", true),
            YogaPose("挺尸式", 60, "仰卧，全身放松，自然呼吸", false)
        )
    }
}

data class YogaPose(
    val name: String,
    val durationSeconds: Int,
    val instruction: String,
    val breathIn: Boolean
)
