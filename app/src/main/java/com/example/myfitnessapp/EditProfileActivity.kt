package com.example.myfitnessapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.data.viewmodel.UserProfileViewModel

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
        setupButtons()
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

    private fun setupButtons() {
        findViewById<View>(R.id.btn_save_profile).setOnClickListener {
            saveProfile()
        }

        findViewById<View>(R.id.btn_cancel_profile).setOnClickListener {
            finish()
        }
    }

    private fun saveProfile() {
        val username = etUsername.text.toString().trim()
        val bio = etBio.text.toString().trim()
        val heightStr = etHeight.text.toString().trim()
        val weightStr = etWeight.text.toString().trim()
        val birthday = etBirthday.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rb_male -> "MALE"
            R.id.rb_female -> "FEMALE"
            else -> "OTHER"
        }

        val height = heightStr.toIntOrNull() ?: 0
        val weight = weightStr.toDoubleOrNull() ?: 0.0

        viewModel.userProfile.observe(this) { profile ->
            val updated = profile.copy(
                username = username,
                bio = bio,
                gender = gender,
                heightCm = height,
                weightKg = weight,
                birthday = birthday
            )
            viewModel.submitEdit(updated)
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
