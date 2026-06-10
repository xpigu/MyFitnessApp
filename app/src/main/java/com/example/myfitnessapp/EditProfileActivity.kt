package com.example.myfitnessapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.data.viewmodel.UserProfileViewModel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var viewModel: UserProfileViewModel
    private lateinit var etUsername: EditText
    private lateinit var etBio: EditText
    private lateinit var etHeight: EditText
    private lateinit var etWeight: EditText
    private lateinit var etBirthday: EditText
    private lateinit var rgGender: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        viewModel = ViewModelProvider(this).get(UserProfileViewModel::class.java)

        initializeViews()
        observeProfileData()
        setupListeners()
    }

    private fun initializeViews() {
        etUsername = findViewById(R.id.et_username)
        etBio = findViewById(R.id.et_bio)
        etHeight = findViewById(R.id.et_height)
        etWeight = findViewById(R.id.et_weight)
        etBirthday = findViewById(R.id.et_birthday)
        rgGender = findViewById(R.id.rg_gender)
    }

    private fun observeProfileData() {
        viewModel.userProfile.observe(this) { profile ->
            etUsername.setText(profile.username)
            etBio.setText(profile.bio)
            if (profile.heightCm > 0) etHeight.setText(profile.heightCm.toString())
            if (profile.weightKg > 0) etWeight.setText(profile.weightKg.toString())
            etBirthday.setText(profile.birthday)

            when (profile.gender) {
                "MALE" -> rgGender.check(R.id.rb_male)
                "FEMALE" -> rgGender.check(R.id.rb_female)
                else -> rgGender.check(R.id.rb_other)
            }
        }
    }

    private fun setupListeners() {
        // 返回按钮
        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // 取消按钮
        findViewById<View>(R.id.btn_cancel_profile).setOnClickListener {
            finish()
        }

        // 保存按钮 (Toolbar)
        findViewById<View>(R.id.btn_toolbar_save).setOnClickListener {
            saveProfile()
        }

        // 保存按钮 (Bottom)
        findViewById<View>(R.id.btn_save_profile).setOnClickListener {
            saveProfile()
        }

        // 生日选择
        etBirthday.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        // 尝试解析现有日期
        val currentStr = etBirthday.text.toString()
        if (currentStr.isNotBlank()) {
            val parts = currentStr.split("-")
            if (parts.size == 3) {
                calendar.set(Calendar.YEAR, parts[0].toInt())
                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            }
        }

        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val dateStr = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth)
                etBirthday.setText(dateStr)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun saveProfile() {
        val username = etUsername.text.toString().trim()
        val bio = etBio.text.toString().trim()
        val heightStr = etHeight.text.toString().trim()
        val weightStr = etWeight.text.toString().trim()
        val birthday = etBirthday.text.toString().trim()

        if (username.isEmpty()) {
            etUsername.error = getString(R.string.profile_name_empty)
            return
        }

        val height = heightStr.toIntOrNull()
        if (heightStr.isNotEmpty() && (height == null || height !in 80..260)) {
            etHeight.error = getString(R.string.profile_height_invalid)
            return
        }

        val weight = weightStr.toDoubleOrNull()
        if (weightStr.isNotEmpty() && (weight == null || weight !in 20.0..300.0)) {
            etWeight.error = getString(R.string.profile_weight_invalid)
            return
        }

        if (birthday.isNotEmpty() && !isValidBirthday(birthday)) {
            etBirthday.error = getString(R.string.profile_birthday_invalid)
            return
        }

        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rb_male -> "MALE"
            R.id.rb_female -> "FEMALE"
            else -> "OTHER"
        }

        // 提交更新
        val current = viewModel.userProfile.value ?: return
        val updated = current.copy(
            username = username,
            bio = bio,
            gender = gender,
            heightCm = height ?: 0,
            weightKg = weight ?: 0.0,
            birthday = birthday,
            lastUpdated = System.currentTimeMillis()
        )
        viewModel.updateUserProfile(updated)
        Toast.makeText(this, R.string.profile_save_success, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun isValidBirthday(value: String): Boolean {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            isLenient = false
        }
        val birthdayDate = try {
            formatter.parse(value)
        } catch (_: ParseException) {
            null
        } ?: return false
        return !birthdayDate.after(Date())
    }
}
