package com.example.myfitnessapp

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

class WorkoutTrackingActivity : AppCompatActivity() {

    // 计时相关
    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0
    private var isRunning = true
    private var isPaused = false

    // 模拟数据
    private var totalDistance = 0.0    // 公里
    private var totalCalories = 0      // kcal
    private var currentHeartRate = 72  // bpm
    private var currentPace = "6'00\""

    // 运动类型
    private var sportType = "跑步"
    private var sportIconRes = R.drawable.ic_running

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                elapsedSeconds++
                updateTimerDisplay()
                updateSimulatedData()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_tracking)

        // 接收 Intent 参数
        parseIntentData()

        // 初始化 UI
        setupHeader()
        startTimer()

        // 控制按钮
        findViewById<View>(R.id.btn_pause_resume).setOnClickListener { togglePauseResume() }
        findViewById<View>(R.id.btn_stop).setOnClickListener { showStopConfirmDialog() }

        // 返回键拦截
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })
    }

    // ============================================================
    // Intent 参数解析
    // ============================================================
    private fun parseIntentData() {
        val name = intent.getStringExtra(EXTRA_SPORT_NAME)
        val iconRes = intent.getIntExtra(EXTRA_SPORT_ICON, R.drawable.ic_running)

        if (!name.isNullOrEmpty()) {
            sportType = name
        }
        sportIconRes = iconRes
    }

    // ============================================================
    // 顶部信息初始化
    // ============================================================
    private fun setupHeader() {
        findViewById<TextView>(R.id.tv_tracking_sport_name).text = sportType
        findViewById<android.widget.ImageView>(R.id.iv_tracking_sport_icon)
            .setImageResource(sportIconRes)
    }

    // ============================================================
    // 计时器
    // ============================================================
    private fun startTimer() {
        isRunning = true
        isPaused = false
        handler.post(timerRunnable)
        updateStatusLabel("运动中")
    }

    private fun togglePauseResume() {
        if (isPaused) {
            resumeTimer()
        } else {
            pauseTimer()
        }
    }

    private fun pauseTimer() {
        isRunning = false
        isPaused = true
        val btn = findViewById<TextView>(R.id.btn_pause_resume)
        btn.text = "继续"
        btn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_resume, 0, 0)
        updateStatusLabel("已暂停")
    }

    private fun resumeTimer() {
        isRunning = true
        isPaused = false
        val btn = findViewById<TextView>(R.id.btn_pause_resume)
        btn.text = "暂停"
        btn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_pause, 0, 0)
        updateStatusLabel("运动中")
        handler.post(timerRunnable)
    }

    private fun updateTimerDisplay() {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        findViewById<TextView>(R.id.tv_tracking_timer).text =
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateStatusLabel(label: String) {
        findViewById<TextView>(R.id.tv_tracking_status).text = label
    }

    // ============================================================
    // 模拟数据更新（每秒递增）
    // ============================================================
    private fun updateSimulatedData() {
        // 模拟距离：每3秒约0.01公里（约12km/h）
        totalDistance += 0.0033
        // 模拟热量：每秒约0.1 kcal
        totalCalories += 0.12.toInt().coerceAtLeast(1)
        // 模拟心率波动
        currentHeartRate = (120 + (Math.random() * 30 - 10)).toInt().coerceIn(60, 180)
        // 模拟配速
        val paceMin = 5 + (Math.random() * 2).toInt()
        val paceSec = (Math.random() * 60).toInt()
        currentPace = String.format(Locale.getDefault(), "%d'%02d\"", paceMin, paceSec)

        // 更新 UI
        findViewById<TextView>(R.id.tv_tracking_heart_rate).text = currentHeartRate.toString()
        findViewById<TextView>(R.id.tv_tracking_calories).text = totalCalories.toString()
        findViewById<TextView>(R.id.tv_tracking_distance).text =
            String.format(Locale.getDefault(), "%.2f", totalDistance)
        findViewById<TextView>(R.id.tv_tracking_pace).text = currentPace
    }

    // ============================================================
    // 结束确认弹窗
    // ============================================================
    private fun showStopConfirmDialog() {
        // 先暂停计时
        if (isRunning) {
            pauseTimer()
        }

        AlertDialog.Builder(this)
            .setTitle("结束运动")
            .setMessage("确定要结束本次运动吗？")
            .setPositiveButton("确定") { _, _ ->
                showSummarySheet()
            }
            .setNegativeButton("取消") { _, _ ->
                resumeTimer()
            }
            .show()
    }

    // ============================================================
    // 运动摘要底部弹窗
    // ============================================================
    private fun showSummarySheet() {
        val bottomSheet = BottomSheetDialog(this)
        val view = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet_workout_summary, null)

        // 绑定摘要数据
        val duration = findViewById<TextView>(R.id.tv_tracking_timer).text.toString()
        val distance = findViewById<TextView>(R.id.tv_tracking_distance).text.toString()
        val calories = findViewById<TextView>(R.id.tv_tracking_calories).text.toString()
        val heartRate = findViewById<TextView>(R.id.tv_tracking_heart_rate).text.toString()
        val pace = findViewById<TextView>(R.id.tv_tracking_pace).text.toString()

        view.findViewById<TextView>(R.id.summary_tv_duration).text = duration
        view.findViewById<TextView>(R.id.summary_tv_distance).text = "$distance 公里"
        view.findViewById<TextView>(R.id.summary_tv_calories).text = "$calories kcal"
        view.findViewById<TextView>(R.id.summary_tv_avg_heart_rate).text = "$heartRate bpm"
        view.findViewById<TextView>(R.id.summary_tv_avg_pace).text = pace

        // 保存记录按钮
        view.findViewById<View>(R.id.summary_btn_save).setOnClickListener {
            saveWorkoutRecord()
            bottomSheet.dismiss()
            Toast.makeText(this, "记录已保存", Toast.LENGTH_SHORT).show()
            onFinishWorkout()
        }

        // 放弃按钮
        view.findViewById<View>(R.id.summary_btn_discard).setOnClickListener {
            bottomSheet.dismiss()
            Toast.makeText(this, "已放弃本次记录", Toast.LENGTH_SHORT).show()
            onFinishWorkout()
        }

        bottomSheet.setContentView(view)
        bottomSheet.setCancelable(false)
        bottomSheet.show()
    }

    // ============================================================
    // 保存记录 — 预留接口
    // ============================================================
    private fun saveWorkoutRecord() {
        // TODO: 后续接入 Room/ViewModel 持久化存储
        // 需要保存的字段：
        //   - sportType: 运动类型
        //   - elapsedSeconds: 总时长（秒）
        //   - totalDistance: 总距离（公里）
        //   - totalCalories: 总热量（kcal）
        //   - currentHeartRate: 最终心率（bpm）
        //   - currentPace: 最终配速
        //   - timestamp: 完成时间戳
    }

    // ============================================================
    // 结束并返回
    // ============================================================
    private fun onFinishWorkout() {
        // TODO: 后续可通过 setResult + Intent 回传数据给 TrainingActivity
        finish()
    }

    // ============================================================
    // 系统返回键拦截
    // ============================================================
    private fun handleBackPressed() {
        if (elapsedSeconds > 0 && isRunning) {
            pauseTimer()
            AlertDialog.Builder(this)
                .setTitle("放弃运动")
                .setMessage("当前运动数据将不会保存，确定退出吗？")
                .setPositiveButton("退出") { _, _ ->
                    stopTimer()
                    finish()
                }
                .setNegativeButton("继续运动") { _, _ ->
                    resumeTimer()
                }
                .show()
        } else {
            stopTimer()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    private fun stopTimer() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
    }

    companion object {
        const val EXTRA_SPORT_NAME = "extra_sport_name"
        const val EXTRA_SPORT_ICON = "extra_sport_icon"
    }
}