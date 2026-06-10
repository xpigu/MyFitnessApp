package com.example.myfitnessapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.myfitnessapp.data.entity.UserProfile
import com.example.myfitnessapp.data.viewmodel.UserProfileViewModel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PersonalInfoActivity : AppCompatActivity() {

    private lateinit var viewModel: UserProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info)

        viewModel = ViewModelProvider(this)[UserProfileViewModel::class.java]

        setupActions()
        observeProfile()
    }

    private fun setupActions() {
        findViewById<View>(R.id.btn_personal_info_back).setOnClickListener {
            finish()
        }
        findViewById<View>(R.id.btn_personal_info_edit).setOnClickListener { openEditProfile() }
        findViewById<View>(R.id.btn_personal_info_complete_action).setOnClickListener { openEditProfile() }
        findViewById<View>(R.id.btn_personal_info_quick_edit).setOnClickListener { openEditProfile() }
        findViewById<View>(R.id.btn_personal_info_quick_goal).setOnClickListener {
            startActivity(Intent(this, GoalSettingActivity::class.java))
        }
        findViewById<View>(R.id.btn_personal_info_quick_stats).setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        findViewById<View>(R.id.btn_personal_info_quick_reminder).setOnClickListener {
            startActivity(Intent(this, ReminderSettingsActivity::class.java))
        }
    }

    private fun observeProfile() {
        viewModel.userProfile.observe(this) { profile ->
            bindProfile(profile)
        }
    }

    private fun bindProfile(profile: UserProfile) {
        val privacySettings = SettingsPrefs.getPrivacySettings(this)
        val completion = calculateProfileCompletion(profile)
        val insight = buildProfileInsight(profile)
        val bmiStatus = bmiStatus(profile)

        findViewById<TextView>(R.id.tv_personal_info_username).text = profile.username
        findViewById<TextView>(R.id.tv_personal_info_bio).text =
            profile.bio.ifBlank { getString(R.string.personal_info_bio_placeholder) }
        findViewById<TextView>(R.id.tv_personal_info_chip_level).text =
            getString(R.string.personal_info_chip_level_format, profile.level, levelTitle(profile.level))
        findViewById<TextView>(R.id.tv_personal_info_chip_bmi).text =
            if (calculateBmi(profile) == null) {
                getString(R.string.personal_info_chip_bmi_missing)
            } else {
                bmiStatus
            }
        findViewById<TextView>(R.id.tv_personal_info_chip_completion).text =
            getString(R.string.personal_info_completion_chip_format, completion.score)
        findViewById<TextView>(R.id.tv_personal_info_completion_percent).text =
            getString(R.string.personal_info_completion_percent_format, completion.score)
        findViewById<ProgressBar>(R.id.progress_personal_info_completion).progress = completion.score
        findViewById<TextView>(R.id.tv_personal_info_completion_summary).text =
            completion.summary
        findViewById<TextView>(R.id.btn_personal_info_complete_action).text =
            getString(if (completion.score >= 100) R.string.personal_info_complete_maintain else R.string.personal_info_complete_action)
        findViewById<View>(R.id.card_personal_info_body).visibility =
            if (privacySettings.showBodyMetrics) View.VISIBLE else View.GONE
        findViewById<View>(R.id.card_personal_info_insight).visibility =
            if (privacySettings.enablePersonalizedInsights) View.VISIBLE else View.GONE
        if (privacySettings.enablePersonalizedInsights) {
            findViewById<TextView>(R.id.tv_personal_info_insight_title).text = insight.title
            findViewById<TextView>(R.id.tv_personal_info_insight_summary).text = insight.summary
            findViewById<TextView>(R.id.tv_personal_info_insight_action).text = insight.action
        }
        findViewById<TextView>(R.id.tv_personal_info_gender).text = displayGender(profile.gender)
        findViewById<TextView>(R.id.tv_personal_info_birthday).text = profile.birthday.ifBlank {
            getString(R.string.personal_info_not_filled)
        }
        findViewById<TextView>(R.id.tv_personal_info_age).text = profileAge(profile)
        findViewById<TextView>(R.id.tv_personal_info_height).text = formatHeight(profile.heightCm)
        findViewById<TextView>(R.id.tv_personal_info_weight).text = formatWeight(profile.weightKg)
        findViewById<TextView>(R.id.tv_personal_info_bmi).text = formatBmi(profile)
        findViewById<TextView>(R.id.tv_personal_info_bmi_status).text = bmiStatus
        findViewById<TextView>(R.id.tv_personal_info_level).text =
            getString(R.string.profile_level_format, profile.level)
        findViewById<TextView>(R.id.tv_personal_info_workouts).text = profile.totalWorkouts.toString()
        findViewById<TextView>(R.id.tv_personal_info_active_days).text = profile.activeDays.toString()
        findViewById<TextView>(R.id.tv_personal_info_daily_calories).text =
            getString(R.string.personal_info_daily_calories_format, profile.targetDailyCalories)
        findViewById<TextView>(R.id.tv_personal_info_daily_water).text =
            getString(R.string.personal_info_daily_water_format, profile.targetDailyWater)
        findViewById<TextView>(R.id.tv_personal_info_daily_steps).text =
            getString(R.string.personal_info_daily_steps_format, profile.targetDailySteps)
        findViewById<TextView>(R.id.tv_personal_info_weekly_workouts).text =
            getString(R.string.personal_info_weekly_workouts_format, profile.targetWeeklyWorkouts)
        findViewById<TextView>(R.id.tv_personal_info_last_updated).text =
            getString(R.string.personal_info_last_updated_format, formatTimestamp(profile.lastUpdated))

        val avatarView = findViewById<ImageView>(R.id.iv_personal_info_avatar)
        if (privacySettings.allowLocalAvatarAccess && profile.avatarUri.isNotBlank()) {
            Glide.with(this)
                .load(Uri.parse(profile.avatarUri))
                .transform(CircleCrop())
                .placeholder(R.drawable.avatar_placeholder)
                .into(avatarView)
        } else {
            avatarView.setImageResource(R.drawable.avatar_placeholder)
        }
    }

    private fun openEditProfile() {
        startActivity(Intent(this, EditProfileActivity::class.java))
    }

    private fun displayGender(gender: String): String = when (gender) {
        "MALE" -> getString(R.string.profile_gender_male)
        "FEMALE" -> getString(R.string.profile_gender_female)
        "OTHER" -> getString(R.string.profile_gender_other)
        else -> getString(R.string.personal_info_not_filled)
    }

    private fun levelTitle(level: Int): String = when (level) {
        in 1..2 -> "新手"
        in 3..5 -> "进阶"
        in 6..9 -> "自律"
        in 10..14 -> "挑战"
        in 15..20 -> "高能"
        else -> "掌控"
    }

    private fun profileAge(profile: UserProfile): String {
        val birthday = parseBirthday(profile.birthday) ?: return getString(R.string.personal_info_not_filled)
        val today = Calendar.getInstance()
        val birth = Calendar.getInstance().apply { time = birthday }
        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (
            today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)
        ) {
            age -= 1
        }
        return if (age in 1..120) {
            getString(R.string.personal_info_age_format, age)
        } else {
            getString(R.string.personal_info_not_filled)
        }
    }

    private fun formatHeight(heightCm: Int): String {
        return if (heightCm > 0) {
            getString(R.string.personal_info_height_format, heightCm)
        } else {
            getString(R.string.personal_info_not_filled)
        }
    }

    private fun formatWeight(weightKg: Double): String {
        return if (weightKg > 0) {
            getString(R.string.personal_info_weight_format, formatDecimal(weightKg))
        } else {
            getString(R.string.personal_info_not_filled)
        }
    }

    private fun formatBmi(profile: UserProfile): String {
        val bmi = calculateBmi(profile) ?: return getString(R.string.personal_info_not_filled)
        return String.format(Locale.getDefault(), "%.1f", bmi)
    }

    private fun bmiStatus(profile: UserProfile): String {
        val bmi = calculateBmi(profile) ?: return getString(R.string.personal_info_bmi_hint)
        return when {
            bmi < 18.5 -> getString(R.string.personal_info_bmi_underweight)
            bmi < 24.0 -> getString(R.string.personal_info_bmi_normal)
            bmi < 28.0 -> getString(R.string.personal_info_bmi_overweight)
            else -> getString(R.string.personal_info_bmi_obese)
        }
    }

    private fun calculateBmi(profile: UserProfile): Double? {
        if (profile.heightCm <= 0 || profile.weightKg <= 0) return null
        val heightMeter = profile.heightCm / 100.0
        if (heightMeter <= 0.0) return null
        return profile.weightKg / (heightMeter * heightMeter)
    }

    private fun calculateProfileCompletion(profile: UserProfile): CompletionUi {
        val items = listOf(
            profile.username.isNotBlank() to getString(R.string.personal_info_missing_username),
            profile.bio.isNotBlank() to getString(R.string.personal_info_missing_bio),
            profile.gender.isNotBlank() to getString(R.string.personal_info_missing_gender),
            profile.birthday.isNotBlank() to getString(R.string.personal_info_missing_birthday),
            (profile.heightCm > 0) to getString(R.string.personal_info_missing_height),
            (profile.weightKg > 0) to getString(R.string.personal_info_missing_weight),
            profile.avatarUri.isNotBlank() to getString(R.string.personal_info_missing_avatar)
        )
        val completed = items.count { it.first }
        val score = ((completed * 100f) / items.size).toInt().coerceIn(0, 100)
        val missing = items.filterNot { it.first }.map { it.second }
        val summary = if (missing.isEmpty()) {
            getString(R.string.personal_info_completion_summary_complete)
        } else {
            val focusFields = missing.take(3).joinToString("、")
            getString(R.string.personal_info_completion_summary_missing, focusFields)
        }
        return CompletionUi(score = score, summary = summary)
    }

    private fun buildProfileInsight(profile: UserProfile): ProfileInsightUi {
        val bmi = calculateBmi(profile)
        if (bmi == null) {
            return ProfileInsightUi(
                title = getString(R.string.personal_info_insight_missing_title),
                summary = getString(R.string.personal_info_insight_missing_summary),
                action = getString(R.string.personal_info_insight_missing_action)
            )
        }
        return when {
            bmi < 18.5 -> ProfileInsightUi(
                title = getString(R.string.personal_info_insight_underweight_title),
                summary = getString(R.string.personal_info_insight_underweight_summary),
                action = getString(R.string.personal_info_insight_underweight_action)
            )
            bmi < 24.0 -> ProfileInsightUi(
                title = getString(R.string.personal_info_insight_balanced_title),
                summary = getString(R.string.personal_info_insight_balanced_summary),
                action = getString(R.string.personal_info_insight_balanced_action)
            )
            bmi < 28.0 -> ProfileInsightUi(
                title = getString(R.string.personal_info_insight_overweight_title),
                summary = getString(R.string.personal_info_insight_overweight_summary),
                action = getString(R.string.personal_info_insight_overweight_action)
            )
            else -> ProfileInsightUi(
                title = getString(R.string.personal_info_insight_obese_title),
                summary = getString(R.string.personal_info_insight_obese_summary),
                action = getString(R.string.personal_info_insight_obese_action)
            )
        }
    }

    private fun parseBirthday(value: String): Date? {
        if (value.isBlank()) return null
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            isLenient = false
        }
        return try {
            formatter.parse(value)
        } catch (_: ParseException) {
            null
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", value)
        }
    }

    private data class CompletionUi(
        val score: Int,
        val summary: String
    )

    private data class ProfileInsightUi(
        val title: String,
        val summary: String,
        val action: String
    )
}
