package com.example.myfitnessapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class TrainingActivity : AppCompatActivity() {

    private val categoryChips = mutableListOf<TextView>()
    private var selectedCategoryIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        setupBottomNavigation()
        setupCategoryChips()
        setupWorkoutCards()
        setupStartWorkout()
        setupHistoryItems()
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
    // 训练分类 Chip 切换
    // ============================================================
    private fun setupCategoryChips() {
        categoryChips.add(findViewById(R.id.chip_cardio))
        categoryChips.add(findViewById(R.id.chip_strength))
        categoryChips.add(findViewById(R.id.chip_flexibility))
        categoryChips.add(findViewById(R.id.chip_balance))
        categoryChips.add(findViewById(R.id.chip_hiit))

        for ((index, chip) in categoryChips.withIndex()) {
            chip.setOnClickListener {
                selectCategory(index)
            }
        }
    }

    private fun selectCategory(index: Int) {
        selectedCategoryIndex = index
        for ((i, chip) in categoryChips.withIndex()) {
            if (i == index) {
                chip.background = getDrawable(R.drawable.category_chip_selected)
                chip.setTextColor(getColor(android.R.color.white))
            } else {
                chip.background = getDrawable(R.drawable.category_chip_unselected)
                chip.setTextColor(getColor(R.color.text_secondary))
            }
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
            R.id.workout_hiit to getString(R.string.workout_hiit)
        )

        for ((id, name) in workoutMap) {
            findViewById<View>(id).setOnClickListener {
                startWorkoutTracking(name, getSportIconRes(id))
            }
        }
    }

    // ============================================================
    // 开始训练按钮
    // ============================================================
    private fun setupStartWorkout() {
        findViewById<TextView>(R.id.btn_start_workout).setOnClickListener {
            startWorkoutTracking(getString(R.string.workout_running), R.drawable.ic_running)
        }
    }

    // ============================================================
    // 跳转到运动追踪页面
    // ============================================================
    private fun startWorkoutTracking(sportName: String, iconRes: Int) {
        val intent = Intent(this, WorkoutTrackingActivity::class.java).apply {
            putExtra(WorkoutTrackingActivity.EXTRA_SPORT_NAME, sportName)
            putExtra(WorkoutTrackingActivity.EXTRA_SPORT_ICON, iconRes)
        }
        startActivity(intent)
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
            R.id.workout_hiit -> R.drawable.ic_hiit
            else -> R.drawable.ic_running
        }
    }

    // ============================================================
    // 最近训练记录点击
    // ============================================================
    private fun setupHistoryItems() {
        val historyIds = listOf(
            R.id.workout_history_1,
            R.id.workout_history_2,
            R.id.workout_history_3
        )

        for (id in historyIds) {
            findViewById<View>(id).setOnClickListener {
                Toast.makeText(this, "查看训练详情", Toast.LENGTH_SHORT).show()
                // TODO: 跳转到训练详情页面
            }
        }
    }
}