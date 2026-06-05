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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog

class StatsActivity : AppCompatActivity() {

    private lateinit var sportAdapter: SportRecordAdapter
    private val dayRecords = mutableListOf<SportRecord>()
    private val monthRecords = mutableListOf<SportRecord>()
    private var isDayMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        initMockData()
        setupBottomNavigation()
        setupTabToggle()
        setupRecyclerView()
    }

    // ============================================================
    // 模拟数据
    // ============================================================
    private fun initMockData() {
        // 日视图数据
        dayRecords.addAll(listOf(
            SportRecord(R.drawable.ic_running, "户外跑步", "早上 08:30", "5.2 公里 / 450 千卡",
                "32:15", "5.20", "9.7", "6'12\"", "450 kcal"),
            SportRecord(R.drawable.ic_cycling, "户外骑行", "早上 10:00", "15.8 公里 / 480 千卡",
                "45:00", "15.80", "21.1", "2'51\"", "480 kcal"),
            SportRecord(R.drawable.ic_strength_white, "力量训练", "下午 16:00", "350 千卡",
                "40:00", "--", "--", "--", "350 kcal"),
            SportRecord(R.drawable.ic_yoga, "瑜伽拉伸", "晚上 19:30", "120 千卡",
                "20:00", "--", "--", "--", "120 kcal")
        ))

        // 月视图数据（汇总）
        monthRecords.addAll(listOf(
            SportRecord(R.drawable.ic_running, "跑步总计", "本月 12 次", "62.4 公里 / 5,200 千卡",
                "6h 24m", "62.40", "9.8", "6'08\"", "5,200 kcal"),
            SportRecord(R.drawable.ic_cycling, "骑行总计", "本月 8 次", "126.4 公里 / 3,840 千卡",
                "6h 00m", "126.40", "21.1", "2'51\"", "3,840 kcal"),
            SportRecord(R.drawable.ic_strength_white, "力量总计", "本月 10 次", "3,500 千卡",
                "6h 40m", "--", "--", "--", "3,500 kcal"),
            SportRecord(R.drawable.ic_swimming, "游泳总计", "本月 4 次", "8.0 公里 / 1,600 千卡",
                "2h 40m", "8.00", "3.0", "20'00\"", "1,600 kcal")
        ))
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
        updateTabUI()
        sportAdapter.updateData(dayRecords)
        updateSummary(dayRecords)
    }

    private fun switchToMonthMode() {
        isDayMode = false
        updateTabUI()
        sportAdapter.updateData(monthRecords)
        updateSummary(monthRecords)
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

    private fun updateSummary(records: List<SportRecord>) {
        val totalWorkouts = findViewById<TextView>(R.id.tv_total_workouts)
        val totalCalories = findViewById<TextView>(R.id.tv_total_calories)
        val totalDuration = findViewById<TextView>(R.id.tv_total_duration)
        val avgHeartRate = findViewById<TextView>(R.id.tv_avg_heart_rate)

        totalWorkouts.text = "${records.size}"

        // 模拟汇总数据
        if (isDayMode) {
            totalCalories.text = "1,400"
            totalDuration.text = "2h 17m"
            avgHeartRate.text = "132"
        } else {
            totalCalories.text = "14,140"
            totalDuration.text = "21h 44m"
            avgHeartRate.text = "128"
        }
    }

    // ============================================================
    // RecyclerView 初始化
    // ============================================================
    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rv_sport_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        sportAdapter = SportRecordAdapter(dayRecords) { record ->
            showDetailBottomSheet(record)
        }
        recyclerView.adapter = sportAdapter
    }

    // ============================================================
    // BottomSheet 弹窗详情
    // ============================================================
    private fun showDetailBottomSheet(record: SportRecord) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet_sport_detail, null)

        // 绑定数据
        view.findViewById<ImageView>(R.id.detail_iv_sport_type)
            .setImageResource(record.iconRes)
        view.findViewById<TextView>(R.id.detail_tv_sport_name).text = record.name
        view.findViewById<TextView>(R.id.detail_tv_sport_time).text = record.time
        view.findViewById<TextView>(R.id.detail_tv_duration).text = record.duration
        view.findViewById<TextView>(R.id.detail_tv_distance).text = record.distance
        view.findViewById<TextView>(R.id.detail_tv_avg_speed).text = record.avgSpeed
        view.findViewById<TextView>(R.id.detail_tv_pace).text = record.pace
        view.findViewById<TextView>(R.id.detail_tv_calories).text = record.calories

        dialog.setContentView(view)
        dialog.show()
    }
}

// ============================================================
// 数据模型
// ============================================================
data class SportRecord(
    val iconRes: Int,
    val name: String,
    val time: String,
    val summary: String,
    val duration: String,
    val distance: String,
    val avgSpeed: String,
    val pace: String,
    val calories: String
)

// ============================================================
// RecyclerView Adapter
// ============================================================
class SportRecordAdapter(
    private var records: List<SportRecord>,
    private val onItemClick: (SportRecord) -> Unit
) : RecyclerView.Adapter<SportRecordAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivSportType: ImageView = view.findViewById(R.id.iv_sport_type)
        val tvSportName: TextView = view.findViewById(R.id.tv_sport_name)
        val tvSportTime: TextView = view.findViewById(R.id.tv_sport_time)
        val tvSportSummary: TextView = view.findViewById(R.id.tv_sport_summary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sport_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.ivSportType.setImageResource(record.iconRes)
        holder.tvSportName.text = record.name
        holder.tvSportTime.text = record.time
        holder.tvSportSummary.text = record.summary

        holder.itemView.setOnClickListener {
            onItemClick(record)
        }
    }

    override fun getItemCount(): Int = records.size

    fun updateData(newRecords: List<SportRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}