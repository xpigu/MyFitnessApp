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

class LoginActivity : AppCompatActivity() {

    private lateinit var authService: AuthService
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var usernameHelper: TextView
    private lateinit var passwordHelper: TextView
    private lateinit var showPasswordCheckbox: CheckBox
    private lateinit var loginButton: Button
    private lateinit var registerAction: TextView
    private lateinit var phoneLoginAction: TextView
    private var isSubmitting = false
    private val allowWhileLoggedIn by lazy { intent.getBooleanExtra(EXTRA_ALLOW_WHILE_LOGGED_IN, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AuthSessionManager.isLoggedIn(this) && !allowWhileLoggedIn) {
            openMain()
            return
        }

        setContentView(R.layout.activity_login)
        authService = AuthService(this)
        handlePendingFeedback()

        usernameInput = findViewById(R.id.et_login_username)
        passwordInput = findViewById(R.id.et_login_password)
        usernameHelper = findViewById(R.id.tv_login_username_helper)
        passwordHelper = findViewById(R.id.tv_login_password_helper)
        showPasswordCheckbox = findViewById(R.id.cb_login_show_password)
        loginButton = findViewById(R.id.btn_login_submit)
        registerAction = findViewById(R.id.tv_go_register)
        phoneLoginAction = findViewById(R.id.tv_go_phone_login)

        val rememberedUsername = AuthSessionManager.getUsername(this)
        if (rememberedUsername.isNotBlank()) {
            usernameInput.setText(rememberedUsername)
            usernameInput.setSelection(rememberedUsername.length)
        }

        setupValidation()
        updatePasswordVisibility(showPasswordCheckbox.isChecked)

        loginButton.setOnClickListener {
            val validation = buildValidationState()
            if (!validation.canSubmit) {
                applyValidationState(validation)
                focusFirstInvalidField(validation)
                return@setOnClickListener
            }
            setLoading(true)
            lifecycleScope.launch {
                val result = authService.login(
                    username = usernameInput.text.toString(),
                    password = passwordInput.text.toString()
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

        registerAction.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
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
        showPasswordCheckbox.setOnCheckedChangeListener { _, isChecked ->
            updatePasswordVisibility(isChecked)
        }
        applyValidationState(buildValidationState())
    }

    private fun buildValidationState(): LoginValidationState {
        val username = usernameInput.text.toString()
        val password = passwordInput.text.toString()
        val usernameError = when {
            username.trim().isBlank() -> "请输入用户名"
            else -> null
        }
        val passwordError = when {
            password.isBlank() -> "请输入密码"
            else -> null
        }
        return LoginValidationState(
            username = username,
            password = password,
            usernameError = usernameError,
            passwordError = passwordError
        )
    }

    private fun applyValidationState(state: LoginValidationState) {
        when {
            state.username.isBlank() -> setHelperMessage(
                usernameHelper,
                getString(R.string.auth_login_username_helper_default),
                HelperTone.NEUTRAL
            )
            state.usernameError != null -> setHelperMessage(usernameHelper, state.usernameError, HelperTone.ERROR)
            else -> setHelperMessage(
                usernameHelper,
                getString(R.string.auth_login_username_valid),
                HelperTone.SUCCESS
            )
        }

        when {
            state.password.isBlank() -> setHelperMessage(
                passwordHelper,
                getString(R.string.auth_login_password_helper_default),
                HelperTone.NEUTRAL
            )
            state.passwordError != null -> setHelperMessage(passwordHelper, state.passwordError, HelperTone.ERROR)
            else -> setHelperMessage(
                passwordHelper,
                getString(R.string.auth_login_password_valid),
                HelperTone.SUCCESS
            )
        }

        loginButton.isEnabled = state.canSubmit && !isSubmitting
        loginButton.alpha = if (loginButton.isEnabled) 1f else 0.65f
    }

    private fun applyServerValidation(message: String) {
        when {
            message.contains("账号") || message.contains("用户名") -> {
                setHelperMessage(usernameHelper, message, HelperTone.ERROR)
            }
            message.contains("密码") -> {
                setHelperMessage(passwordHelper, message, HelperTone.ERROR)
            }
        }
    }

    private fun focusFirstInvalidField(state: LoginValidationState) {
        when {
            state.usernameError != null -> usernameInput.requestFocus()
            state.passwordError != null -> passwordInput.requestFocus()
        }
    }

    private fun updatePasswordVisibility(isVisible: Boolean) {
        val selectionStart = passwordInput.selectionStart
        passwordInput.transformationMethod = if (isVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        passwordInput.setSelection(selectionStart.coerceAtLeast(0).coerceAtMost(passwordInput.text?.length ?: 0))
    }

    private fun setHelperMessage(view: TextView, message: String, tone: HelperTone) {
        view.text = message
        view.setTextColor(
            getColor(
                when (tone) {
                    HelperTone.NEUTRAL -> R.color.text_secondary
                    HelperTone.SUCCESS -> R.color.emerald_dark
                    HelperTone.ERROR -> R.color.reminder_delete_text
                }
            )
        )
    }

    private fun setLoading(isLoading: Boolean) {
        isSubmitting = isLoading
        loginButton.isEnabled = buildValidationState().canSubmit && !isLoading
        loginButton.alpha = if (loginButton.isEnabled) 1f else 0.65f
        registerAction.isEnabled = !isLoading
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

    private data class LoginValidationState(
        val username: String,
        val password: String,
        val usernameError: String?,
        val passwordError: String?
    ) {
        val canSubmit: Boolean
            get() = usernameError == null && passwordError == null
    }

    private enum class HelperTone {
        NEUTRAL,
        SUCCESS,
        ERROR
    }
}
