package com.example.myfitnessapp.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.entity.UserProfile
import com.example.myfitnessapp.data.repository.UserProfileRepository
import kotlinx.coroutines.launch

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserProfileRepository

    val userProfile: LiveData<UserProfile>

    private val _editingProfile = MutableLiveData<UserProfile>()
    val editingProfile: LiveData<UserProfile> = _editingProfile

    init {
        val dao = AppDatabase.getInstance(application).userProfileDao()
        repository = UserProfileRepository(dao)
        userProfile = repository.getUserProfile()

        // 初始化：如果数据库中没有用户资料，则插入默认值
        viewModelScope.launch {
            val existing = repository.getUserProfileSync()
            if (existing == null) {
                val defaultProfile = UserProfile()
                repository.insertUserProfile(defaultProfile)
            }
        }
    }

    /** 更新用户资料 */
    fun updateUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            val updated = profile.copy(lastUpdated = System.currentTimeMillis())
            repository.updateUserProfile(updated)
        }
    }

    /** 设置编辑中的资料 */
    fun setEditingProfile(profile: UserProfile) {
        _editingProfile.value = profile
    }

    /** 提交编辑 */
    fun submitEdit(profile: UserProfile) {
        updateUserProfile(profile)
    }
}
