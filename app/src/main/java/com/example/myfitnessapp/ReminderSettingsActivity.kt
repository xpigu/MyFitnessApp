package com.example.myfitnessapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale

class ReminderSettingsActivity : AppCompatActivity() {

    private lateinit var switchWorkout: SwitchCompat
    private lateinit var switchWater: SwitchCompat
    private lateinit var switchCheckin: SwitchCompat
    private lateinit var containerWorkoutTimes: LinearLayout
    private lateinit var containerWaterTimes: LinearLayout
    private lateinit var containerCheckinTimes: LinearLayout
    private lateinit var chipWorkoutDaily: TextView
    private lateinit var chipWorkoutWorkday: TextView
    private lateinit var chipWaterDaily: TextView
    private lateinit var chipWaterWorkday: TextView
    private lateinit var chipCheckinDaily: TextView
    private lateinit var chipCheckinWorkday: TextView

    private val workoutTimes = mutableListOf<ReminderTime>()
    private val waterTimes = mutableListOf<ReminderTime>()
    private val checkinTimes = mutableListOf<ReminderTime>()
    private var workoutRepeatMode = ReminderRepeatMode.DAILY
    private var waterRepeatMode = ReminderRepeatMode.DAILY
    private var checkinRepeatMode = ReminderRepeatMode.DAILY

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ReminderScheduler.rescheduleAll(this)
            showAppFeedback(getString(R.string.reminder_permission_granted), FeedbackType.SUCCESS)
        } else {
            showAppFeedback(getString(R.string.reminder_permission_denied), FeedbackType.WARNING)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_settings)

        bindViews()
        bindInitialValues()
        setupActions()
        ReminderScheduler.createChannel(this)
    }

    private fun bindViews() {
        switchWorkout = findViewById(R.id.switch_workout_reminder)
        switchWater = findViewById(R.id.switch_water_reminder)
        switchCheckin = findViewById(R.id.switch_checkin_reminder)
        containerWorkoutTimes = findViewById(R.id.container_workout_times)
        containerWaterTimes = findViewById(R.id.container_water_times)
        containerCheckinTimes = findViewById(R.id.container_checkin_times)
        chipWorkoutDaily = findViewById(R.id.chip_workout_daily)
        chipWorkoutWorkday = findViewById(R.id.chip_workout_workday)
        chipWaterDaily = findViewById(R.id.chip_water_daily)
        chipWaterWorkday = findViewById(R.id.chip_water_workday)
        chipCheckinDaily = findViewById(R.id.chip_checkin_daily)
        chipCheckinWorkday = findViewById(R.id.chip_checkin_workday)
    }

    private fun bindInitialValues() {
        val settings = ReminderPrefs.getSettings(this)

        switchWorkout.isChecked = settings.workout.enabled
        switchWater.isChecked = settings.water.enabled
        switchCheckin.isChecked = settings.checkin.enabled

        workoutTimes.clear()
        workoutTimes.addAll(settings.workout.times)
        waterTimes.clear()
        waterTimes.addAll(settings.water.times)
        checkinTimes.clear()
        checkinTimes.addAll(settings.checkin.times)
        workoutRepeatMode = settings.workout.repeatMode
        waterRepeatMode = settings.water.repeatMode
        checkinRepeatMode = settings.checkin.repeatMode

        renderAllTimeGroups()
        updateRepeatModeChips()
    }

    private fun setupActions() {
        findViewById<TextView>(R.id.btn_reminder_back).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btn_save_reminders).setOnClickListener {
            saveAllSettings()
        }

        findViewById<TextView>(R.id.btn_test_notification).setOnClickListener {
            if (ensureNotificationPermission()) {
                ReminderScheduler.showTestNotification(this)
                showAppFeedback(getString(R.string.reminder_test_sent), FeedbackType.INFO)
            }
        }

        findViewById<TextView>(R.id.btn_add_workout_time).setOnClickListener {
            addReminderTime(ReminderType.WORKOUT, ReminderTime(19, 30))
        }
        findViewById<TextView>(R.id.btn_add_water_time).setOnClickListener {
            addReminderTime(ReminderType.WATER, ReminderTime(15, 0))
        }
        findViewById<TextView>(R.id.btn_add_checkin_time).setOnClickListener {
            addReminderTime(ReminderType.CHECKIN, ReminderTime(21, 0))
        }

        chipWorkoutDaily.setOnClickListener { setRepeatMode(ReminderType.WORKOUT, ReminderRepeatMode.DAILY) }
        chipWorkoutWorkday.setOnClickListener { setRepeatMode(ReminderType.WORKOUT, ReminderRepeatMode.WORKDAY) }
        chipWaterDaily.setOnClickListener { setRepeatMode(ReminderType.WATER, ReminderRepeatMode.DAILY) }
        chipWaterWorkday.setOnClickListener { setRepeatMode(ReminderType.WATER, ReminderRepeatMode.WORKDAY) }
        chipCheckinDaily.setOnClickListener { setRepeatMode(ReminderType.CHECKIN, ReminderRepeatMode.DAILY) }
        chipCheckinWorkday.setOnClickListener { setRepeatMode(ReminderType.CHECKIN, ReminderRepeatMode.WORKDAY) }

        switchWorkout.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) ensureNotificationPermission()
        }
        switchWater.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) ensureNotificationPermission()
        }
        switchCheckin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) ensureNotificationPermission()
        }
    }

    private fun saveAllSettings() {
        if (!validateReminderTimes()) return

        ReminderPrefs.updateWorkout(this, switchWorkout.isChecked, workoutTimes.sortedTimes(), workoutRepeatMode)
        ReminderPrefs.updateWater(this, switchWater.isChecked, waterTimes.sortedTimes(), waterRepeatMode)
        ReminderPrefs.updateCheckin(this, switchCheckin.isChecked, checkinTimes.sortedTimes(), checkinRepeatMode)

        if (ensureNotificationPermissionForEnabledItems()) {
            ReminderScheduler.rescheduleAll(this)
        }

        if (
            (switchWorkout.isChecked || switchWater.isChecked || switchCheckin.isChecked) &&
            !ReminderScheduler.canScheduleExactAlarms(this)
        ) {
            showAppFeedback(getString(R.string.reminder_exact_alarm_fallback), FeedbackType.WARNING)
        }

        showAppFeedback(getString(R.string.reminder_save_success), FeedbackType.SUCCESS)
    }

    private fun ensureNotificationPermissionForEnabledItems(): Boolean {
        val hasEnabledReminder = switchWorkout.isChecked || switchWater.isChecked || switchCheckin.isChecked
        return if (hasEnabledReminder) ensureNotificationPermission() else true
    }

    private fun ensureNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        return granted
    }

    private fun validateReminderTimes(): Boolean {
        if (switchWorkout.isChecked && workoutTimes.isEmpty()) {
            showAppFeedback(getString(R.string.reminder_need_time), FeedbackType.WARNING)
            return false
        }
        if (switchWater.isChecked && waterTimes.isEmpty()) {
            showAppFeedback(getString(R.string.reminder_need_time), FeedbackType.WARNING)
            return false
        }
        if (switchCheckin.isChecked && checkinTimes.isEmpty()) {
            showAppFeedback(getString(R.string.reminder_need_time), FeedbackType.WARNING)
            return false
        }
        return true
    }

    private fun renderAllTimeGroups() {
        renderTimeGroup(ReminderType.WORKOUT)
        renderTimeGroup(ReminderType.WATER)
        renderTimeGroup(ReminderType.CHECKIN)
    }

    private fun renderTimeGroup(type: ReminderType) {
        val container = getContainer(type)
        val times = getTimes(type)

        container.removeAllViews()
        if (times.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = getString(R.string.reminder_no_time)
                textSize = 12f
                setTextColor(getColor(R.color.text_secondary))
            }
            container.addView(emptyView)
            return
        }

        val sortedTimes = times.sortedTimes()
        var lastSection: String? = null
        sortedTimes.forEachIndexed { index, time ->
            val sectionTitle = getSectionTitle(time)
            if (sectionTitle != lastSection) {
                container.addView(buildSectionHeader(sectionTitle))
                lastSection = sectionTitle
            }

            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_reminder_time, container, false)

            val timeText = itemView.findViewById<TextView>(R.id.tv_reminder_time)
            val deleteButton = itemView.findViewById<TextView>(R.id.btn_delete_time)

            timeText.text = buildTimeDisplay(time, getRepeatMode(type))
            timeText.setOnClickListener {
                showTimePicker(time.hour, time.minute) { hour, minute ->
                    val original = sortedTimes[index]
                    val replaceIndex = times.indexOfFirst { it.hour == original.hour && it.minute == original.minute }
                    if (replaceIndex >= 0) {
                        times[replaceIndex] = ReminderTime(hour, minute)
                    }
                    renderTimeGroup(type)
                }
            }
            deleteButton.setOnClickListener {
                val removeTarget = sortedTimes[index]
                val removeIndex = times.indexOfFirst { it.hour == removeTarget.hour && it.minute == removeTarget.minute }
                if (removeIndex >= 0) {
                    times.removeAt(removeIndex)
                }
                renderTimeGroup(type)
            }

            container.addView(itemView)
        }
    }

    private fun addReminderTime(type: ReminderType, defaultTime: ReminderTime) {
        showTimePicker(defaultTime.hour, defaultTime.minute) { hour, minute ->
            getTimes(type).add(ReminderTime(hour, minute))
            renderTimeGroup(type)
        }
    }

    private fun setRepeatMode(type: ReminderType, repeatMode: ReminderRepeatMode) {
        when (type) {
            ReminderType.WORKOUT -> workoutRepeatMode = repeatMode
            ReminderType.WATER -> waterRepeatMode = repeatMode
            ReminderType.CHECKIN -> checkinRepeatMode = repeatMode
        }
        updateRepeatModeChips()
        renderTimeGroup(type)
    }

    private fun getContainer(type: ReminderType): LinearLayout {
        return when (type) {
            ReminderType.WORKOUT -> containerWorkoutTimes
            ReminderType.WATER -> containerWaterTimes
            ReminderType.CHECKIN -> containerCheckinTimes
        }
    }

    private fun getTimes(type: ReminderType): MutableList<ReminderTime> {
        return when (type) {
            ReminderType.WORKOUT -> workoutTimes
            ReminderType.WATER -> waterTimes
            ReminderType.CHECKIN -> checkinTimes
        }
    }

    private fun getRepeatMode(type: ReminderType): ReminderRepeatMode {
        return when (type) {
            ReminderType.WORKOUT -> workoutRepeatMode
            ReminderType.WATER -> waterRepeatMode
            ReminderType.CHECKIN -> checkinRepeatMode
        }
    }

    private fun showTimePicker(hour: Int, minute: Int, onSelected: (Int, Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .setTitleText("选择提醒时间")
            .setTheme(R.style.ThemeOverlay_MyFitnessApp_MaterialTimePicker)
            .build()

        picker.addOnPositiveButtonClickListener {
            onSelected(picker.hour, picker.minute)
        }

        picker.show(supportFragmentManager, "reminderTimePicker")
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun buildTimeDisplay(time: ReminderTime, repeatMode: ReminderRepeatMode): String {
        val repeatLabel = when (repeatMode) {
            ReminderRepeatMode.DAILY -> getString(R.string.reminder_repeat_daily_short)
            ReminderRepeatMode.WORKDAY -> getString(R.string.reminder_repeat_workday_short)
        }
        return "${formatTime(time.hour, time.minute)}  $repeatLabel"
    }

    private fun getSectionTitle(time: ReminderTime): String {
        return when (time.hour) {
            in 5..11 -> getString(R.string.reminder_section_morning)
            in 12..17 -> getString(R.string.reminder_section_afternoon)
            else -> getString(R.string.reminder_section_evening)
        }
    }

    private fun buildSectionHeader(title: String): View {
        return TextView(this).apply {
            text = title
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, 8.dp(), 0, 4.dp())
        }
    }

    private fun updateRepeatModeChips() {
        updateChipGroup(chipWorkoutDaily, chipWorkoutWorkday, workoutRepeatMode)
        updateChipGroup(chipWaterDaily, chipWaterWorkday, waterRepeatMode)
        updateChipGroup(chipCheckinDaily, chipCheckinWorkday, checkinRepeatMode)
    }

    private fun updateChipGroup(
        dailyChip: TextView,
        workdayChip: TextView,
        repeatMode: ReminderRepeatMode
    ) {
        val isDaily = repeatMode == ReminderRepeatMode.DAILY
        styleRepeatChip(dailyChip, isDaily)
        styleRepeatChip(workdayChip, !isDaily)
    }

    private fun styleRepeatChip(chip: TextView, selected: Boolean) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.category_chip_selected)
            chip.setTextColor(getColor(android.R.color.white))
        } else {
            chip.setBackgroundResource(R.drawable.category_chip_unselected)
            chip.setTextColor(getColor(R.color.text_secondary))
        }
    }

    private fun List<ReminderTime>.sortedTimes(): List<ReminderTime> {
        return distinct().sortedWith(compareBy({ it.hour }, { it.minute }))
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private enum class ReminderType {
        WORKOUT,
        WATER,
        CHECKIN
    }
}
