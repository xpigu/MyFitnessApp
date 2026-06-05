package com.example.myfitnessapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity() {

    // 用户数据（后续可从 SharedPreferences / ViewModel 加载）
    private var username = "健身达人"
    private var bio = "坚持运动，遇见更好的自己"
    private var totalWorkouts = 34
    private var activeDays = 28
    private var level = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setupBottomNavigation()
        setupHeaderActions()
        setupMenuActions()
        bindUserData()
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
    private fun bindUserData() {
        findViewById<TextView>(R.id.tv_profile_username).text = username
        findViewById<TextView>(R.id.tv_profile_bio).text = bio
        findViewById<TextView>(R.id.tv_profile_workouts).text = totalWorkouts.toString()
        findViewById<TextView>(R.id.tv_profile_active_days).text = activeDays.toString()
        findViewById<TextView>(R.id.tv_profile_level).text = getString(R.string.profile_level_format, level)
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

        findViewById<View>(R.id.menu_goal_setting).setOnClickListener {
            showComingSoon("目标设置")
        }

        findViewById<View>(R.id.menu_achievements).setOnClickListener {
            showComingSoon("成就徽章")
        }

        // 设置相关菜单
        findViewById<View>(R.id.menu_notifications).setOnClickListener {
            showComingSoon("通知提醒")
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
        // TODO: 后续实现完整的编辑资料页面（EditProfileActivity）
        // 可编辑字段：头像、用户名、个性签名、生日、性别、身高、体重等
        Toast.makeText(this, "编辑资料功能开发中", Toast.LENGTH_SHORT).show()
    }

    // ============================================================
    // 头像点击 — 预留接口
    // ============================================================
    private fun onAvatarClicked() {
        // TODO: 后续接入图片选择器（如 PhotoPicker / Camera）
        Toast.makeText(this, "更换头像功能开发中", Toast.LENGTH_SHORT).show()
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
        // TODO: 后续实现完整的退出逻辑
        // 1. 清除本地登录状态（SharedPreferences / Token）
        // 2. 跳转到登录页面（LoginActivity）
        // 3. 调用 API 登出接口
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
        finish()
    }
}