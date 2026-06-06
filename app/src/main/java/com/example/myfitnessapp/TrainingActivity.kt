package com.example.myfitnessapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.example.myfitnessapp.course.data.ActiveCourseSessionStore
import com.example.myfitnessapp.course.domain.ActiveCourseSession
import com.example.myfitnessapp.course.data.TrainingCourseRepository
import com.example.myfitnessapp.course.domain.TrainingCourse
import com.example.myfitnessapp.course.navigation.CourseNavigator
import com.google.android.material.bottomnavigation.BottomNavigationView

class TrainingActivity : AppCompatActivity() {

    private val courseTabs = mutableListOf<TextView>()
    private val courseTabConfigs = listOf(
        TabConfig(R.id.tab_course_running, R.drawable.ic_running, "RUN"),
        TabConfig(R.id.tab_course_cycling, R.drawable.ic_cycling, "CYCLING"),
        TabConfig(R.id.tab_course_yoga, R.drawable.ic_yoga, "YOGA"),
        TabConfig(R.id.tab_course_strength, R.drawable.ic_strength_white, "STRENGTH"),
        TabConfig(R.id.tab_course_swimming, R.drawable.ic_swimming, "SWIMMING"),
        TabConfig(R.id.tab_course_jump_rope, R.drawable.ic_jump_rope, "JUMP_ROPE")
    )
    private var selectedCourseTabIndex = 0
    private var currentCourses: List<TrainingCourse> = emptyList()
    private val courseRepository by lazy { TrainingCourseRepository() }
    private val courseSessionStore by lazy { ActiveCourseSessionStore(this) }
    private val courseNavigator by lazy { CourseNavigator(courseSessionStore) }

    // 课程卡片视图引用
    private lateinit var courseItem1: View
    private lateinit var courseItem2: View
    private lateinit var courseItem3: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        setupBottomNavigation()
        setupHeaderActions()
        setupCourseTabs()
        setupWorkoutCards()
        setupCourseItems()
        loadCoursesForSportType("RUN") // 默认加载跑步课程
        handleReminderEntry(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderEntry(intent)
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
        updateCourseCards()
    }

    // ============================================================
    // 底部导航
    // ============================================================
    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_training

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_health -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_training -> true
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    // ============================================================
    // 头部交互
    // ============================================================
    private fun setupHeaderActions() {
        findViewById<View>(R.id.iv_calendar).setOnClickListener {
            startActivity(Intent(this, ReminderSettingsActivity::class.java))
        }
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        findViewById<View>(R.id.view_training_notification_badge).visibility =
            if (ReminderPrefs.hasAnyEnabledReminder(this)) View.VISIBLE else View.GONE
    }

    private fun handleReminderEntry(intent: Intent?) {
        if (intent?.getBooleanExtra(ReminderScheduler.EXTRA_FROM_REMINDER, false) != true) return

        if (ReminderType.from(intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TYPE)) == ReminderType.WORKOUT) {
            val scrollView = findViewById<NestedScrollView>(R.id.nested_scroll_training)
            val quickStartCard = findViewById<View>(R.id.cv_quick_start)
            val runningWorkoutCard = findViewById<View>(R.id.workout_running)
            quickStartCard.scrollIntoContainer(scrollView, 16.dp())
            runningWorkoutCard.playReminderFocusAnimation()
            Toast.makeText(this, R.string.reminder_workout_opened_hint, Toast.LENGTH_SHORT).show()
        }

