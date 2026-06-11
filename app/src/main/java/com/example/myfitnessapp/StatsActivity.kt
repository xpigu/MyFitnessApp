package com.example.myfitnessapp

import android.content.res.ColorStateList
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myfitnessapp.data.WorkoutRecordHelper
import com.example.myfitnessapp.data.entity.WorkoutRecord
import com.example.myfitnessapp.data.viewmodel.DateGroup
import com.example.myfitnessapp.data.viewmodel.SummaryData
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Calendar

class StatsActivity : AppCompatActivity() {

    private lateinit var viewModel: WorkoutRecordViewModel
    private lateinit var groupedAdapter: GroupedRecordAdapter
    private var isDayMode = true
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)

    // Phase 2 增强：最佳成绩显示
    private lateinit var pbAdapter: PBRecordsAdapter
    private var pbRecyclerView: RecyclerView? = null
    private val pbPrefs by lazy { getSharedPreferences("stats_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        viewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)

        setupBottomNavigation()
        setupTabToggle()
        setupRecyclerView()
        observeData()
    }

    // ============================================================
    // 底部导航
    // ============================================================
    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_stats

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
                R.id.nav_stats -> true
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
    // 日/月 Tab 切换
    // ============================================================
    private fun setupTabToggle() {
        val tabDay = findViewById<TextView>(R.id.tab_day)
        val tabMonth = findViewById<TextView>(R.id.tab_month)
        val prevYearButton = findViewById<TextView>(R.id.btn_prev_year)
        val nextYearButton = findViewById<TextView>(R.id.btn_next_year)

        tabDay.setOnClickListener { switchToDayMode() }
        tabMonth.setOnClickListener { switchToMonthMode() }
        prevYearButton.setOnClickListener {
            if (!prevYearButton.isEnabled) return@setOnClickListener
            selectedYear -= 1
            if (!isDayMode) {
                viewModel.setCurrentDate(String.format("%04d-01-01", selectedYear))
                updateSummaryForMode(viewModel.allRecords.value.orEmpty())
                refreshStatsDisplay(viewModel.allRecords.value.orEmpty())
                updateYearNavigation(viewModel.allRecords.value.orEmpty())
            }
            updateTabUI()
        }
        nextYearButton.setOnClickListener {
            if (!nextYearButton.isEnabled) return@setOnClickListener
            selectedYear += 1
            if (!isDayMode) {
                viewModel.setCurrentDate(String.format("%04d-01-01", selectedYear))
                updateSummaryForMode(viewModel.allRecords.value.orEmpty())
                refreshStatsDisplay(viewModel.allRecords.value.orEmpty())
                updateYearNavigation(viewModel.allRecords.value.orEmpty())
            }
            updateTabUI()
        }
    }

    private fun switchToDayMode() {
        isDayMode = true
        viewModel.setCurrentDate(WorkoutRecordViewModel.todayStr())
        viewModel.setDayMode(true)
        updateTabUI()
        updateYearNavigation(viewModel.allRecords.value.orEmpty())
        updateSummaryForMode(viewModel.allRecords.value.orEmpty())
        refreshStatsDisplay(viewModel.allRecords.value.orEmpty())
    }

    private fun switchToMonthMode() {
        isDayMode = false
        viewModel.setCurrentDate(String.format("%04d-01-01", selectedYear))
        viewModel.setDayMode(false)
        updateTabUI()
        updateYearNavigation(viewModel.allRecords.value.orEmpty())
        updateSummaryForMode(viewModel.allRecords.value.orEmpty())
        refreshStatsDisplay(viewModel.allRecords.value.orEmpty())
    }

    private fun updateTabUI() {
        val tabDay = findViewById<TextView>(R.id.tab_day)
        val tabMonth = findViewById<TextView>(R.id.tab_month)
        val yearSelector = findViewById<View>(R.id.layout_year_selector)
        val selectedYearText = findViewById<TextView>(R.id.tv_selected_year)
        val summaryScope = findViewById<TextView>(R.id.tv_stats_summary_scope)
        val recordSectionTitle = findViewById<TextView>(R.id.tv_record_section_title)

        if (isDayMode) {
            tabDay.background = getDrawable(R.drawable.tab_bg_selected)
            tabDay.setTextColor(getColor(android.R.color.white))
            tabDay.textSize = 14f
            tabDay.setTypeface(null, android.graphics.Typeface.BOLD)
            tabMonth.background = getDrawable(R.drawable.tab_bg_unselected)
            tabMonth.setTextColor(getColor(R.color.text_secondary))
            tabMonth.setTypeface(null, android.graphics.Typeface.NORMAL)
            yearSelector.visibility = View.GONE
            summaryScope.text = getString(R.string.stats_summary_today)
            recordSectionTitle.text = getString(R.string.stats_record_title_day)
        } else {
            tabMonth.background = getDrawable(R.drawable.tab_bg_selected)
            tabMonth.setTextColor(getColor(android.R.color.white))
            tabMonth.setTypeface(null, android.graphics.Typeface.BOLD)
            tabDay.background = getDrawable(R.drawable.tab_bg_unselected)
            tabDay.setTextColor(getColor(R.color.text_secondary))
            tabDay.setTypeface(null, android.graphics.Typeface.NORMAL)
            yearSelector.visibility = View.VISIBLE
            selectedYearText.text = getString(R.string.stats_year_format, selectedYear)
            summaryScope.text = getString(R.string.stats_summary_year_format, selectedYear)
            recordSectionTitle.text = getString(R.string.stats_record_title_month)
        }
    }

    // ============================================================
    // 观察数据变化
    // ============================================================
    private fun observeData() {
        // 观察所有记录，构建分组
        viewModel.allRecords.observe(this) { records ->
            updateSummaryForMode(records)
            refreshStatsDisplay(records)
            updateYearNavigation(records)

            // Phase 2 增强：加载最佳成绩
            if (records.isNotEmpty()) {
                loadPBRecords()
            } else {
                hidePBRecords()
            }
        }
    }

    private fun updateSummary(data: SummaryData) {
        findViewById<TextView>(R.id.tv_total_workouts).text = data.workoutCount.toString()
        findViewById<TextView>(R.id.tv_total_calories).text = data.totalCalories.toString()
        findViewById<TextView>(R.id.tv_total_duration).text = data.formatDuration()
    }

    private fun updateSummaryForMode(records: List<WorkoutRecord>) {
        val summary = if (isDayMode) {
            val today = WorkoutRecordViewModel.todayStr()
            val todayRecords = records.filter { it.date == today }
            SummaryData(
                workoutCount = todayRecords.size,
                totalCalories = todayRecords.sumOf { it.totalCalories },
                totalDurationSeconds = todayRecords.sumOf { it.elapsedSeconds }
            )
        } else {
            val yearPrefix = String.format("%04d-", selectedYear)
            val yearRecords = records.filter { it.date.startsWith(yearPrefix) }
            SummaryData(
                workoutCount = yearRecords.size,
                totalCalories = yearRecords.sumOf { it.totalCalories },
                totalDurationSeconds = yearRecords.sumOf { it.elapsedSeconds }
            )
        }
        updateSummary(summary)
    }

    private fun updateYearNavigation(records: List<WorkoutRecord>) {
        val prevYearButton = findViewById<TextView>(R.id.btn_prev_year)
        val nextYearButton = findViewById<TextView>(R.id.btn_next_year)
        val availableYears = records.mapNotNull { it.date.take(4).toIntOrNull() }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val minYear = availableYears.minOrNull() ?: currentYear
        val maxYear = maxOf(availableYears.maxOrNull() ?: currentYear, currentYear)
        prevYearButton.isEnabled = selectedYear > minYear
        nextYearButton.isEnabled = selectedYear < maxYear
        prevYearButton.alpha = if (prevYearButton.isEnabled) 1f else 0.45f
        nextYearButton.alpha = if (nextYearButton.isEnabled) 1f else 0.45f
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val emptyTitle = findViewById<TextView>(R.id.tv_empty_title)
        val emptySubtitle = findViewById<TextView>(R.id.tv_empty_subtitle)
        findViewById<View>(R.id.ll_empty_state).visibility = if (isEmpty) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(R.id.rv_sport_list).visibility = if (isEmpty) View.GONE else View.VISIBLE
        if (isDayMode) {
            emptyTitle.text = getString(R.string.stats_empty_day_title)
            emptySubtitle.text = getString(R.string.stats_empty_day_subtitle)
        } else {
            emptyTitle.text = getString(R.string.stats_empty_year_title_format, selectedYear)
            emptySubtitle.text = getString(R.string.stats_empty_year_subtitle)
        }
    }

    private fun refreshStatsDisplay(records: List<WorkoutRecord>) {
        val groups = if (isDayMode) {
            buildDayGroups(records)
        } else {
            buildMonthGroups(records, selectedYear)
        }
        groupedAdapter.updateData(groups)
        updateEmptyState(groups.isEmpty())
    }

    private fun buildDayGroups(records: List<WorkoutRecord>): List<DateGroup> {
        val dates = records.map { it.date }.distinct().sortedDescending()
        return dates.map { date ->
            val dayRecords = records.filter { it.date == date }
            DateGroup(
                date = date,
                title = WorkoutRecordHelper.dateToDisplay(date),
                records = dayRecords
            )
        }
    }

    private fun buildMonthGroups(records: List<WorkoutRecord>, year: Int): List<DateGroup> {
        val yearPrefix = String.format("%04d-", year)
        val recordsInYear = records.filter { it.date.startsWith(yearPrefix) }
        val groupedByMonth = recordsInYear.groupBy { it.date.substring(0, 7) }
        return groupedByMonth.keys.sortedDescending().map { month ->
            DateGroup(
                date = month,
                title = WorkoutRecordHelper.monthToDisplay(month),
                records = groupedByMonth[month].orEmpty()
            )
        }
    }

    // ============================================================
    // 最佳成绩显示 — Phase 2 增强
    // ============================================================
    private fun loadPBRecords() {
        viewModel.getPBRecords { pbRecords ->
            if (pbRecords.hasAnyData()) {
                pbAdapter = PBRecordsAdapter(pbRecords.cards) { record ->
                    showDetailBottomSheet(record)
                }
                pbRecyclerView?.adapter = pbAdapter
                showPBRecords()
                maybePlayPBNudge()
            } else {
                hidePBRecords()
            }
        }
    }

    private fun showPBRecords() {
        findViewById<View>(R.id.layout_pb_header).visibility = View.VISIBLE
        pbRecyclerView?.visibility = View.VISIBLE
    }

    private fun hidePBRecords() {
        findViewById<View>(R.id.layout_pb_header).visibility = View.GONE
        pbRecyclerView?.visibility = View.GONE
    }

    private fun maybePlayPBNudge() {
        val hasShownHint = pbPrefs.getBoolean(KEY_PB_SWIPE_HINT_SHOWN, false)
        if (hasShownHint) return

        pbRecyclerView?.postDelayed({
            val recyclerView = pbRecyclerView ?: return@postDelayed
            if (recyclerView.adapter == null || recyclerView.adapter?.itemCount == 0) return@postDelayed

            val nudgeDistance = (resources.displayMetrics.density * 72).toInt()
            recyclerView.smoothScrollBy(nudgeDistance, 0)
            recyclerView.postDelayed({
                recyclerView.smoothScrollBy(-nudgeDistance, 0)
            }, 450)

            pbPrefs.edit().putBoolean(KEY_PB_SWIPE_HINT_SHOWN, true).apply()
        }, 450)
    }

    // ============================================================
    // RecyclerView 初始化
    // ============================================================
    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rv_sport_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        groupedAdapter = GroupedRecordAdapter(
            onItemClick = { record -> showDetailBottomSheet(record) },
            onDeleteClick = { record -> viewModel.deleteRecord(record) }
        )
        recyclerView.adapter = groupedAdapter

        // Phase 2 增强：设置最佳成绩 RecyclerView
        pbRecyclerView = findViewById(R.id.rv_pb_records)
        pbRecyclerView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    // ============================================================
    // BottomSheet 弹窗详情
    // ============================================================
    private fun showDetailBottomSheet(record: WorkoutRecord) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet_sport_detail, null)

        val label = WorkoutRecordHelper.getSportLabel(record.sportType)
        val time = WorkoutRecordHelper.timestampToDateTime(record.timestamp)
        val duration = WorkoutRecordHelper.formatDuration(record.elapsedSeconds)

        view.findViewById<ImageView>(R.id.detail_iv_sport_type)
            .apply {
                setImageResource(record.sportIconResId)
                backgroundTintList = ColorStateList.valueOf(getColor(sportBadgeColor(record.sportType)))
            }
        view.findViewById<TextView>(R.id.detail_tv_sport_name).text = label
        view.findViewById<TextView>(R.id.detail_tv_sport_time).text = time
        view.findViewById<TextView>(R.id.detail_tv_duration).text = duration
        applyDetailMetricColors(view, record)

        when (record.sportType.uppercase()) {
            "RUN", "CYCLING" -> {
                configureDetailMetricLabels(
                    view = view,
                    distanceLabelRes = R.string.detail_distance,
                    distanceUnitRes = R.string.detail_km,
                    avgLabelRes = R.string.detail_avg_speed,
                    avgUnitRes = if (record.sportType.uppercase() == "CYCLING") R.string.detail_kmh else R.string.detail_unit_none,
                    paceLabelRes = R.string.detail_pace,
                    paceUnitRes = if (record.sportType.uppercase() == "RUN") R.string.detail_min_per_km else R.string.detail_unit_none
                )
                view.findViewById<TextView>(R.id.detail_tv_distance).text = String.format("%.2f", record.totalDistance)
                view.findViewById<TextView>(R.id.detail_tv_avg_speed).text = if (record.sportType.uppercase() == "CYCLING") record.pace else "--"
                view.findViewById<TextView>(R.id.detail_tv_pace).text = if (record.sportType.uppercase() == "RUN") record.pace else "--"
            }
            "JUMP_ROPE" -> {
                configureDetailMetricLabels(
                    view = view,
                    distanceLabelRes = R.string.detail_total_count,
                    distanceUnitRes = R.string.detail_unit_count,
                    avgLabelRes = R.string.detail_avg_frequency,
                    avgUnitRes = R.string.detail_unit_count_per_min,
                    paceLabelRes = R.string.detail_pace,
                    paceUnitRes = R.string.detail_unit_none
                )
                view.findViewById<TextView>(R.id.detail_tv_distance).text = "${record.jumpCount} 个"
                view.findViewById<TextView>(R.id.detail_tv_avg_speed).text = "${record.jumpFrequency} 个/分"
                view.findViewById<TextView>(R.id.detail_tv_pace).text = "--"
            }
            "STRENGTH" -> {
                configureDetailMetricLabels(
                    view = view,
                    distanceLabelRes = R.string.detail_completed_sets,
                    distanceUnitRes = R.string.detail_unit_sets,
                    avgLabelRes = R.string.detail_max_weight,
                    avgUnitRes = R.string.detail_unit_kg,
                    paceLabelRes = R.string.detail_total_volume,
                    paceUnitRes = R.string.detail_unit_kg
                )
                view.findViewById<TextView>(R.id.detail_tv_distance).text = "${record.strengthSets} 组"
                view.findViewById<TextView>(R.id.detail_tv_avg_speed).text = "${record.strengthMaxWeight.toInt()} kg"
                view.findViewById<TextView>(R.id.detail_tv_pace).text = "${record.strengthVolume.toInt()} kg"
            }
            "SWIMMING" -> {
                configureDetailMetricLabels(
                    view = view,
                    distanceLabelRes = R.string.detail_distance,
                    distanceUnitRes = R.string.detail_unit_meter,
                    avgLabelRes = R.string.detail_stroke,
                    avgUnitRes = R.string.detail_unit_none,
                    paceLabelRes = R.string.detail_pace,
                    paceUnitRes = R.string.detail_unit_none
                )
                view.findViewById<TextView>(R.id.detail_tv_distance).text = "${record.swimDistanceM} m"
                view.findViewById<TextView>(R.id.detail_tv_avg_speed).text = record.swimStroke
                view.findViewById<TextView>(R.id.detail_tv_pace).text = "--"
            }
            "YOGA" -> {
                configureDetailMetricLabels(
                    view = view,
                    distanceLabelRes = R.string.detail_distance,
                    distanceUnitRes = R.string.detail_unit_none,
                    avgLabelRes = R.string.detail_avg_speed,
                    avgUnitRes = R.string.detail_unit_none,
                    paceLabelRes = R.string.detail_pace,
                    paceUnitRes = R.string.detail_unit_none
                )
                view.findViewById<TextView>(R.id.detail_tv_distance).text = "${record.yogaPosesCompleted} 体式"
                view.findViewById<TextView>(R.id.detail_tv_avg_speed).text = "--"
                view.findViewById<TextView>(R.id.detail_tv_pace).text = "--"
            }
        }

        view.findViewById<TextView>(R.id.detail_tv_calories).text = "${record.totalCalories} kcal"

        dialog.setContentView(view)
        dialog.show()
    }

    private fun configureDetailMetricLabels(
        view: View,
        distanceLabelRes: Int,
        distanceUnitRes: Int,
        avgLabelRes: Int,
        avgUnitRes: Int,
        paceLabelRes: Int,
        paceUnitRes: Int
    ) {
        view.findViewById<TextView>(R.id.detail_label_distance).setText(distanceLabelRes)
        view.findViewById<TextView>(R.id.detail_unit_distance).setText(distanceUnitRes)
        view.findViewById<TextView>(R.id.detail_label_avg_speed).setText(avgLabelRes)
        view.findViewById<TextView>(R.id.detail_unit_avg_speed).setText(avgUnitRes)
        view.findViewById<TextView>(R.id.detail_label_pace).setText(paceLabelRes)
        view.findViewById<TextView>(R.id.detail_unit_pace).setText(paceUnitRes)
    }

    private fun applyDetailMetricColors(view: View, record: WorkoutRecord) {
        view.findViewById<TextView>(R.id.detail_tv_duration)
            .setTextColor(getColor(R.color.training_course_action_text))
        view.findViewById<TextView>(R.id.detail_tv_distance)
            .setTextColor(getColor(metricAccentColor(record.sportType)))
        view.findViewById<TextView>(R.id.detail_tv_avg_speed)
            .setTextColor(getColor(metricSecondaryColor(record.sportType)))
        view.findViewById<TextView>(R.id.detail_tv_pace)
            .setTextColor(getColor(metricTertiaryColor(record.sportType)))
    }

    private fun sportBadgeColor(sportType: String): Int {
        return when (sportType.uppercase()) {
            "RUN" -> R.color.training_running_badge_fill
            "CYCLING" -> R.color.training_cycling_badge_fill
            "YOGA" -> R.color.training_yoga_badge_fill
            "STRENGTH" -> R.color.training_strength_badge_fill
            "SWIMMING" -> R.color.training_swimming_badge_fill
            "JUMP_ROPE" -> R.color.training_jump_rope_badge_fill
            else -> R.color.training_running_badge_fill
        }
    }

    private fun metricAccentColor(sportType: String): Int {
        return when (sportType.uppercase()) {
            "RUN" -> R.color.tracking_metric_running
            "CYCLING", "SWIMMING" -> R.color.tracking_metric_cycling
            "STRENGTH" -> R.color.training_strength_badge_fill
            "JUMP_ROPE" -> R.color.tracking_summary_primary
            "YOGA" -> R.color.training_yoga_badge_fill
            else -> R.color.training_course_action_text
        }
    }

    private fun metricSecondaryColor(sportType: String): Int {
        return when (sportType.uppercase()) {
            "RUN", "JUMP_ROPE" -> R.color.tracking_summary_primary
            "CYCLING", "SWIMMING" -> R.color.tracking_metric_cycling
            "STRENGTH" -> R.color.tracking_metric_fast
            "YOGA" -> R.color.text_secondary
            else -> R.color.text_secondary
        }
    }

    private fun metricTertiaryColor(sportType: String): Int {
        return when (sportType.uppercase()) {
            "RUN" -> R.color.tracking_stop_text
            "CYCLING", "SWIMMING", "YOGA" -> R.color.text_secondary
            "STRENGTH" -> R.color.tracking_summary_calories
            "JUMP_ROPE" -> R.color.text_secondary
            else -> R.color.text_secondary
        }
    }

    companion object {
        private const val KEY_PB_SWIPE_HINT_SHOWN = "pb_swipe_hint_shown"
    }
}

