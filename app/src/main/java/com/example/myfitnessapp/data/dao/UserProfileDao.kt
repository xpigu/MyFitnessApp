package com.example.myfitnessapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myfitnessapp.data.entity.UserProfile

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfile): Long

    @Update
    suspend fun update(profile: UserProfile)

    @Query("SELECT * FROM user_profiles WHERE account_username = :accountUsername LIMIT 1")
    fun getUserProfile(accountUsername: String): LiveData<UserProfile>

    @Query("SELECT * FROM user_profiles WHERE account_username = :accountUsername LIMIT 1")
    suspend fun getUserProfileSync(accountUsername: String): UserProfile?

    @Query("DELETE FROM user_profiles WHERE account_username = :accountUsername")
    suspend fun deleteByAccountUsername(accountUsername: String)
}
