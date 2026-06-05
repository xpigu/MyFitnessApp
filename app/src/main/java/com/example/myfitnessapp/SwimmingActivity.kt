package com.example.myfitnessapp

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.data.WorkoutRecordHelper
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel

class SwimmingActivity : AppCompatActivity() {

    private lateinit var etDistance: EditText
    private lateinit var etStrokes: EditText
    private lateinit var tvHours: TextView
    private lateinit var tvMinutes: TextView
    private lateinit var tvCalories: TextView
    private lateinit var rgStroke: RadioGroup

    private var hours = 0
    private var minutes = 30
    private lateinit var viewModel: WorkoutRecordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swimming)

        viewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)

        etDistance = findViewById(R.id.et_distance)
        etStrokes = findViewById(R.id.et_strokes)
        tvHours = findViewById(R.id.tv_hours)
        tvMinutes = findViewById(R.id.tv_minutes)
        tvCalories = findViewById(R.id.tv_swimming_calories)
        rgStroke = findViewById(R.id.rg_stroke)

        findViewById<View>(R.id.btn_swimming_back).setOnClickListener { finish() }

        // 时间调整
        findViewById<View>(R.id.btn_hours_minus).setOnClickListener { adjustTime("hours", -1) }
        findViewById<View>(R.id.btn_hours_plus).setOnClickListener { adjustTime("hours", 1) }
        findViewById<View>(R.id.btn_minutes_minus).setOnClickListener { adjustTime("minutes", -5) }
        findViewById<View>(R.id.btn_minutes_plus).setOnClickListener { adjustTime("minutes", 5) }

        // 距离变化时更新热量
        etDistance.setOnFocusChangeListener { _, _ -> updateCalories() }

        findViewById<View>(R.id.btn_swimming_save).setOnClickListener { saveRecord() }

        updateCalories()
    }

    private fun adjustTime(field: String, delta: Int) {
        if (field == "hours") {
            hours = (hours + delta).coerceIn(0, 5)
        } else {
            minutes = (minutes + delta).coerceIn(0, 55)
        }
        tvHours.text = hours.toString()
        tvMinutes.text = String.format("%02d", minutes)
        updateCalories()
    }

    private fun updateCalories() {
        val distance = etDistance.text.toString().toIntOrNull() ?: 0
        val totalMinutes = hours * 60 + minutes
        val estimatedCal = (distance * 0.04 + totalMinutes * 6.5).toInt()
        tvCalories.text = "$estimatedCal kcal"
    }

    private fun saveRecord() {
        val distance = etDistance.text.toString().toIntOrNull() ?: 0
        if (distance <= 0) {
            Toast.makeText(this, "请输入游泳距离", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedStroke = when (rgStroke.checkedRadioButtonId) {
            R.id.rb_freestyle -> "自由泳"
            R.id.rb_breaststroke -> "蛙泳"
            R.id.rb_backstroke -> "仰泳"
            R.id.rb_butterfly -> "蝶泳"
            else -> "自由泳"
        }

        val totalMinutes = hours * 60 + minutes
        val strokes = etStrokes.text.toString().toIntOrNull() ?: 0
        val cal = (distance * 0.04 + totalMinutes * 6.5).toInt()

        val record = com.example.myfitnessapp.data.entity.WorkoutRecord(
            sportType = "SWIMMING",
            sportIconResId = WorkoutRecordHelper.getIconRes("SWIMMING"),
            elapsedSeconds = totalMinutes * 60,
            totalDistance = distance / 1000.0,
            totalCalories = cal,
            pace = selectedStroke,
            timestamp = WorkoutRecordHelper.nowTimestamp(),
            date = WorkoutRecordHelper.todayDate(),
            swimDistanceM = distance,
            swimStroke = selectedStroke
        )
        viewModel.saveRecord(record)

        Toast.makeText(
            this,
            "已保存: $selectedStroke ${distance}米 ${totalMinutes}分钟",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }
}