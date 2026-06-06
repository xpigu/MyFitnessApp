package com.example.myfitnessapp.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myfitnessapp.data.dao.TypeCount
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.entity.WorkoutRecord
import com.example.myfitnessapp.data.repository.WorkoutRecordRepository
import kotlinx.coroutines.launch

class WorkoutRecordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkoutRecordRepository

    /** 所有记录，按时间倒序 */
    val allRecords: LiveData<List<WorkoutRecord>>

    /** 所有有记录的日期，倒序 */
    val allDates: LiveData<List<String>>

    /** 当前汇总数据 */
    private val _summary = MutableLiveData<SummaryData>()
    val summary: LiveData<SummaryData> = _summary

    /** 当前模式下的记录列表（按日期分组后的扁平化列表） */
    private val _groupedRecords = MutableLiveData<List<DateGroup>>()
    val groupedRecords: LiveData<List<DateGroup>> = _groupedRecords

    private var isDayMode = true
    private var currentDate = todayStr()

    init {
        val dao = AppDatabase.getInstance(application).workoutRecordDao()
        repository = WorkoutRecordRepository(dao)
        allRecords = repository.allRecords
        allDates = repository.allDates
        refreshSummary()
    }

    /** 切换日/月模式 */
    fun setDayMode(dayMode: Boolean) {
        isDayMode = dayMode
        refreshSummary()
    }

    /** 设置当前查看的日期 */
    fun setCurrentDate(date: String) {
        currentDate = date
        refreshSummary()
    }

    /** 保存一条运动记录 */
    fun saveRecord(record: WorkoutRecord) {
        viewModelScope.launch {
            repository.insert(record)
            refreshSummary()
        }
    }

    /** 删除一条记录 */
    fun deleteRecord(record: WorkoutRecord) {
        viewModelScope.launch {
            repository.delete(record)
            refreshSummary()
        }
    }

    /** 刷新汇总数据 */
    private fun refreshSummary() {
        viewModelScope.launch {
            if (isDayMode) {
                val date = currentDate
                val count = repository.getWorkoutCountByDate(date)
                val calories = repository.getTotalCaloriesByDate(date)
                val duration = repository.getTotalDurationByDate(date)
                _summary.value = SummaryData(
                    workoutCount = count,
                    totalCalories = calories,
                    totalDurationSeconds = duration
                )
            } else {
                val pattern = currentDate.substring(0, 7) + "%"  // "2026-06%"
                val count = repository.getWorkoutCountByMonth(pattern)
                val calories = repository.getTotalCaloriesByMonth(pattern)
                val duration = repository.getTotalDurationByMonth(pattern)
                _summary.value = SummaryData(
                    workoutCount = count,
                    totalCalories = calories,
                    totalDurationSeconds = duration
                )
            }
        }
    }

    /** 根据日期列表和记录构建分组数据 */
    fun buildGroupedRecords(dates: List<String>, records: List<WorkoutRecord>): List<DateGroup> {
        val grouped = mutableListOf<DateGroup>()
        val recordsByDate = records.groupBy { it.date }

        for (date in dates) {
            val dayRecords = recordsByDate[date] ?: emptyList()
            grouped.add(DateGroup(date, dayRecords))
        }
        return grouped
    }

    // ============================================================
    // 统计方法 — Phase 2 增强
    // ============================================================

    /** 获取最佳成绩 */
    fun getPBRecords(onResult: (PBRecords) -> Unit) {
        viewModelScope.launch {
            val pbRecords = repository.getPBRecords()
            onResult(pbRecords)
        }
    }

    companion object {
        fun todayStr(): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            return sdf.format(java.util.Date())
        }

        fun get30DaysAgoDate(): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_MONTH, -29) // 包含今天是30天
            return sdf.format(calendar.time)
        }

        fun getMonthPattern(date: String = todayStr()): String {
            // 从 "2026-06-05" 获取 "2026-06"
            return date.substring(0, 7)
        }
    }
}

/** 汇总数据 */
data class SummaryData(
    val workoutCount: Int = 0,
    val totalCalories: Int = 0,
    val totalDurationSeconds: Int = 0
) {
    fun formatDuration(): String {
        val h = totalDurationSeconds / 3600
        val m = (totalDurationSeconds % 3600) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}

/** 日期分组 */
data class DateGroup(
    val date: String,       // "2026-06-05"
    val records: List<WorkoutRecord>
)
