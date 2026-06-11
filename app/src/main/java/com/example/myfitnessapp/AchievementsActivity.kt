package com.example.myfitnessapp

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myfitnessapp.data.entity.AchievementBadge
import com.example.myfitnessapp.data.entity.DailyCheckin
import com.example.myfitnessapp.data.entity.UserProfile
import com.example.myfitnessapp.data.entity.WorkoutRecord
import com.example.myfitnessapp.data.viewmodel.AchievementBadgeViewModel
import com.example.myfitnessapp.data.viewmodel.DailyCheckinViewModel
import com.example.myfitnessapp.data.viewmodel.UserProfileViewModel
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AchievementsActivity : AppCompatActivity() {

    private lateinit var viewModel: AchievementBadgeViewModel
    private lateinit var profileViewModel: UserProfileViewModel
    private lateinit var workoutViewModel: WorkoutRecordViewModel
    private lateinit var checkinViewModel: DailyCheckinViewModel
    private lateinit var badgeAdapter: BadgeAdapter

    private var selectedSortOption = AchievementSortOption.TIME
    private var showUnlockedBadges = false
    private var currentProfile = UserProfile()
    private var currentWorkouts: List<WorkoutRecord> = emptyList()
    private var currentCheckins: List<DailyCheckin> = emptyList()
    private var currentBadges: List<AchievementBadge> = emptyList()

    private enum class AchievementSortOption(val label: String) {
        TIME("按时间"),
        LEVEL("按等级")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        viewModel = ViewModelProvider(this).get(AchievementBadgeViewModel::class.java)
        profileViewModel = ViewModelProvider(this).get(UserProfileViewModel::class.java)
        workoutViewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)
        checkinViewModel = ViewModelProvider(this).get(DailyCheckinViewModel::class.java)

        setupViews()
        observeData()
    }

    private fun setupViews() {
        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_badges)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2列网格布局
        badgeAdapter = BadgeAdapter(
            onBadgeClick = { badge -> showBadgeDetailDialog(badge) },
            progressProvider = { badge -> badgeProgress(badge) }
        )
        recyclerView.adapter = badgeAdapter

        val sortSpinner = findViewById<Spinner>(R.id.spinner_achievement_sort)
        val sortAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            AchievementSortOption.entries.map { it.label }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        sortSpinner.adapter = sortAdapter
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSortOption = AchievementSortOption.entries[position]
                applyBadgeFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        findViewById<SwitchCompat>(R.id.switch_badge_unlock_view).setOnCheckedChangeListener { _, isChecked ->
            showUnlockedBadges = isChecked
            updateViewModeSummary()
            applyBadgeFilter()
        }
        updateViewModeSummary()
    }

    private fun observeData() {
        viewModel.allBadges.observe(this) { badges ->
            currentBadges = badges
            updateViewModeSummary()
            applyBadgeFilter()
            findViewById<TextView>(R.id.tv_total_count).text = "/ ${badges.size}"
            handleUnlockedBadgeFeedback(badges)
        }

        viewModel.unlockedCount.observe(this) { count ->
            findViewById<TextView>(R.id.tv_unlocked_count).text = count.toString()
            updateAchievementHint(count)
        }

        profileViewModel.userProfile.observe(this) { profile ->
            currentProfile = profile
            updateOverview()
            updateAchievementHint(findViewById<TextView>(R.id.tv_unlocked_count).text.toString().toIntOrNull() ?: 0)
            applyBadgeFilter()
        }

        workoutViewModel.allRecords.observe(this) { records ->
            currentWorkouts = records
            updateOverview()
            updateAchievementHint(findViewById<TextView>(R.id.tv_unlocked_count).text.toString().toIntOrNull() ?: 0)
            applyBadgeFilter()
        }

        checkinViewModel.allCheckins.observe(this) { checkins ->
            currentCheckins = checkins
            updateOverview()
            updateAchievementHint(findViewById<TextView>(R.id.tv_unlocked_count).text.toString().toIntOrNull() ?: 0)
            applyBadgeFilter()
        }
    }

    private fun updateOverview() {
        findViewById<TextView>(R.id.tv_achievement_level).text =
            getString(R.string.profile_level_format, currentProfile.level)
        findViewById<TextView>(R.id.tv_achievement_workouts).text = currentWorkouts.size.toString()
        findViewById<TextView>(R.id.tv_achievement_checkin_streak).text = calculateCurrentCheckinStreak().toString()
    }

    private fun updateAchievementHint(unlockedCount: Int) {
        val hintView = findViewById<TextView>(R.id.tv_achievement_hint)
        hintView.text = when {
            unlockedCount >= 8 -> "你的成长节奏很稳定，继续冲击更高等级徽章"
            currentProfile.level < 5 -> "提升等级到 Lv.5，可以解锁成长进阶徽章"
            currentWorkouts.size < 20 -> "再完成 ${20 - currentWorkouts.size} 次训练，可解锁训练积累者"
            else -> "继续训练和签到，会自动解锁更多徽章"
        }
    }

    private fun calculateCurrentCheckinStreak(): Int {
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

    private fun currentDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun offsetDateString(offsetDays: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun handleUnlockedBadgeFeedback(badges: List<AchievementBadge>) {
        val username = CurrentAccount.requireUsername(this)
        val currentUnlockedIds = badges.filter { it.isUnlocked }.map { it.id }.toSet()
        val newUnlockedIds = SettingsPrefs.consumeNewUnlockedBadgeIds(this, username, currentUnlockedIds)
        if (newUnlockedIds.isEmpty()) return

        val newUnlockedBadges = badges.filter { it.isUnlocked && it.id in newUnlockedIds }
        newUnlockedBadges.forEach { badge ->
            showAppFeedback("已解锁徽章：${badge.name}", FeedbackType.SUCCESS)
        }
    }

    private fun showBadgeDetailDialog(badge: AchievementBadge) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_badge_detail, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        dialogView.findViewById<ImageView>(R.id.iv_badge_detail_icon).setImageResource(badge.iconResId)
        dialogView.findViewById<TextView>(R.id.tv_badge_detail_name).text = badge.name
        dialogView.findViewById<TextView>(R.id.tv_badge_detail_status).apply {
            text = if (badge.isUnlocked) "已解锁" else "未解锁"
            setBackgroundResource(
                if (badge.isUnlocked) R.drawable.category_chip_selected else R.drawable.category_chip_unselected
            )
            setTextColor(
                getColor(if (badge.isUnlocked) R.color.white else R.color.text_secondary)
            )
        }
        dialogView.findViewById<TextView>(R.id.tv_badge_detail_category).text = "分类：${badgeCategoryLabel(badge.category)}"
        dialogView.findViewById<TextView>(R.id.tv_badge_detail_desc).text = badge.description
        dialogView.findViewById<TextView>(R.id.tv_badge_detail_meta).text =
            if (badge.isUnlocked) {
                val unlockDateText = badge.unlockedDate?.let { formatDate(it) } ?: "未知时间"
                "解锁时间：$unlockDateText\n继续保持当前节奏，你会更快点亮后续进阶徽章。"
            } else {
                val progress = badgeProgress(badge)
                val progressLine = progress?.let { "\n当前进度：${it.label} ${it.current.coerceAtMost(it.target)}/${it.target}" } ?: ""
                "解锁方式：${badge.description}$progressLine\n当前还未达成，继续通过训练、签到和升级来推进进度。"
            }

        dialogView.findViewById<View>(R.id.btn_badge_detail_close).setOnClickListener {
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

    private fun badgeCategoryLabel(category: String): String = when (category) {
        "WORKOUT" -> "训练"
        "STREAK" -> "连续训练"
        "CHECKIN" -> "签到"
        "LEVEL" -> "等级成长"
        "ACTIVE" -> "活跃表现"
        "DIET" -> "饮食"
        else -> "综合"
    }

    private fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    private fun updateViewModeSummary() {
        val visibleCount = currentBadges.count { it.isUnlocked == showUnlockedBadges }
        val summaryView = findViewById<TextView>(R.id.tv_badge_view_mode)
        val switchView = findViewById<SwitchCompat>(R.id.switch_badge_unlock_view)
        val emptyHintView = findViewById<TextView>(R.id.tv_badge_filter_hint)
        val currentModeLabel = if (showUnlockedBadges) "已解锁" else "未解锁"
        summaryView.text = "当前查看：${currentModeLabel} ${visibleCount} 项"
        switchView.text = if (showUnlockedBadges) "切换到未解锁" else "切换到已解锁"
        emptyHintView.text =
            if (showUnlockedBadges) "可按解锁时间查看已点亮的成长轨迹"
            else "优先查看还未完成的徽章，继续推进训练目标"
    }

    private fun applyBadgeFilter() {
        val filteredBadges = currentBadges
            .filter { badge -> badge.isUnlocked == showUnlockedBadges }

        val sortedBadges = filteredBadges.sortedWith(currentSortComparator())

        badgeAdapter.submitList(sortedBadges)
        updateEmptyState(sortedBadges.isEmpty())
    }

    private fun currentSortComparator(): Comparator<AchievementBadge> {
        return when (selectedSortOption) {
            AchievementSortOption.TIME -> {
                if (showUnlockedBadges) {
                    compareByDescending<AchievementBadge> { it.unlockedDate ?: 0L }
                        .thenBy { it.name }
                } else {
                    compareByDescending<AchievementBadge> { badgeProgressRatio(it) }
                        .thenBy { badgeDifficultyRank(it) }
                        .thenBy { it.name }
                }
            }
            AchievementSortOption.LEVEL -> {
                compareBy<AchievementBadge> { badgeDifficultyRank(it) }
                    .thenByDescending { badgeProgressRatio(it) }
                    .thenBy { it.name }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        findViewById<TextView>(R.id.tv_badge_empty_state).visibility = if (isEmpty) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(R.id.rv_badges).visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun badgeDifficultyRank(badge: AchievementBadge): Int {
        return when (badge.id) {
            "first_workout", "early_bird" -> 1
            "workout_3_days", "checkin_streak_3", "level_5" -> 3
            "workout_7_days", "checkin_streak_7", "active_7_days" -> 7
            "level_10" -> 10
            "workout_20_sessions" -> 20
            "active_30_days" -> 30
            "calories_1000" -> 1000
            else -> badgeProgress(badge)?.target ?: Int.MAX_VALUE
        }
    }

    private fun badgeProgress(badge: AchievementBadge): BadgeProgressUi? {
        val workoutCount = currentWorkouts.size
        val workoutStreak = calculateLongestWorkoutStreak()
        val checkinStreak = calculateCurrentCheckinStreak()
        val activeDays = buildSet {
            currentWorkouts.forEach { add(it.date) }
            currentCheckins.forEach { add(it.date) }
        }.size
        val maxCalories = currentWorkouts.maxOfOrNull { it.totalCalories } ?: 0
        val earlyWorkoutCount = currentWorkouts.count { hourOf(it.timestamp) < 6 }

        return when (badge.id) {
            "first_workout" -> BadgeProgressUi(workoutCount, 1, "训练")
            "workout_3_days" -> BadgeProgressUi(workoutStreak, 3, "连练")
            "workout_7_days" -> BadgeProgressUi(workoutStreak, 7, "连练")
            "calories_1000" -> BadgeProgressUi(maxCalories, 1000, "消耗")
            "early_bird" -> BadgeProgressUi(earlyWorkoutCount, 1, "晨练")
            "active_7_days" -> BadgeProgressUi(activeDays, 7, "活跃")
            "active_30_days" -> BadgeProgressUi(activeDays, 30, "活跃")
            "checkin_streak_3" -> BadgeProgressUi(checkinStreak, 3, "签到")
            "checkin_streak_7" -> BadgeProgressUi(checkinStreak, 7, "签到")
            "level_5" -> BadgeProgressUi(currentProfile.level, 5, "等级")
            "level_10" -> BadgeProgressUi(currentProfile.level, 10, "等级")
            "workout_20_sessions" -> BadgeProgressUi(workoutCount, 20, "训练")
            else -> null
        }
    }

    private fun badgeProgressRatio(badge: AchievementBadge): Float {
        if (badge.isUnlocked) return 1f
        val progress = badgeProgress(badge) ?: return 0f
        if (progress.target <= 0) return 0f
        return progress.current
            .coerceIn(0, progress.target)
            .toFloat() / progress.target.toFloat()
    }

    private fun calculateLongestWorkoutStreak(): Int {
        val dates = currentWorkouts.map { it.date }.distinct().sorted()
        if (dates.isEmpty()) return 0

        var longest = 1
        var current = 1
        for (index in 1 until dates.size) {
            if (isNextDay(dates[index - 1], dates[index])) {
                current += 1
                longest = maxOf(longest, current)
            } else {
                current = 1
            }
        }
        return longest
    }

    private fun isNextDay(previous: String, current: String): Boolean {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val previousDate = formatter.parse(previous) ?: return false
        val currentDate = formatter.parse(current) ?: return false
        val calendar = Calendar.getInstance().apply {
            time = previousDate
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return formatter.format(calendar.time) == formatter.format(currentDate)
    }

    private fun hourOf(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
}
