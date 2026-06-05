package com.example.myfitnessapp

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.data.WorkoutRecordHelper
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

class JumpRopeActivity : AppCompatActivity() {

    private lateinit var tvCount: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvFrequency: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnStart: TextView
    private lateinit var btnStop: TextView

    private var count = 0
    private var elapsedSeconds = 0
    private var calories = 0
    private var isActive = false
    private var isPaused = false
    private lateinit var viewModel: WorkoutRecordViewModel
    private val handler = Handler(Looper.getMainLooper())
    private val countAnimatorInterval = 800L

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isActive && !isPaused) {
                elapsedSeconds++
                updateTimerDisplay()
                count += 1
                updateCountDisplay()
                updateCalories()
                updateFrequency()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jump_rope)

        viewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)

        tvCount = findViewById(R.id.tv_jump_count)
        tvTimer = findViewById(R.id.tv_jump_timer)
        tvFrequency = findViewById(R.id.tv_jump_frequency)
        tvCalories = findViewById(R.id.tv_jump_calories)
        tvStatus = findViewById(R.id.tv_jump_status)
        btnStart = findViewById(R.id.btn_jump_start)
        btnStop = findViewById(R.id.btn_jump_stop)

        btnStart.setOnClickListener {
            if (!isActive) {
                startWorkout()
            } else if (!isPaused) {
                pauseWorkout()
            } else {
                resumeWorkout()
            }
        }

        btnStop.setOnClickListener {
            if (isActive) {
                showSummary()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isActive) {
                    showSummary()
                } else {
                    finish()
                }
            }
        })
    }

    private fun startWorkout() {
        isActive = true
        isPaused = false
        btnStart.text = "暂停"
        tvStatus.text = "运动中"
        handler.post(timerRunnable)
    }

    private fun pauseWorkout() {
        isPaused = true
        btnStart.text = "继续"
        tvStatus.text = "已暂停"
        handler.removeCallbacks(timerRunnable)
    }

    private fun resumeWorkout() {
        isPaused = false
        btnStart.text = "暂停"
        tvStatus.text = "运动中"
        handler.post(timerRunnable)
    }

    private fun updateCountDisplay() {
        val anim = ValueAnimator.ofInt(tvCount.text.toString().toIntOrNull() ?: 0, count)
        anim.duration = 300
        anim.addUpdateListener { tvCount.text = it.animatedValue.toString() }
        anim.start()
    }

    private fun updateTimerDisplay() {
        val min = elapsedSeconds / 60
        val sec = elapsedSeconds % 60
        tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }

    private fun updateCalories() {
        calories = (elapsedSeconds * 0.15).toInt()
        tvCalories.text = calories.toString()
    }

    private fun updateFrequency() {
        if (elapsedSeconds > 0) {
            val freq = (count * 60) / elapsedSeconds
            tvFrequency.text = freq.toString()
        }
    }

    private fun showSummary() {
        isActive = false
        isPaused = false
        handler.removeCallbacks(timerRunnable)
        btnStart.text = "开始"
        tvStatus.text = "已完成"

        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_workout_summary, null)
        dialog.setContentView(sheetView)

        sheetView.findViewById<TextView>(R.id.summary_tv_duration).text = tvTimer.text
        sheetView.findViewById<TextView>(R.id.summary_tv_distance).text = "$count 个"
        sheetView.findViewById<TextView>(R.id.summary_tv_calories).text = "$calories kcal"
        sheetView.findViewById<TextView>(R.id.summary_tv_avg_pace).text = "频率: ${tvFrequency.text} 个/分钟"

        sheetView.findViewById<View>(R.id.summary_btn_discard).setOnClickListener {
            dialog.dismiss()
            resetWorkout()
        }

        sheetView.findViewById<View>(R.id.summary_btn_save).setOnClickListener {
            val freq = if (elapsedSeconds > 0) (count * 60) / elapsedSeconds else 0
            val record = com.example.myfitnessapp.data.entity.WorkoutRecord(
                sportType = "JUMP_ROPE",
                sportIconResId = WorkoutRecordHelper.getIconRes("JUMP_ROPE"),
                elapsedSeconds = elapsedSeconds,
                totalDistance = 0.0,
                totalCalories = calories,
                pace = "$freq 个/分钟",
                timestamp = WorkoutRecordHelper.nowTimestamp(),
                date = WorkoutRecordHelper.todayDate(),
                jumpCount = count,
                jumpFrequency = freq
            )
            viewModel.saveRecord(record)
            Toast.makeText(this, "记录已保存", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            resetWorkout()
        }

        dialog.show()
    }

    private fun resetWorkout() {
        count = 0
        elapsedSeconds = 0
        calories = 0
        tvCount.text = "0"
        tvTimer.text = "00:00"
        tvFrequency.text = "0"
        tvCalories.text = "0"
        tvStatus.text = "准备开始"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }
}