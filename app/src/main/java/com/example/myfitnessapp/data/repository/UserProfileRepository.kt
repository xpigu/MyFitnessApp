package com.example.myfitnessapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myfitnessapp.data.dao.UserProfileDao
import com.example.myfitnessapp.data.entity.UserProfile

class UserProfileRepository(
    private val dao: UserProfileDao,
    private val accountUsername: String
) {

    fun getUserProfile(): LiveData<UserProfile> = dao.getUserProfile(accountUsername)

    suspend fun updateUserProfile(profile: UserProfile) = dao.update(profile)

    suspend fun insertUserProfile(profile: UserProfile): Long =
        dao.insert(profile.copy(accountUsername = accountUsername))

    suspend fun getUserProfileSync(): UserProfile? = dao.getUserProfileSync(accountUsername)
}
