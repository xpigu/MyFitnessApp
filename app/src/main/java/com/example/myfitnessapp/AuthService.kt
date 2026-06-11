package com.example.myfitnessapp

import android.content.Context
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.entity.AuthAccount
import com.example.myfitnessapp.data.entity.UserProfile
import com.example.myfitnessapp.data.repository.AuthRepository
import com.example.myfitnessapp.data.repository.UserProfileRepository

data class AuthActionResult(
    val success: Boolean,
    val message: String
)

data class PhoneCodeResult(
    val success: Boolean,
    val message: String,
    val code: String? = null
)

class AuthService(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val authRepository = AuthRepository(database.authAccountDao())

    suspend fun hasAnyAccount(): Boolean = authRepository.countAccounts() > 0

    suspend fun register(username: String, password: String, confirmPassword: String): AuthActionResult {
        val normalizedUsername = username.trim()
        val usernameError = validateUsername(normalizedUsername)
        if (usernameError != null) {
            return AuthActionResult(success = false, message = usernameError)
        }

        val passwordError = validatePassword(password, confirmPassword)
        if (passwordError != null) {
            return AuthActionResult(success = false, message = passwordError)
        }

        if (authRepository.getByUsername(normalizedUsername) != null) {
            return AuthActionResult(success = false, message = "该用户名已存在，请更换后重试")
        }

        val now = System.currentTimeMillis()
        authRepository.insertAccount(
            AuthAccount(
                username = normalizedUsername,
                passwordHash = PasswordHasher.sha256(password),
                createdAt = now,
                lastLoginAt = now
            )
        )
        syncProfileUsername(normalizedUsername)
        AuthSessionManager.saveLogin(appContext, normalizedUsername)
        return AuthActionResult(success = true, message = "注册成功")
    }

    suspend fun login(username: String, password: String): AuthActionResult {
        val normalizedUsername = username.trim()
        if (normalizedUsername.isBlank() || password.isBlank()) {
            return AuthActionResult(success = false, message = "请输入用户名和密码")
        }

        val account = authRepository.getByUsername(normalizedUsername)
            ?: return AuthActionResult(success = false, message = "账号不存在，请先注册")

        if (account.passwordHash != PasswordHasher.sha256(password)) {
            return AuthActionResult(success = false, message = "密码错误，请重新输入")
        }

        authRepository.updateLastLogin(account.id, System.currentTimeMillis())
        syncProfileUsername(account.username)
        AuthSessionManager.saveLogin(appContext, account.username)
        return AuthActionResult(success = true, message = "登录成功")
    }

    suspend fun sendPhoneCode(phone: String): PhoneCodeResult {
        val normalizedPhone = phone.trim()
        val phoneError = validatePhone(normalizedPhone)
        if (phoneError != null) {
            return PhoneCodeResult(success = false, message = phoneError)
        }

        val code = (100000..999999).random().toString()
        return PhoneCodeResult(
            success = true,
            message = "验证码已发送（模拟）：$code",
            code = code
        )
    }

    suspend fun loginOrRegisterByPhone(phone: String, code: String, inputCode: String): AuthActionResult {
        val normalizedPhone = phone.trim()
        if (code != inputCode) {
            return AuthActionResult(success = false, message = "验证码错误，请重新输入")
        }

        val existingAccount = authRepository.getByPhone(normalizedPhone)
        if (existingAccount != null) {
            authRepository.updateLastLogin(existingAccount.id, System.currentTimeMillis())
            syncProfileUsername(existingAccount.username)
            AuthSessionManager.saveLogin(appContext, existingAccount.username)
            return AuthActionResult(success = true, message = "登录成功")
        }

        val username = "用户${normalizedPhone.takeLast(4)}"
        val now = System.currentTimeMillis()
        val newAccount = AuthAccount(
            username = username,
            passwordHash = PasswordHasher.sha256(normalizedPhone),
            phone = normalizedPhone,
            createdAt = now,
            lastLoginAt = now
        )
        authRepository.insertAccount(newAccount)
        syncProfileUsername(username)
        AuthSessionManager.saveLogin(appContext, username)
        return AuthActionResult(success = true, message = "注册并登录成功")
    }

    private suspend fun syncProfileUsername(username: String) {
        val userProfileRepository = UserProfileRepository(database.userProfileDao(), username)
        val existingProfile = userProfileRepository.getUserProfileSync()
        if (existingProfile == null) {
            userProfileRepository.insertUserProfile(
                UserProfile(
                    accountUsername = username,
                    username = username
                )
            )
            return
        }
    }

    private fun validateUsername(username: String): String? {
        return when {
            username.isBlank() -> "请输入用户名"
            username.length < 3 -> "用户名至少需要 3 个字符"
            username.length > 20 -> "用户名最多 20 个字符"
            !Regex("^[A-Za-z0-9_\\u4e00-\\u9fa5]+$").matches(username) ->
                "用户名仅支持中文、字母、数字和下划线"
            else -> null
        }
    }

    private fun validatePassword(password: String, confirmPassword: String): String? {
        return when {
            password.isBlank() || confirmPassword.isBlank() -> "请输入完整密码信息"
            password.length < 6 -> "密码至少需要 6 位"
            password != confirmPassword -> "两次输入的密码不一致"
            else -> null
        }
    }

    private fun validatePhone(phone: String): String? {
        return when {
            phone.isBlank() -> "请输入手机号"
            !Regex("^1[3-9]\\d{9}$").matches(phone) ->
                "请输入正确的手机号"
            else -> null
        }
    }
}
