package com.example.myfitnessapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val authService = AuthService(this@LauncherActivity)
            val destination = when {
                AuthSessionManager.isLoggedIn(this@LauncherActivity) -> MainActivity::class.java
                authService.hasAnyAccount() -> LoginActivity::class.java
                else -> RegisterActivity::class.java
            }

            startActivity(
                Intent(this@LauncherActivity, destination).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.getStringExtra(MainActivity.EXTRA_PENDING_FEEDBACK_MESSAGE)?.let {
                        putExtra(MainActivity.EXTRA_PENDING_FEEDBACK_MESSAGE, it)
                    }
                    intent.getStringExtra(MainActivity.EXTRA_PENDING_FEEDBACK_TYPE)?.let {
                        putExtra(MainActivity.EXTRA_PENDING_FEEDBACK_TYPE, it)
                    }
                }
            )
            finish()
        }
    }
}
