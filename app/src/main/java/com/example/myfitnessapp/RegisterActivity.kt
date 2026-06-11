package com.example.myfitnessapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.CheckBox
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var authService: AuthService
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var usernameHelper: TextView
    private lateinit var passwordHelper: TextView
    private lateinit var confirmPasswordHelper: TextView
    private lateinit var showPasswordCheckbox: CheckBox
    private lateinit var registerButton: Button
    private lateinit var loginAction: TextView
    private lateinit var phoneLoginAction: TextView
    private var isSubmitting = false
    private val allowWhileLoggedIn by lazy { intent.getBooleanExtra(EXTRA_ALLOW_WHILE_LOGGED_IN, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AuthSessionManager.isLoggedIn(this) && !allowWhileLoggedIn) {
            openMain()
            return
        }

        setContentView(R.layout.activity_register)
        authService = AuthService(this)
        handlePendingFeedback()

        usernameInput = findViewById(R.id.et_register_username)
        passwordInput = findViewById(R.id.et_register_password)
        confirmPasswordInput = findViewById(R.id.et_register_confirm_password)
        usernameHelper = findViewById(R.id.tv_register_username_helper)
        passwordHelper = findViewById(R.id.tv_register_password_helper)
        confirmPasswordHelper = findViewById(R.id.tv_register_confirm_password_helper)
        showPasswordCheckbox = findViewById(R.id.cb_register_show_password)
        registerButton = findViewById(R.id.btn_register_submit)
        loginAction = findViewById(R.id.tv_go_login)
        phoneLoginAction = findViewById(R.id.tv_go_phone_register)

        setupValidation()
        updatePasswordVisibility(showPasswordCheckbox.isChecked)

        registerButton.setOnClickListener {
            val validation = buildValidationState()
            if (!validation.canSubmit) {
                applyValidationState(validation)
                focusFirstInvalidField(validation)
                return@setOnClickListener
            }
            setLoading(true)
            lifecycleScope.launch {
                val result = authService.register(
                    username = usernameInput.text.toString(),
                    password = passwordInput.text.toString(),
                    confirmPassword = confirmPasswordInput.text.toString()
                )
                setLoading(false)
                if (result.success) {
                    openMain(result.message, FeedbackType.SUCCESS)
                } else {
                    applyServerValidation(result.message)
                    showAppFeedback(result.message, FeedbackType.WARNING)
                }
            }
        }

        loginAction.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        phoneLoginAction.setOnClickListener {
            startActivity(
                Intent(this, PhoneAuthActivity::class.java).apply {
                    putExtra(EXTRA_ALLOW_WHILE_LOGGED_IN, allowWhileLoggedIn)
                }
            )
            finish()
        }
    }

    private fun setupValidation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                applyValidationState(buildValidationState())
            }
        }
        usernameInput.addTextChangedListener(watcher)
        passwordInput.addTextChangedListener(watcher)
        confirmPasswordInput.addTextChangedListener(watcher)
        showPasswordCheckbox.setOnCheckedChangeListener { _, isChecked ->
            updatePasswordVisibility(isChecked)
        }
        applyValidationState(buildValidationState())
    }

    private fun buildValidationState(): RegisterValidationState {
        val username = usernameInput.text.toString()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()
        val usernameError = validateUsername(username.trim())
        val passwordError = when {
            password.isBlank() -> "请输入密码"
            password.length < 6 -> "密码至少需要 6 位"
            else -> null
        }
        val confirmPasswordError = when {
            confirmPassword.isBlank() -> "请再次输入密码"
            confirmPassword != password -> "两次输入的密码不一致"
            else -> null
        }
        return RegisterValidationState(
            username = username,
            password = password,
            confirmPassword = confirmPassword,
            usernameError = usernameError,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError
        )
    }

    private fun applyValidationState(state: RegisterValidationState) {
        when {
            state.username.isBlank() -> setHelperMessage(
                usernameHelper,
                getString(R.string.auth_register_username_helper_default),
                HelperTone.NEUTRAL
            )
            state.usernameError != null -> setHelperMessage(usernameHelper, state.usernameError, HelperTone.ERROR)
            else -> setHelperMessage(
                usernameHelper,
                getString(R.string.auth_register_username_valid),
                HelperTone.SUCCESS
            )
        }

        when {
            state.password.isBlank() -> setHelperMessage(
                passwordHelper,
                getString(R.string.auth_register_password_helper_default),
                HelperTone.NEUTRAL
            )
            state.passwordError != null -> setHelperMessage(passwordHelper, state.passwordError, HelperTone.ERROR)
            else -> setHelperMessage(passwordHelper, passwordStrengthMessage(state.password), passwordStrengthTone(state.password))
        }

        when {
            state.confirmPassword.isBlank() -> setHelperMessage(
                confirmPasswordHelper,
                getString(R.string.auth_register_confirm_password_helper_default),
                HelperTone.NEUTRAL
            )
            state.confirmPasswordError != null -> setHelperMessage(
                confirmPasswordHelper,
                state.confirmPasswordError,
                HelperTone.ERROR
            )
            else -> setHelperMessage(
                confirmPasswordHelper,
                getString(R.string.auth_register_confirm_password_match),
                HelperTone.SUCCESS
            )
        }

        registerButton.isEnabled = state.canSubmit && !isSubmitting
        registerButton.alpha = if (registerButton.isEnabled) 1f else 0.65f
    }

    private fun applyServerValidation(message: String) {
        when {
            message.contains("用户名") -> setHelperMessage(usernameHelper, message, HelperTone.ERROR)
            message.contains("密码") -> setHelperMessage(passwordHelper, message, HelperTone.ERROR)
        }
    }

    private fun focusFirstInvalidField(state: RegisterValidationState) {
        when {
            state.usernameError != null -> usernameInput.requestFocus()
            state.passwordError != null -> passwordInput.requestFocus()
            state.confirmPasswordError != null -> confirmPasswordInput.requestFocus()
        }
    }

    private fun updatePasswordVisibility(isVisible: Boolean) {
        val selectionStart = passwordInput.selectionStart
        val confirmSelectionStart = confirmPasswordInput.selectionStart
        val method = if (isVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        passwordInput.transformationMethod = method
        confirmPasswordInput.transformationMethod = method
        passwordInput.setSelection(selectionStart.coerceAtLeast(0).coerceAtMost(passwordInput.text?.length ?: 0))
        confirmPasswordInput.setSelection(
            confirmSelectionStart.coerceAtLeast(0).coerceAtMost(confirmPasswordInput.text?.length ?: 0)
        )
    }

    private fun setHelperMessage(view: TextView, message: String, tone: HelperTone) {
        view.text = message
        view.setTextColor(
            getColor(
                when (tone) {
                    HelperTone.NEUTRAL -> R.color.text_secondary
                    HelperTone.SUCCESS -> R.color.emerald_dark
                    HelperTone.WARNING -> R.color.gold_dark
                    HelperTone.ERROR -> R.color.reminder_delete_text
                }
            )
        )
    }

    private fun passwordStrengthMessage(password: String): String {
        return when (passwordStrength(password)) {
            PasswordStrength.WEAK -> getString(R.string.auth_register_password_strength_weak)
            PasswordStrength.MEDIUM -> getString(R.string.auth_register_password_strength_medium)
            PasswordStrength.STRONG -> getString(R.string.auth_register_password_strength_strong)
        }
    }

    private fun passwordStrengthTone(password: String): HelperTone {
        return when (passwordStrength(password)) {
            PasswordStrength.WEAK -> HelperTone.ERROR
            PasswordStrength.MEDIUM -> HelperTone.WARNING
            PasswordStrength.STRONG -> HelperTone.SUCCESS
        }
    }

    private fun passwordStrength(password: String): PasswordStrength {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        return when {
            password.length >= 10 && hasLetter && hasDigit && hasSymbol -> PasswordStrength.STRONG
            password.length >= 8 && hasLetter && hasDigit -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
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

    private fun setLoading(isLoading: Boolean) {
        isSubmitting = isLoading
        registerButton.isEnabled = buildValidationState().canSubmit && !isLoading
        registerButton.alpha = if (registerButton.isEnabled) 1f else 0.65f
        loginAction.isEnabled = !isLoading
        phoneLoginAction.isEnabled = !isLoading
        showPasswordCheckbox.isEnabled = !isLoading
    }

    private fun handlePendingFeedback() {
        val message = intent.getStringExtra(MainActivity.EXTRA_PENDING_FEEDBACK_MESSAGE).orEmpty()
        if (message.isBlank()) return

        val type = intent.getStringExtra(MainActivity.EXTRA_PENDING_FEEDBACK_TYPE)
            ?.let { runCatching { FeedbackType.valueOf(it) }.getOrNull() }
            ?: FeedbackType.INFO

        showAppFeedback(message, type)
        intent.removeExtra(MainActivity.EXTRA_PENDING_FEEDBACK_MESSAGE)
        intent.removeExtra(MainActivity.EXTRA_PENDING_FEEDBACK_TYPE)
    }

    private fun openMain(
        feedbackMessage: String? = null,
        feedbackType: FeedbackType? = null
    ) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                if (!feedbackMessage.isNullOrBlank() && feedbackType != null) {
                    putExtra(MainActivity.EXTRA_PENDING_FEEDBACK_MESSAGE, feedbackMessage)
                    putExtra(MainActivity.EXTRA_PENDING_FEEDBACK_TYPE, feedbackType.name)
                }
            }
        )
        finish()
    }

    private data class RegisterValidationState(
        val username: String,
        val password: String,
        val confirmPassword: String,
        val usernameError: String?,
        val passwordError: String?,
        val confirmPasswordError: String?
    ) {
        val canSubmit: Boolean
            get() = usernameError == null && passwordError == null && confirmPasswordError == null
    }

    private enum class HelperTone {
        NEUTRAL,
        SUCCESS,
        WARNING,
        ERROR
    }

    private enum class PasswordStrength {
        WEAK,
        MEDIUM,
        STRONG
    }
}
