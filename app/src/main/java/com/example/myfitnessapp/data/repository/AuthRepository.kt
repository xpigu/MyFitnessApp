package com.example.myfitnessapp.data.repository

import com.example.myfitnessapp.data.dao.AuthAccountDao
import com.example.myfitnessapp.data.entity.AuthAccount

class AuthRepository(private val dao: AuthAccountDao) {

    suspend fun insertAccount(account: AuthAccount): Long = dao.insert(account)

    suspend fun getByUsername(username: String): AuthAccount? = dao.getByUsername(username)

    suspend fun getByPhone(phone: String): AuthAccount? = dao.getByPhone(phone)

    suspend fun countAccounts(): Int = dao.countAccounts()

    suspend fun getAllAccounts(): List<AuthAccount> = dao.getAllAccounts()

    suspend fun updateLastLogin(accountId: Long, lastLoginAt: Long) {
        dao.updateLastLogin(accountId, lastLoginAt)
    }

    suspend fun updatePassword(username: String, passwordHash: String) {
        dao.updatePassword(username, passwordHash)
    }

    suspend fun deleteByUsername(username: String) {
        dao.deleteByUsername(username)
    }
}
