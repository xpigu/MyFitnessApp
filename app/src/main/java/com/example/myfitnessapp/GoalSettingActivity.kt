package com.example.myfitnessapp

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.data.viewmodel.UserProfileViewModel

class GoalSettingActivity : AppCompatActivity() {

    private lateinit var viewModel: UserProfileViewModel

    private lateinit var etTargetCalories: EditText
    private lateinit var etTargetWater: EditText
    private lateinit var etTargetSteps: EditText
    private lateinit var etTargetWorkouts: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_setting)

        viewModel = ViewModelProvider(this).get(UserProfileViewModel::class.java)

        initViews()
        setupActions()
        observeData()
    }

    private fun initViews() {
        etTargetCalories = findViewById(R.id.et_target_calories)
        etTargetWater = findViewById(R.id.et_target_water)
        etTargetSteps = findViewById(R.id.et_target_steps)
        etTargetWorkouts = findViewById(R.id.et_target_workouts)
    }

    private fun setupActions() {
        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btn_save_goals).setOnClickListener {
            saveGoals()
        }
    }

    private fun observeData() {
        viewModel.userProfile.observe(this) { profile ->
            // 只有在 EditText 为空时才填充，避免覆盖用户正在输入的内容
            if (etTargetCalories.text.isEmpty()) {
                etTargetCalories.setText(profile.targetDailyCalories.toString())
            }
            if (etTargetWater.text.isEmpty()) {
                etTargetWater.setText(profile.targetDailyWater.toString())
            }
            if (etTargetSteps.text.isEmpty()) {
                etTargetSteps.setText(profile.targetDailySteps.toString())
            }
            if (etTargetWorkouts.text.isEmpty()) {
                etTargetWorkouts.setText(profile.targetWeeklyWorkouts.toString())
            }
        }
    }

    private fun saveGoals() {
        val caloriesStr = etTargetCalories.text.toString()
        val waterStr = etTargetWater.text.toString()
        val stepsStr = etTargetSteps.text.toString()
        val workoutsStr = etTargetWorkouts.text.toString()

        if (caloriesStr.isEmpty() || waterStr.isEmpty() || stepsStr.isEmpty() || workoutsStr.isEmpty()) {
            Toast.makeText(this, "请填写所有目标", Toast.LENGTH_SHORT).show()
            return
        }

        val currentProfile = viewModel.userProfile.value ?: return

        val updatedProfile = currentProfile.copy(
            targetDailyCalories = caloriesStr.toIntOrNull() ?: 2000,
            targetDailyWater = waterStr.toIntOrNull() ?: 8,
            targetDailySteps = stepsStr.toIntOrNull() ?: 10000,
            targetWeeklyWorkouts = workoutsStr.toIntOrNull() ?: 3,
            lastUpdated = System.currentTimeMillis()
        )

        viewModel.updateUserProfile(updatedProfile)
        Toast.makeText(this, "目标已保存", Toast.LENGTH_SHORT).show()
        finish()
    }
}
