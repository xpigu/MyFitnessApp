package com.example.myfitnessapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class TrainingActivity : AppCompatActivity() {

    private val courseTabs = mutableListOf<TextView>()
    private val courseTabConfigs = listOf(
        TabConfig(R.id.tab_course_running, R.drawable.ic_running),
        TabConfig(R.id.tab_course_cycling, R.drawable.ic_cycling),
        TabConfig(R.id.tab_course_yoga, R.drawable.ic_yoga),
        TabConfig(R.id.tab_course_strength, R.drawable.ic_strength_white),
        TabConfig(R.id.tab_course_swimming, R.drawable.ic_swimming),
        TabConfig(R.id.tab_course_jump_rope, R.drawable.ic_jump_rope)
    )
    private var selectedCourseTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        setupBottomNavigation()
        setupCourseTabs()
        setupWorkoutCards()
        setupCourseItems()
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
        // TODO: 根据选中的运动类型加载对应课程列表
        Toast.makeText(this, "切换到: ${courseTabs[index].text}", Toast.LENGTH_SHORT).show()
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
    // 课程卡片点击
    // ============================================================
    private fun setupCourseItems() {
        val courseIds = listOf(
            R.id.course_item_1,
            R.id.course_item_2,
            R.id.course_item_3
        )
        for (id in courseIds) {
            findViewById<View>(id).setOnClickListener {
                val config = courseTabConfigs[selectedCourseTabIndex]
                startWorkoutTracking(
                    findViewById<TextView>(config.tabId).text.toString(),
                    config.iconRes
                )
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
}

// ============================================================
// 课程 Tab 配置
// ============================================================
private data class TabConfig(val tabId: Int, val iconRes: Int)