package com.example.myfitnessapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.course.data.ActiveCourseSessionStore
import com.example.myfitnessapp.course.navigation.CourseNavigator
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

        findViewById<View>(R.id.btn_swimming_back).setOnClickListener {
            finish()
        }

        // 时间调整
        findViewById<View>(R.id.btn_hours_minus).setOnClickListener { adjustTime("hours", -1) }
        findViewById<View>(R.id.btn_hours_plus).setOnClickListener { adjustTime("hours", 1) }
        findViewById<View>(R.id.btn_minutes_minus).setOnClickListener { adjustTime("minutes", -5) }
        findViewById<View>(R.id.btn_minutes_plus).setOnClickListener { adjustTime("minutes", 5) }

        val recalculateWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCalories()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        }
        etDistance.addTextChangedListener(recalculateWatcher)
        etStrokes.addTextChangedListener(recalculateWatcher)
        rgStroke.setOnCheckedChangeListener { _, _ -> updateCalories() }

        findViewById<View>(R.id.btn_swimming_save).setOnClickListener { confirmSaveRecord() }

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
        val strokes = etStrokes.text.toString().toIntOrNull() ?: 0
        val totalMinutes = hours * 60 + minutes
        val estimatedCal = estimateSwimmingCalories(
            distanceMeters = distance,
            totalMinutes = totalMinutes,
            strokeName = getSelectedStrokeName(),
            strokes = strokes
        )
        tvCalories.text = "$estimatedCal kcal"
    }

    private fun confirmSaveRecord() {
        createAppAlertDialogBuilder()
            .setTitle("结束游泳")
            .setMessage("是否保存本次游泳记录？")
            .setPositiveButton("保存记录") { _, _ ->
                saveRecord()
            }
            .setNegativeButton("放弃记录", null)
            .create()
            .also {
                it.show()
                it.applyAppDialogStyling(this)
            }
    }

    private fun saveRecord() {
        val distance = etDistance.text.toString().toIntOrNull() ?: 0
        if (distance <= 0) {
            showAppFeedback("请输入游泳距离", FeedbackType.WARNING)
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
        val cal = estimateSwimmingCalories(
            distanceMeters = distance,
            totalMinutes = totalMinutes,
            strokeName = selectedStroke,
            strokes = strokes
        )

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
        viewModel.saveRecord(record) {
            ActiveCourseSessionStore(this).clear(CourseNavigator.courseIdOf(intent))
            showAppFeedback("已保存: $selectedStroke ${distance}米 ${totalMinutes}分钟", FeedbackType.SUCCESS)
            finish()
        }
    }

    private fun getSelectedStrokeName(): String {
        return when (rgStroke.checkedRadioButtonId) {
            R.id.rb_freestyle -> "自由泳"
            R.id.rb_breaststroke -> "蛙泳"
            R.id.rb_backstroke -> "仰泳"
            R.id.rb_butterfly -> "蝶泳"
            else -> "自由泳"
        }
    }

    private fun estimateSwimmingCalories(
        distanceMeters: Int,
        totalMinutes: Int,
        strokeName: String,
        strokes: Int
    ): Int {
        if (distanceMeters <= 0 || totalMinutes <= 0) {
            return 0
        }

        val baseMet = when (strokeName) {
            "蛙泳" -> 7.0
            "仰泳" -> 6.8
            "蝶泳" -> 11.0
            else -> 8.3
        }

        val metersPerMinute = distanceMeters.toDouble() / totalMinutes
        val referencePace = when (strokeName) {
            "蛙泳" -> 28.0
            "仰泳" -> 26.0
            "蝶泳" -> 32.0
            else -> 30.0
        }
        val paceFactor = (metersPerMinute / referencePace).coerceIn(0.75, 1.35)

        val strokeFactor = if (strokes > 0) {
            val lengths = (distanceMeters / 25.0).coerceAtLeast(1.0)
            val strokesPer25m = strokes / lengths
            val referenceStrokesPer25m = when (strokeName) {
                "蛙泳" -> 12.0
                "仰泳" -> 13.0
                "蝶泳" -> 11.0
                else -> 14.0
            }
            (strokesPer25m / referenceStrokesPer25m).coerceIn(0.9, 1.15)
        } else {
            1.0
        }

        val assumedWeightKg = 70.0
        val caloriesPerMinute = baseMet * 3.5 * assumedWeightKg / 200.0
        return (caloriesPerMinute * totalMinutes * paceFactor * strokeFactor).toInt()
    }
}
