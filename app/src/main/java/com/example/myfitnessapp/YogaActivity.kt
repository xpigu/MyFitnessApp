package com.example.myfitnessapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
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
        setupBackPress()

        findViewById<View>(R.id.btn_yoga_back).setOnClickListener {
            handleBackPressed()
        }
        findViewById<View>(R.id.btn_yoga_skip).setOnClickListener { nextPose() }
        findViewById<View>(R.id.btn_yoga_end).setOnClickListener {
            showFinishOptionsDialog()
        }

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

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@YogaActivity.handleBackPressed()
            }
        })
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
            showAppFeedback("下一个体式: ${poses[currentPoseIndex].name}", FeedbackType.INFO)
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

    private fun showFinishOptionsDialog() {
        handler.removeCallbacks(poseTimer)
        isActive = false

        val completedPoses = (currentPoseIndex + 1).coerceAtMost(poses.size)
        val completionRate = ((completedPoses * 100f) / poses.size.coerceAtLeast(1)).toInt()
        createAppAlertDialogBuilder()
            .setTitle(activeCourseTitle ?: "结束瑜伽")
            .setMessage("已完成 $completedPoses/${poses.size} 个步骤，完成度 $completionRate%\n是否保存本次瑜伽记录？")
            .setPositiveButton("保存记录", null)
            .setNegativeButton("放弃记录") { _, _ ->
                finishSessionWithoutSaving()
            }
            .setNeutralButton("继续练习") { _, _ ->
                resumeActiveSession()
            }
            .create()
            .also { dialog ->
                dialog.show()
                dialog.applyAppDialogStyling(this)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    if (!canSaveYogaRecord(completedPoses)) {
                        return@setOnClickListener
                    }
                    dialog.dismiss()
                    showDifficultyDialog(completedPoses, completionRate)
                }
            }
    }

    private fun hasSessionProgress(): Boolean {
        return totalElapsedSeconds > 0 || currentPoseIndex > 0 || restoredPoseElapsedSeconds > 0
    }

    private fun handleBackPressed() {
        if (hasSessionProgress()) {
            showFinishOptionsDialog()
        } else {
            finish()
        }
    }

    private fun showDifficultyDialog(completedPoses: Int, completionRate: Int) {
        val difficulties = arrayOf("轻松", "适中", "有挑战", "困难")
        val density = resources.displayMetrics.density
        val radioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(
                (4 * density).toInt(),
                (8 * density).toInt(),
                (4 * density).toInt(),
                0
            )
        }
        difficulties.forEachIndexed { index, label ->
            val button = AppCompatRadioButton(this).apply {
                id = View.generateViewId()
                text = label
                textSize = 16f
                buttonTintList = ContextCompat.getColorStateList(context, R.color.training_yoga_badge_fill)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (6 * density).toInt()
                }
                setPadding(0, (2 * density).toInt(), 0, (2 * density).toInt())
                isChecked = index == 1
            }
            radioGroup.addView(button)
        }

        createAppAlertDialogBuilder()
            .setTitle(activeCourseTitle ?: "课程完成")
            .setMessage("已完成 $completedPoses/${poses.size} 个步骤，完成度 $completionRate%\n请评价本次瑜伽难度")
            .setView(radioGroup)
            .setPositiveButton("保存记录") { _, _ ->
                val checkedId = radioGroup.checkedRadioButtonId
                val selectedIndex = radioGroup.indexOfChild(radioGroup.findViewById(checkedId)).coerceAtLeast(0)
                saveYogaRecord(
                    completedPoses = completedPoses,
                    difficulty = selectedIndex + 1,
                    difficultyLabel = difficulties[selectedIndex]
                )
            }
            .setNegativeButton("返回") { _, _ ->
                showFinishOptionsDialog()
            }
            .create()
            .also {
                it.show()
                it.applyAppDialogStyling(this)
            }
    }

    private fun saveYogaRecord(completedPoses: Int, difficulty: Int, difficultyLabel: String) {
        if (!canSaveYogaRecord(completedPoses)) {
            return
        }
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
            yogaDifficulty = difficulty
        )
        viewModel.saveRecord(record) {
            shouldPersistCourseSession = false
            sessionStore.clear(CourseNavigator.courseIdOf(intent))
            showAppFeedback("已保存瑜伽记录，难度：$difficultyLabel", FeedbackType.SUCCESS)
            finish()
        }
    }

    private fun canSaveYogaRecord(completedPoses: Int): Boolean {
        val guardMessage = currentYogaSaveGuardMessage(completedPoses)
        if (guardMessage == null) {
            return true
        }
        showAppFeedback(guardMessage, FeedbackType.WARNING)
        return false
    }

    private fun currentYogaSaveGuardMessage(completedPoses: Int): String? {
        if (completedPoses >= MIN_YOGA_SAVE_POSES && totalElapsedSeconds >= MIN_YOGA_SAVE_DURATION_SECONDS) {
            return null
        }
        return "本次瑜伽未达到保存条件，至少需要完成 ${MIN_YOGA_SAVE_POSES} 个体式，且时长不少于 ${MIN_YOGA_SAVE_DURATION_SECONDS} 秒。"
    }

    private fun finishSession() {
        isActive = false
        handler.removeCallbacks(poseTimer)
        showFinishOptionsDialog()
    }

    private fun finishSessionWithoutSaving() {
        shouldPersistCourseSession = false
        sessionStore.clear(CourseNavigator.courseIdOf(intent))
        showAppFeedback("已放弃本次瑜伽记录", FeedbackType.WARNING)
        finish()
    }

    private fun resumeActiveSession() {
        isActive = true
        handler.removeCallbacks(poseTimer)
        handler.post(poseTimer)
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

    companion object {
        private const val MIN_YOGA_SAVE_POSES = 1
        private const val MIN_YOGA_SAVE_DURATION_SECONDS = 30
    }
}

data class YogaPose(
    val name: String,
    val durationSeconds: Int,
    val instruction: String,
    val breathIn: Boolean
)
