package com.example.myfitnessapp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PhoneAuthActivity : AppCompatActivity() {

    private lateinit var authService: AuthService
    private lateinit var phoneInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var phoneHelper: TextView
    private lateinit var codeHelper: TextView
    private lateinit var sendCodeButton: TextView
    private lateinit var submitButton: Button
    private lateinit var passwordLoginAction: TextView
    private var currentCode: String? = null
    private var countDownTimer: CountDownTimer? = null
    private var isSubmitting = false
    private var isCountingDown = false
    private val allowWhileLoggedIn by lazy { intent.getBooleanExtra(EXTRA_ALLOW_WHILE_LOGGED_IN, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AuthSessionManager.isLoggedIn(this) && !allowWhileLoggedIn) {
            openMain()
            return
        }

        setContentView(R.layout.activity_phone_auth)
        authService = AuthService(this)

        phoneInput = findViewById(R.id.et_phone)
        codeInput = findViewById(R.id.et_code)
        phoneHelper = findViewById(R.id.tv_phone_helper)
        codeHelper = findViewById(R.id.tv_code_helper)
        sendCodeButton = findViewById(R.id.tv_send_code)
        submitButton = findViewById(R.id.btn_phone_submit)
        passwordLoginAction = findViewById(R.id.tv_go_password_login)

        setupValidation()

        sendCodeButton.setOnClickListener {
            val validation = buildValidationState()
            if (validation.phoneError != null) {
                applyValidationState(validation)
                phoneInput.requestFocus()
                return@setOnClickListener
            }
            val phone = validation.phone.trim()
            lifecycleScope.launch {
                val result = authService.sendPhoneCode(phone)
                if (result.success) {
                    currentCode = result.code
                    setHelperMessage(phoneHelper, getString(R.string.auth_phone_valid), HelperTone.SUCCESS)
                    setHelperMessage(codeHelper, getString(R.string.auth_code_sent), HelperTone.SUCCESS)
                    startCountDown(sendCodeButton)
                    result.code?.let { showVerificationCodeDialog(it) }
                } else {
                    setHelperMessage(phoneHelper, result.message, HelperTone.ERROR)
                }
            }
        }

        submitButton.setOnClickListener {
            val validation = buildValidationState()
            if (!validation.canSubmit) {
                applyValidationState(validation)
                focusFirstInvalidField(validation)
                return@setOnClickListener
            }
            val phone = validation.phone.trim()
            val inputCode = validation.code.trim()
            val code = currentCode

            if (code.isNullOrBlank()) {
                setHelperMessage(codeHelper, "请先获取验证码", HelperTone.ERROR)
                showAppFeedback("请先获取验证码", FeedbackType.WARNING)
                return@setOnClickListener
            }

            setLoading(true)
            lifecycleScope.launch {
                val result = authService.loginOrRegisterByPhone(phone, code, inputCode)
                setLoading(false)
                if (result.success) {
                    openMain(result.message, FeedbackType.SUCCESS)
                } else {
                    applyServerValidation(result.message)
                    showAppFeedback(result.message, FeedbackType.WARNING)
                }
            }
        }

        passwordLoginAction.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
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
        phoneInput.addTextChangedListener(watcher)
        codeInput.addTextChangedListener(watcher)
        applyValidationState(buildValidationState())
    }

    private fun buildValidationState(): PhoneAuthValidationState {
        val phone = phoneInput.text.toString()
        val code = codeInput.text.toString()
        val phoneError = when {
            phone.trim().isBlank() -> "请输入手机号"
            !Regex("^1[3-9]\\d{9}$").matches(phone.trim()) -> "请输入正确的手机号"
            else -> null
        }
        val codeError = when {
            code.isBlank() -> "请输入验证码"
            !Regex("^\\d{6}$").matches(code.trim()) -> "请输入 6 位验证码"
            else -> null
        }
        return PhoneAuthValidationState(
            phone = phone,
            code = code,
            phoneError = phoneError,
            codeError = codeError
        )
    }

    private fun applyValidationState(state: PhoneAuthValidationState) {
        when {
            state.phone.isBlank() -> setHelperMessage(
                phoneHelper,
                getString(R.string.auth_phone_helper_default),
                HelperTone.NEUTRAL
            )
            state.phoneError != null -> setHelperMessage(phoneHelper, state.phoneError, HelperTone.ERROR)
            else -> setHelperMessage(phoneHelper, getString(R.string.auth_phone_valid), HelperTone.SUCCESS)
        }

        when {
            currentCode != null && state.code.isBlank() -> setHelperMessage(
                codeHelper,
                getString(R.string.auth_code_sent),
                HelperTone.SUCCESS
            )
            state.code.isBlank() -> setHelperMessage(
                codeHelper,
                getString(R.string.auth_code_helper_default),
                HelperTone.NEUTRAL
            )
            state.codeError != null -> setHelperMessage(codeHelper, state.codeError, HelperTone.ERROR)
            else -> setHelperMessage(codeHelper, getString(R.string.auth_code_valid), HelperTone.SUCCESS)
        }

        sendCodeButton.isEnabled = state.phoneError == null && !isCountingDown
        sendCodeButton.alpha = if (sendCodeButton.isEnabled) 1f else 0.65f
        submitButton.isEnabled = state.canSubmit && currentCode != null && !isSubmitting
        submitButton.alpha = if (submitButton.isEnabled) 1f else 0.65f
    }

    private fun applyServerValidation(message: String) {
        when {
            message.contains("手机号") -> setHelperMessage(phoneHelper, message, HelperTone.ERROR)
            message.contains("验证码") -> setHelperMessage(codeHelper, message, HelperTone.ERROR)
        }
    }

    private fun focusFirstInvalidField(state: PhoneAuthValidationState) {
        when {
            state.phoneError != null -> phoneInput.requestFocus()
            currentCode == null -> phoneInput.requestFocus()
            state.codeError != null -> codeInput.requestFocus()
        }
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

    private fun startCountDown(button: TextView) {
        countDownTimer?.cancel()
        isCountingDown = true
        button.isEnabled = false
        button.alpha = 0.65f

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                button.text = getString(
                    R.string.auth_send_code_retry_format,
                    (millisUntilFinished / 1000).toInt()
                )
            }

            override fun onFinish() {
                isCountingDown = false
                button.text = getString(R.string.auth_send_code)
                applyValidationState(buildValidationState())
            }
        }.start()
    }

    private fun setLoading(isLoading: Boolean) {
        isSubmitting = isLoading
        applyValidationState(buildValidationState())
        passwordLoginAction.isEnabled = !isLoading
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

    private fun showVerificationCodeDialog(code: String) {
        val density = resources.displayMetrics.density
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt(), 0)
        }

        val summaryView = TextView(this).apply {
            text = getString(R.string.auth_code_dialog_summary)
            setTextColor(getColor(R.color.text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setLineSpacing(0f, 1.15f)
        }

        val codeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.dialog_goal_preview_bg)
            setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (14 * density).toInt()
            }
        }

        val badgeView = TextView(this).apply {
            text = getString(R.string.auth_code_dialog_badge)
            background = getDrawable(R.drawable.category_chip_selected)
            setTextColor(getColor(R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
        }

        val codeView = TextView(this).apply {
            text = code
            setTextColor(getColor(R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.18f
            setPadding(0, (14 * density).toInt(), 0, 0)
        }

        val hintView = TextView(this).apply {
            text = getString(R.string.auth_code_dialog_hint)
            setTextColor(getColor(R.color.text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, (8 * density).toInt(), 0, 0)
        }

        codeCard.addView(badgeView)
        codeCard.addView(codeView)
        codeCard.addView(hintView)
        container.addView(summaryView)
        container.addView(codeCard)

        createAppAlertDialogBuilder()
            .setTitle(R.string.auth_code_dialog_title)
            .setView(container)
            .setNeutralButton(R.string.auth_code_dialog_autofill) { _, _ ->
                codeInput.setText(code)
                codeInput.setSelection(code.length)
                applyValidationState(buildValidationState())
            }
            .setPositiveButton(R.string.auth_code_dialog_confirm, null)
            .create()
            .also {
                it.show()
                it.applyAppDialogStyling(this)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private data class PhoneAuthValidationState(
        val phone: String,
        val code: String,
        val phoneError: String?,
        val codeError: String?
    ) {
        val canSubmit: Boolean
            get() = phoneError == null && codeError == null
    }

    private enum class HelperTone {
        NEUTRAL,
        SUCCESS,
        ERROR
    }
}
