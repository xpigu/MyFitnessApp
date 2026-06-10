package com.example.myfitnessapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.data.model.BasicFoodCatalog
import com.example.myfitnessapp.data.model.DailyMealPlan
import com.example.myfitnessapp.data.model.DietGoalType
import com.example.myfitnessapp.data.model.FoodCatalogItem
import com.example.myfitnessapp.data.model.HealthyDietPlanner
import com.example.myfitnessapp.data.model.MacroSummary
import com.example.myfitnessapp.data.model.MealType
import com.example.myfitnessapp.data.model.MealPlanRecommendation
import com.example.myfitnessapp.data.model.PlannedFoodPortion
import com.example.myfitnessapp.data.entity.CustomFood
import com.example.myfitnessapp.data.entity.DietRecord
import com.example.myfitnessapp.data.entity.FavoriteMealCombo
import com.example.myfitnessapp.data.entity.UserProfile
import com.example.myfitnessapp.data.entity.WorkoutRecord
import com.example.myfitnessapp.data.viewmodel.DailyCheckinViewModel
import com.example.myfitnessapp.data.viewmodel.DietRecordViewModel
import com.example.myfitnessapp.data.viewmodel.SummaryData
import com.example.myfitnessapp.data.viewmodel.UserProfileViewModel
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var dietViewModel: DietRecordViewModel
    private lateinit var userProfileViewModel: UserProfileViewModel
    private lateinit var checkinViewModel: DailyCheckinViewModel
    private lateinit var workoutViewModel: WorkoutRecordViewModel

    private var currentProfile = UserProfile()
    private var budgetCalories = 2000
    private var targetSteps = 10000
    private var targetWater = 8
    private var currentConsumedCalories = 0
    private var currentWaterCount = 0
    private var currentWorkoutSummary = SummaryData()
    private var allWorkoutRecords: List<WorkoutRecord> = emptyList()
    private var todayWorkoutRecords: List<WorkoutRecord> = emptyList()
    private var allDietRecords: List<DietRecord> = emptyList()
    private var todayDietRecords: List<DietRecord> = emptyList()
    private var isCheckedInToday = false
    private var currentStreak = 0
    private var primaryAction = HealthPrimaryAction.TRAINING
    private var riskAction = HealthPrimaryAction.STATS
    private var currentMealPlan: DailyMealPlan? = null
    private var currentDietGoal = DietGoalType.MAINTAIN
    private var customFoods: List<CustomFood> = emptyList()
    private var favoriteCombos: List<FavoriteMealCombo> = emptyList()

    private var hasShownCheckinPopup = false

    private data class DietGoalOptionViews(
        val container: View,
        val badge: TextView,
        val title: TextView,
        val label: TextView,
        val summary: TextView,
        val focus: TextView,
        val macro: TextView,
        val marker: View
    )

    private data class DietToolCardViews(
        val container: View,
        val chip: TextView,
        val meta: TextView
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dietViewModel = ViewModelProvider(this).get(DietRecordViewModel::class.java)
        userProfileViewModel = ViewModelProvider(this).get(UserProfileViewModel::class.java)
        checkinViewModel = ViewModelProvider(this).get(DailyCheckinViewModel::class.java)
        workoutViewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)
        currentDietGoal = loadDietGoal()

        dietViewModel.resetDailyWaterIfNeeded()
        ReminderScheduler.rescheduleAll(this)

        setupBottomNavigation()
        setupHeaderActions()
        setupHealthFocusActions()
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
            currentProfile = profile
            budgetCalories = profile.targetDailyCalories
            targetSteps = profile.targetDailySteps
            targetWater = profile.targetDailyWater

            updateGreeting(profile)
            updateCaloriesDisplay(currentConsumedCalories)
            updateWaterDisplay(currentWaterCount)
            updateTodayProgress()
            updateHealthFocus()
            updateActiveChallenges()
            updateHealthScoreCard()
            updateWeekTrendCard()
            updateRiskAlertsCard()
            updateMealPlanCard()
        }

        dietViewModel.todayTotalCalories.observe(this) { totalConsumed ->
            currentConsumedCalories = totalConsumed
            updateCaloriesDisplay(totalConsumed)
            updateHealthFocus()
            updateActiveChallenges()
            updateHealthScoreCard()
            updateWeekTrendCard()
            updateRiskAlertsCard()
            updateMealPlanCard()
        }

        dietViewModel.waterCount.observe(this) { count ->
            currentWaterCount = count
            updateWaterDisplay(count)
            updateHealthFocus()
            updateActiveChallenges()
            updateHealthScoreCard()
            updateWeekTrendCard()
            updateRiskAlertsCard()
            updateMealPlanCard()
        }

        dietViewModel.allRecords.observe(this) { records ->
            allDietRecords = records
            updateHealthScoreCard()
            updateWeekTrendCard()
            updateRiskAlertsCard()
            updateMealPlanCard()
        }

        dietViewModel.customFoods.observe(this) { foods ->
            customFoods = foods
        }

        dietViewModel.favoriteCombos.observe(this) { combos ->
            favoriteCombos = combos
        }

        dietViewModel.todayRecords.observe(this) { records ->
            todayDietRecords = records
            renderMealSections(records)
            updateHealthFocus()
            updateActiveChallenges()
            updateHealthScoreCard()
            updateWeekTrendCard()
            updateRiskAlertsCard()
            updateMealPlanCard()
        }

        checkinViewModel.currentStreak.observe(this) { streak ->
            currentStreak = streak
            updateHealthFocus()
            updateActiveChallenges()
            updateHealthScoreCard()
            updateWeekTrendCard()
            updateRiskAlertsCard()
            updateMealPlanCard()
        }

        checkinViewModel.isCheckedInToday.observe(this) { checkedIn ->
            isCheckedInToday = checkedIn
            if (!checkedIn && !hasShownCheckinPopup) {
                showCheckinDialog()
                hasShownCheckinPopup = true
            }
            updateHealthFocus()
            updateActiveChallenges()
            updateHealthScoreCard()
            updateWeekTrendCard()
            updateRiskAlertsCard()
            updateMealPlanCard()
        }

        workoutViewModel.summary.observe(this) { summary ->
            currentWorkoutSummary = summary
            updateTodayProgress()
            updateCaloriesDisplay(currentConsumedCalories)
            updateHealthFocus()
            updateActiveChallenges()
            updateHealthScoreCard()
            updateWeekTrendCard()
            updateRiskAlertsCard()
            updateMealPlanCard()
        }


        workoutViewModel.allRecords.observe(this) { records ->
            allWorkoutRecords = records
            val today = DietRecordViewModel.todayDate()
            todayWorkoutRecords = records.filter { it.date == today }
            updateTodayProgress()
            updateHealthFocus()
            updateActiveChallenges()
            updateHealthScoreCard()
            updateWeekTrendCard()
            updateRiskAlertsCard()
            updateMealPlanCard()
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

    private fun updateGreeting(profile: UserProfile) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }

        findViewById<TextView>(R.id.tv_greeting).setText(greetingRes)
        findViewById<TextView>(R.id.tv_user_name).text =
            profile.username.ifBlank { getString(R.string.profile_username) }
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
        findViewById<View>(R.id.iv_avatar).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        findViewById<View>(R.id.view_notification_badge).visibility =
            if (ReminderPrefs.hasAnyEnabledReminder(this)) View.VISIBLE else View.GONE
    }

    private fun setupHealthFocusActions() {
        findViewById<View>(R.id.btn_health_primary_action).setOnClickListener {
            when (primaryAction) {
                HealthPrimaryAction.CHECKIN -> {
                    startActivity(Intent(this, DailyCheckinActivity::class.java))
                }
                HealthPrimaryAction.WATER -> {
                    focusWaterAction()
                }
                HealthPrimaryAction.TRAINING -> {
                    startActivity(Intent(this, TrainingActivity::class.java))
                }
                HealthPrimaryAction.DIET -> {
                    focusDietAction()
                }
                HealthPrimaryAction.STATS -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                }
            }
        }

        findViewById<View>(R.id.btn_health_secondary_action).setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        findViewById<View>(R.id.btn_health_risk_action).setOnClickListener {
            when (riskAction) {
                HealthPrimaryAction.CHECKIN -> startActivity(Intent(this, DailyCheckinActivity::class.java))
                HealthPrimaryAction.WATER -> focusWaterAction()
                HealthPrimaryAction.TRAINING -> startActivity(Intent(this, TrainingActivity::class.java))
                HealthPrimaryAction.DIET -> focusDietAction()
                HealthPrimaryAction.STATS -> startActivity(Intent(this, StatsActivity::class.java))
            }
        }

        findViewById<View>(R.id.challenge_card_1).setOnClickListener { focusWaterAction() }
        findViewById<View>(R.id.challenge_card_2).setOnClickListener {
            startActivity(Intent(this, TrainingActivity::class.java))
        }
        findViewById<View>(R.id.challenge_card_3).setOnClickListener {
            startActivity(Intent(this, TrainingActivity::class.java))
        }
        findViewById<View>(R.id.challenge_card_4).setOnClickListener { focusDietAction() }
    }

    private fun handleReminderEntry(intent: Intent?) {
        if (intent?.getBooleanExtra(ReminderScheduler.EXTRA_FROM_REMINDER, false) != true) return

        when (ReminderType.from(intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TYPE))) {
            ReminderType.WATER -> {
                focusWaterAction()
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
            showDietToolsDialog()
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

        findViewById<View>(R.id.btn_generate_meal_plan).setOnClickListener {
            showMealPlanDialog()
        }

        findViewById<View>(R.id.btn_apply_meal_plan).setOnClickListener {
            applyCurrentMealRecommendation()
        }

        findViewById<View>(R.id.btn_change_diet_goal).setOnClickListener {
            showDietGoalDialog()
        }

        // 快速加餐按钮
        findViewById<View>(R.id.btn_quick_snack).setOnClickListener {
            addSmartSnack()
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
            showFoodPickerDialog(mealType, mealLabel)
        }
    }

    private fun showFoodPickerDialog(mealType: String, mealLabel: String) {
        val meal = mealType.toMealType()
        val foods = BasicFoodCatalog.foodsForMeal(meal) + customFoods
            .filter { it.mealType == meal.name }
            .map { it.toFoodCatalogItem() }
        val labels = foods.map { food ->
            val customTag = if (food.code.startsWith("custom_")) " · 自定义" else ""
            "${food.name} · ${food.caloriesPer100g} kcal/100g$customTag"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择${mealLabel}食物")
            .setItems(labels) { _, which ->
                showPortionPickerDialog(mealType, mealLabel, foods[which])
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPortionPickerDialog(mealType: String, mealLabel: String, food: FoodCatalogItem) {
        val portions = HealthyDietPlanner.portionOptions(food)
        val labels = portions.map { grams ->
            val calories = HealthyDietPlanner.calculateCalories(food, grams)
            "${grams}g · ${calories} kcal"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择${food.name}份量")
            .setItems(labels) { _, which ->
                val grams = portions[which]
                val calories = HealthyDietPlanner.calculateCalories(food, grams)
                val notes = "${grams}g · ${food.caloriesPer100g} kcal/100g"
                dietViewModel.addDietRecord(mealType, food.name, calories, notes)
                Toast.makeText(this, "${mealLabel}已添加 ${food.name} ${grams}g", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addSmartSnack() {
        val snack = HealthyDietPlanner.recommendedSnack(remainingDietCalories(), currentDietGoal)
        addPlannedFood(MealType.SNACK, snack, "智能推荐")
        Toast.makeText(this, "已添加智能加餐：${snack.food.name} ${snack.grams}g", Toast.LENGTH_SHORT).show()
    }

    private fun updateCaloriesDisplay(totalConsumed: Int) {
        val burnedCalories = currentWorkoutSummary.totalCalories
        val remaining = budgetCalories - totalConsumed + burnedCalories
        findViewById<TextView>(R.id.tv_budget_calories).text = budgetCalories.toString()
        findViewById<TextView>(R.id.tv_burned_calories).text = burnedCalories.toString()
        findViewById<TextView>(R.id.tv_consumed_calories).text = totalConsumed.toString()
        findViewById<TextView>(R.id.tv_remaining_calories).text = remaining.toString()

        val progressView = findViewById<ImageView>(R.id.iv_calories_progress_chart)
        val progressPercent = percentageToImageLevel(totalConsumed, budgetCalories)
        progressView.setImageLevel(progressPercent)

        val adviceTv = findViewById<TextView>(R.id.tv_diet_advice)
        adviceTv.text = when {
            remaining < 0 -> "已超出预算 ${-remaining} kcal，建议先按${currentDietGoal.label}目标收紧后续饮食并增加活动量"
            currentDietGoal == DietGoalType.CUT && remaining < 300 -> "减脂阶段剩余热量仅 $remaining kcal，建议优先蔬菜、瘦肉和低糖食物"
            currentDietGoal == DietGoalType.GAIN && remaining > 500 -> "增肌阶段仍有约 $remaining kcal，可补充优质碳水和蛋白质帮助恢复"
            remaining < 800 -> "今日还可摄入约 $remaining kcal，建议按${currentDietGoal.label}目标合理分配三餐"
            else -> "今日还可摄入约 $remaining kcal，建议围绕${currentDietGoal.label}目标搭配蛋白质、碳水和蔬菜"
        }
    }

    private fun updateMealPlanCard() {
        val completedMeals = todayDietRecords
            .mapNotNull { record -> record.mealType.toMealTypeOrNull() }
            .toSet()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val plan = HealthyDietPlanner.buildDailyPlan(
            budgetCalories = budgetCalories,
            consumedCalories = currentConsumedCalories,
            burnedCalories = currentWorkoutSummary.totalCalories,
            completedMeals = completedMeals,
            currentHour = hour,
            goalType = currentDietGoal
        )
        currentMealPlan = plan
        val nextMealLabel = mealLabel(plan.nextMeal.mealType)
        val summary = if (remainingDietCalories() <= 0) {
            "当前以${currentDietGoal.label}为目标，今天摄入已经接近预算，建议优先选择：${plan.nextMeal.summary()}。"
        } else {
            "当前目标：${currentDietGoal.label}。建议优先安排$nextMealLabel：${plan.nextMeal.summary()}，当前剩余约 ${plan.remainingCalories} kcal。"
        }
        findViewById<TextView>(R.id.tv_meal_plan_summary).text = summary
        val recommendationMacros = HealthyDietPlanner.summarizeMacros(plan.nextMeal.items)
        findViewById<TextView>(R.id.btn_apply_meal_plan).text =
            "一键加入推荐 · ${nextMealLabel} ${plan.nextMeal.totalCalories} kcal"
        findViewById<TextView>(R.id.tv_diet_goal_summary).text =
            "当前以${currentDietGoal.label}为目标，${currentDietGoal.summary}"
        updateMacroSummary(recommendationMacros, plan.targetMacros)
        updateDietInsights(plan.targetMacros)
    }

    private fun showMealPlanDialog() {
        val plan = currentMealPlan ?: HealthyDietPlanner.buildDailyPlan(
            budgetCalories = budgetCalories,
            consumedCalories = currentConsumedCalories,
            burnedCalories = currentWorkoutSummary.totalCalories,
            completedMeals = todayDietRecords.mapNotNull { it.mealType.toMealTypeOrNull() }.toSet(),
            currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            goalType = currentDietGoal
        )
        currentMealPlan = plan

        val dialogView = layoutInflater.inflate(R.layout.dialog_meal_plan, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        // 绑定状态预览
        dialogView.findViewById<TextView>(R.id.tv_meal_plan_goal_label).text = plan.goalType.label
        dialogView.findViewById<TextView>(R.id.tv_meal_plan_remaining_calories).text =
            "今日剩余热量：${plan.remainingCalories} kcal"
        dialogView.findViewById<TextView>(R.id.tv_meal_plan_target_macros).text =
            "目标营养素：蛋白质 ${formatMacro(plan.targetMacros.protein)}g · 碳水 ${formatMacro(plan.targetMacros.carbs)}g · 脂肪 ${formatMacro(plan.targetMacros.fat)}g"

        // 动态填充推荐列表
        val container = dialogView.findViewById<LinearLayout>(R.id.layout_meal_recommendations_container)
        plan.recommendations.forEach { recommendation ->
            val itemView = layoutInflater.inflate(R.layout.item_meal_recommendation, container, false)
            
            val isNext = recommendation == plan.nextMeal
            itemView.findViewById<TextView>(R.id.tv_item_meal_type_label).apply {
                text = mealLabel(recommendation.mealType)
                if (isNext) {
                    setBackgroundResource(R.drawable.category_chip_selected)
                    setTextColor(getColor(R.color.white))
                }
            }
            
            itemView.findViewById<TextView>(R.id.tv_item_meal_title).text = recommendation.title
            itemView.findViewById<TextView>(R.id.tv_item_meal_calories).text = "${recommendation.totalCalories} kcal"
            itemView.findViewById<TextView>(R.id.tv_item_meal_summary).text = recommendation.summary()
            
            val macros = HealthyDietPlanner.summarizeMacros(recommendation.items)
            itemView.findViewById<TextView>(R.id.tv_item_meal_macros).text =
                "蛋白质 ${formatMacro(macros.protein)}g · 碳水 ${formatMacro(macros.carbs)}g · 脂肪 ${formatMacro(macros.fat)}g"

            if (isNext) {
                itemView.setBackgroundResource(R.drawable.dialog_goal_option_selected_bg)
            }

            container.addView(itemView)
        }

        dialogView.findViewById<View>(R.id.btn_meal_plan_dialog_close).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btn_meal_plan_dialog_apply_next).setOnClickListener {
            applyMealRecommendation(plan.nextMeal)
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

    private fun showDietGoalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_diet_goal, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        var selectedGoal = currentDietGoal
        val previewTitle = dialogView.findViewById<TextView>(R.id.tv_diet_goal_preview_title)
        val previewSummary = dialogView.findViewById<TextView>(R.id.tv_diet_goal_preview_summary)
        val confirmButton = dialogView.findViewById<TextView>(R.id.btn_diet_goal_dialog_confirm)
        val optionViews = mapOf(
            DietGoalType.CUT to DietGoalOptionViews(
                container = dialogView.findViewById(R.id.layout_goal_option_cut),
                badge = dialogView.findViewById(R.id.tv_goal_option_badge_cut),
                title = dialogView.findViewById(R.id.tv_goal_option_title_cut),
                label = dialogView.findViewById(R.id.tv_goal_option_label_cut),
                summary = dialogView.findViewById(R.id.tv_goal_option_summary_cut),
                focus = dialogView.findViewById(R.id.tv_goal_option_focus_cut),
                macro = dialogView.findViewById(R.id.tv_goal_option_macro_cut),
                marker = dialogView.findViewById(R.id.view_goal_marker_cut)
            ),
            DietGoalType.MAINTAIN to DietGoalOptionViews(
                container = dialogView.findViewById(R.id.layout_goal_option_maintain),
                badge = dialogView.findViewById(R.id.tv_goal_option_badge_maintain),
                title = dialogView.findViewById(R.id.tv_goal_option_title_maintain),
                label = dialogView.findViewById(R.id.tv_goal_option_label_maintain),
                summary = dialogView.findViewById(R.id.tv_goal_option_summary_maintain),
                focus = dialogView.findViewById(R.id.tv_goal_option_focus_maintain),
                macro = dialogView.findViewById(R.id.tv_goal_option_macro_maintain),
                marker = dialogView.findViewById(R.id.view_goal_marker_maintain)
            ),
            DietGoalType.GAIN to DietGoalOptionViews(
                container = dialogView.findViewById(R.id.layout_goal_option_gain),
                badge = dialogView.findViewById(R.id.tv_goal_option_badge_gain),
                title = dialogView.findViewById(R.id.tv_goal_option_title_gain),
                label = dialogView.findViewById(R.id.tv_goal_option_label_gain),
                summary = dialogView.findViewById(R.id.tv_goal_option_summary_gain),
                focus = dialogView.findViewById(R.id.tv_goal_option_focus_gain),
                macro = dialogView.findViewById(R.id.tv_goal_option_macro_gain),
                marker = dialogView.findViewById(R.id.view_goal_marker_gain)
            )
        )

        optionViews.forEach { (goal, views) ->
            views.title.text = goal.label
            views.label.text = dietGoalQuickLabel(goal)
            views.summary.text = goal.summary
            views.focus.text = dietGoalFocusLabel(goal)
            views.macro.text = buildDietGoalMacroHint(goal)
            views.container.setOnClickListener {
                selectedGoal = goal
                renderDietGoalSelection(
                    optionViews = optionViews,
                    selectedGoal = selectedGoal,
                    previewTitle = previewTitle,
                    previewSummary = previewSummary,
                    confirmButton = confirmButton
                )
            }
        }
        renderDietGoalSelection(
            optionViews = optionViews,
            selectedGoal = selectedGoal,
            previewTitle = previewTitle,
            previewSummary = previewSummary,
            confirmButton = confirmButton
        )

        dialogView.findViewById<View>(R.id.btn_diet_goal_dialog_cancel).setOnClickListener {
            dialog.dismiss()
        }
        confirmButton.setOnClickListener {
            currentDietGoal = selectedGoal
            saveDietGoal(currentDietGoal)
            updateCaloriesDisplay(currentConsumedCalories)
            updateMealPlanCard()
            Toast.makeText(this, "已切换为${currentDietGoal.label}目标", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val targetWidth = (screenWidth * 0.9f).toInt()
            val maxWidth = (420 * displayMetrics.density).toInt() // 限制最大宽度，防止大屏/横屏拉伸
            setLayout(
                if (targetWidth > maxWidth) maxWidth else targetWidth,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun renderDietGoalSelection(
        optionViews: Map<DietGoalType, DietGoalOptionViews>,
        selectedGoal: DietGoalType,
        previewTitle: TextView,
        previewSummary: TextView,
        confirmButton: TextView
    ) {
        optionViews.forEach { (goal, views) ->
            val isSelected = goal == selectedGoal
            views.container.setBackgroundResource(
                if (isSelected) R.drawable.dialog_goal_option_selected_bg else R.drawable.dialog_goal_option_bg
            )
            views.badge.visibility = if (isSelected) View.VISIBLE else View.GONE
            views.label.setBackgroundResource(
                if (isSelected) R.drawable.category_chip_selected else R.drawable.category_chip_unselected
            )
            views.focus.setBackgroundResource(
                if (isSelected) R.drawable.category_chip_selected else R.drawable.category_chip_unselected
            )
            views.title.setTextColor(
                getColor(if (isSelected) R.color.indigo_primary else R.color.text_primary)
            )
            views.label.setTextColor(
                getColor(if (isSelected) R.color.white else R.color.gray_600)
            )
            views.summary.setTextColor(
                getColor(if (isSelected) R.color.indigo_dark else R.color.text_secondary)
            )
            views.focus.setTextColor(
                getColor(if (isSelected) R.color.white else R.color.gray_600)
            )
            views.macro.setTextColor(
                getColor(if (isSelected) R.color.text_primary else R.color.text_secondary)
            )
            views.marker.alpha = if (isSelected) 1f else 0.7f
            views.container.animate()
                .scaleX(if (isSelected) 1.02f else 1f)
                .scaleY(if (isSelected) 1.02f else 1f)
                .setDuration(160)
                .start()
        }
        val preview = buildDietGoalPreview(selectedGoal)
        previewTitle.text = preview.first
        previewSummary.text = preview.second
        confirmButton.text = "保存为${selectedGoal.label}目标"
    }

    private fun dietGoalQuickLabel(goal: DietGoalType): String = when (goal) {
        DietGoalType.CUT -> "高蛋白优先"
        DietGoalType.MAINTAIN -> "均衡稳定"
        DietGoalType.GAIN -> "恢复增肌"
    }

    private fun dietGoalFocusLabel(goal: DietGoalType): String = when (goal) {
        DietGoalType.CUT -> "控制热量密度"
        DietGoalType.MAINTAIN -> "三餐分配更稳"
        DietGoalType.GAIN -> "补足碳水和蛋白"
    }

    private fun buildDietGoalMacroHint(goal: DietGoalType): String {
        val macros = HealthyDietPlanner.targetMacros(budgetCalories, goal)
        return "目标营养：蛋白质 ${formatMacro(macros.protein)}g · 碳水 ${formatMacro(macros.carbs)}g · 脂肪 ${formatMacro(macros.fat)}g"
    }

    private fun buildDietGoalPreview(goal: DietGoalType): Pair<String, String> {
        val macros = HealthyDietPlanner.targetMacros(budgetCalories, goal)
        val previewTitle = "${goal.label}目标预览"
        val previewSummary = when (goal) {
            DietGoalType.CUT -> "今日预算 $budgetCalories kcal，当前已摄入 $currentConsumedCalories kcal，剩余参考 ${remainingDietCalories()} kcal。建议继续优先瘦肉、蔬菜和低糖主食，蛋白质 ${formatMacro(macros.protein)}g 为重点。"
            DietGoalType.MAINTAIN -> "今日预算 $budgetCalories kcal，当前已摄入 $currentConsumedCalories kcal，剩余参考 ${remainingDietCalories()} kcal。建议把三餐和加餐分配得更均衡，目标营养更强调稳定和可持续。"
            DietGoalType.GAIN -> "今日预算 $budgetCalories kcal，当前已摄入 $currentConsumedCalories kcal，剩余参考 ${remainingDietCalories()} kcal。建议围绕训练恢复补充优质碳水和蛋白质，目标蛋白质 ${formatMacro(macros.protein)}g、碳水 ${formatMacro(macros.carbs)}g。"
        }
        return previewTitle to previewSummary
    }

    private fun showDietToolsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_diet_tools, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        val statusSummary = dialogView.findViewById<TextView>(R.id.tv_diet_tools_status_summary)
        val addCustomFoodCard = DietToolCardViews(
            container = dialogView.findViewById(R.id.layout_tool_add_custom_food),
            chip = dialogView.findViewById(R.id.tv_tool_add_custom_food_chip),
            meta = dialogView.findViewById(R.id.tv_tool_add_custom_food_meta)
        )
        val saveRecommendationCard = DietToolCardViews(
            container = dialogView.findViewById(R.id.layout_tool_save_recommendation),
            chip = dialogView.findViewById(R.id.tv_tool_save_recommendation_chip),
            meta = dialogView.findViewById(R.id.tv_tool_save_recommendation_meta)
        )
        val useFavoriteComboCard = DietToolCardViews(
            container = dialogView.findViewById(R.id.layout_tool_use_favorite_combo),
            chip = dialogView.findViewById(R.id.tv_tool_use_favorite_combo_chip),
            meta = dialogView.findViewById(R.id.tv_tool_use_favorite_combo_meta)
        )

        val nextRecommendation = currentMealPlan?.nextMeal
        statusSummary.text = buildDietToolsStatusSummary(nextRecommendation)

        addCustomFoodCard.chip.text = "录入入口"
        addCustomFoodCard.meta.text = if (customFoods.isEmpty()) {
            "当前还没有自定义食物，适合先录入你常吃但系统里没有的食物。"
        } else {
            "已保存 ${customFoods.size} 个自定义食物，后续可直接加入早餐、午餐、晚餐或加餐。"
        }
        bindDietToolCard(
            card = addCustomFoodCard,
            enabled = true
        ) {
            dialog.dismiss()
            showCustomFoodDialog()
        }

        val recommendationEnabled = nextRecommendation != null
        saveRecommendationCard.chip.text = if (recommendationEnabled) "当前可收藏" else "需先生成食谱"
        saveRecommendationCard.meta.text = if (nextRecommendation == null) {
            "先点击健康食谱规划里的“生成食谱”，这里才会出现可收藏的下一餐推荐。"
        } else {
            "当前可收藏 ${mealLabel(nextRecommendation.mealType)}推荐，约 ${nextRecommendation.totalCalories} kcal。"
        }
        bindDietToolCard(
            card = saveRecommendationCard,
            enabled = recommendationEnabled
        ) {
            dialog.dismiss()
            saveCurrentRecommendationAsFavorite()
        }

        val favoriteEnabled = favoriteCombos.isNotEmpty()
        useFavoriteComboCard.chip.text = if (favoriteEnabled) "已收藏 ${favoriteCombos.size} 个" else "暂无收藏"
        useFavoriteComboCard.meta.text = if (favoriteEnabled) {
            "最近可以直接复用常用组合，快速完成饮食记录和热量计算。"
        } else {
            "先把一份当前推荐收藏起来，这里就能一键复用你常用的搭配。"
        }
        bindDietToolCard(
            card = useFavoriteComboCard,
            enabled = favoriteEnabled
        ) {
            dialog.dismiss()
            showFavoriteCombosDialog()
        }

        dialogView.findViewById<View>(R.id.btn_diet_tools_dialog_close).setOnClickListener {
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

    private fun bindDietToolCard(
        card: DietToolCardViews,
        enabled: Boolean,
        onClick: () -> Unit
    ) {
        card.container.alpha = if (enabled) 1f else 0.68f
        card.chip.setBackgroundResource(
            if (enabled) R.drawable.category_chip_selected else R.drawable.category_chip_unselected
        )
        card.chip.setTextColor(
            getColor(if (enabled) R.color.white else R.color.gray_600)
        )
        card.container.setOnClickListener(
            if (enabled) {
                {
                    card.container.animate()
                        .scaleX(1.02f)
                        .scaleY(1.02f)
                        .setDuration(100)
                        .withEndAction {
                            card.container.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .withEndAction(onClick)
                                .start()
                        }
                        .start()
                }
            } else {
                null
            }
        )
    }

    private fun buildDietToolsStatusSummary(nextRecommendation: MealPlanRecommendation?): String {
        val customFoodSummary = if (customFoods.isEmpty()) {
            "还没有自定义食物"
        } else {
            "已有 ${customFoods.size} 个自定义食物"
        }
        val favoriteSummary = if (favoriteCombos.isEmpty()) {
            "暂无常用组合"
        } else {
            "已收藏 ${favoriteCombos.size} 个常用组合"
        }
        val recommendationSummary = if (nextRecommendation == null) {
            "当前还没有可收藏的推荐"
        } else {
            "当前可收藏 ${mealLabel(nextRecommendation.mealType)}推荐"
        }
        return "$customFoodSummary，$favoriteSummary，$recommendationSummary。"
    }

    private fun showCustomFoodDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_food_v2, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        val etName = dialogView.findViewById<EditText>(R.id.et_custom_food_name_v2)
        val etCalories = dialogView.findViewById<EditText>(R.id.et_custom_food_calories_v2)
        val etProtein = dialogView.findViewById<EditText>(R.id.et_custom_food_protein_v2)
        val etCarbs = dialogView.findViewById<EditText>(R.id.et_custom_food_carbs_v2)
        val etFat = dialogView.findViewById<EditText>(R.id.et_custom_food_fat_v2)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinner_custom_food_meal_type_v2)

        val mealTypes = MealType.entries.toTypedArray()
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            mealTypes.map { mealLabel(it) }
        )

        dialogView.findViewById<View>(R.id.btn_custom_food_dialog_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btn_custom_food_dialog_save).setOnClickListener {
            val name = etName.text.toString().trim()
            val calories = etCalories.text.toString().trim().toIntOrNull()
            val protein = etProtein.text.toString().trim().toDoubleOrNull()
            val carbs = etCarbs.text.toString().trim().toDoubleOrNull()
            val fat = etFat.text.toString().trim().toDoubleOrNull()
            val mealType = mealTypes[spinner.selectedItemPosition]

            if (name.isBlank() || calories == null || protein == null || carbs == null || fat == null) {
                Toast.makeText(this, "请完整填写食物信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dietViewModel.addCustomFood(
                name = name,
                caloriesPer100g = calories,
                proteinPer100g = protein,
                carbsPer100g = carbs,
                fatPer100g = fat,
                mealType = mealType.name
            )
            Toast.makeText(this, "已保存自定义食物：$name", Toast.LENGTH_SHORT).show()
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

    private fun saveCurrentRecommendationAsFavorite() {
        val recommendation = currentMealPlan?.nextMeal
        if (recommendation == null) {
            Toast.makeText(this, "当前还没有可收藏的推荐组合", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_save_favorite, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        val etName = dialogView.findViewById<EditText>(R.id.et_favorite_combo_name)
        val tvType = dialogView.findViewById<TextView>(R.id.tv_favorite_combo_preview_type)
        val tvSummary = dialogView.findViewById<TextView>(R.id.tv_favorite_combo_preview_summary)

        val defaultName = "${mealLabel(recommendation.mealType)}常用组合"
        etName.setText(defaultName)
        etName.setSelection(defaultName.length)

        tvType.text = "${mealLabel(recommendation.mealType)} · ${recommendation.totalCalories} kcal"
        tvSummary.text = recommendation.summary()

        dialogView.findViewById<View>(R.id.btn_save_favorite_dialog_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btn_save_favorite_dialog_confirm).setOnClickListener {
            val name = etName.text.toString().trim().ifBlank { defaultName }
            val payload = HealthyDietPlanner.serializeFavoriteItems(recommendation.items)
            dietViewModel.addFavoriteCombo(
                name = name,
                mealType = recommendation.mealType.name,
                itemsPayload = payload,
                totalCalories = recommendation.totalCalories
            )
            Toast.makeText(this, "已收藏常用组合：$name", Toast.LENGTH_SHORT).show()
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

    private fun showFavoriteCombosDialog() {
        if (favoriteCombos.isEmpty()) {
            Toast.makeText(this, R.string.diet_favorite_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_favorite_combos, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        val container = dialogView.findViewById<LinearLayout>(R.id.layout_favorite_combos_container)
        favoriteCombos.forEach { combo ->
            val itemView = layoutInflater.inflate(R.layout.item_favorite_combo, container, false)
            itemView.findViewById<TextView>(R.id.tv_combo_name).text = combo.name
            itemView.findViewById<TextView>(R.id.tv_combo_meal_type).text = mealLabel(combo.mealType.toMealType())
            itemView.findViewById<TextView>(R.id.tv_combo_calories).text = "${combo.totalCalories} kcal"
            
            itemView.setOnClickListener {
                applyFavoriteCombo(combo)
                dialog.dismiss()
            }
            container.addView(itemView)
        }

        dialogView.findViewById<View>(R.id.btn_favorite_combos_dialog_close).setOnClickListener {
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

    private fun applyFavoriteCombo(combo: FavoriteMealCombo) {
        val mealType = combo.mealType.toMealType()
        val portions = HealthyDietPlanner.deserializeFavoriteItems(combo.itemsPayload, mealType)
        if (portions.isEmpty()) {
            Toast.makeText(this, "这个常用组合数据已损坏，无法使用", Toast.LENGTH_SHORT).show()
            return
        }
        portions.forEach { addPlannedFood(mealType, it, "常用组合") }
        Toast.makeText(this, "已添加常用组合：${combo.name}", Toast.LENGTH_SHORT).show()
    }

    private fun applyCurrentMealRecommendation() {
        val plan = currentMealPlan ?: return
        applyMealRecommendation(plan.nextMeal)
    }

    private fun applyMealRecommendation(recommendation: MealPlanRecommendation) {
        recommendation.items.forEach { portion ->
            addPlannedFood(recommendation.mealType, portion, "食谱规划")
        }
        Toast.makeText(
            this,
            "已加入${mealLabel(recommendation.mealType)}推荐，共 ${recommendation.totalCalories} kcal",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun addPlannedFood(mealType: MealType, portion: PlannedFoodPortion, source: String) {
        val notes = "${portion.grams}g · ${portion.food.caloriesPer100g} kcal/100g · $source"
        dietViewModel.addDietRecord(mealType.name, portion.food.name, portion.calories, notes)
    }

    private fun updateTodayProgress() {
        val steps = calculateTodaySteps(todayWorkoutRecords)
        val workoutCalories = currentWorkoutSummary.totalCalories
        val workoutMinutes = ((currentWorkoutSummary.totalDurationSeconds + 59) / 60).coerceAtLeast(0)

        findViewById<TextView>(R.id.tv_steps_value).text = formatNumber(steps)
        findViewById<TextView>(R.id.tv_calories_value).text = workoutCalories.toString()
        findViewById<TextView>(R.id.tv_exercise_value).text = workoutMinutes.toString()

        findViewById<ImageView>(R.id.iv_steps_progress)
            .setImageLevel(percentageToImageLevel(steps, targetSteps))
        findViewById<ImageView>(R.id.iv_calories_progress)
            .setImageLevel(percentageToImageLevel(workoutCalories, workoutCalorieTarget()))
        findViewById<ImageView>(R.id.iv_exercise_progress)
            .setImageLevel(percentageToImageLevel(workoutMinutes, 60))
    }

    private fun calculateTodaySteps(records: List<WorkoutRecord>): Int {
        val explicitSteps = records.sumOf { it.runSteps }
        if (explicitSteps > 0) return explicitSteps

        val runningDistance = records
            .filter { it.sportType == "RUN" }
            .sumOf { it.totalDistance }
        return if (runningDistance > 0.0) (runningDistance * 1300).toInt() else 0
    }

    private fun renderMealSections(records: List<DietRecord>) {
        renderMealSection(
            mealType = "BREAKFAST",
            recordListId = R.id.ll_breakfast_record_list,
            emptyId = R.id.tv_breakfast_empty,
            totalId = R.id.tv_breakfast_calories
        )
        renderMealSection(
            mealType = "LUNCH",
            recordListId = R.id.ll_lunch_record_list,
            emptyId = R.id.tv_lunch_empty,
            totalId = R.id.tv_lunch_calories
        )
        renderMealSection(
            mealType = "DINNER",
            recordListId = R.id.ll_dinner_record_list,
            emptyId = R.id.tv_dinner_empty,
            totalId = R.id.tv_dinner_calories
        )
        renderMealSection(
            mealType = "SNACK",
            recordListId = R.id.ll_snack_record_list,
            emptyId = R.id.tv_snack_empty,
            totalId = R.id.tv_snack_calories
        )
    }

    private fun renderMealSection(
        mealType: String,
        recordListId: Int,
        emptyId: Int,
        totalId: Int
    ) {
        val mealRecords = todayDietRecords
            .filter { it.mealType == mealType }
            .sortedByDescending { it.timestamp }
        val recordList = findViewById<LinearLayout>(recordListId)
        val emptyView = findViewById<TextView>(emptyId)
        val totalView = findViewById<TextView>(totalId)

        recordList.removeAllViews()
        totalView.text = "${mealRecords.sumOf { it.calories }} kcal"

        if (mealRecords.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            return
        }

        emptyView.visibility = View.GONE
        mealRecords.forEachIndexed { index, record ->
            val itemView = TextView(this).apply {
                text = record.displayText()
                textSize = 12f
                setTextColor(getColor(R.color.text_secondary))
                if (index > 0) {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 4.dp()
                    }
                }
            }
            recordList.addView(itemView)
        }
    }

    private fun updateMacroSummary(recommendationMacros: MacroSummary? = null, targetMacros: MacroSummary? = null) {
        val todayMacros = summarizeTodayMacros(todayDietRecords)
        val summary = buildString {
            append("今日已摄入：蛋白质 ${formatMacro(todayMacros.protein)}g")
            append(" · 碳水 ${formatMacro(todayMacros.carbs)}g")
            append(" · 脂肪 ${formatMacro(todayMacros.fat)}g")
            if (targetMacros != null) {
                append("\n${currentDietGoal.label}目标：蛋白质 ${formatMacro(targetMacros.protein)}g")
                append(" · 碳水 ${formatMacro(targetMacros.carbs)}g")
                append(" · 脂肪 ${formatMacro(targetMacros.fat)}g")
            }
            if (recommendationMacros != null) {
                append("\n下一餐推荐：蛋白质 ${formatMacro(recommendationMacros.protein)}g")
                append(" · 碳水 ${formatMacro(recommendationMacros.carbs)}g")
                append(" · 脂肪 ${formatMacro(recommendationMacros.fat)}g")
            }
        }
        findViewById<TextView>(R.id.tv_macro_summary).text = summary
    }

    private fun updateDietInsights(targetMacros: MacroSummary) {
        val todayMacros = summarizeMacros(todayDietRecords)
        val recentDays = buildRecentMacroSeries()
        val loggedDays = recentDays.count { it.macro.protein > 0 || it.macro.carbs > 0 || it.macro.fat > 0 }
        val averageMacro = if (recentDays.isEmpty()) {
            MacroSummary()
        } else {
            MacroSummary(
                protein = recentDays.sumOf { it.macro.protein } / recentDays.size,
                carbs = recentDays.sumOf { it.macro.carbs } / recentDays.size,
                fat = recentDays.sumOf { it.macro.fat } / recentDays.size
            )
        }

        val proteinProgress = macroProgress(todayMacros.protein, targetMacros.protein)
        val carbsProgress = macroProgress(todayMacros.carbs, targetMacros.carbs)
        val fatProgress = macroProgress(todayMacros.fat, targetMacros.fat)

        val todayScore = macroBalanceScore(todayMacros, targetMacros)
        val averageScore = if (recentDays.isEmpty()) 0 else recentDays.map { macroBalanceScore(it.macro, targetMacros) }.average().toInt()

        val summary = when {
            loggedDays == 0 -> "近 7 天还没有形成有效饮食记录，继续记录后这里会显示趋势。"
            loggedDays < 3 -> "近 7 天只有 $loggedDays 天饮食记录，平均每日蛋白质 ${formatMacro(averageMacro.protein)}g，建议继续补齐记录。"
            else -> "近 7 天有 $loggedDays 天记录饮食，平均每日蛋白质 ${formatMacro(averageMacro.protein)}g，碳水 ${formatMacro(averageMacro.carbs)}g，脂肪 ${formatMacro(averageMacro.fat)}g。"
        }
        findViewById<TextView>(R.id.tv_diet_insight_summary).text = summary
        findViewById<TextView>(R.id.tv_diet_target_completion).text =
            "今日达成：蛋白质 $proteinProgress% · 碳水 $carbsProgress% · 脂肪 $fatProgress%"
        findViewById<TextView>(R.id.tv_diet_trend_signal).text = when {
            loggedDays == 0 -> "先连续记录几天饮食，系统会更准确地判断你是否接近${currentDietGoal.label}目标。"
            todayScore >= averageScore + 8 -> "今天比近 7 天平均更接近${currentDietGoal.label}目标，当前营养分配更稳。"
            todayScore <= averageScore - 8 -> "今天离${currentDietGoal.label}目标还有差距，建议优先补齐偏低的营养素。"
            else -> "今天和近 7 天平均水平接近，继续保持会更容易稳定贴近${currentDietGoal.label}目标。"
        }
    }

    private fun updateHealthFocus() {
        val waterDone = targetWater > 0 && currentWaterCount >= targetWater
        val workoutMinutes = ((currentWorkoutSummary.totalDurationSeconds + 59) / 60).coerceAtLeast(0)
        val workoutDone = currentWorkoutSummary.workoutCount > 0 || workoutMinutes >= 20
        val completedCount = listOf(isCheckedInToday, waterDone, workoutDone).count { it }
        val statusTitleView = findViewById<TextView>(R.id.tv_health_focus_title)
        val summaryView = findViewById<TextView>(R.id.tv_health_focus_summary)
        val metaView = findViewById<TextView>(R.id.tv_health_focus_meta)

        findViewById<TextView>(R.id.tv_focus_checkin_status).text =
            "今日签到: " + if (isCheckedInToday) "已完成，当前连续 $currentStreak 天" else "待完成，建议尽快补签"
        findViewById<TextView>(R.id.tv_focus_water_status).text =
            if (waterDone) "饮水目标: 已完成 $currentWaterCount/$targetWater 杯" else "饮水目标: $currentWaterCount/$targetWater 杯，还差 ${(targetWater - currentWaterCount).coerceAtLeast(0)} 杯"
        findViewById<TextView>(R.id.tv_focus_workout_status).text =
            if (workoutDone) "训练完成: 已完成 ${currentWorkoutSummary.workoutCount} 次，累计 ${workoutMinutes} 分钟" else "训练完成: 今日尚未形成有效训练，建议至少活动 20 分钟"

        metaView.text = "已完成 $completedCount/3 项今日关键任务"

        val primaryButton = findViewById<TextView>(R.id.btn_health_primary_action)
        when {
            !isCheckedInToday -> {
                primaryAction = HealthPrimaryAction.CHECKIN
                statusTitleView.text = "先补签到"
                summaryView.text = "今天的健康闭环还没开始，先完成签到，再继续安排补水和训练。"
                primaryButton.text = "去签到"
            }
            !waterDone -> {
                primaryAction = HealthPrimaryAction.WATER
                statusTitleView.text = "补水优先"
                summaryView.text = "今天的补水进度还没达标，先补 ${(targetWater - currentWaterCount).coerceAtLeast(0)} 杯水，状态会更稳定。"
                primaryButton.text = "去补水"
            }
            !workoutDone -> {
                primaryAction = HealthPrimaryAction.TRAINING
                statusTitleView.text = "继续推进"
                summaryView.text = "签到和补水都在轨道上，接下来建议安排一段训练，把今天的健康任务补完整。"
                primaryButton.text = "去训练"
            }
            todayDietRecords.isEmpty() -> {
                primaryAction = HealthPrimaryAction.DIET
                statusTitleView.text = "记录饮食"
                summaryView.text = "今天的核心目标已经完成，但饮食记录还是空的，补上记录后页面数据会更完整。"
                primaryButton.text = "去记录"
            }
            else -> {
                primaryAction = HealthPrimaryAction.STATS
                statusTitleView.text = "状态优秀"
                summaryView.text = "今天的关键健康任务完成度很高，建议看看统计页，继续追踪长期变化。"
                primaryButton.text = "看统计"
            }
        }
    }

    private fun updateActiveChallenges() {
        val todaySteps = calculateTodaySteps(todayWorkoutRecords)
        val weekWorkoutDays = calculateCurrentWeekWorkoutDays(allWorkoutRecords)
        val targetWeeklyWorkouts = currentProfile.targetWeeklyWorkouts.coerceAtLeast(1)
        val recordedMeals = todayDietRecords
            .map { it.mealType }
            .filter { it == "BREAKFAST" || it == "LUNCH" || it == "DINNER" }
            .distinct()
            .size

        val challengeCards = listOf(
            ChallengeCardUi(
                title = "补水节奏",
                progress = percentageToProgress(currentWaterCount, targetWater),
                progressText = if (targetWater > 0) "$currentWaterCount/$targetWater 杯" else "暂未设置目标"
            ),
            ChallengeCardUi(
                title = "今日步数",
                progress = percentageToProgress(todaySteps, targetSteps),
                progressText = if (targetSteps > 0) "${formatNumber(todaySteps)}/${formatNumber(targetSteps)} 步" else "${formatNumber(todaySteps)} 步"
            ),
            ChallengeCardUi(
                title = "本周训练",
                progress = percentageToProgress(weekWorkoutDays, targetWeeklyWorkouts),
                progressText = "$weekWorkoutDays/$targetWeeklyWorkouts 天达成"
            ),
            ChallengeCardUi(
                title = "三餐记录",
                progress = percentageToProgress(recordedMeals, 3),
                progressText = "$recordedMeals/3 餐已记录"
            )
        )

        bindChallengeCard(R.id.tv_challenge_1_name, R.id.pb_challenge_1, R.id.tv_challenge_1_progress, challengeCards[0])
        bindChallengeCard(R.id.tv_challenge_2_name, R.id.pb_challenge_2, R.id.tv_challenge_2_progress, challengeCards[1])
        bindChallengeCard(R.id.tv_challenge_3_name, R.id.pb_challenge_3, R.id.tv_challenge_3_progress, challengeCards[2])
        bindChallengeCard(R.id.tv_challenge_4_name, R.id.pb_challenge_4, R.id.tv_challenge_4_progress, challengeCards[3])
    }

    private fun updateHealthScoreCard() {
        val todaySteps = calculateTodaySteps(todayWorkoutRecords)
        val workoutMinutes = ((currentWorkoutSummary.totalDurationSeconds + 59) / 60).coerceAtLeast(0)
        val waterRatio = ratio(currentWaterCount, targetWater)
        val stepRatio = ratio(todaySteps, targetSteps)
        val workoutRatio = ratio(workoutMinutes, 60)
        val mealCoverage = todayDietRecords
            .map { it.mealType }
            .filter { it == "BREAKFAST" || it == "LUNCH" || it == "DINNER" }
            .distinct()
            .size
        val mealRatio = ratio(mealCoverage, 3)
        val weeklyWorkoutDays = calculateCurrentWeekWorkoutDays(allWorkoutRecords)
        val weeklyTargetRatio = ratio(weeklyWorkoutDays, currentProfile.targetWeeklyWorkouts.coerceAtLeast(1))
        val checkinScore = if (isCheckedInToday) 15 else 0
        val totalScore = (
            checkinScore +
                waterRatio * 20 +
                stepRatio * 20 +
                maxOf(workoutRatio, weeklyTargetRatio) * 25 +
                mealRatio * 20
            ).toInt().coerceIn(0, 100)

        val scoreLevel = when {
            totalScore >= 85 -> "状态优秀"
            totalScore >= 70 -> "节奏稳定"
            totalScore >= 50 -> "继续补齐"
            else -> "需要推进"
        }

        val summary = when {
            !isCheckedInToday -> "今天还没完成签到，先把健康闭环启动起来，评分会立刻提高。"
            waterRatio < 1f -> "补水仍有提升空间，先把饮水目标补齐，整体状态会更稳定。"
            workoutRatio < 0.5f && weeklyTargetRatio < 1f -> "训练量偏少，建议安排一段 20 到 30 分钟的运动拉高今日表现。"
            mealRatio < 1f -> "饮食记录还不够完整，补齐主餐记录后，健康画像会更准确。"
            else -> "今天的健康执行比较均衡，继续保持这个节奏，首页指标会越来越漂亮。"
        }

        findViewById<TextView>(R.id.tv_health_score_value).text = totalScore.toString()
        findViewById<ImageView>(R.id.iv_health_score_progress)
            .setImageLevel(percentageToImageLevel(totalScore, 100))
        findViewById<TextView>(R.id.tv_health_score_level).text = scoreLevel
        findViewById<TextView>(R.id.tv_health_score_summary).text = summary
        findViewById<TextView>(R.id.tv_health_trend_1).text =
            buildWorkoutTrendText(weeklyWorkoutDays, currentProfile.targetWeeklyWorkouts.coerceAtLeast(1))
        findViewById<TextView>(R.id.tv_health_trend_2).text =
            buildStepsTrendText(todaySteps, targetSteps, workoutMinutes)
        findViewById<TextView>(R.id.tv_health_trend_3).text =
            buildDietTrendText(mealCoverage)
    }

    private fun updateWeekTrendCard() {
        val days = buildWeeklyTrendDays()
        val container = findViewById<LinearLayout>(R.id.ll_health_week_bars)
        container.removeAllViews()

        if (days.isEmpty()) {
            findViewById<TextView>(R.id.tv_health_week_summary).text =
                "最近 7 天还没有形成足够的数据，继续记录训练和饮食后这里会显示趋势。"
            return
        }

        days.forEach { day ->
            container.addView(createTrendBarView(day))
        }

        findViewById<TextView>(R.id.tv_health_week_summary).text = buildWeekTrendSummary(days)
    }

    private fun updateRiskAlertsCard() {
        val alerts = buildRiskAlerts()
        val levelView = findViewById<TextView>(R.id.tv_health_risk_level)
        val summaryView = findViewById<TextView>(R.id.tv_health_risk_summary)
        val itemViews = listOf(
            findViewById<TextView>(R.id.tv_health_risk_item_1),
            findViewById<TextView>(R.id.tv_health_risk_item_2),
            findViewById<TextView>(R.id.tv_health_risk_item_3)
        )
        val actionView = findViewById<TextView>(R.id.btn_health_risk_action)

        if (alerts.isEmpty()) {
            riskAction = HealthPrimaryAction.STATS
            levelView.text = "低风险"
            summaryView.text = "今天的健康状态总体稳定，没有明显风险项，继续保持当前节奏即可。"
            itemViews.forEachIndexed { index, textView ->
                textView.visibility = if (index == 0) View.VISIBLE else View.GONE
                if (index == 0) {
                    textView.text = "1. 训练、补水和饮食记录整体比较均衡，建议继续维持。"
                }
            }
            actionView.text = "查看统计"
            return
        }

        val highestSeverity = alerts.maxOf { it.severity }
        levelView.text = when (highestSeverity) {
            3 -> "高风险"
            2 -> "中风险"
            else -> "关注中"
        }
        summaryView.text = when (highestSeverity) {
            3 -> "今天存在需要优先处理的健康风险，建议先解决最上面的高优先级问题。"
            2 -> "今天有几项状态值得尽快调整，及时处理可以避免后面补救成本变高。"
            else -> "当前有轻度风险提示，趁现在补齐会更容易保持稳定节奏。"
        }

        alerts.take(3).forEachIndexed { index, alert ->
            itemViews[index].visibility = View.VISIBLE
            itemViews[index].text = "${index + 1}. ${alert.message}"
        }
        for (index in alerts.size.coerceAtMost(3) until itemViews.size) {
            itemViews[index].visibility = View.GONE
        }

        val topAlert = alerts.maxWithOrNull(compareBy<RiskAlertUi> { it.severity }.thenBy { it.priority })
            ?: alerts.first()
        riskAction = topAlert.action
        actionView.text = topAlert.actionLabel
    }

    private fun bindChallengeCard(nameId: Int, progressId: Int, progressTextId: Int, card: ChallengeCardUi) {
        findViewById<TextView>(nameId).text = card.title
        findViewById<android.widget.ProgressBar>(progressId).progress = card.progress
        findViewById<TextView>(progressTextId).text = card.progressText
    }

    private fun createTrendBarView(day: WeeklyTrendDay): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }

        val scoreText = TextView(this).apply {
            text = day.score.toString()
            textSize = 11f
            setTextColor(getColor(R.color.text_secondary))
            gravity = Gravity.CENTER
        }

        val track = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(20.dp(), 76.dp()).apply {
                topMargin = 8.dp()
                bottomMargin = 8.dp()
            }
            background = getDrawable(R.drawable.progress_bar_bg)
        }

        val fillHeight = (18 + (58 * (day.score / 100f))).toInt().coerceAtLeast(12.dp())
        val fill = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                fillHeight,
                Gravity.BOTTOM
            )
            background = getDrawable(R.drawable.health_trend_bar_fill)
            alpha = if (day.isToday) 1f else 0.72f
        }
        track.addView(fill)

        val label = TextView(this).apply {
            text = day.label
            textSize = 11f
            setTextColor(getColor(if (day.isToday) R.color.indigo_primary else R.color.text_secondary))
            gravity = Gravity.CENTER
        }

        column.addView(scoreText)
        column.addView(track)
        column.addView(label)
        return column
    }

    private fun buildRiskAlerts(): List<RiskAlertUi> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val alerts = mutableListOf<RiskAlertUi>()
        val waterRatio = ratio(currentWaterCount, targetWater)
        val weeklyWorkoutDays = calculateCurrentWeekWorkoutDays(allWorkoutRecords)
        val workoutMinutes = ((currentWorkoutSummary.totalDurationSeconds + 59) / 60).coerceAtLeast(0)
        val hasDinnerRecord = todayDietRecords.any { it.mealType == "DINNER" }
        val remainingCalories = budgetCalories - currentConsumedCalories + currentWorkoutSummary.totalCalories

        if (!isCheckedInToday && hour >= 18) {
            alerts.add(
                RiskAlertUi(
                    severity = 2,
                    priority = 1,
                    message = "现在还没有完成今日签到，连续记录可能中断，建议尽快补签。",
                    action = HealthPrimaryAction.CHECKIN,
                    actionLabel = "去签到"
                )
            )
        }

        if (hour >= 15 && waterRatio < 0.5f) {
            alerts.add(
                RiskAlertUi(
                    severity = 3,
                    priority = 2,
                    message = "当前补水进度低于一半，下午到晚间可能出现疲劳或影响训练状态。",
                    action = HealthPrimaryAction.WATER,
                    actionLabel = "去补水"
                )
            )
        } else if (hour >= 12 && waterRatio < 0.75f) {
            alerts.add(
                RiskAlertUi(
                    severity = 1,
                    priority = 3,
                    message = "补水进度仍偏慢，建议分次补足，避免晚上集中饮水。",
                    action = HealthPrimaryAction.WATER,
                    actionLabel = "去补水"
                )
            )
        }

        if (hour >= 19 && workoutMinutes < 20 && weeklyWorkoutDays < currentProfile.targetWeeklyWorkouts.coerceAtLeast(1)) {
            alerts.add(
                RiskAlertUi(
                    severity = 3,
                    priority = 4,
                    message = "今天有效训练不足，本周训练目标也还没跟上，建议尽快安排一次训练。",
                    action = HealthPrimaryAction.TRAINING,
                    actionLabel = "去训练"
                )
            )
        } else if (hour >= 17 && workoutMinutes == 0) {
            alerts.add(
                RiskAlertUi(
                    severity = 2,
                    priority = 5,
                    message = "今天还没有训练记录，晚间前补一段 20 分钟运动更容易完成日目标。",
                    action = HealthPrimaryAction.TRAINING,
                    actionLabel = "去训练"
                )
            )
        }

        if (hour >= 20 && !hasDinnerRecord) {
            alerts.add(
                RiskAlertUi(
                    severity = 2,
                    priority = 6,
                    message = "晚餐记录缺失，当前热量评估可能失真，建议补齐晚餐或加餐记录。",
                    action = HealthPrimaryAction.DIET,
                    actionLabel = "去记录"
                )
            )
        }

        if (remainingCalories < -200) {
            alerts.add(
                RiskAlertUi(
                    severity = 3,
                    priority = 7,
                    message = "当前已明显超出热量预算，后续进食需要更克制，并适当增加活动量。",
                    action = HealthPrimaryAction.DIET,
                    actionLabel = "看饮食"
                )
            )
        } else if (hour >= 21 && currentConsumedCalories < (budgetCalories * 0.35f).toInt()) {
            alerts.add(
                RiskAlertUi(
                    severity = 1,
                    priority = 8,
                    message = "当前记录热量偏低，可能是漏记饮食，建议检查是否遗漏主餐或加餐。",
                    action = HealthPrimaryAction.DIET,
                    actionLabel = "去记录"
                )
            )
        }

        return alerts.sortedWith(compareByDescending<RiskAlertUi> { it.severity }.thenBy { it.priority })
    }

    private fun focusWaterAction() {
        val scrollView = findViewById<NestedScrollView>(R.id.nested_scroll_view)
        val waterButton = findViewById<View>(R.id.btn_water)
        waterButton.scrollIntoContainer(scrollView, 24.dp())
        waterButton.playReminderFocusAnimation()
    }

    private fun focusDietAction() {
        val scrollView = findViewById<NestedScrollView>(R.id.nested_scroll_view)
        val dietCard = findViewById<View>(R.id.cv_diet_record)
        dietCard.scrollIntoContainer(scrollView, 24.dp())
        expandSuggestedMealSection()
        Toast.makeText(this, "已定位到饮食记录区域，可直接补充当前时段记录", Toast.LENGTH_SHORT).show()
    }

    private fun expandSuggestedMealSection() {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> expandMealSection(R.id.ll_breakfast_items, R.id.iv_breakfast_expand)
            in 11..15 -> expandMealSection(R.id.ll_lunch_items, R.id.iv_lunch_expand)
            in 16..20 -> expandMealSection(R.id.ll_dinner_items, R.id.iv_dinner_expand)
            else -> expandMealSection(R.id.ll_snack_items, R.id.iv_snack_expand)
        }
    }

    private fun expandMealSection(itemsId: Int, expandIconId: Int) {
        val items = findViewById<LinearLayout>(itemsId)
        if (items.visibility == View.GONE) {
            items.visibility = View.VISIBLE
            findViewById<ImageView>(expandIconId).rotation = 180f
        }
    }

    private fun calculateCurrentWeekWorkoutDays(records: List<WorkoutRecord>): Int {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val weekStart = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val weekEnd = calendar.timeInMillis

        return records
            .filter { it.timestamp in weekStart until weekEnd }
            .map { it.date }
            .distinct()
            .size
    }

    private fun buildWorkoutTrendText(weeklyWorkoutDays: Int, weeklyTarget: Int): String {
        val safeTarget = weeklyTarget.coerceAtLeast(1)
        return when {
            weeklyWorkoutDays >= safeTarget -> "本周训练趋势良好，已完成 $weeklyWorkoutDays/$safeTarget 天目标。"
            weeklyWorkoutDays > 0 -> "本周已训练 $weeklyWorkoutDays/$safeTarget 天，再安排一次训练会更接近周目标。"
            else -> "本周还没有形成训练记录，建议尽快开始第一次训练。"
        }
    }

    private fun buildStepsTrendText(todaySteps: Int, stepsTarget: Int, workoutMinutes: Int): String {
        val safeTarget = stepsTarget.coerceAtLeast(1)
        val percent = (ratio(todaySteps, safeTarget) * 100).toInt().coerceIn(0, 100)
        return when {
            percent >= 100 -> "今日步数已达标，活动量很不错，可以继续维持轻量运动。"
            percent >= 70 -> "今日步数完成 $percent%，再活动 ${maxOf(10, 60 - workoutMinutes)} 分钟会更接近目标。"
            else -> "今日步数完成 $percent%，建议增加步行或安排一次训练提升活跃度。"
        }
    }

    private fun buildDietTrendText(mealCoverage: Int): String {
        val sevenDayDietDays = calculateRecentDietLoggedDays(allDietRecords)
        return when {
            mealCoverage >= 3 -> "今天三餐记录较完整，近 7 天已有 $sevenDayDietDays 天保持饮食记录。"
            mealCoverage > 0 -> "今天已记录 $mealCoverage/3 餐，近 7 天有 $sevenDayDietDays 天保留饮食数据。"
            else -> "今天还没有主餐记录，近 7 天有 $sevenDayDietDays 天记录过饮食，建议尽快补充。"
        }
    }

    private fun buildWeeklyTrendDays(): List<WeeklyTrendDay> {
        val formatter = SimpleDateFormat("MM/dd", Locale.getDefault())
        val today = DietRecordViewModel.todayDate()
        val result = mutableListOf<WeeklyTrendDay>()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }

        repeat(7) {
            val dateMillis = calendar.timeInMillis
            val dateText = dateToText(dateMillis)
            val workoutRecords = allWorkoutRecords.filter { it.date == dateText }
            val dietRecords = allDietRecords.filter { it.date == dateText }
            val score = calculateDailyTrendScore(
                dateText = dateText,
                dateMillis = dateMillis,
                workoutRecords = workoutRecords,
                dietRecords = dietRecords,
                isToday = dateText == today
            )
            result.add(
                WeeklyTrendDay(
                    label = formatter.format(Date(dateMillis)),
                    dateText = dateText,
                    score = score,
                    isToday = dateText == today
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    private fun calculateDailyTrendScore(
        dateText: String,
        dateMillis: Long,
        workoutRecords: List<WorkoutRecord>,
        dietRecords: List<DietRecord>,
        isToday: Boolean
    ): Int {
        val steps = if (isToday) {
            calculateTodaySteps(todayWorkoutRecords)
        } else {
            calculateStepsFromWorkoutRecords(workoutRecords)
        }
        val workoutMinutes = ((workoutRecords.sumOf { it.elapsedSeconds } + 59) / 60).coerceAtLeast(0)
        val mealCoverage = dietRecords
            .map { it.mealType }
            .filter { it == "BREAKFAST" || it == "LUNCH" || it == "DINNER" }
            .distinct()
            .size

        val workoutRatio = ratio(workoutMinutes, 60)
        val stepsRatio = ratio(steps, targetSteps)
        val mealRatio = ratio(mealCoverage, 3)
        val waterBonus = if (isToday) ratio(currentWaterCount, targetWater) * 15 else 0f
        val checkinBonus = if (isToday && isCheckedInToday) 10f else 0f
        val consistencyBonus = if (workoutRecords.isNotEmpty() || dietRecords.isNotEmpty()) 10f else 0f

        return (
            workoutRatio * 35 +
                stepsRatio * 25 +
                mealRatio * 20 +
                waterBonus +
                checkinBonus +
                consistencyBonus
            ).toInt().coerceIn(0, 100)
    }

    private fun calculateStepsFromWorkoutRecords(records: List<WorkoutRecord>): Int {
        val explicitSteps = records.sumOf { it.runSteps }
        if (explicitSteps > 0) return explicitSteps
        val runningDistance = records
            .filter { it.sportType == "RUN" }
            .sumOf { it.totalDistance }
        return if (runningDistance > 0.0) (runningDistance * 1300).toInt() else 0
    }

    private fun buildWeekTrendSummary(days: List<WeeklyTrendDay>): String {
        val firstHalf = days.take(3).map { it.score }.average()
        val secondHalf = days.takeLast(3).map { it.score }.average()
        val bestDay = days.maxByOrNull { it.score }
        val delta = (secondHalf - firstHalf).toInt()

        return when {
            delta >= 8 && bestDay != null -> "相比周初，最近几天的健康执行明显回升，当前最佳表现出现在 ${bestDay.label}。"
            delta <= -8 && bestDay != null -> "最近几天的状态比周初略有下降，建议参考 ${bestDay.label} 的节奏，把补水和训练重新拉起来。"
            bestDay != null -> "过去一周整体节奏较稳定，表现最好的是 ${bestDay.label}，可以继续保持这个执行强度。"
            else -> "过去一周的健康节奏会随着记录累积逐步清晰。"
        }
    }

    private fun dateToText(timeMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }

    private fun remainingDietCalories(): Int =
        budgetCalories - currentConsumedCalories + currentWorkoutSummary.totalCalories

    private fun summarizeTodayMacros(records: List<DietRecord>): MacroSummary {
        return summarizeMacros(records)
    }

    private fun summarizeMacros(records: List<DietRecord>): MacroSummary {
        return records.mapNotNull { recordToPortion(it) }
            .let { HealthyDietPlanner.summarizeMacros(it) }
    }

    private fun buildRecentMacroSeries(): List<DailyMacroSnapshot> {
        val grouped = allDietRecords.groupBy { it.date }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return buildList {
            repeat(7) { offset ->
                if (offset > 0) calendar.add(Calendar.DAY_OF_YEAR, -1)
                val date = dateToText(calendar.timeInMillis)
                val records = grouped[date].orEmpty()
                add(DailyMacroSnapshot(date = date, macro = summarizeMacros(records)))
            }
        }.reversed()
    }

    private fun recordToPortion(record: DietRecord): PlannedFoodPortion? {
        val food = BasicFoodCatalog.findByName(record.foodName) ?: return null
        val grams = extractGrams(record.notes)
            ?: inferGramsFromCalories(record.calories, food)
        if (grams <= 0) return null
        return PlannedFoodPortion(food = food, grams = grams)
    }

    private fun extractGrams(notes: String): Int? {
        val match = Regex("(\\d+)g").find(notes) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()
    }

    private fun inferGramsFromCalories(calories: Int, food: FoodCatalogItem): Int {
        if (food.caloriesPer100g <= 0) return 0
        return ((calories.toDouble() / food.caloriesPer100g) * 100).toInt()
    }

    private fun macroProgress(current: Double, target: Double): Int {
        if (target <= 0.0) return 0
        return ((current / target) * 100).toInt().coerceIn(0, 150)
    }

    private fun macroBalanceScore(current: MacroSummary, target: MacroSummary): Int {
        val protein = (current.protein / target.protein.coerceAtLeast(1.0)).coerceIn(0.0, 1.2)
        val carbs = (current.carbs / target.carbs.coerceAtLeast(1.0)).coerceIn(0.0, 1.2)
        val fat = (current.fat / target.fat.coerceAtLeast(1.0)).coerceIn(0.0, 1.2)
        return (((protein + carbs + fat) / 3.0) * 100).toInt().coerceIn(0, 120)
    }

    private fun calculateRecentDietLoggedDays(records: List<DietRecord>): Int {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = calendar.timeInMillis + 24L * 60 * 60 * 1000
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val start = calendar.timeInMillis
        return records
            .filter { it.timestamp in start until end }
            .map { it.date }
            .distinct()
            .size
    }

    private fun ratio(value: Int, target: Int): Float {
        if (target <= 0) return 0f
        return (value.coerceAtLeast(0).toFloat() / target.toFloat()).coerceIn(0f, 1f)
    }

    private fun percentageToImageLevel(value: Int, target: Int): Int {
        if (target <= 0) return 0
        val ratio = (value.coerceAtLeast(0).toFloat() / target.toFloat()).coerceIn(0f, 1f)
        return (ratio * 10000).toInt()
    }

    private fun percentageToProgress(value: Int, target: Int): Int {
        if (target <= 0) return 0
        return ((value.coerceAtLeast(0).toFloat() / target.toFloat()) * 100)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun workoutCalorieTarget(): Int = (budgetCalories / 4).coerceAtLeast(300)

    private fun formatNumber(value: Int): String = String.format(Locale.getDefault(), "%,d", value)

    private fun formatMacro(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", value)
        }
    }

    private fun DietRecord.displayText(): String {
        val noteSuffix = if (notes.isBlank()) "" else " · $notes"
        return "${foodName} - ${calories} kcal$noteSuffix"
    }

    private fun String.toMealType(): MealType = MealType.valueOf(this)

    private fun String.toMealTypeOrNull(): MealType? = MealType.entries.firstOrNull { it.name == this }

    private fun mealLabel(mealType: MealType): String = mealType.label

    private fun loadDietGoal(): DietGoalType {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("diet_goal_type", DietGoalType.MAINTAIN.name)
        return DietGoalType.entries.firstOrNull { it.name == saved } ?: DietGoalType.MAINTAIN
    }

    private fun saveDietGoal(goalType: DietGoalType) {
        val prefs = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("diet_goal_type", goalType.name).apply()
    }

    private fun CustomFood.toFoodCatalogItem(): FoodCatalogItem {
        return FoodCatalogItem(
            code = "custom_$id",
            name = name,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            mealTypes = setOf(mealType.toMealType())
        )
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private data class ChallengeCardUi(
        val title: String,
        val progress: Int,
        val progressText: String
    )

    private data class WeeklyTrendDay(
        val label: String,
        val dateText: String,
        val score: Int,
        val isToday: Boolean
    )

    private data class RiskAlertUi(
        val severity: Int,
        val priority: Int,
        val message: String,
        val action: HealthPrimaryAction,
        val actionLabel: String
    )

    private data class DailyMacroSnapshot(
        val date: String,
        val macro: MacroSummary
    )

    private enum class HealthPrimaryAction {
        CHECKIN,
        WATER,
        TRAINING,
        DIET,
        STATS
    }
}
