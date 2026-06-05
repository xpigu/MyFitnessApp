package com.example.myfitnessapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.data.WorkoutRecordHelper
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import java.util.Locale

class StrengthActivity : AppCompatActivity() {

    private lateinit var tvExerciseName: TextView
    private lateinit var tvSetProgress: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvReps: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvTotalSets: TextView
    private lateinit var tvTotalVolume: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvRestTimer: TextView
    private lateinit var restOverlay: View
    private lateinit var llCompletedSets: LinearLayout
    private lateinit var tvEmptyHistory: TextView

    private val exercises = listOf(
        StrengthExercise("杠铃卧推", 4, 10, 60.0),
        StrengthExercise("哑铃飞鸟", 3, 12, 14.0),
        StrengthExercise("上斜卧推", 3, 10, 50.0),
        StrengthExercise("绳索夹胸", 3, 15, 20.0),
        StrengthExercise("三头下压", 3, 12, 25.0)
    )

    private var currentExerciseIndex = 0
    private var currentSet = 1
    private var totalSets = 0
    private var totalVolume = 0.0
    private var elapsedSeconds = 0
    private var restSeconds = 0
    private var isResting = false
    private lateinit var viewModel: WorkoutRecordViewModel
    private val handler = Handler(Looper.getMainLooper())

