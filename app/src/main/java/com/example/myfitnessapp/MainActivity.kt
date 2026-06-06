package com.example.myfitnessapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.random.Random
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.data.viewmodel.DailyCheckinViewModel
import com.example.myfitnessapp.data.viewmodel.DietRecordViewModel
import com.example.myfitnessapp.data.viewmodel.UserProfileViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var dietViewModel: DietRecordViewModel
    private lateinit var userProfileViewModel: UserProfileViewModel
    private lateinit var checkinViewModel: DailyCheckinViewModel
    
    private var budgetCalories = 2000
    private var targetWater = 8
    private val burnedCalories = 100
    
    private var hasShownCheckinPopup = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dietViewModel = ViewModelProvider(this).get(DietRecordViewModel::class.java)
        userProfileViewModel = ViewModelProvider(this).get(UserProfileViewModel::class.java)
        checkinViewModel = ViewModelProvider(this).get(DailyCheckinViewModel::class.java)
        
        dietViewModel.resetDailyWaterIfNeeded()
        ReminderScheduler.rescheduleAll(this)

        setupBottomNavigation()
        setupHeaderActions()
        setupDietSection()
        observeData()
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
    }

    // ============================================================
    // 观察数据变化
    // ============================================================
    private fun observeData() {
        userProfileViewModel.userProfile.observe(this) { profile ->
            budgetCalories = profile.targetDailyCalories
            targetWater = profile.targetDailyWater
            
            // 重新触发卡路里更新
            val currentConsumed = dietViewModel.todayTotalCalories.value ?: 0
            updateCaloriesDisplay(currentConsumed)
            
            // 重新触发饮水更新
            val currentWater = dietViewModel.waterCount.value ?: 0
            updateWaterDisplay(currentWater)
        }

        dietViewModel.todayTotalCalories.observe(this) { totalConsumed ->
            updateCaloriesDisplay(totalConsumed)
        }

        dietViewModel.waterCount.observe(this) { count ->
            updateWaterDisplay(count)
        }

        checkinViewModel.isCheckedInToday.observe(this) { isCheckedIn ->
            if (!isCheckedIn && !hasShownCheckinPopup) {
                showCheckinDialog()
                hasShownCheckinPopup = true
            }
        }
    }
    
    private fun showCheckinDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_daily_checkin, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // 强制用户选择签到或稍后
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.btn_dialog_checkin).setOnClickListener {
            checkinViewModel.checkinToday(
                onSuccess = { streak ->
                    Toast.makeText(this, "签到成功！连续签到 $streak 天", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                },
                onAlreadyCheckedIn = {
                    Toast.makeText(this, "今天已经签到过了哦", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            )
        }

        dialogView.findViewById<TextView>(R.id.tv_dialog_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateWaterDisplay(count: Int) {
        findViewById<TextView>(R.id.tv_water_count).text = "$count / $targetWater 杯"
    }

    // ============================================================
    // 底部导航
    // ============================================================
    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_health

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_health -> true
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
        findViewById<View>(R.id.ib_notification).setOnClickListener {
            startActivity(Intent(this, ReminderSettingsActivity::class.java))
        }
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        findViewById<View>(R.id.view_notification_badge).visibility =
            if (ReminderPrefs.hasAnyEnabledReminder(this)) View.VISIBLE else View.GONE
    }

    private fun handleReminderEntry(intent: Intent?) {
        if (intent?.getBooleanExtra(ReminderScheduler.EXTRA_FROM_REMINDER, false) != true) return

        when (ReminderType.from(intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TYPE))) {
            ReminderType.WATER -> {
                val scrollView = findViewById<NestedScrollView>(R.id.nested_scroll_view)
                val waterButton = findViewById<View>(R.id.btn_water)
                waterButton.scrollIntoContainer(scrollView, 24.dp())
                waterButton.playReminderFocusAnimation()
                Toast.makeText(this, R.string.reminder_water_opened_hint, Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }

        intent.removeExtra(ReminderScheduler.EXTRA_FROM_REMINDER)
        intent.removeExtra(ReminderScheduler.EXTRA_REMINDER_TYPE)
    }

    // ============================================================
    // 饮食记录模块
    // ============================================================
    private fun setupDietSection() {
        // 设置按钮
        findViewById<View>(R.id.ib_diet_settings).setOnClickListener {
            Toast.makeText(this, "饮食设置", Toast.LENGTH_SHORT).show()
        }

        // 三餐展开/收起
        setupMealExpand(R.id.ll_meal_breakfast_header, R.id.ll_breakfast_items, R.id.iv_breakfast_expand)
        setupMealExpand(R.id.ll_meal_lunch_header, R.id.ll_lunch_items, R.id.iv_lunch_expand)
        setupMealExpand(R.id.ll_meal_dinner_header, R.id.ll_dinner_items, R.id.iv_dinner_expand)
        setupMealExpand(R.id.ll_meal_snack_header, R.id.ll_snack_items, R.id.iv_snack_expand)

        // 添加食物按钮
        setupAddFoodButton(R.id.tv_breakfast_add_food, "BREAKFAST", "早餐")
        setupAddFoodButton(R.id.tv_lunch_add_food, "LUNCH", "午餐")
        setupAddFoodButton(R.id.tv_dinner_add_food, "DINNER", "晚餐")
        setupAddFoodButton(R.id.tv_snack_add_food, "SNACK", "加餐")

        // 饮水按钮
        findViewById<View>(R.id.btn_water).setOnClickListener {
            dietViewModel.addWater()
            Toast.makeText(this, "已饮水 1 杯", Toast.LENGTH_SHORT).show()
        }

        // 快速加餐按钮
        findViewById<View>(R.id.btn_quick_snack).setOnClickListener {
            val cal = 150
            dietViewModel.addDietRecord("SNACK", "快速加餐", cal)
            Toast.makeText(this, "快速加餐 +$cal kcal", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMealExpand(headerId: Int, itemsId: Int, expandIconId: Int) {
        findViewById<View>(headerId).setOnClickListener {
            val items = findViewById<LinearLayout>(itemsId)
            val icon = findViewById<ImageView>(expandIconId)
            if (items.visibility == View.GONE) {
                items.visibility = View.VISIBLE
                icon.animate().rotation(180f).setDuration(200).start()
            } else {
                items.visibility = View.GONE
                icon.animate().rotation(0f).setDuration(200).start()
            }
        }
    }

    private fun setupAddFoodButton(buttonId: Int, mealType: String, mealLabel: String) {
        findViewById<View>(buttonId).setOnClickListener {
            val addedCalories = listOf(50, 100, 150, 200, 250, 300)[Random.nextInt(6)]
            dietViewModel.addDietRecord(mealType, "食物", addedCalories, "自动添加")
            Toast.makeText(this, "$mealLabel +$addedCalories kcal", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCaloriesDisplay(totalConsumed: Int) {
        val remaining = budgetCalories - totalConsumed + burnedCalories
        findViewById<TextView>(R.id.tv_consumed_calories).text = totalConsumed.toString()
        findViewById<TextView>(R.id.tv_remaining_calories).text = remaining.toString()

        // 更新进度圆环
        val progressView = findViewById<ImageView>(R.id.iv_calories_progress)
        val progressPercent = ((budgetCalories - remaining).toFloat() / budgetCalories * 10000).toInt()
        progressView.setImageLevel(progressPercent)

        // 动态更新饮食建议
        val adviceTv = findViewById<TextView>(R.id.tv_diet_advice)
        adviceTv.text = when {
            remaining < 0 -> "已超出预算 ${-remaining} kcal，建议控制饮食并增加运动"
            remaining < 300 -> "剩余热量仅 $remaining kcal，建议多吃蔬菜水果"
            remaining < 800 -> "今日还可摄入约 $remaining kcal，合理分配三餐"
            else -> "今日还可摄入约 $remaining kcal，建议均衡搭配蛋白质和蔬菜"
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
