package com.example.myfitnessapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        bindVersionInfo()
        setupActions()
    }

    private fun bindVersionInfo() {
        findViewById<TextView>(R.id.tv_about_version).text =
            getString(R.string.about_version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

    private fun setupActions() {
        findViewById<View>(R.id.btn_about_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_about_check_update).setOnClickListener {
            Toast.makeText(this, R.string.about_latest_toast, Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btn_about_open_privacy).setOnClickListener {
            startActivity(Intent(this, PrivacySettingsActivity::class.java))
        }
        findViewById<View>(R.id.btn_about_open_theme).setOnClickListener {
            startActivity(Intent(this, ThemeSettingsActivity::class.java))
        }
        findViewById<View>(R.id.btn_about_open_reminder).setOnClickListener {
            startActivity(Intent(this, ReminderSettingsActivity::class.java))
        }
    }
}