    private val mainTimer = object : Runnable {
        override fun run() {
            if (!isResting) {
                elapsedSeconds++
                updateTimer()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val restTimer = object : Runnable {
        override fun run() {
            if (restSeconds > 0) {
                restSeconds--
                updateRestDisplay()
                handler.postDelayed(this, 1000)
            } else {
                endRest()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strength)

        viewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)

        tvExerciseName = findViewById(R.id.tv_exercise_name)
        tvSetProgress = findViewById(R.id.tv_set_progress)
        tvWeight = findViewById(R.id.tv_weight)
        tvReps = findViewById(R.id.tv_reps)
        tvTimer = findViewById(R.id.tv_strength_timer)
        tvTotalSets = findViewById(R.id.tv_total_sets)
        tvTotalVolume = findViewById(R.id.tv_total_volume)
        tvCalories = findViewById(R.id.tv_strength_calories)
        tvRestTimer = findViewById(R.id.tv_rest_timer)
        restOverlay = findViewById(R.id.rest_overlay)
        llCompletedSets = findViewById(R.id.ll_completed_sets)
        tvEmptyHistory = findViewById(R.id.tv_empty_history)

        findViewById<View>(R.id.btn_strength_back).setOnClickListener { finish() }

        loadExercise(currentExerciseIndex)

        findViewById<View>(R.id.btn_weight_minus).setOnClickListener { adjustWeight(-2.5) }
        findViewById<View>(R.id.btn_weight_plus).setOnClickListener { adjustWeight(2.5) }
        findViewById<View>(R.id.btn_reps_minus).setOnClickListener { adjustReps(-1) }
        findViewById<View>(R.id.btn_reps_plus).setOnClickListener { adjustReps(1) }

        findViewById<View>(R.id.btn_complete_set).setOnClickListener { completeSet() }
        findViewById<View>(R.id.btn_skip_rest).setOnClickListener { skipRest() }
        findViewById<View>(R.id.btn_finish_strength).setOnClickListener { finishWorkout() }

        handler.post(mainTimer)
    }

    private fun loadExercise(index: Int) {
        if (index >= exercises.size) {
            finishWorkout()
            return
        }
        val ex = exercises[index]
        tvExerciseName.text = ex.name
        tvWeight.text = ex.targetWeight.toInt().toString()
        tvReps.text = ex.targetReps.toString()
        currentSet = 1
        updateSetProgress()
    }

    private fun updateSetProgress() {
        val ex = exercises[currentExerciseIndex]
        tvSetProgress.text = "第 $currentSet 组 / 共 ${ex.sets} 组"
    }

    private fun adjustWeight(delta: Double) {
        val current = tvWeight.text.toString().toDoubleOrNull() ?: 0.0
        val newVal = (current + delta).coerceAtLeast(0.0)
        tvWeight.text = if (newVal == newVal.toInt().toDouble()) newVal.toInt().toString() else newVal.toString()
    }

    private fun adjustReps(delta: Int) {
        val current = tvReps.text.toString().toIntOrNull() ?: 0
        val newVal = (current + delta).coerceAtLeast(1)
        tvReps.text = newVal.toString()
    }

    private fun completeSet() {
        val weight = tvWeight.text.toString().toDoubleOrNull() ?: 0.0
        val reps = tvReps.text.toString().toIntOrNull() ?: 0
        totalSets++
        totalVolume += weight * reps
        updateSummary()

        // 添加历史记录
        addHistoryEntry(weight, reps)

        val ex = exercises[currentExerciseIndex]
        if (currentSet >= ex.sets) {
            // 当前动作完成，进入下一个动作
            currentExerciseIndex++
            if (currentExerciseIndex >= exercises.size) {
                finishWorkout()
                return
            }
            currentSet = 1
            loadExercise(currentExerciseIndex)
            startRest()
        } else {
            currentSet++
            updateSetProgress()
            startRest()
        }
    }

    private fun addHistoryEntry(weight: Double, reps: Int) {
        tvEmptyHistory.visibility = View.GONE
        val item = layoutInflater.inflate(R.layout.workout_set_item, llCompletedSets, false)
        val ex = exercises[currentExerciseIndex]
        item.findViewById<TextView>(R.id.tv_set_item_name).text = ex.name
        item.findViewById<TextView>(R.id.tv_set_item_detail).text = "第${currentSet}组  ${weight.toInt()}kg × $reps"
        llCompletedSets.addView(item, 0)
    }

    private fun startRest() {
        isResting = true
        restSeconds = 60
        restOverlay.visibility = View.VISIBLE
        updateRestDisplay()
        handler.post(restTimer)
    }

    private fun skipRest() {
        handler.removeCallbacks(restTimer)
        endRest()
    }

    private fun endRest() {
        isResting = false
        restSeconds = 0
        restOverlay.visibility = View.GONE
        handler.removeCallbacks(restTimer)
        Toast.makeText(this, "休息结束，开始下一组！", Toast.LENGTH_SHORT).show()
    }

    private fun updateRestDisplay() {
        tvRestTimer.text = restSeconds.toString()
    }

    private fun updateSummary() {
        tvTotalSets.text = totalSets.toString()
        tvTotalVolume.text = totalVolume.toInt().toString()
        tvCalories.text = (elapsedSeconds * 0.12).toInt().toString()
    }

    private fun updateTimer() {
        val min = elapsedSeconds / 60
        val sec = elapsedSeconds % 60
        tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", min, sec)
        updateSummary()
    }

    private fun finishWorkout() {
        handler.removeCallbacks(mainTimer)
        handler.removeCallbacks(restTimer)

        val cal = (elapsedSeconds * 0.12).toInt()
        val record = com.example.myfitnessapp.data.entity.WorkoutRecord(
            sportType = "STRENGTH",
            sportIconResId = WorkoutRecordHelper.getIconRes("STRENGTH"),
            elapsedSeconds = elapsedSeconds,
            totalDistance = 0.0,
            totalCalories = cal,
            pace = "--",
            timestamp = WorkoutRecordHelper.nowTimestamp(),
            date = WorkoutRecordHelper.todayDate(),
            strengthSets = totalSets,
            strengthVolume = totalVolume
        )
        viewModel.saveRecord(record)

        Toast.makeText(this, "力量训练完成！总组数: $totalSets, 容量: ${totalVolume.toInt()}kg", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(mainTimer)
        handler.removeCallbacks(restTimer)
    }
}

data class StrengthExercise(
    val name: String,
    val sets: Int,
    val targetReps: Int,
    val targetWeight: Double
)