package com.example.myfitnessapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.myfitnessapp.data.entity.DailyCheckin
import com.example.myfitnessapp.data.entity.UserProfile
import com.example.myfitnessapp.data.entity.WorkoutRecord
import com.example.myfitnessapp.data.viewmodel.DailyCheckinViewModel
import com.example.myfitnessapp.data.viewmodel.UserProfileViewModel
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var viewModel: UserProfileViewModel
    private lateinit var workoutViewModel: WorkoutRecordViewModel
    private lateinit var checkinViewModel: DailyCheckinViewModel

    private var currentProfile = UserProfile()
    private var currentWorkoutRecords: List<WorkoutRecord> = emptyList()
    private var currentCheckins: List<DailyCheckin> = emptyList()
    private var lastRenderedLevel: Int? = null

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) startCrop(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        viewModel = ViewModelProvider(this).get(UserProfileViewModel::class.java)
        workoutViewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)
        checkinViewModel = ViewModelProvider(this).get(DailyCheckinViewModel::class.java)

        setupBottomNavigation()
        setupHeaderActions()
        setupMenuActions()
        setupGrowthActions()
        observeUserData()
    }

    // ============================================================
    // 观察用户数据变化
    // ============================================================
    private fun observeUserData() {
        viewModel.userProfile.observe(this) { profile ->
            currentProfile = profile
            bindUserData(profile)
        }

        workoutViewModel.allRecords.observe(this) { records ->
            currentWorkoutRecords = records
            updateProfileStats()
        }

        checkinViewModel.allCheckins.observe(this) { checkins ->
            currentCheckins = checkins
            updateProfileStats()
        }
    }

    // ============================================================
    // 底部导航
    // ============================================================
    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_health -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_training -> {
                    startActivity(Intent(this, TrainingActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    // ============================================================
    // 用户数据绑定
    // ============================================================
    private fun bindUserData(profile: UserProfile) {
        findViewById<TextView>(R.id.tv_profile_username).text = profile.username
        findViewById<TextView>(R.id.tv_profile_bio).text = profile.bio
        findViewById<TextView>(R.id.tv_profile_level).text = getString(R.string.profile_level_format, profile.level)

        val avatarView = findViewById<ImageView>(R.id.iv_profile_avatar)
        if (SettingsPrefs.getPrivacySettings(this).allowLocalAvatarAccess && profile.avatarUri.isNotEmpty()) {
            Glide.with(this)
                .load(Uri.parse(profile.avatarUri))
                .transform(CircleCrop())
                .placeholder(R.drawable.avatar_placeholder)
                .into(avatarView)
        } else {
            avatarView.setImageResource(R.drawable.avatar_placeholder)
        }

        updateProfileStats()
        updateLevelPresentation(profile)
        maybeShowLevelUpFeedback(profile)
    }

    private fun updateProfileStats() {
        val workoutCount = currentWorkoutRecords.size
        val activeDays = buildSet {
            currentWorkoutRecords.forEach { add(it.date) }
            currentCheckins.forEach { add(it.date) }
        }.size

        findViewById<TextView>(R.id.tv_profile_workouts).text = workoutCount.toString()
        findViewById<TextView>(R.id.tv_profile_active_days).text = activeDays.toString()
    }

    private fun updateLevelPresentation(profile: UserProfile) {
        val currentStreak = calculateCurrentStreak()
        val experience = calculateExperience(
            totalWorkouts = profile.totalWorkouts,
            activeDays = profile.activeDays,
            currentStreak = currentStreak
        )
        val trainingExp = profile.totalWorkouts * 12
        val activeExp = profile.activeDays * 8
        val streakExp = currentStreak * 15
        val progressBar = findViewById<ProgressBar>(R.id.progress_profile_level)
        val titleView = findViewById<TextView>(R.id.tv_profile_level_title)
        val progressView = findViewById<TextView>(R.id.tv_profile_level_progress)
        val trainingExpView = findViewById<TextView>(R.id.tv_profile_exp_training)
        val activeExpView = findViewById<TextView>(R.id.tv_profile_exp_active_days)
        val streakExpView = findViewById<TextView>(R.id.tv_profile_exp_streak)
        val weeklyExpView = findViewById<TextView>(R.id.tv_profile_weekly_exp)

        titleView.text = levelTitle(profile.level)
        trainingExpView.text = "训练 +$trainingExp"
        activeExpView.text = "活跃 +$activeExp"
        streakExpView.text = "签到 +$streakExp"
        weeklyExpView.text = getString(R.string.profile_weekly_exp_format, calculateWeeklyExperience())

        if (profile.level >= 30) {
            progressBar.max = 120
            progressBar.progress = 120
            progressView.text = getString(R.string.profile_level_maxed)
            return
        }

        val progress = experience % 120
        val remaining = 120 - progress
        progressBar.max = 120
        progressBar.progress = progress
        progressView.text = getString(R.string.profile_level_progress_format, remaining)
    }

    private fun maybeShowLevelUpFeedback(profile: UserProfile) {
        val previousLevel = lastRenderedLevel
        lastRenderedLevel = profile.level

        if (previousLevel == null || profile.level <= previousLevel) {
            return
        }

        Toast.makeText(
            this,
            getString(R.string.profile_level_up_message, profile.level, levelTitle(profile.level)),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun calculateCurrentStreak(): Int {
        if (currentCheckins.isEmpty()) return 0
        val today = currentDateString()
        val yesterday = offsetDateString(-1)

        return when {
            currentCheckins.firstOrNull { it.date == today } != null ->
                currentCheckins.first { it.date == today }.streakCount
            currentCheckins.firstOrNull { it.date == yesterday } != null ->
                currentCheckins.first { it.date == yesterday }.streakCount
            else -> 0
        }
    }

    private fun calculateExperience(totalWorkouts: Int, activeDays: Int, currentStreak: Int): Int {
        return totalWorkouts * 12 + activeDays * 8 + currentStreak * 15
    }

    private fun calculateWeeklyExperience(): Int {
        val weekStart = startOfWeekString()
        val weeklyWorkouts = currentWorkoutRecords.filter { it.date >= weekStart }
        val weeklyCheckins = currentCheckins.filter { it.date >= weekStart }
        val weeklyActiveDays = buildSet {
            weeklyWorkouts.forEach { add(it.date) }
            weeklyCheckins.forEach { add(it.date) }
        }.size

        return weeklyWorkouts.size * 12 + weeklyActiveDays * 8 + weeklyCheckins.size * 15
    }

    private fun levelTitle(level: Int): String = when (level) {
        in 1..2 -> "新手起步"
        in 3..5 -> "稳定训练者"
        in 6..9 -> "自律达人"
        in 10..14 -> "进阶挑战者"
        in 15..20 -> "高能运动家"
        in 21..30 -> "巅峰掌控者"
        else -> "稳定训练者"
    }

    private fun currentDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun startOfWeekString(): String {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val delta = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
        calendar.add(Calendar.DAY_OF_MONTH, delta)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun offsetDateString(offsetDays: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun setupGrowthActions() {
        findViewById<View>(R.id.btn_profile_level_guide).setOnClickListener {
            showLevelGuideDialog()
        }
    }

    private fun showLevelGuideDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_level_guide, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        val currentStreak = calculateCurrentStreak()
        val experience = calculateExperience(
            totalWorkouts = currentProfile.totalWorkouts,
            activeDays = currentProfile.activeDays,
            currentStreak = currentStreak
        )
        val weeklyExperience = calculateWeeklyExperience()
        val summaryView = dialogView.findViewById<TextView>(R.id.tv_level_guide_current_summary)
        summaryView.text =
            "当前 ${getString(R.string.profile_level_format, currentProfile.level)} · ${levelTitle(currentProfile.level)}\n" +
                "累计经验 $experience，本周新增 $weeklyExperience。\n" +
                "训练 ${currentProfile.totalWorkouts} 次，活跃 ${currentProfile.activeDays} 天，连续签到 $currentStreak 天。"

        dialogView.findViewById<View>(R.id.btn_level_guide_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val targetWidth = (screenWidth * 0.9f).toInt()
            val maxWidth = (420 * displayMetrics.density).toInt()
            setLayout(
                if (targetWidth > maxWidth) maxWidth else targetWidth,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    // ============================================================
    // 头部区域交互
    // ============================================================
    private fun setupHeaderActions() {
        // 编辑资料
        findViewById<View>(R.id.btn_edit_profile).setOnClickListener {
            onEditProfileClicked()
        }

        // 头像点击（预留图片选择）
        findViewById<ImageView>(R.id.iv_profile_avatar).setOnClickListener {
            onAvatarClicked()
        }
    }

    // ============================================================
    // 菜单点击事件
    // ============================================================
    private fun setupMenuActions() {
        // 个人相关菜单
        findViewById<View>(R.id.menu_personal_info).setOnClickListener {
            startActivity(Intent(this, PersonalInfoActivity::class.java))
        }

        findViewById<View>(R.id.menu_daily_checkin).setOnClickListener {
            startActivity(Intent(this, DailyCheckinActivity::class.java))
        }

        findViewById<View>(R.id.menu_goal_setting).setOnClickListener {
            startActivity(Intent(this, GoalSettingActivity::class.java))
        }

        findViewById<View>(R.id.menu_achievements).setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }

        // 设置相关菜单
        findViewById<View>(R.id.menu_notifications).setOnClickListener {
            startActivity(Intent(this, ReminderSettingsActivity::class.java))
        }

        findViewById<View>(R.id.menu_privacy).setOnClickListener {
            startActivity(Intent(this, PrivacySettingsActivity::class.java))
        }

        findViewById<View>(R.id.menu_theme).setOnClickListener {
            startActivity(Intent(this, ThemeSettingsActivity::class.java))
        }

        findViewById<View>(R.id.menu_about).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        // 退出登录
        findViewById<View>(R.id.btn_logout).setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    // ============================================================
    // 编辑资料 — 预留接口
    // ============================================================
    private fun onEditProfileClicked() {
        startActivity(Intent(this, EditProfileActivity::class.java))
    }

    // ============================================================
    // 头像点击 — 预留接口
    // ============================================================
    private fun onAvatarClicked() {
        if (!SettingsPrefs.getPrivacySettings(this).allowLocalAvatarAccess) {
            Toast.makeText(this, R.string.privacy_settings_avatar_disabled, Toast.LENGTH_SHORT).show()
            return
        }
        pickAvatarLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "avatar_crop.jpg"))
        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(600, 600)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK && data != null) {
            val resultUri = UCrop.getOutput(data) ?: return
            val current = viewModel.userProfile.value ?: return
            viewModel.updateUserProfile(current.copy(avatarUri = resultUri.toString()))
            Toast.makeText(this, "头像已更新", Toast.LENGTH_SHORT).show()
        }
        if (requestCode == UCrop.REQUEST_CROP && resultCode == UCrop.RESULT_ERROR && data != null) {
            Toast.makeText(this, "头像裁剪失败", Toast.LENGTH_SHORT).show()
        }
    }

    // ============================================================
    // 功能开发中提示
    // ============================================================
    private fun showComingSoon(feature: String) {
        Toast.makeText(this, "$feature 功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
    }

    // ============================================================
    // 退出登录确认弹窗
    // ============================================================
    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出当前账号吗？")
            .setPositiveButton("确定") { _, _ ->
                onLogoutConfirmed()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ============================================================
    // 退出登录逻辑 — 预留接口
    // ============================================================
    private fun onLogoutConfirmed() {
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
        finish()
    }
}