        intent.removeExtra(ReminderScheduler.EXTRA_FROM_REMINDER)
        intent.removeExtra(ReminderScheduler.EXTRA_REMINDER_TYPE)
    }

    // ============================================================
    // 运动课程 Tab 切换
    // ============================================================
    private fun setupCourseTabs() {
        for (config in courseTabConfigs) {
            courseTabs.add(findViewById(config.tabId))
        }
        for ((index, tab) in courseTabs.withIndex()) {
            tab.setOnClickListener { selectCourseTab(index) }
        }
    }

    private fun selectCourseTab(index: Int) {
        selectedCourseTabIndex = index
        for ((i, tab) in courseTabs.withIndex()) {
            if (i == index) {
                tab.background = getDrawable(R.drawable.category_chip_selected)
                tab.setTextColor(getColor(android.R.color.white))
            } else {
                tab.background = getDrawable(R.drawable.category_chip_unselected)
                tab.setTextColor(getColor(R.color.text_secondary))
            }
        }
        // 加载对应运动类型的课程列表
        val sportType = courseTabConfigs[index].sportType
        loadCoursesForSportType(sportType)
    }

    // ============================================================
    // 加载指定运动类型的课程
    // ============================================================
    private fun loadCoursesForSportType(sportType: String) {
        currentCourses = courseRepository.getCoursesBySportType(sportType)
        updateCourseCards()
    }

    // ============================================================
    // 更新课程卡片显示
    // ============================================================
    private fun updateCourseCards() {
        courseItem1 = findViewById(R.id.course_item_1)
        courseItem2 = findViewById(R.id.course_item_2)
        courseItem3 = findViewById(R.id.course_item_3)

        courseItem1.visibility = if (currentCourses.isNotEmpty()) View.VISIBLE else View.GONE
        courseItem2.visibility = if (currentCourses.size >= 2) View.VISIBLE else View.GONE
        courseItem3.visibility = if (currentCourses.size >= 3) View.VISIBLE else View.GONE

        if (currentCourses.isNotEmpty()) {
            updateSingleCourseCard(courseItem1, currentCourses[0], R.id.tv_course_1_title, R.id.tv_course_1_info)
        }
        if (currentCourses.size >= 2) {
            updateSingleCourseCard(courseItem2, currentCourses[1], R.id.tv_course_2_title, R.id.tv_course_2_info)
        }
        if (currentCourses.size >= 3) {
            updateSingleCourseCard(courseItem3, currentCourses[2], R.id.tv_course_3_title, R.id.tv_course_3_info)
        }
    }

    private fun updateSingleCourseCard(
        cardView: View,
        course: TrainingCourse,
        titleId: Int,
        infoId: Int
    ) {
        // 更新图标
        val iconView = cardView.findViewById<ImageView>(R.id.iv_course_icon)
        iconView?.setImageResource(course.iconResId)

        // 更新标题
        val titleView = cardView.findViewById<TextView>(titleId)
        titleView?.text = course.title

        // 更新信息
        val infoView = cardView.findViewById<TextView>(infoId)
        val activeSession = courseSessionStore.getActiveFor(course.id)
        val isActiveCourse = activeSession != null
        val statusLabel = if (activeSession != null) {
            "${getString(R.string.course_status_in_progress)} · ${buildCourseProgressLabel(course, activeSession)}"
        } else {
            getString(R.string.course_status_not_started)
        }
        infoView?.text = "${course.formatCardInfo()} · $statusLabel"

        cardView.setOnClickListener {
            showCourseDetail(course)
        }

        val startBtn = cardView.findViewById<TextView>(R.id.btn_course_start)
        startBtn?.text = if (isActiveCourse) {
            getString(R.string.course_action_continue)
        } else {
            getString(R.string.course_action_start)
        }
        startBtn?.setOnClickListener {
            launchCourse(course)
        }
    }

    // ============================================================
    // 快速开始 - 训练卡片点击
    // ============================================================
    private fun setupWorkoutCards() {
        val workoutMap = mapOf(
            R.id.workout_running to getString(R.string.workout_running),
            R.id.workout_cycling to getString(R.string.workout_cycling),
            R.id.workout_yoga to getString(R.string.workout_yoga),
            R.id.workout_strength to getString(R.string.workout_strength),
            R.id.workout_swimming to getString(R.string.workout_swimming),
            R.id.workout_jump_rope to getString(R.string.workout_jump_rope)
        )

        for ((id, name) in workoutMap) {
            findViewById<View>(id).setOnClickListener {
                startWorkoutTracking(name, getSportIconRes(id))
            }
        }
    }

    // ============================================================
    // 课程卡片点击（已废弃，使用动态课程数据）
    // ============================================================
    private fun setupCourseItems() {
        // 初始化课程卡片引用
        courseItem1 = findViewById(R.id.course_item_1)
        courseItem2 = findViewById(R.id.course_item_2)
        courseItem3 = findViewById(R.id.course_item_3)

        courseItem1.setOnClickListener {
            if (currentCourses.isNotEmpty()) {
                showCourseDetail(currentCourses[0])
            }
        }
        courseItem2.setOnClickListener {
            if (currentCourses.size >= 2) {
                showCourseDetail(currentCourses[1])
            }
        }
        courseItem3.setOnClickListener {
            if (currentCourses.size >= 3) {
                showCourseDetail(currentCourses[2])
            }
        }
    }

    // ============================================================
    // 运动类型 → 图标映射
    // ============================================================
    private fun getSportIconRes(viewId: Int): Int {
        return when (viewId) {
            R.id.workout_running -> R.drawable.ic_running
            R.id.workout_cycling -> R.drawable.ic_cycling
            R.id.workout_yoga -> R.drawable.ic_yoga
            R.id.workout_strength -> R.drawable.ic_strength_white
            R.id.workout_swimming -> R.drawable.ic_swimming
            R.id.workout_jump_rope -> R.drawable.ic_jump_rope
            else -> R.drawable.ic_running
        }
    }

    // ============================================================
    // 跳转到对应运动记录页面（路由分发器）
    // ============================================================
    private fun startWorkoutTracking(sportName: String, iconRes: Int) {
        val intent = when (sportName) {
            getString(R.string.workout_running) -> {
                Intent(this, WorkoutTrackingActivity::class.java).apply {
                    putExtra(WorkoutTrackingActivity.EXTRA_SPORT_NAME, sportName)
                    putExtra(WorkoutTrackingActivity.EXTRA_SPORT_ICON, iconRes)
                    putExtra(WorkoutTrackingActivity.EXTRA_SPORT_TYPE, "RUN")
                }
            }
            getString(R.string.workout_cycling) -> {
                Intent(this, WorkoutTrackingActivity::class.java).apply {
                    putExtra(WorkoutTrackingActivity.EXTRA_SPORT_NAME, sportName)
                    putExtra(WorkoutTrackingActivity.EXTRA_SPORT_ICON, iconRes)
                    putExtra(WorkoutTrackingActivity.EXTRA_SPORT_TYPE, "CYCLING")
                }
            }
            getString(R.string.workout_jump_rope) -> {
                Intent(this, JumpRopeActivity::class.java)
            }
            getString(R.string.workout_strength) -> {
                Intent(this, StrengthActivity::class.java)
            }
            getString(R.string.workout_swimming) -> {
                Intent(this, SwimmingActivity::class.java)
            }
            getString(R.string.workout_yoga) -> {
                Intent(this, YogaActivity::class.java)
            }
            else -> {
                Intent(this, WorkoutTrackingActivity::class.java).apply {
                    putExtra(WorkoutTrackingActivity.EXTRA_SPORT_NAME, sportName)
                    putExtra(WorkoutTrackingActivity.EXTRA_SPORT_ICON, iconRes)
                    putExtra(WorkoutTrackingActivity.EXTRA_SPORT_TYPE, "RUN")
                }
            }
        }
        startActivity(intent)
    }

    // ============================================================
    // 开始课程训练（传递课程信息）
    // ============================================================
    private fun showCourseDetail(course: TrainingCourse) {
        val activeSession = courseSessionStore.getActiveFor(course.id)
        val stepsSummary = course.plan.steps.joinToString("\n") { step ->
            val minutes = (step.durationSeconds / 60).coerceAtLeast(1)
            "• ${step.title}  ${minutes}分钟"
        }
        val message = buildString {
            appendLine(course.description)
            appendLine()
            appendLine("课程目标：${course.goal}")
            appendLine("适合器械：${course.formatEquipment()}")
            appendLine("课程标签：${course.tags.joinToString(" / ")}")
            activeSession?.let {
                appendLine("当前进度：${buildCourseProgressLabel(course, it)}")
            }
            appendLine()
            appendLine("课程结构：")
            append(stepsSummary)
        }
        val startLabel = if (activeSession != null) {
            getString(R.string.course_action_continue)
        } else {
            getString(R.string.course_action_start)
        }
        AlertDialog.Builder(this)
            .setTitle(course.title)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(startLabel) { _, _ ->
                launchCourse(course)
            }
            .show()
    }

    private fun launchCourse(course: TrainingCourse) {
        courseNavigator.openCourse(this, course)
        Toast.makeText(this, getString(R.string.course_start_toast, course.title), Toast.LENGTH_SHORT).show()
    }

    private fun buildCourseProgressLabel(course: TrainingCourse, session: ActiveCourseSession): String {
        val totalSteps = course.plan.steps.size.coerceAtLeast(1)
        val currentStep = session.currentStepIndex.coerceIn(0, totalSteps - 1) + 1
        return "第 $currentStep/$totalSteps ${courseProgressUnit(course)}"
    }

    private fun courseProgressUnit(course: TrainingCourse): String {
        return when (course.sportType) {
            "STRENGTH" -> "动作"
            "JUMP_ROPE" -> "回合"
            "YOGA" -> "步骤"
            else -> "阶段"
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}

// ============================================================
// 课程 Tab 配置
// ============================================================
private data class TabConfig(
    val tabId: Int,
    val iconRes: Int,
    val sportType: String
)
