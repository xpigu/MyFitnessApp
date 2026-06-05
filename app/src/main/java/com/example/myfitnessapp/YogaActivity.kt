package com.example.myfitnessapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.data.WorkoutRecordHelper
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import java.util.Locale

class YogaActivity : AppCompatActivity() {

    private lateinit var tvPoseName: TextView
    private lateinit var tvPoseInstruction: TextView
    private lateinit var tvPoseTimer: TextView
    private lateinit var tvPoseProgress: TextView
    private lateinit var tvBreathText: TextView
    private lateinit var btnPause: TextView

    private val poses = listOf(
        YogaPose("山式", 30, "双脚并拢，身体直立，双臂自然下垂", true),
        YogaPose("前屈式", 30, "从髋部前屈，双手触地", false),
        YogaPose("战士一式", 45, "右腿前弓步，双臂上举，目视前方", true),
        YogaPose("战士二式", 45, "双臂侧平举，目视右手指尖", false),
        YogaPose("三角式", 30, "右手触右脚踝，左臂向上伸展", true),
        YogaPose("下犬式", 45, "双手双脚撑地，臀部向上推", false),
        YogaPose("上犬式", 30, "双臂伸直，胸部前推，目视上方", true),
        YogaPose("婴儿式", 30, "跪坐，前额贴地，双臂向前伸展", false),
        YogaPose("猫牛式", 30, "吸气弓背，呼气塌腰", true),
        YogaPose("桥式", 30, "仰卧屈膝，臀部向上推起", false),
        YogaPose("仰卧扭转", 30, "仰卧，双膝倒向一侧，转头看对侧", true),
        YogaPose("挺尸式", 60, "仰卧，全身放松，自然呼吸", false)
    )

    private var currentPoseIndex = 0
    private var poseRemainingSeconds = 0
    private var breathCycle = 0
    private var isPaused = false
    private var isActive = true
    private var totalElapsedSeconds = 0
    private lateinit var viewModel: WorkoutRecordViewModel
    private val handler = Handler(Looper.getMainLooper())

    private val poseTimer = object : Runnable {
        override fun run() {
            if (!isActive) return
            if (isPaused) {
                handler.postDelayed(this, 200)
                return
            }

            if (poseRemainingSeconds > 0) {
                poseRemainingSeconds--
                totalElapsedSeconds++
                updateTimerDisplay()
                updateBreath()
                handler.postDelayed(this, 1000)
            } else {
                nextPose()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yoga)

        viewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)

        tvPoseName = findViewById(R.id.tv_pose_name)
        tvPoseInstruction = findViewById(R.id.tv_pose_instruction)
        tvPoseTimer = findViewById(R.id.tv_pose_timer)
        tvPoseProgress = findViewById(R.id.tv_pose_progress)
        tvBreathText = findViewById(R.id.tv_breath_text)
        btnPause = findViewById(R.id.btn_yoga_pause)

        findViewById<View>(R.id.btn_yoga_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_yoga_skip).setOnClickListener { nextPose() }
        findViewById<View>(R.id.btn_yoga_end).setOnClickListener { showFinishDialog() }

        btnPause.setOnClickListener {
            if (isPaused) {
                resumeSession()
            } else {
                pauseSession()
            }
        }

        loadPose(currentPoseIndex)
        handler.post(poseTimer)
    }

    private fun loadPose(index: Int) {
        if (index >= poses.size) {
            finishSession()
            return
        }
        val pose = poses[index]
        tvPoseName.text = pose.name
        tvPoseInstruction.text = pose.instruction
        poseRemainingSeconds = pose.durationSeconds
        tvPoseProgress.text = "${index + 1}/${poses.size}"
        updateTimerDisplay()
        updateBreath()
    }

    private fun updateTimerDisplay() {
        tvPoseTimer.text = poseRemainingSeconds.toString()
    }

    private fun updateBreath() {
        breathCycle++
        if (breathCycle % 4 < 2) {
            tvBreathText.text = "吸气"
        } else {
            tvBreathText.text = "呼气"
        }
    }

    private fun nextPose() {
        currentPoseIndex++
        handler.removeCallbacks(poseTimer)
        if (currentPoseIndex >= poses.size) {
            finishSession()
        } else {
            loadPose(currentPoseIndex)
            Toast.makeText(this, "下一个体式: ${poses[currentPoseIndex].name}", Toast.LENGTH_SHORT).show()
            handler.post(poseTimer)
        }
    }

    private fun pauseSession() {
        isPaused = true
        btnPause.text = "继续"
    }

    private fun resumeSession() {
        isPaused = false
        btnPause.text = "暂停"
    }

    private fun showFinishDialog() {
        handler.removeCallbacks(poseTimer)
        isActive = false

        val completedPoses = currentPoseIndex + 1
        val difficulties = arrayOf("轻松", "适中", "有挑战", "困难")
        AlertDialog.Builder(this)
            .setTitle("课程完成")
            .setMessage("请评价本次瑜伽难度")
            .setItems(difficulties) { _, which ->
                val diff = which + 1
                val cal = (totalElapsedSeconds * 0.06).toInt()
                val record = com.example.myfitnessapp.data.entity.WorkoutRecord(
                    sportType = "YOGA",
                    sportIconResId = WorkoutRecordHelper.getIconRes("YOGA"),
                    elapsedSeconds = totalElapsedSeconds,
                    totalDistance = 0.0,
                    totalCalories = cal,
                    pace = "$completedPoses 个体式",
                    timestamp = WorkoutRecordHelper.nowTimestamp(),
                    date = WorkoutRecordHelper.todayDate(),
                    yogaPosesCompleted = completedPoses,
                    yogaDifficulty = diff
                )
                viewModel.saveRecord(record)
                Toast.makeText(this, "难度评价: ${difficulties[which]}, 已完成 $completedPoses 个体式", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun finishSession() {
        isActive = false
        handler.removeCallbacks(poseTimer)
        showFinishDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(poseTimer)
    }
}

data class YogaPose(
    val name: String,
    val durationSeconds: Int,
    val instruction: String,
    val breathIn: Boolean
)