package com.example.myfitnessapp.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.entity.DietRecord
import com.example.myfitnessapp.data.repository.DietRecordRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DietRecordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DietRecordRepository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 所有膳食记录，按时间倒序 */
    val allRecords: LiveData<List<DietRecord>>

    /** 当前日期的总摄入卡路里 */
    private val _todayTotalCalories = MutableLiveData<Int>()
    val todayTotalCalories: LiveData<Int> = _todayTotalCalories

    /** 当前日期的膳食记录 */
    private val _todayRecords = MutableLiveData<List<DietRecord>>()
    val todayRecords: LiveData<List<DietRecord>> = _todayRecords

    /** 水分摄入计数 */
    private val _waterCount = MutableLiveData<Int>()
    val waterCount: LiveData<Int> = _waterCount

    init {
        val dao = AppDatabase.getInstance(application).dietRecordDao()
        repository = DietRecordRepository(dao)
        allRecords = repository.allRecords

        // 初始化水分摄入计数（从 SharedPreferences 读取或默认为 0）
        val prefs = application.getSharedPreferences("fitness_prefs", 0)
        _waterCount.value = prefs.getInt("water_count_${todayDate()}", 0)

        refreshTodayData()
    }

    /** 添加一条膳食记录 */
    fun addDietRecord(mealType: String, foodName: String, calories: Int, notes: String = "") {
        val record = DietRecord(
            mealType = mealType,
            foodName = foodName,
            calories = calories,
            timestamp = System.currentTimeMillis(),
            date = todayDate(),
            notes = notes
        )
        viewModelScope.launch {
            repository.insert(record)
            refreshTodayData()
        }
    }

    /** 删除一条膳食记录 */
    fun deleteDietRecord(record: DietRecord) {
        viewModelScope.launch {
            repository.delete(record)
            refreshTodayData()
        }
    }

    /** 增加饮水计数 */
    fun addWater() {
        val current = _waterCount.value ?: 0
        _waterCount.value = current + 1
        // 保存到 SharedPreferences
        val prefs = getApplication<Application>().getSharedPreferences("fitness_prefs", 0)
        prefs.edit().putInt("water_count_${todayDate()}", current + 1).apply()
    }

    /** 重置每日饮水（每天午夜自动调用）*/
    fun resetDailyWaterIfNeeded() {
        val prefs = getApplication<Application>().getSharedPreferences("fitness_prefs", 0)
        val lastDate = prefs.getString("last_water_reset_date", "")
        val today = todayDate()
        if (lastDate != today) {
            _waterCount.value = 0
            prefs.edit()
                .putInt("water_count_$today", 0)
                .putString("last_water_reset_date", today)
                .apply()
        }
    }

    /** 刷新今日数据 */
    private fun refreshTodayData() {
        viewModelScope.launch {
            val today = todayDate()
            val totalCal = repository.getTotalCaloriesByDate(today)
            _todayTotalCalories.value = totalCal

            // 获取今天的所有记录
            repository.getRecordsByDate(today).observeForever { records ->
                _todayRecords.value = records
            }
        }
    }

    companion object {
        fun todayDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
