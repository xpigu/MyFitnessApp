package com.example.myfitnessapp.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.myfitnessapp.CurrentAccount
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.entity.CustomFood
import com.example.myfitnessapp.data.entity.DietRecord
import com.example.myfitnessapp.data.entity.FavoriteMealCombo
import com.example.myfitnessapp.data.repository.CustomFoodRepository
import com.example.myfitnessapp.data.repository.DietRecordRepository
import com.example.myfitnessapp.data.repository.FavoriteMealComboRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DietRecordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DietRecordRepository
    private val customFoodRepository: CustomFoodRepository
    private val favoriteMealComboRepository: FavoriteMealComboRepository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val currentUsername = CurrentAccount.requireUsername(application)
    private var todayRecordsSource: LiveData<List<DietRecord>>? = null
    private var todayRecordsObserver: Observer<List<DietRecord>>? = null

    /** 所有膳食记录，按时间倒序 */
    val allRecords: LiveData<List<DietRecord>>

    /** 自定义食物 */
    val customFoods: LiveData<List<CustomFood>>

    /** 常用组合 */
    val favoriteCombos: LiveData<List<FavoriteMealCombo>>

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
        val database = AppDatabase.getInstance(application)
        repository = DietRecordRepository(database.dietRecordDao(), currentUsername)
        customFoodRepository = CustomFoodRepository(database.customFoodDao(), currentUsername)
        favoriteMealComboRepository = FavoriteMealComboRepository(database.favoriteMealComboDao(), currentUsername)
        allRecords = repository.allRecords
        customFoods = customFoodRepository.allFoods
        favoriteCombos = favoriteMealComboRepository.allCombos

        // 初始化水分摄入计数（从 SharedPreferences 读取或默认为 0）
        val prefs = application.getSharedPreferences("fitness_prefs", 0)
        _waterCount.value = prefs.getInt(waterCountKey(todayDate()), 0)

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

    fun addCustomFood(
        name: String,
        caloriesPer100g: Int,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        mealType: String
    ) {
        val food = CustomFood(
            name = name,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            mealType = mealType
        )
        viewModelScope.launch {
            customFoodRepository.insert(food)
        }
    }

    fun addFavoriteCombo(
        name: String,
        mealType: String,
        itemsPayload: String,
        totalCalories: Int
    ) {
        val combo = FavoriteMealCombo(
            name = name,
            mealType = mealType,
            itemsPayload = itemsPayload,
            totalCalories = totalCalories
        )
        viewModelScope.launch {
            favoriteMealComboRepository.insert(combo)
        }
    }

    /** 增加饮水计数 */
    fun addWater() {
        val current = _waterCount.value ?: 0
        _waterCount.value = current + 1
        // 保存到 SharedPreferences
        val prefs = getApplication<Application>().getSharedPreferences("fitness_prefs", 0)
        prefs.edit().putInt(waterCountKey(todayDate()), current + 1).apply()
    }

    /** 重置每日饮水（每天午夜自动调用），并确保当前账号的水计数正确*/
    fun resetDailyWaterIfNeeded() {
        val prefs = getApplication<Application>().getSharedPreferences("fitness_prefs", 0)
        val lastDate = prefs.getString(lastWaterResetKey(), "")
        val today = todayDate()
        val todayWaterKey = waterCountKey(today)
        
        if (lastDate != today) {
            _waterCount.value = 0
            prefs.edit()
                .putInt(todayWaterKey, 0)
                .putString(lastWaterResetKey(), today)
                .apply()
        } else {
            // 如果日期相同，确保加载的是当前账号正确的水计数值
            val currentWaterCount = prefs.getInt(todayWaterKey, 0)
            _waterCount.value = currentWaterCount
        }
    }

    /** 刷新今日数据 */
    private fun refreshTodayData() {
        viewModelScope.launch {
            val today = todayDate()
            val totalCal = repository.getTotalCaloriesByDate(today)
            _todayTotalCalories.value = totalCal

            val previousSource = todayRecordsSource
            val previousObserver = todayRecordsObserver
            if (previousSource != null && previousObserver != null) {
                previousSource.removeObserver(previousObserver)
            }
            val source = repository.getRecordsByDate(today)
            val observer = Observer<List<DietRecord>> { records ->
                _todayRecords.value = records
            }
            todayRecordsSource = source
            todayRecordsObserver = observer
            source.observeForever(observer)
        }
    }

    override fun onCleared() {
        val source = todayRecordsSource
        val observer = todayRecordsObserver
        if (source != null && observer != null) {
            source.removeObserver(observer)
        }
        super.onCleared()
    }

    private fun waterCountKey(date: String): String = "water_count_${currentUsername}_$date"

    private fun lastWaterResetKey(): String = "last_water_reset_date_$currentUsername"

    companion object {
        fun todayDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
