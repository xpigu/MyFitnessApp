package com.example.myfitnessapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

class StatsActivity : AppCompatActivity() {

    private lateinit var viewModel: WorkoutRecordViewModel
    private lateinit var groupedAdapter: GroupedRecordAdapter
    private var isDayMode = true

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

        tabDay.setOnClickListener { switchToDayMode() }
        tabMonth.setOnClickListener { switchToMonthMode() }
    }

    private fun switchToDayMode() {
        isDayMode = true
        viewModel.setDayMode(true)
        updateTabUI()
    }

    private fun switchToMonthMode() {
        isDayMode = false
        viewModel.setDayMode(false)
        updateTabUI()
    }

    private fun updateTabUI() {
        val tabDay = findViewById<TextView>(R.id.tab_day)
        val tabMonth = findViewById<TextView>(R.id.tab_month)

        if (isDayMode) {
            tabDay.background = getDrawable(R.drawable.tab_bg_selected)
            tabDay.setTextColor(getColor(android.R.color.white))
            tabDay.textSize = 14f
            tabDay.setTypeface(null, android.graphics.Typeface.BOLD)
            tabMonth.background = getDrawable(R.drawable.tab_bg_unselected)
            tabMonth.setTextColor(getColor(R.color.text_secondary))
            tabMonth.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            tabMonth.background = getDrawable(R.drawable.tab_bg_selected)
            tabMonth.setTextColor(getColor(android.R.color.white))
            tabMonth.setTypeface(null, android.graphics.Typeface.BOLD)
            tabDay.background = getDrawable(R.drawable.tab_bg_unselected)
            tabDay.setTextColor(getColor(R.color.text_secondary))
            tabDay.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    // ============================================================
    // 观察数据变化
    // ============================================================
    private fun observeData() {
        // 观察汇总数据
        viewModel.summary.observe(this) { data ->
            updateSummary(data)
        }

        // 观察所有记录，构建分组
        viewModel.allRecords.observe(this) { records ->
            val dates = records.map { it.date }.distinct().sortedDescending()
            val groups = viewModel.buildGroupedRecords(dates, records)
            groupedAdapter.updateData(groups)
            updateEmptyState(records.isEmpty())
        }
    }

    private fun updateSummary(data: SummaryData) {
        findViewById<TextView>(R.id.tv_total_workouts).text = data.workoutCount.toString()
        findViewById<TextView>(R.id.tv_total_calories).text = data.totalCalories.toString()
        findViewById<TextView>(R.id.tv_total_duration).text = data.formatDuration()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        findViewById<View>(R.id.ll_empty_state).visibility = if (isEmpty) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(R.id.rv_sport_list).visibility = if (isEmpty) View.GONE else View.VISIBLE
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
            .setImageResource(record.sportIconResId)
        view.findViewById<TextView>(R.id.detail_tv_sport_name).text = label
        view.findViewById<TextView>(R.id.detail_tv_sport_time).text = time
        view.findViewById<TextView>(R.id.detail_tv_duration).text = duration

        // 根据运动类型显示不同数据
        when (record.sportType.uppercase()) {
            "RUN", "CYCLING" -> {
                view.findViewById<TextView>(R.id.detail_tv_distance).text =
                    String.format("%.2f", record.totalDistance)
                view.findViewById<TextView>(R.id.detail_tv_avg_speed).text =
                    if (record.sportType == "CYCLING") record.pace else "--"
                view.findViewById<TextView>(R.id.detail_tv_pace).text =
                    if (record.sportType == "RUN") record.pace else "--"
            }
            "JUMP_ROPE" -> {
                view.findViewById<TextView>(R.id.detail_tv_distance).text = "${record.jumpCount} 个"
                view.findViewById<TextView>(R.id.detail_tv_avg_speed).text = "${record.jumpFrequency} 个/分"
                view.findViewById<TextView>(R.id.detail_tv_pace).text = "--"
            }
            "STRENGTH" -> {
                view.findViewById<TextView>(R.id.detail_tv_distance).text = "${record.strengthSets} 组"
                view.findViewById<TextView>(R.id.detail_tv_avg_speed).text = "${record.strengthVolume.toInt()} kg"
                view.findViewById<TextView>(R.id.detail_tv_pace).text = "--"
            }
            "SWIMMING" -> {
                view.findViewById<TextView>(R.id.detail_tv_distance).text = "${record.swimDistanceM} m"
                view.findViewById<TextView>(R.id.detail_tv_avg_speed).text = record.swimStroke
                view.findViewById<TextView>(R.id.detail_tv_pace).text = "--"
            }
            "YOGA" -> {
                view.findViewById<TextView>(R.id.detail_tv_distance).text = "${record.yogaPosesCompleted} 体式"
                view.findViewById<TextView>(R.id.detail_tv_avg_speed).text = "--"
                view.findViewById<TextView>(R.id.detail_tv_pace).text = "--"
            }
        }

        view.findViewById<TextView>(R.id.detail_tv_calories).text = "${record.totalCalories} kcal"

        dialog.setContentView(view)
        dialog.show()
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
        data class HeaderItem(val date: String, val count: Int, val calories: Int) : ListItem()
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
            items.add(ListItem.HeaderItem(group.date, group.records.size, totalCal))
            for (record in group.records) {
                items.add(ListItem.RecordItem(record))
            }
        }
        notifyDataSetChanged()
    }

    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: ListItem.HeaderItem) {
            val display = WorkoutRecordHelper.dateToDisplay(item.date)
            itemView.findViewById<TextView>(R.id.tv_date_header).text = display
            itemView.findViewById<TextView>(R.id.tv_date_summary).text =
                "${item.count} 次运动 · ${item.calories} kcal"
        }
    }

    class RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(record: WorkoutRecord, onClick: (WorkoutRecord) -> Unit) {
            itemView.findViewById<ImageView>(R.id.iv_sport_type)
                .setImageResource(record.sportIconResId)
            itemView.findViewById<TextView>(R.id.tv_sport_name).text =
                WorkoutRecordHelper.getSportLabel(record.sportType)
            itemView.findViewById<TextView>(R.id.tv_sport_time).text =
                WorkoutRecordHelper.timestampToTime(record.timestamp)
            itemView.findViewById<TextView>(R.id.tv_sport_summary).text =
                WorkoutRecordHelper.buildSummary(record)

            itemView.setOnClickListener { onClick(record) }
        }
    }
}
