package com.example.myfitnessapp

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.myfitnessapp.data.viewmodel.UserProfileViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yalantis.ucrop.UCrop
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var viewModel: UserProfileViewModel

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) startCrop(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        viewModel = ViewModelProvider(this).get(UserProfileViewModel::class.java)

        setupBottomNavigation()
        setupHeaderActions()
        setupMenuActions()
        observeUserData()
    }

    // ============================================================
    // 观察用户数据变化
    // ============================================================
    private fun observeUserData() {
        viewModel.userProfile.observe(this) { profile ->
            bindUserData(profile)
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
    private fun bindUserData(profile: com.example.myfitnessapp.data.entity.UserProfile) {
        findViewById<TextView>(R.id.tv_profile_username).text = profile.username
        findViewById<TextView>(R.id.tv_profile_bio).text = profile.bio
        findViewById<TextView>(R.id.tv_profile_workouts).text = profile.totalWorkouts.toString()
        findViewById<TextView>(R.id.tv_profile_active_days).text = profile.activeDays.toString()
        findViewById<TextView>(R.id.tv_profile_level).text = getString(R.string.profile_level_format, profile.level)

        val avatarView = findViewById<ImageView>(R.id.iv_profile_avatar)
        if (profile.avatarUri.isNotEmpty()) {
            Glide.with(this)
                .load(Uri.parse(profile.avatarUri))
                .transform(CircleCrop())
                .placeholder(R.drawable.avatar_placeholder)
                .into(avatarView)
        } else {
            avatarView.setImageResource(R.drawable.avatar_placeholder)
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
            showComingSoon("个人资料")
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
            showComingSoon("隐私设置")
        }

        findViewById<View>(R.id.menu_theme).setOnClickListener {
            showComingSoon("主题切换")
        }

        findViewById<View>(R.id.menu_about).setOnClickListener {
            showComingSoon("关于我们")
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
