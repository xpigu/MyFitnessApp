package com.example.myfitnessapp.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myfitnessapp.CurrentAccount
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.entity.DailyCheckin
import com.example.myfitnessapp.data.repository.DailyCheckinRepository
import kotlinx.coroutines.launch

class DailyCheckinViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DailyCheckinRepository
    private val currentUsername = CurrentAccount.requireUsername(application)

    val allCheckins: LiveData<List<DailyCheckin>>
    val totalCheckinCount: LiveData<Int>

    private val _isCheckedInToday = MutableLiveData<Boolean>()
    val isCheckedInToday: LiveData<Boolean> = _isCheckedInToday

    private val _currentStreak = MutableLiveData<Int>()
    val currentStreak: LiveData<Int> = _currentStreak

    init {
        val dao = AppDatabase.getInstance(application).dailyCheckinDao()
        repository = DailyCheckinRepository(dao, currentUsername)
        allCheckins = repository.allCheckins
        totalCheckinCount = repository.totalCheckinCount

        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _isCheckedInToday.value = repository.isCheckedInToday()
            _currentStreak.value = repository.getCurrentStreak()
        }
    }

    fun checkinToday(onSuccess: (Int) -> Unit, onAlreadyCheckedIn: () -> Unit) {
        viewModelScope.launch {
            val success = repository.checkinToday()
            if (success) {
                refreshStatus()
                val streak = repository.getCurrentStreak()
                onSuccess(streak)
            } else {
                onAlreadyCheckedIn()
            }
        }
    }
}
