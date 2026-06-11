package com.example.myfitnessapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AccountManagementActivity : AppCompatActivity() {

    private lateinit var service: AccountManagementService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AuthSessionManager.isLoggedIn(this)) {
            startLauncher()
            return
        }

        setContentView(R.layout.activity_account_management)
        service = AccountManagementService(this)

        findViewById<View>(R.id.btn_account_management_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_account_login_other).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).putExtra(EXTRA_ALLOW_WHILE_LOGGED_IN, true))
        }
        findViewById<View>(R.id.btn_account_register_new).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java).putExtra(EXTRA_ALLOW_WHILE_LOGGED_IN, true))
        }
        findViewById<View>(R.id.btn_account_change_password).setOnClickListener {
            changePassword()
        }
    }

    override fun onResume() {
        super.onResume()
        renderCurrentAccount()
        loadAccounts()
    }

    private fun renderCurrentAccount() {
        findViewById<TextView>(R.id.tv_account_current_username).apply {
            text = AuthSessionManager.getUsername(this@AccountManagementActivity)
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
    }

    private fun loadAccounts() {
        lifecycleScope.launch {
            val accounts = service.getAccounts()
            val container = findViewById<LinearLayout>(R.id.layout_account_list_container)
            val emptyView = findViewById<TextView>(R.id.tv_account_list_empty)
            container.removeAllViews()
            emptyView.isVisible = accounts.isEmpty()

            accounts.forEach { account ->
                val itemView = layoutInflater.inflate(R.layout.item_account_manage, container, false)
                itemView.findViewById<TextView>(R.id.tv_account_item_username).apply {
                    text = account.username
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
                itemView.findViewById<TextView>(R.id.tv_account_item_meta).apply {
                    text = "创建于 ${account.createdAtLabel} · 最近登录 ${account.lastLoginAtLabel}"
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }

                val chip = itemView.findViewById<TextView>(R.id.tv_account_item_current_chip)
                val switchButton = itemView.findViewById<TextView>(R.id.btn_account_item_switch)
                val deleteButton = itemView.findViewById<TextView>(R.id.btn_account_item_delete)

                chip.isVisible = account.isCurrent
                switchButton.isVisible = !account.isCurrent
                switchButton.setOnClickListener { switchAccount(account.username) }
                deleteButton.setOnClickListener { confirmDelete(account.username, account.isCurrent) }

                container.addView(itemView)
            }
        }
    }

    private fun switchAccount(username: String) {
        val messageView = TextView(this).apply {
            text = getString(R.string.account_management_switch_verify_message, username)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
        val passwordInput = EditText(this).apply {
            hint = getString(R.string.account_management_switch_password_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            minHeight = dpToPx(48)
            setBackgroundResource(R.drawable.edit_text_bg)
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }
        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(4), dpToPx(8), dpToPx(4), 0)
            addView(messageView)
            addView(
                passwordInput,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(12)
                }
            )
        }

        val dialog = createAppAlertDialogBuilder()
            .setTitle(R.string.account_management_switch_verify_title)
            .setView(dialogContent)
            .setPositiveButton(R.string.account_management_switch_verify_confirm, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            passwordInput.requestFocus()
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            positiveButton.setOnClickListener {
                positiveButton.isEnabled = false
                negativeButton.isEnabled = false
                lifecycleScope.launch {
                    val result = service.switchAccount(username, passwordInput.text.toString())
                        if (result.success) {
                            dialog.dismiss()
                            startLauncher(result.message, FeedbackType.SUCCESS)
                        } else {
                            showAppFeedback(result.message, FeedbackType.WARNING)
                            positiveButton.isEnabled = true
                            negativeButton.isEnabled = true
                            if (result.message.contains("密码")) {
                                passwordInput.error = result.message
                            }
                        }
                }
            }
        }

        dialog.show()
        dialog.applyAppDialogStyling(this)
    }

    private fun changePassword() {
        val oldPassword = findViewById<EditText>(R.id.et_account_old_password).text.toString()
        val newPassword = findViewById<EditText>(R.id.et_account_new_password).text.toString()
        val confirmPassword = findViewById<EditText>(R.id.et_account_confirm_password).text.toString()
        val currentUsername = AuthSessionManager.getUsername(this)

        lifecycleScope.launch {
            val result = service.changePassword(
                username = currentUsername,
                oldPassword = oldPassword,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )
            if (result.success) {
                showAppFeedback(result.message, FeedbackType.SUCCESS)
                findViewById<EditText>(R.id.et_account_old_password).text?.clear()
                findViewById<EditText>(R.id.et_account_new_password).text?.clear()
                findViewById<EditText>(R.id.et_account_confirm_password).text?.clear()
            } else {
                showAppFeedback(result.message, FeedbackType.WARNING)
            }
        }
    }

    private fun confirmDelete(username: String, isCurrent: Boolean) {
        val message = if (isCurrent) {
            "删除当前账号后，该账号下的资料、训练、饮食和签到数据都会被清除，是否继续？"
        } else {
            "删除账号 $username 后，该账号下的本地数据也会一并删除，是否继续？"
        }
        createAppAlertDialogBuilder()
            .setTitle("删除账号")
            .setMessage(message)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    val result = service.deleteAccount(username)
                    if (result.success && (result.nextUsername != null || result.requiresAuthRedirect)) {
                        startLauncher(result.message, FeedbackType.SUCCESS)
                    } else if (result.success) {
                        showAppFeedback(result.message, FeedbackType.SUCCESS)
                        loadAccounts()
                    } else {
                        showAppFeedback(result.message, FeedbackType.WARNING)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .create()
            .also {
                it.show()
                it.applyAppDialogStyling(this)
            }
    }

    private fun startLauncher(
        feedbackMessage: String? = null,
        feedbackType: FeedbackType? = null
    ) {
        startActivity(
            Intent(this, LauncherActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                if (!feedbackMessage.isNullOrBlank() && feedbackType != null) {
                    putExtra(MainActivity.EXTRA_PENDING_FEEDBACK_MESSAGE, feedbackMessage)
                    putExtra(MainActivity.EXTRA_PENDING_FEEDBACK_TYPE, feedbackType.name)
                }
            }
        )
        finish()
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
