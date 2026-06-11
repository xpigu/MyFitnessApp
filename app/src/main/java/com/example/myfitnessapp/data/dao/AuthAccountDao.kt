package com.example.myfitnessapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myfitnessapp.data.entity.AuthAccount

@Dao
interface AuthAccountDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AuthAccount): Long

    @Query("SELECT * FROM auth_accounts WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): AuthAccount?

    @Query("SELECT * FROM auth_accounts WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): AuthAccount?

    @Query("SELECT COUNT(*) FROM auth_accounts")
    suspend fun countAccounts(): Int

    @Query("SELECT * FROM auth_accounts ORDER BY last_login_at DESC, created_at DESC")
    suspend fun getAllAccounts(): List<AuthAccount>

    @Query("UPDATE auth_accounts SET last_login_at = :lastLoginAt WHERE id = :accountId")
    suspend fun updateLastLogin(accountId: Long, lastLoginAt: Long)

    @Query("UPDATE auth_accounts SET password_hash = :passwordHash WHERE username = :username")
    suspend fun updatePassword(username: String, passwordHash: String)

    @Query("DELETE FROM auth_accounts WHERE username = :username")
    suspend fun deleteByUsername(username: String)
}