// ============================================================
// 分组适配器：日期标题 + 记录列表
// ============================================================
class GroupedRecordAdapter(
    private val onItemClick: (WorkoutRecord) -> Unit,
    private val onDeleteClick: (WorkoutRecord) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ListItem>()

    companion object {
        const val TYPE_DATE_HEADER = 0
        const val TYPE_RECORD = 1
    }

    sealed class ListItem {
        data class HeaderItem(val title: String, val count: Int, val calories: Int) : ListItem()
        data class RecordItem(val record: WorkoutRecord) : ListItem()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.HeaderItem -> TYPE_DATE_HEADER
            is ListItem.RecordItem -> TYPE_RECORD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val view = inflater.inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_sport_record, parent, false)
                RecordViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.HeaderItem -> {
                (holder as DateHeaderViewHolder).bind(item)
            }
            is ListItem.RecordItem -> {
                (holder as RecordViewHolder).bind(item.record, onItemClick)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(groups: List<DateGroup>) {
        items.clear()
        for (group in groups) {
            val totalCal = group.records.sumOf { it.totalCalories }
            items.add(ListItem.HeaderItem(group.title, group.records.size, totalCal))
            for (record in group.records) {
                items.add(ListItem.RecordItem(record))
            }
        }
        notifyDataSetChanged()
    }

    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: ListItem.HeaderItem) {
            itemView.findViewById<TextView>(R.id.tv_date_header).text = item.title
            itemView.findViewById<TextView>(R.id.tv_date_summary).text =
                "${item.count} 次运动 · ${item.calories} kcal"
        }
    }

    class RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(record: WorkoutRecord, onClick: (WorkoutRecord) -> Unit) {
            itemView.findViewById<ImageView>(R.id.iv_sport_type)
                .apply {
                    setImageResource(record.sportIconResId)
                    backgroundTintList = ColorStateList.valueOf(
                        context.getColor(
                            when (record.sportType.uppercase()) {
                                "RUN" -> R.color.training_running_badge_fill
                                "CYCLING" -> R.color.training_cycling_badge_fill
                                "YOGA" -> R.color.training_yoga_badge_fill
                                "STRENGTH" -> R.color.training_strength_badge_fill
                                "SWIMMING" -> R.color.training_swimming_badge_fill
                                "JUMP_ROPE" -> R.color.training_jump_rope_badge_fill
                                else -> R.color.training_running_badge_fill
                            }
                        )
                    )
                }
            itemView.findViewById<TextView>(R.id.tv_sport_name).text =
                WorkoutRecordHelper.getSportLabel(record.sportType)
            itemView.findViewById<TextView>(R.id.tv_sport_time).text =
                WorkoutRecordHelper.timestampToTime(record.timestamp)
            itemView.findViewById<TextView>(R.id.tv_sport_summary).apply {
                text = WorkoutRecordHelper.buildSummary(record)
                setTextColor(
                    context.getColor(
                        when (record.sportType.uppercase()) {
                            "RUN" -> R.color.tracking_metric_running
                            "CYCLING", "SWIMMING" -> R.color.tracking_metric_cycling
                            "STRENGTH" -> R.color.training_strength_badge_fill
                            "JUMP_ROPE" -> R.color.tracking_summary_primary
                            "YOGA" -> R.color.training_yoga_badge_fill
                            else -> R.color.training_course_action_text
                        }
                    )
                )
            }

            itemView.setOnClickListener { onClick(record) }
        }
    }
}
