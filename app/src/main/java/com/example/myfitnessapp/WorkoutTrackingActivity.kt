package com.example.myfitnessapp

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.example.myfitnessapp.data.WorkoutRecordHelper
import com.example.myfitnessapp.data.entity.WorkoutRecord
import com.example.myfitnessapp.data.viewmodel.WorkoutRecordViewModel
import com.example.myfitnessapp.model.workout.OutdoorWorkoutConfig
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.example.myfitnessapp.service.LocationTrackingService
import com.example.myfitnessapp.service.StepTrackingService
import android.os.Build
import com.example.myfitnessapp.course.data.ActiveCourseSessionStore
import com.example.myfitnessapp.course.data.TrainingCourseRepository
import com.example.myfitnessapp.course.domain.CourseStep
import com.example.myfitnessapp.course.domain.TrainingCourse
import com.example.myfitnessapp.course.navigation.CourseNavigator

class WorkoutTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var config: OutdoorWorkoutConfig
    private lateinit var viewModel: WorkoutRecordViewModel
    private lateinit var locationService: LocationTrackingService
    private lateinit var stepService: StepTrackingService
    private val courseSessionStore by lazy { ActiveCourseSessionStore(this) }
    
    private var googleMap: GoogleMap? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val ACTIVITY_RECOGNITION_REQUEST_CODE = 1002

    // 计时
    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0
    private var isTimerRunning = true
    private var isPaused = false

    // 模拟数据
    private var totalDistance = 0.0
    private var totalCalories = 0
    private var currentPaceSpeed = 0.0 // 跑步=秒/公里，骑行=km/h
    private var maxSpeed = 0.0
    private var totalSteps = 0
    private var cadence = 0
    private var elevation = 0
    private var grade = 0.0
    private var gpsSignalWeak = false // GPS 信号弱标记

    // 分段计时
    private var lapSeconds = 0
    private var lapCount = 0
    private var isMetronomeOn = false
    private var isAutoPauseOn = true

    // 地图折叠状态
    private var isMapCollapsed = false
    private val courseRepository by lazy { TrainingCourseRepository() }
    private var activeCourse: TrainingCourse? = null
    private var shouldPersistCourseSession = true

    // 二级卡片 View 引用（最多4个）
    private val cardViews = mutableListOf<View>()
    private val cardLabelViews = mutableListOf<TextView>()
    private val cardValueViews = mutableListOf<TextView>()
    private val cardUnitViews = mutableListOf<TextView>()

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                elapsedSeconds++
                updateTimerDisplay()
                updateCourseRuntimeUi()
                persistCourseSessionProgress()
                
                // 如果没有定位权限，则使用模拟数据
                if (ContextCompat.checkSelfPermission(this@WorkoutTrackingActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    updateSimulatedData()
                } else {
                    // 有定位权限时，只模拟卡路里等非 GPS 数据
                    updateSimulatedNonGpsData()
                }
                
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_tracking)

        parseIntentAndBuildConfig()
        viewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)
        locationService = LocationTrackingService(this)
        stepService = StepTrackingService(this)
        
        cacheCardViews()
        applyConfig()
        setupMap()
        setupControls()
        setupBackPress()
        
        checkPermissions()
    }
    
    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap?.isMyLocationEnabled = true
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // 检查定位权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // 检查计步权限 (Android 10+ 需要)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startTracking()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            var locationGranted = false
            var activityRecognitionGranted = false

            for (i in permissions.indices) {
                if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    locationGranted = true
                }
                if (permissions[i] == Manifest.permission.ACTIVITY_RECOGNITION && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    activityRecognitionGranted = true
                }
            }

            if (locationGranted) {
                try {
                    googleMap?.isMyLocationEnabled = true
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "未授予定位权限，将使用模拟数据", Toast.LENGTH_SHORT).show()
            }

            if (!activityRecognitionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Toast.makeText(this, "未授予计步权限，将使用模拟步数", Toast.LENGTH_SHORT).show()
            }

            startTracking()
        }
    }

    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationService.startTracking()
        }
        
        if (config.isRunning()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || 
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                if (stepService.hasStepSensor()) {
                    stepService.startTracking()
                } else {
                    Toast.makeText(this, "设备不支持计步传感器，将使用模拟步数", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        startTimer()
        observeSensorData()
    }

    private fun observeSensorData() {
        // 观察 GPS 数据
        locationService.currentLocation.observe(this) { location ->
            // 更新地图中心点
            val latLng = LatLng(location.latitude, location.longitude)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }

        locationService.routePoints.observe(this) { points ->
            // 绘制轨迹
            googleMap?.clear()
            if (points.isNotEmpty()) {
                val polylineOptions = PolylineOptions()
                    .addAll(points)
                    .color(Color.parseColor("#2ECC71"))
                    .width(10f)
                googleMap?.addPolyline(polylineOptions)
            }
        }

        locationService.totalDistance.observe(this) { distance ->
            totalDistance = distance
            updateMainDataDisplay()
        }

        locationService.currentSpeed.observe(this) { speed ->
            // 将 m/s 转换为 km/h 或 配速
            if (config.isRunning()) {
                // 跑步配速 (分钟/公里)
                if (speed > 0) {
                    val paceSecondsPerKm = 1000 / speed
                    currentPaceSpeed = paceSecondsPerKm.toDouble()
                }
            } else {
                // 骑行速度 (km/h)
                currentPaceSpeed = (speed * 3.6).toDouble()
                if (currentPaceSpeed > maxSpeed) {
                    maxSpeed = currentPaceSpeed
                    findViewById<TextView>(R.id.tv_max_speed).apply {
                        visibility = View.VISIBLE
                        text = String.format(Locale.getDefault(), "最高 %.1f km/h", maxSpeed)
                    }
                }
            }
            updateMainDataDisplay()
        }

        // 观察计步传感器数据
        stepService.currentSteps.observe(this) { steps ->
            if (config.isRunning()) {
                totalSteps = steps
                updateSecondaryCards()
            }
        }

        stepService.currentCadence.observe(this) { currentCadence ->
            if (config.isRunning()) {
                cadence = currentCadence
                updateSecondaryCards()
            }
        }
    }

    // ============================================================
    // 解析 Intent 并构建配置
    // ============================================================
    private fun parseIntentAndBuildConfig() {
        val type = intent.getStringExtra(EXTRA_SPORT_TYPE) ?: "RUN"
        activeCourse = CourseNavigator.courseIdOf(intent)?.let { courseRepository.getCourseById(it) }
        config = when (type.uppercase()) {
            "CYCLING" -> OutdoorWorkoutConfig.forCycling()
            else -> OutdoorWorkoutConfig.forRunning()
        }
        restoreCourseProgress()
    }

    // ============================================================
    // 缓存卡片 View 引用
    // ============================================================
    private fun cacheCardViews() {
        val cardIds = listOf(
            R.id.card_secondary_0, R.id.card_secondary_1,
            R.id.card_secondary_2, R.id.card_secondary_3
        )
        val labelIds = listOf(
            R.id.tv_secondary_label_0, R.id.tv_secondary_label_1,
            R.id.tv_secondary_label_2, R.id.tv_secondary_label_3
        )
        val valueIds = listOf(
            R.id.tv_secondary_0, R.id.tv_secondary_1,
            R.id.tv_secondary_2, R.id.tv_secondary_3
        )
        val unitIds = listOf(
            R.id.tv_secondary_unit_0, R.id.tv_secondary_unit_1,
            R.id.tv_secondary_unit_2, R.id.tv_secondary_unit_3
        )

        for (i in cardIds.indices) {
            cardViews.add(findViewById(cardIds[i]))
            cardLabelViews.add(findViewById(labelIds[i]))
            cardValueViews.add(findViewById(valueIds[i]))
            cardUnitViews.add(findViewById(unitIds[i]))
        }
    }

    // ============================================================
    // 根据配置初始化界面
    // ============================================================
    private fun applyConfig() {
        // 地图比例
        val guideline = findViewById<Guideline>(R.id.guideline_map)
        guideline.setGuidelinePercent(config.mapHeightRatio)

        // 地图折叠按钮
        findViewById<View>(R.id.btn_toggle_map).isVisible = config.mapCollapsible

        // 运动图标 & 名称
        val iconRes = intent.getIntExtra(EXTRA_SPORT_ICON, R.drawable.ic_running)
        val name = intent.getStringExtra(EXTRA_SPORT_NAME) ?: config.type.label
        findViewById<android.widget.ImageView>(R.id.iv_tracking_sport_icon).setImageResource(iconRes)
        findViewById<TextView>(R.id.tv_tracking_sport_name).text = name
        updateCourseRuntimeUi()

        // 最高速度标签（骑行专属）
        findViewById<TextView>(R.id.tv_max_speed).isVisible = config.showMaxSpeed

        // 跑步专属：Lap + 节拍器
        findViewById<View>(R.id.ll_run_extras).isVisible = config.showLapButton
        findViewById<TextView>(R.id.chip_metronome).isVisible = config.showMetronome

        // 一级数据标签
        findViewById<TextView>(R.id.tv_primary_label).text = "${config.primaryLabel} ${config.primaryUnit}"
        findViewById<TextView>(R.id.tv_primary_value).setTextColor(config.primaryColor)

        // 二级数据卡
        applySecondaryCards()

        // 控制区按钮
        findViewById<View>(R.id.btn_lock_screen).isVisible = config.showLockScreen
        findViewById<View>(R.id.btn_auto_pause).isVisible = config.showAutoPause
        if (config.showAutoPause) {
            updateAutoPauseButtonState()
        }
    }

    private fun applySecondaryCards() {
        val cards = config.secondaryCards
        for (i in cardViews.indices) {
            if (i < cards.size) {
                cardViews[i].isVisible = true
                cardLabelViews[i].text = cards[i].label
                cardValueViews[i].setTextColor(cards[i].color)
                cardUnitViews[i].text = cards[i].unit
            } else {
                cardViews[i].isVisible = false
            }
        }
    }

    // ============================================================
    // 控制按钮
    // ============================================================
    private fun setupControls() {
        findViewById<View>(R.id.btn_pause_resume).setOnClickListener { togglePauseResume() }
        findViewById<View>(R.id.btn_stop).setOnClickListener { showStopConfirmDialog() }

        // 地图折叠
        findViewById<View>(R.id.btn_toggle_map).setOnClickListener { toggleMap() }

        // 跑步专属
        findViewById<View>(R.id.btn_lap).setOnClickListener { recordLap() }
        findViewById<View>(R.id.chip_metronome).setOnClickListener { toggleMetronome() }

        // 骑行专属
        findViewById<View>(R.id.btn_auto_pause).setOnClickListener { toggleAutoPause() }

        // 锁屏 & 播报（占位）
        findViewById<View>(R.id.btn_lock_screen).setOnClickListener {
            Toast.makeText(this, "锁屏功能开发中", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btn_voice).setOnClickListener {
            Toast.makeText(this, "语音播报功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })
    }

    // ============================================================
    // 地图折叠
    // ============================================================
    private fun toggleMap() {
        if (!config.mapCollapsible) return

        isMapCollapsed = !isMapCollapsed
        val guideline = findViewById<Guideline>(R.id.guideline_map)
        val btn = findViewById<TextView>(R.id.btn_toggle_map)
        val controlBar = findViewById<View>(R.id.control_bar)
        val collapsedTranslation = -28f * resources.displayMetrics.density

        if (isMapCollapsed) {
            guideline.setGuidelinePercent(0.18f)
            btn.text = "⌄"
            controlBar.animate()
                .translationY(collapsedTranslation)
                .setDuration(220)
                .start()
        } else {
            guideline.setGuidelinePercent(config.mapHeightRatio)
            btn.text = "⌃"
            controlBar.animate()
                .translationY(0f)
                .setDuration(220)
                .start()
        }
    }

    // ============================================================
    // 计时器
    // ============================================================
    private fun startTimer() {
        isTimerRunning = true
        isPaused = false
        handler.post(timerRunnable)
        updateStatusLabel("运动中")
        updateCourseRuntimeUi()
    }

    private fun togglePauseResume() {
        if (isPaused) {
            resumeTimer()
        } else {
            pauseTimer()
        }
    }

    private fun pauseTimer() {
        isTimerRunning = false
        isPaused = true
        val btn = findViewById<TextView>(R.id.btn_pause_resume)
        btn.text = "继续"
        btn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_resume, 0, 0)
        updateStatusLabel("已暂停")
        locationService.pauseTracking()
        stepService.pauseTracking()
    }

    private fun resumeTimer() {
        isTimerRunning = true
        isPaused = false
        val btn = findViewById<TextView>(R.id.btn_pause_resume)
        btn.text = "暂停"
        btn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_pause, 0, 0)
        updateStatusLabel("运动中")
        updateCourseRuntimeUi()
        handler.post(timerRunnable)
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationService.startTracking()
        }
        
        if (config.isRunning() && stepService.hasStepSensor()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || 
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                stepService.startTracking()
            }
        }
    }

    private fun updateTimerDisplay() {
        val timeStr = formatTime(elapsedSeconds)
        findViewById<TextView>(R.id.tv_tracking_timer).text = timeStr
        findViewById<TextView>(R.id.tv_sub_timer).text = timeStr
    }

    private fun updateStatusLabel(label: String) {
        findViewById<TextView>(R.id.tv_tracking_status).text = label
    }

    private fun updateCourseRuntimeUi() {
        val phaseView = findViewById<TextView>(R.id.tv_course_phase)
        val detailView = findViewById<TextView>(R.id.tv_course_phase_detail)
        val course = activeCourse
        if (course == null || course.plan.steps.isEmpty()) {
            phaseView.isVisible = false
            detailView.isVisible = false
            return
        }

        phaseView.isVisible = true
        detailView.isVisible = true

        val totalCourseSeconds = course.plan.steps.sumOf { it.durationSeconds }
        if (elapsedSeconds >= totalCourseSeconds) {
            updateStatusLabel("课程目标已完成")
            phaseView.text = "课程计划已完成"
            detailView.text = "可继续自由训练，或点击结束保存本次课程记录"
            return
        }

        val currentStep = findCurrentCourseStep(course.plan.steps, elapsedSeconds)
        val stepIndex = course.plan.steps.indexOf(currentStep)
        val elapsedInStep = elapsedSeconds - course.plan.steps.take(stepIndex).sumOf { it.durationSeconds }
        val remaining = (currentStep.durationSeconds - elapsedInStep).coerceAtLeast(0)

        updateStatusLabel("课程进行中 ${stepIndex + 1}/${course.plan.steps.size}")
        phaseView.text = currentStep.title
        val target = currentStep.target ?: currentStep.instruction
        detailView.text = "目标：$target · 剩余 ${formatShortTime(remaining)}"
    }

    private fun restoreCourseProgress() {
        val course = activeCourse ?: return
        val session = courseSessionStore.getActiveFor(course.id) ?: return
        elapsedSeconds = session.totalElapsedSeconds
        updateTimerDisplay()
    }

    private fun persistCourseSessionProgress() {
        val course = activeCourse ?: return
        val stepIndex = findCurrentCourseStepIndex(course.plan.steps, elapsedSeconds)
        val stepElapsed = elapsedSeconds - course.plan.steps.take(stepIndex).sumOf { it.durationSeconds }
        val existing = courseSessionStore.getActiveFor(course.id)
        courseSessionStore.save(
            (existing ?: defaultCourseSession(course)).copy(
                currentStepIndex = stepIndex,
                currentStepElapsedSeconds = stepElapsed.coerceAtLeast(0),
                totalElapsedSeconds = elapsedSeconds
            )
        )
    }

    private fun defaultCourseSession(course: TrainingCourse) = com.example.myfitnessapp.course.domain.ActiveCourseSession(
        courseId = course.id,
        courseTitle = course.title,
        sportType = course.sportType,
        status = com.example.myfitnessapp.course.domain.CourseSessionStatus.IN_PROGRESS,
        startedAt = System.currentTimeMillis()
    )

    private fun findCurrentCourseStep(steps: List<CourseStep>, totalElapsed: Int): CourseStep {
        var elapsed = totalElapsed
        for (step in steps) {
            if (elapsed < step.durationSeconds) {
                return step
            }
            elapsed -= step.durationSeconds
        }
        return steps.last()
    }

    private fun findCurrentCourseStepIndex(steps: List<CourseStep>, totalElapsed: Int): Int {
        var elapsed = totalElapsed
        steps.forEachIndexed { index, step ->
            if (elapsed < step.durationSeconds) {
                return index
            }
            elapsed -= step.durationSeconds
        }
        return (steps.size - 1).coerceAtLeast(0)
    }

    private fun formatShortTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainSeconds)
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }

    // ============================================================
    // 模拟数据更新
    // ============================================================
    private fun updateSimulatedNonGpsData() {
        // 仅模拟卡路里等非 GPS 数据
        totalCalories += 1
        
        // 如果没有计步传感器权限或设备不支持，则模拟步数
        if (config.isRunning() && (!stepService.hasStepSensor() || 
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
             ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED))) {
            totalSteps += 2
            cadence = 160 + (Math.random() * 20).toInt()
        } else if (config.isCycling()) {
            cadence = 80 + (Math.random() * 15).toInt()
        }
        
        updateSecondaryCards()
    }

    private fun updateMainDataDisplay() {
        // 距离
        findViewById<TextView>(R.id.tv_sub_distance).text =
            String.format(Locale.getDefault(), "%.2f", totalDistance)

        // 速度/配速
        val primaryStr = config.primaryFormat(currentPaceSpeed)
        findViewById<TextView>(R.id.tv_primary_value).text = primaryStr
        
        if (config.isRunning()) {
            updatePaceColor()
        } else {
            updateSpeedColor()
        }
    }

    // ============================================================
    // 模拟数据更新
    // ============================================================
    private fun updateSimulatedData() {
        totalDistance += config.distanceMultiplier
        totalCalories += config.caloriesMultiplier.toInt().coerceAtLeast(1)

        if (config.isRunning()) {
            updateRunningData()
        } else {
            updateCyclingData()
        }

        updateMainDataDisplay()
    }

    // ============================================================
    // 跑步数据更新
    // ============================================================
    private fun updateSecondaryCards() {
        if (config.isRunning()) {
            cardValueViews[0].text = cadence.toString()
            cardValueViews[1].text = totalCalories.toString()
            cardValueViews[2].text = totalSteps.toString()
        } else {
            cardValueViews[0].text = if (gpsSignalWeak) "--" else elevation.toString()
            cardValueViews[1].text = String.format(Locale.getDefault(), "%.1f", grade)
            cardValueViews[2].text = totalCalories.toString()
            cardValueViews[3].text = String.format(Locale.getDefault(), "%.1f", maxSpeed)
        }
    }

    private fun updateRunningData() {
        // 配速波动 5'30"~6'30" (330~390秒/公里)
        currentPaceSpeed = 330 + (Math.random() * 60)

        // 步频（模拟，后续由加速度传感器 + ViewModel 接入）
        cadence = updateCadenceFromSensor()
        // 步数
        totalSteps += (cadence / 60).coerceAtLeast(2)

        updateSecondaryCards()
    }

    // ============================================================
    // 骑行数据更新
    // ============================================================
    private fun updateCyclingData() {
        // 速度 22~28 km/h
        currentPaceSpeed = 22.0 + (Math.random() * 6)

        // 最高速度
        if (currentPaceSpeed > maxSpeed) {
            maxSpeed = currentPaceSpeed
            findViewById<TextView>(R.id.tv_max_speed).text =
                String.format(Locale.getDefault(), "最高 %.1f km/h", maxSpeed)
        }

        // 海拔爬升（GPS/气压计，信号弱显示 "--"）
        elevation = updateElevationFromGPS()
        // 平均坡度 = 爬升高度 / 水平距离
        grade = if (totalDistance > 0.01) {
            (elevation / (totalDistance * 1000)) * 100.0
        } else {
            0.0
        }

        updateSecondaryCards()
    }

    // ============================================================
    // 步频更新接口（预留 ViewModel 接入）
    // 当前为模拟数据，后续接入加速度传感器后替换
    // ============================================================
    private fun updateCadenceFromSensor(): Int {
        // TODO: 接入加速度传感器计步 + 时间计算
        // ViewModel 接口: cadenceViewModel.getCadence()
        // 传感器方案: SensorManager.registerListener(TYPE_STEP_DETECTOR)
        return (160 + (Math.random() * 20)).toInt()
    }

    // ============================================================
    // 海拔更新接口（预留 GPS/气压计接入）
    // ============================================================
    private fun updateElevationFromGPS(): Int {
        // TODO: 接入 GPS 高程或气压计
        // 若信号弱 gpsSignalWeak = true，卡片显示 "--"
        gpsSignalWeak = (Math.random() < 0.05) // 5% 概率模拟信号弱
        if (gpsSignalWeak) return elevation // 保持上次值
        return elevation + (Math.random() * 3).toInt()
    }

    // ============================================================
    // 动态变色：配速/速度
    // ============================================================
    private fun updatePaceColor() {
        val tv = findViewById<TextView>(R.id.tv_primary_value)
        when {
            currentPaceSpeed < config.paceFast -> tv.setTextColor(0xFF2ECC71.toInt())   // 快 → 绿
            currentPaceSpeed > config.paceSlow -> tv.setTextColor(0xFFE74C3C.toInt())   // 慢 → 红
            else -> tv.setTextColor(config.primaryColor)                                 // 正常 → 主色
        }
    }

    private fun updateSpeedColor() {
        val tv = findViewById<TextView>(R.id.tv_primary_value)
        when {
            currentPaceSpeed > config.paceFast -> tv.setTextColor(0xFF2ECC71.toInt())   // 快 → 绿
            currentPaceSpeed < config.paceSlow -> tv.setTextColor(0xFFE74C3C.toInt())   // 慢 → 红
            else -> tv.setTextColor(config.primaryColor)                                 // 正常 → 主色
        }
    }

    // ============================================================
    // 分段计时 (Lap)
    // ============================================================
    private fun recordLap() {
        lapCount++
        val lapTime = formatTime(lapSeconds)
        lapSeconds = 0
        Toast.makeText(
            this,
            "第 $lapCount 段: $lapTime (总: ${formatTime(elapsedSeconds)})",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ============================================================
    // 节拍器
    // ============================================================
    private fun toggleMetronome() {
        isMetronomeOn = !isMetronomeOn
        val chip = findViewById<TextView>(R.id.chip_metronome)
        if (isMetronomeOn) {
            chip.setBackgroundColor(Color.parseColor("#FF6B35"))
            chip.text = "节拍器: 开"
            Toast.makeText(this, "节拍器已开启 (180bpm)", Toast.LENGTH_SHORT).show()
        } else {
            chip.setBackgroundColor(Color.parseColor("#33FFFFFF"))
            chip.text = "节拍器"
        }
    }

    // ============================================================
    // 自动暂停
    // ============================================================
    private fun toggleAutoPause() {
        isAutoPauseOn = !isAutoPauseOn
        updateAutoPauseButtonState()
    }

    private fun updateAutoPauseButtonState() {
        val btn = findViewById<TextView>(R.id.btn_auto_pause)
        if (isAutoPauseOn) {
            btn.setBackgroundColor(Color.parseColor("#1E88E5"))
            btn.text = "自动暂停: 开"
        } else {
            btn.setBackgroundColor(Color.parseColor("#33FFFFFF"))
            btn.text = "自动暂停: 关"
        }
    }

    // ============================================================
    // 结束确认弹窗
    // ============================================================
    private fun showStopConfirmDialog() {
        if (isTimerRunning) {
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
    // 运动摘要弹窗
    // ============================================================
    private fun showSummarySheet() {
        val bottomSheet = BottomSheetDialog(this)
        val view = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet_workout_summary, null)

        val duration = formatTime(elapsedSeconds)
        val distance = String.format(Locale.getDefault(), "%.2f", totalDistance)
        val primaryStr = if (config.isRunning()) {
            "配速: ${config.primaryFormat(currentPaceSpeed)}"
        } else {
            "速度: ${config.primaryFormat(currentPaceSpeed)} km/h"
        }

        val titleView = view.findViewById<TextView>(R.id.summary_tv_title)
        val subtitleView = view.findViewById<TextView>(R.id.summary_tv_subtitle)
        view.findViewById<TextView>(R.id.summary_tv_duration).text = duration
        view.findViewById<TextView>(R.id.summary_tv_distance).text = "$distance 公里"
        view.findViewById<TextView>(R.id.summary_tv_calories).text = "$totalCalories kcal"
        view.findViewById<TextView>(R.id.summary_tv_avg_pace).text = primaryStr
        bindCourseSummary(view, titleView, subtitleView)

        view.findViewById<View>(R.id.summary_btn_save).setOnClickListener {
            saveWorkoutRecord()
            bottomSheet.dismiss()
            Toast.makeText(this, "记录已保存", Toast.LENGTH_SHORT).show()
            onFinishWorkout()
        }

        view.findViewById<View>(R.id.summary_btn_discard).setOnClickListener {
            bottomSheet.dismiss()
            Toast.makeText(this, "已放弃本次记录", Toast.LENGTH_SHORT).show()
            onFinishWorkout()
        }

        bottomSheet.setContentView(view)
        bottomSheet.setCancelable(false)
        bottomSheet.show()
    }

    private fun bindCourseSummary(view: View, titleView: TextView, subtitleView: TextView) {
        val course = activeCourse
        val container = view.findViewById<View>(R.id.summary_course_container)
        if (course == null) {
            container.visibility = View.GONE
            titleView.text = "运动完成！"
            subtitleView.text = "本次运动数据摘要"
            return
        }

        val totalSteps = course.plan.steps.size
        val completedSteps = calculateCompletedCourseSteps(course)
        val completionRate = ((elapsedSeconds.coerceAtMost(courseTotalSeconds(course)) * 100f) /
            courseTotalSeconds(course).coerceAtLeast(1)).toInt()
        val goalMessage = if (elapsedSeconds >= courseTotalSeconds(course)) {
            "目标达成：已完成课程计划时长与主要阶段"
        } else {
            "目标达成：已完成 ${completedSteps}/${totalSteps} 步，建议下次继续完成整节课"
        }

        container.visibility = View.VISIBLE
        titleView.text = "课程完成"
        subtitleView.text = "${course.title} · 本次课程结果"
        view.findViewById<TextView>(R.id.summary_tv_course_title).text = "课程：${course.title}"
        view.findViewById<TextView>(R.id.summary_tv_course_progress).text =
            "课程进度：${completedSteps}/${totalSteps} 步，完成度 ${completionRate.coerceIn(0, 100)}%"
        view.findViewById<TextView>(R.id.summary_tv_course_goal).text = goalMessage
    }

    private fun calculateCompletedCourseSteps(course: TrainingCourse): Int {
        var remaining = elapsedSeconds
        var completed = 0
        for (step in course.plan.steps) {
            if (remaining >= step.durationSeconds) {
                completed++
                remaining -= step.durationSeconds
            } else {
                break
            }
        }
        return completed
    }

    private fun courseTotalSeconds(course: TrainingCourse): Int {
        return course.plan.steps.sumOf { it.durationSeconds }
    }

    // ============================================================
    // 保存记录
    // ============================================================
    private fun saveWorkoutRecord() {
        val sportType = if (config.isRunning()) "RUN" else "CYCLING"
        val iconRes = WorkoutRecordHelper.getIconRes(sportType)
        val timestamp = WorkoutRecordHelper.nowTimestamp()
        val date = WorkoutRecordHelper.todayDate()
        val paceStr = config.primaryFormat(currentPaceSpeed)

        val record = WorkoutRecord(
            sportType = sportType,
            sportIconResId = iconRes,
            elapsedSeconds = elapsedSeconds,
            totalDistance = totalDistance,
            totalCalories = totalCalories,
            pace = paceStr,
            timestamp = timestamp,
            date = date,
            runSteps = if (config.isRunning()) totalSteps else 0,
            runCadence = if (config.isRunning()) cadence else 0,
            cyclingElevation = if (config.isCycling()) elevation else 0,
            cyclingMaxSpeed = if (config.isCycling()) maxSpeed else 0.0
        )

        viewModel.saveRecord(record)
    }

    private fun onFinishWorkout() {
        shouldPersistCourseSession = false
        ActiveCourseSessionStore(this).clear(CourseNavigator.courseIdOf(intent))
        finish()
    }

    private fun handleBackPressed() {
        if (elapsedSeconds > 0 && isTimerRunning) {
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
        if (shouldPersistCourseSession) {
            persistCourseSessionProgress()
        }
        stopTimer()
    }

    private fun stopTimer() {
        isTimerRunning = false
        handler.removeCallbacks(timerRunnable)
        locationService.stopTracking()
        stepService.stopTracking()
    }

    companion object {
        const val EXTRA_SPORT_NAME = "sport_name"
        const val EXTRA_SPORT_ICON = "sport_icon"
        const val EXTRA_SPORT_TYPE = "sport_type" // RUN / CYCLING
    }
}
