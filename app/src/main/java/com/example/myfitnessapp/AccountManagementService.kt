package com.example.myfitnessapp

import android.content.Context
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.entity.AuthAccount
import com.example.myfitnessapp.data.repository.AuthRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AccountListItem(
    val username: String,
    val isCurrent: Boolean,
    val createdAtLabel: String,
    val lastLoginAtLabel: String
)

data class AccountOperationResult(
    val success: Boolean,
    val message: String,
    val nextUsername: String? = null,
    val requiresAuthRedirect: Boolean = false
)

class AccountManagementService(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val authRepository = AuthRepository(database.authAccountDao())

    suspend fun getAccounts(): List<AccountListItem> {
        val currentUsername = AuthSessionManager.getUsername(appContext)
        return authRepository.getAllAccounts().map { account ->
            account.toListItem(currentUsername)
        }
    }

    suspend fun switchAccount(username: String, password: String): AccountOperationResult {
        val normalizedUsername = username.trim()
        if (password.isBlank()) {
            return AccountOperationResult(false, "请输入 ${normalizedUsername} 的密码")
        }

        val target = authRepository.getByUsername(normalizedUsername)
            ?: return AccountOperationResult(false, "目标账号不存在")

        if (target.passwordHash != PasswordHasher.sha256(password)) {
            return AccountOperationResult(false, "密码错误，请重新输入")
        }

        authRepository.updateLastLogin(target.id, System.currentTimeMillis())
        AuthSessionManager.saveLogin(appContext, target.username)
        return AccountOperationResult(true, "已切换到 ${target.username}", nextUsername = target.username)
    }

    suspend fun changePassword(
        username: String,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): AccountOperationResult {
        val account = authRepository.getByUsername(username)
            ?: return AccountOperationResult(false, "当前账号不存在")

        if (oldPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            return AccountOperationResult(false, "请完整填写密码信息")
        }
        if (PasswordHasher.sha256(oldPassword) != account.passwordHash) {
            return AccountOperationResult(false, "原密码不正确")
        }
        if (newPassword.length < 6) {
            return AccountOperationResult(false, "新密码至少需要 6 位")
        }
        if (newPassword != confirmPassword) {
            return AccountOperationResult(false, "两次输入的新密码不一致")
        }
        if (oldPassword == newPassword) {
            return AccountOperationResult(false, "新密码不能与原密码相同")
        }

        authRepository.updatePassword(username, PasswordHasher.sha256(newPassword))
        return AccountOperationResult(true, "密码已更新")
    }

    suspend fun deleteAccount(username: String): AccountOperationResult {
        val existingAccount = authRepository.getByUsername(username)
            ?: return AccountOperationResult(false, "账号不存在或已被删除")

        database.workoutRecordDao().deleteByOwnerUsername(username)
        database.dietRecordDao().deleteByOwnerUsername(username)
        database.dailyCheckinDao().deleteByOwnerUsername(username)
        database.achievementBadgeDao().deleteByOwnerUsername(username)
        database.customFoodDao().deleteByOwnerUsername(username)
        database.favoriteMealComboDao().deleteByOwnerUsername(username)
        database.userProfileDao().deleteByAccountUsername(username)
        authRepository.deleteByUsername(username)
        cleanupAccountPrefs(username)

        val remainingAccounts = authRepository.getAllAccounts()
        val currentUsername = AuthSessionManager.getUsername(appContext)
        if (currentUsername != username) {
            return AccountOperationResult(true, "账号 $username 已删除")
        }

        if (remainingAccounts.isEmpty()) {
            AuthSessionManager.clearSession(appContext)
            return AccountOperationResult(
                success = true,
                message = "当前账号已删除，请重新注册或登录",
                requiresAuthRedirect = true
            )
        }

        val nextAccount = remainingAccounts.first()
        AuthSessionManager.saveLogin(appContext, nextAccount.username)
        return AccountOperationResult(
            success = true,
            message = "已删除当前账号，已切换到 ${nextAccount.username}",
            nextUsername = nextAccount.username
        )
    }

    private fun AuthAccount.toListItem(currentUsername: String): AccountListItem {
        return AccountListItem(
            username = username,
            isCurrent = username == currentUsername,
            createdAtLabel = formatTime(createdAt),
            lastLoginAtLabel = formatTime(lastLoginAt)
        )
    }

    private fun formatTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    private fun cleanupAccountPrefs(username: String) {
        val prefs = appContext.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.startsWith("water_count_${username}_")) {
                editor.remove(key)
            }
        }
        editor.remove("last_water_reset_date_$username")
        editor.remove("diet_goal_type_$username")
        editor.apply()
    }
}
