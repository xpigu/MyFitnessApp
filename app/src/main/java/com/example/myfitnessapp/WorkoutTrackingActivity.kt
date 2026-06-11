package com.example.myfitnessapp

import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
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
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myfitnessapp.service.LocationTrackingService
import com.example.myfitnessapp.service.StepTrackingService
import android.os.Build
import com.example.myfitnessapp.course.data.ActiveCourseSessionStore
import com.example.myfitnessapp.course.data.TrainingCourseRepository
import com.example.myfitnessapp.course.domain.CourseStep
import com.example.myfitnessapp.course.domain.TrainingCourse
import com.example.myfitnessapp.course.navigation.CourseNavigator

class WorkoutTrackingActivity : AppCompatActivity() {

    private lateinit var config: OutdoorWorkoutConfig
    private lateinit var viewModel: WorkoutRecordViewModel
    private lateinit var locationService: LocationTrackingService
    private lateinit var stepService: StepTrackingService
    private val courseSessionStore by lazy { ActiveCourseSessionStore(this) }
    
    private lateinit var mapView: MapView
    private var aMap: AMap? = null
    private var currentLocationMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var hasCenteredOnUser = false
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
    private var calorieAccumulator = 0.0
    private var lastCalorieDistance = 0.0
    private var latestLocationSpeedMps = 0f
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
    private var isMetronomeEnabled = false
    private var isLocked = false
    private var isVoiceBroadcastEnabled = false
    private var isVoiceEngineReady = false
    private var isActivityVisible = true
    private var pendingStartVoiceAnnouncement = false
    private var hasAnnouncedWorkoutEnd = false
    private var metronomeBpm = 170
    private var isAutoPauseOn = true
    private var nextVoiceDistanceKm = 1.0
    private var nextVoiceElapsedSeconds = VOICE_ANNOUNCEMENT_INTERVAL_SECONDS
    private var lastVoiceDistanceCheckpointKm = 0.0
    private var lastVoiceDistanceCheckpointElapsedSeconds = 0
    private var lastVoiceDistanceCheckpointSteps = 0
    private var textToSpeech: TextToSpeech? = null
    private val metronomeToneGenerator by lazy(LazyThreadSafetyMode.NONE) {
        ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    }
    private val metronomeRunnable = object : Runnable {
        override fun run() {
            if (!isMetronomeOn || isPaused || !isTimerRunning) return
            metronomeToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
            handler.postDelayed(this, metronomeIntervalMs())
        }
    }

    // 地图折叠状态
    private var isMapCollapsed = false
    private val courseRepository by lazy { TrainingCourseRepository() }
    private var activeCourse: TrainingCourse? = null
    private var shouldPersistCourseSession = true
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // 二级卡片 View 引用（最多 4 个）
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

                maybeSpeakProgressUpdate()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_tracking)

        mapView = findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        parseIntentAndBuildConfig()
        viewModel = ViewModelProvider(this).get(WorkoutRecordViewModel::class.java)
        locationService = LocationTrackingService(this)
        stepService = StepTrackingService(this)
        
        cacheCardViews()
        restoreVoiceBroadcastPreference()
        applyConfig()
        setupMap()
        setupControls()
        setupBackPress()
        initializeVoiceEngine()
        
        checkPermissions()
    }
    
    private fun setupMap() {
        aMap = mapView.map.apply {
            uiSettings?.isMyLocationButtonEnabled = false
            uiSettings?.isZoomControlsEnabled = false
            uiSettings?.isCompassEnabled = false
            moveCamera(CameraUpdateFactory.zoomTo(16f))
        }
        updateMapLocationLayer()
        renderRoute(emptyList())
    }

    private fun hasAnyLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasPreciseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateMapLocationLayer() {
        val hasLocationPermission = hasAnyLocationPermission()
        try {
            aMap?.apply {
                isMyLocationEnabled = hasLocationPermission
                if (hasLocationPermission) {
                    myLocationStyle = MyLocationStyle().apply {
                        myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                        interval(1000L)
                        radiusFillColor(Color.TRANSPARENT)
                        strokeColor(Color.TRANSPARENT)
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun renderRoute(points: List<android.location.Location>) {
        routePolyline?.remove()
        routePolyline = null

        if (points.isEmpty()) return

        val amapPoints = points.map { LatLng(it.latitude, it.longitude) }
        routePolyline = aMap?.addPolyline(
            PolylineOptions()
                .addAll(amapPoints)
                .color(ContextCompat.getColor(this, R.color.tracking_route_line))
                .width(18f)
        )
    }

    private fun updateCurrentLocationMarker(latLng: LatLng) {
        currentLocationMarker?.position = latLng
        if (currentLocationMarker == null) {
            currentLocationMarker = aMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("当前位置")
                    .anchor(0.5f, 0.5f)
            )
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
            var preciseLocationGranted = false
            var activityRecognitionGranted = false

            for (i in permissions.indices) {
                if ((permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION ||
                    permissions[i] == Manifest.permission.ACCESS_COARSE_LOCATION) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    locationGranted = true
                }
                if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    preciseLocationGranted = true
                }
                if (permissions[i] == Manifest.permission.ACTIVITY_RECOGNITION && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    activityRecognitionGranted = true
                }
            }

            if (locationGranted) {
                updateMapLocationLayer()
                if (!preciseLocationGranted) {
                    showAppFeedback("当前仅允许大致位置，定位可能会有偏差，建议在系统设置中开启精确位置", FeedbackType.WARNING)
                }
            } else {
                showAppFeedback("未授予定位权限，将使用模拟数据", FeedbackType.WARNING)
            }

            if (!activityRecognitionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                showAppFeedback("未授予计步权限，将使用模拟步数", FeedbackType.WARNING)
            }

            startTracking()
        }
    }

    private fun startTracking() {
        if (hasAnyLocationPermission()) {
            locationService.startTracking()
        }
        
        if (config.isRunning()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || 
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                if (stepService.hasStepSensor()) {
                    stepService.startTracking()
                } else {
                    showAppFeedback("设备不支持计步传感器，将使用模拟步数", FeedbackType.WARNING)
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
            updateCurrentLocationMarker(latLng)
            if (!hasCenteredOnUser) {
                hasCenteredOnUser = true
                aMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            } else {
                aMap?.animateCamera(CameraUpdateFactory.changeLatLng(latLng))
            }
        }

        locationService.routePoints.observe(this) { points ->
            renderRoute(points)
        }

        locationService.totalDistance.observe(this) { distance ->
            totalDistance = distance
            updatePrimaryMetricFromLatestSignal()
            updateMainDataDisplay()
        }

        locationService.currentSpeed.observe(this) { speed ->
            latestLocationSpeedMps = speed
            updatePrimaryMetricFromLatestSignal()
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
        updateMetronomeChipState()
        findViewById<TextView>(R.id.btn_voice).isVisible = config.isRunning()
        updateVoiceButtonState()

        // 一级数据标签
        findViewById<TextView>(R.id.tv_primary_label).text = "${config.primaryLabel} ${config.primaryUnit}"
        findViewById<TextView>(R.id.tv_primary_value).setTextColor(getColor(config.primaryColorRes))

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
                cardValueViews[i].setTextColor(getColor(cards[i].colorRes))
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
        findViewById<View>(R.id.btn_stop).setOnClickListener {
            showStopConfirmDialog()
        }

        // 地图折叠
        findViewById<View>(R.id.btn_toggle_map).setOnClickListener { toggleMap() }

        // 跑步专属
        findViewById<View>(R.id.btn_lap).setOnClickListener { recordLap() }
        findViewById<View>(R.id.chip_metronome).setOnClickListener { toggleMetronome() }

        // 骑行专属
        findViewById<View>(R.id.btn_auto_pause).setOnClickListener { toggleAutoPause() }

        // 锁屏 & 播报（占位）
        findViewById<View>(R.id.btn_lock_screen).setOnClickListener {
            setLockState(true)
        }
        
        findViewById<View>(R.id.layout_lock_overlay).setOnLongClickListener {
            setLockState(false)
            true
        }

        findViewById<View>(R.id.btn_voice).setOnClickListener {
            toggleVoiceBroadcast()
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
        hasAnnouncedWorkoutEnd = false
        handler.post(timerRunnable)
        updateStatusLabel("运动中")
        updateCourseRuntimeUi()
        if (isVoiceBroadcastEnabled && config.isRunning()) {
            resetVoiceAnnouncementSchedule()
            requestStartVoiceAnnouncement()
        }
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
        suspendMetronome()
        textToSpeech?.stop()
        if (isVoiceBroadcastEnabled && config.isRunning()) {
            speakVoice("运动已暂停", flush = true, allowWhenPaused = true)
        }
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

        if (isMetronomeEnabled) {
            startMetronome(showToast = false)
        }

        if (isVoiceBroadcastEnabled && config.isRunning()) {
            speakVoice("继续运动", flush = true, allowWhenPaused = true)
        }
    }

    private fun initializeVoiceEngine() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                isVoiceEngineReady = false
                return@TextToSpeech
            }

            val tts = textToSpeech ?: return@TextToSpeech
            val preferred = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
            isVoiceEngineReady = preferred != TextToSpeech.LANG_MISSING_DATA &&
                preferred != TextToSpeech.LANG_NOT_SUPPORTED

            if (!isVoiceEngineReady) {
                val fallback = tts.setLanguage(Locale.CHINESE)
                isVoiceEngineReady = fallback != TextToSpeech.LANG_MISSING_DATA &&
                    fallback != TextToSpeech.LANG_NOT_SUPPORTED
            }

            if (isVoiceEngineReady) {
                tts.setSpeechRate(1.02f)
                flushPendingStartVoiceAnnouncement()
            }
        }
    }

    private fun toggleVoiceBroadcast() {
        if (!config.isRunning()) {
            showAppFeedback("当前版本仅支持跑步语音播报", FeedbackType.WARNING)
            return
        }

        if (isVoiceBroadcastEnabled) {
            setVoiceBroadcastEnabled(false)
            return
        }

        if (!isVoiceEngineReady) {
            showAppFeedback("系统语音引擎初始化中，请稍后再试", FeedbackType.INFO)
            return
        }

        setVoiceBroadcastEnabled(true)
    }

    private fun setVoiceBroadcastEnabled(enabled: Boolean) {
        if (enabled == isVoiceBroadcastEnabled) {
            return
        }

        isVoiceBroadcastEnabled = enabled
        saveVoiceBroadcastPreference(enabled)
        updateVoiceButtonState()

        if (enabled) {
            resetVoiceAnnouncementSchedule()
            showAppFeedback("语音播报已开启", FeedbackType.SUCCESS)
        } else {
            pendingStartVoiceAnnouncement = false
            textToSpeech?.stop()
            showAppFeedback("语音播报已关闭", FeedbackType.INFO)
        }
    }

    private fun requestStartVoiceAnnouncement() {
        if (isVoiceEngineReady) {
            pendingStartVoiceAnnouncement = false
            speakVoice("开始跑步", flush = true, allowWhenPaused = true)
        } else {
            pendingStartVoiceAnnouncement = true
        }
    }

    private fun flushPendingStartVoiceAnnouncement() {
        if (!pendingStartVoiceAnnouncement || !isVoiceBroadcastEnabled || !config.isRunning() || !isTimerRunning) {
            return
        }
        pendingStartVoiceAnnouncement = false
        speakVoice("开始跑步", flush = true, allowWhenPaused = true)
    }

    private fun updateVoiceButtonState() {
        val button = findViewById<TextView>(R.id.btn_voice)
        if (!config.isRunning()) {
            button.isVisible = false
            return
        }
        if (isVoiceBroadcastEnabled) {
            button.setBackgroundResource(R.drawable.tracking_primary_btn_bg)
            button.text = "🔊"
            button.setTextColor(getColor(R.color.tracking_primary_button_text))
        } else {
            button.setBackgroundResource(R.drawable.tracking_control_circle_bg)
            button.text = "🔈"
            button.setTextColor(getColor(R.color.tracking_on_primary))
        }
    }

    private fun restoreVoiceBroadcastPreference() {
        isVoiceBroadcastEnabled = prefs.getBoolean(voiceBroadcastPreferenceKey(), false)
    }

    private fun saveVoiceBroadcastPreference(enabled: Boolean) {
        prefs.edit().putBoolean(voiceBroadcastPreferenceKey(), enabled).apply()
    }

    private fun voiceBroadcastPreferenceKey(): String {
        val username = AuthSessionManager.getUsername(this).ifBlank { "guest" }
        return "${PREF_VOICE_BROADCAST_PREFIX}_$username"
    }

    private fun updateTimerDisplay() {
        val timeStr = formatTime(elapsedSeconds)
        findViewById<TextView>(R.id.tv_tracking_timer).text = timeStr
        findViewById<TextView>(R.id.tv_sub_timer).text = timeStr
        updatePrimaryMetricFromLatestSignal()
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
        // 如果没有计步传感器权限或设备不支持，则模拟步数
        if (config.isRunning() && (!stepService.hasStepSensor() || 
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
             ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED))) {
            totalSteps += 2
            cadence = 160 + (Math.random() * 20).toInt()
        } else if (config.isCycling()) {
            cadence = 80 + (Math.random() * 15).toInt()
        }

        updateEstimatedCalories(isSimulatedMovement = false)
        updateSecondaryCards()
    }

    private fun updateMainDataDisplay() {
        // 距离
        findViewById<TextView>(R.id.tv_sub_distance).text =
            String.format(Locale.getDefault(), "%.2f", totalDistance)

        // 速度/配速
        val primaryStr = formatPrimaryMetricForDisplay()
        findViewById<TextView>(R.id.tv_primary_value).text = primaryStr
        
        if (config.isRunning()) {
            updatePaceColor()
        } else {
            updateSpeedColor()
        }
    }

    private fun maybeSpeakProgressUpdate() {
        if (!config.isRunning() || !isVoiceBroadcastEnabled || !isVoiceEngineReady || !isActivityVisible || isPaused || !isTimerRunning) {
            return
        }

        var hasSpoken = false

        if (totalDistance >= nextVoiceDistanceKm) {
            val kilometerIndex = nextVoiceDistanceKm.toInt().coerceAtLeast(1)
            val distanceAnnouncement = buildDistanceProgressAnnouncement(kilometerIndex)
            lastVoiceDistanceCheckpointKm = totalDistance
            lastVoiceDistanceCheckpointElapsedSeconds = elapsedSeconds
            lastVoiceDistanceCheckpointSteps = totalSteps
            while (totalDistance >= nextVoiceDistanceKm) {
                nextVoiceDistanceKm += 1.0
            }
            speakVoice(distanceAnnouncement, flush = true)
            hasSpoken = true
        }

        if (elapsedSeconds >= nextVoiceElapsedSeconds) {
            val elapsedMilestone = nextVoiceElapsedSeconds
            while (elapsedSeconds >= nextVoiceElapsedSeconds) {
                nextVoiceElapsedSeconds += VOICE_ANNOUNCEMENT_INTERVAL_SECONDS
            }
            speakVoice(buildTimeProgressAnnouncement(elapsedMilestone), flush = !hasSpoken)
        }
    }

    private fun resetVoiceAnnouncementSchedule() {
        nextVoiceDistanceKm = ((totalDistance / 1.0).toInt() + 1).toDouble()
        nextVoiceElapsedSeconds =
            ((elapsedSeconds / VOICE_ANNOUNCEMENT_INTERVAL_SECONDS) + 1) * VOICE_ANNOUNCEMENT_INTERVAL_SECONDS
        lastVoiceDistanceCheckpointKm = totalDistance
        lastVoiceDistanceCheckpointElapsedSeconds = elapsedSeconds
        lastVoiceDistanceCheckpointSteps = totalSteps
    }

    private fun buildDistanceProgressAnnouncement(kilometerIndex: Int): String {
        val segmentDistanceKm = (totalDistance - lastVoiceDistanceCheckpointKm).coerceAtLeast(0.001)
        val segmentElapsedSeconds =
            (elapsedSeconds - lastVoiceDistanceCheckpointElapsedSeconds).coerceAtLeast(0)
        val segmentSteps = (totalSteps - lastVoiceDistanceCheckpointSteps).coerceAtLeast(0)
        val segmentPaceSecondsPerKm = if (segmentDistanceKm > 0) {
            segmentElapsedSeconds / segmentDistanceKm
        } else {
            0.0
        }
        val segmentCadence = if (segmentElapsedSeconds > 0) {
            ((segmentSteps * 60.0) / segmentElapsedSeconds).toInt().coerceAtLeast(0)
        } else {
            cadence.coerceAtLeast(0)
        }

        return "第 $kilometerIndex 公里，用时 ${formatVoiceDuration(elapsedSeconds)}。" +
            "最近一公里配速 ${formatVoicePace(segmentPaceSecondsPerKm)}。" +
            "最近一公里平均步频 ${segmentCadence} 次每分钟。"
    }

    private fun buildTimeProgressAnnouncement(elapsedMilestoneSeconds: Int): String {
        val averagePace = if (totalDistance > 0.01) elapsedSeconds / totalDistance else 0.0
        return "已完成 ${formatVoiceHalfHourMilestone(elapsedMilestoneSeconds)}。" +
            "当前已跑 ${formatVoiceDistance(totalDistance)} 公里。" +
            "平均配速 ${formatVoicePace(averagePace)}。"
    }

    private fun formatVoiceDistance(distanceKm: Double): String {
        return String.format(Locale.getDefault(), "%.1f", distanceKm.coerceAtLeast(0.0))
    }

    private fun formatVoiceDuration(seconds: Int): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val hours = safeSeconds / 3600
        val minutes = (safeSeconds % 3600) / 60
        val remainSeconds = safeSeconds % 60
        return when {
            hours > 0 -> "${hours}小时${minutes}分${remainSeconds}秒"
            minutes > 0 -> "${minutes}分${remainSeconds}秒"
            else -> "${remainSeconds}秒"
        }
    }

    private fun formatVoicePace(secondsPerKm: Double): String {
        if (secondsPerKm <= 0.0 || secondsPerKm.isInfinite() || secondsPerKm.isNaN()) {
            return "暂未稳定"
        }
        val totalSeconds = secondsPerKm.toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes}分${seconds.toString().padStart(2, '0')}秒每公里"
    }

    private fun formatVoiceHalfHourMilestone(seconds: Int): String {
        val totalMinutes = (seconds / 60).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}小时${minutes}分钟"
            hours > 0 -> "${hours}小时"
            else -> "${minutes}分钟"
        }
    }

    private fun speakVoice(
        text: String,
        flush: Boolean = true,
        allowWhenPaused: Boolean = false
    ) {
        if (!isVoiceEngineReady || !isActivityVisible) return
        if (!allowWhenPaused && (isPaused || !isTimerRunning)) return
        val tts = textToSpeech ?: return
        tts.speak(
            text,
            if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
            null,
            "workout-voice-$elapsedSeconds"
        )
    }

    // ============================================================
    // 模拟数据更新
    // ============================================================
    private fun updateSimulatedData() {
        totalDistance += config.distanceMultiplier

        if (config.isRunning()) {
            updateRunningData()
        } else {
            updateCyclingData()
        }

        updateEstimatedCalories(isSimulatedMovement = true)
        updateSecondaryCards()
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

    private fun updateEstimatedCalories(isSimulatedMovement: Boolean) {
        val distanceDeltaKm = (totalDistance - lastCalorieDistance).coerceAtLeast(0.0)
        lastCalorieDistance = totalDistance

        if (distanceDeltaKm <= 0.0) return
        if (!isSimulatedMovement && !hasMeaningfulMovement(distanceDeltaKm)) return

        calorieAccumulator += distanceDeltaKm * estimatedCaloriesPerKm()
        totalCalories = calorieAccumulator.toInt()
    }

    private fun hasMeaningfulMovement(distanceDeltaKm: Double): Boolean {
        return if (config.isRunning()) {
            distanceDeltaKm >= 0.0015
        } else {
            distanceDeltaKm >= 0.003
        }
    }

    private fun estimatedCaloriesPerKm(): Double {
        return if (config.isRunning()) {
            when {
                cadence >= 170 -> 72.0
                cadence >= 155 -> 66.0
                cadence >= 140 -> 60.0
                else -> 55.0
            }
        } else {
            val speedCalories = when {
                currentPaceSpeed >= 28.0 -> 35.0
                currentPaceSpeed >= 22.0 -> 30.0
                currentPaceSpeed >= 16.0 -> 24.0
                currentPaceSpeed > 0.0 -> 18.0
                else -> 0.0
            }
            speedCalories + grade.coerceAtLeast(0.0) * 0.6
        }
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
        if (currentPaceSpeed <= 0.0) {
            tv.setTextColor(getColor(config.primaryColorRes))
            return
        }
        when {
            currentPaceSpeed < config.paceFast -> tv.setTextColor(getColor(R.color.tracking_metric_fast))
            currentPaceSpeed > config.paceSlow -> tv.setTextColor(getColor(R.color.tracking_metric_slow))
            else -> tv.setTextColor(getColor(config.primaryColorRes))
        }
    }

    private fun updateSpeedColor() {
        val tv = findViewById<TextView>(R.id.tv_primary_value)
        if (currentPaceSpeed <= 0.0) {
            tv.setTextColor(getColor(config.primaryColorRes))
            return
        }
        when {
            currentPaceSpeed > config.paceFast -> tv.setTextColor(getColor(R.color.tracking_metric_fast))
            currentPaceSpeed < config.paceSlow -> tv.setTextColor(getColor(R.color.tracking_metric_slow))
            else -> tv.setTextColor(getColor(config.primaryColorRes))
        }
    }

    private fun formatPrimaryMetricForDisplay(): String {
        if (currentPaceSpeed <= 0.0) {
            return if (config.isRunning()) "--'--\"" else "--.-"
        }
        return config.primaryFormat(currentPaceSpeed)
    }

    private fun updatePrimaryMetricFromLatestSignal() {
        if (config.isRunning()) {
            currentPaceSpeed = when {
                latestLocationSpeedMps > 0f -> (1000 / latestLocationSpeedMps).toDouble()
                hasMeaningfulRunningPaceFallback() -> elapsedSeconds / totalDistance
                else -> 0.0
            }
            return
        }

        currentPaceSpeed = when {
            latestLocationSpeedMps > 0f -> (latestLocationSpeedMps * 3.6).toDouble()
            hasMeaningfulCyclingSpeedFallback() -> (totalDistance / elapsedSeconds.toDouble()) * 3600
            else -> 0.0
        }
        if (currentPaceSpeed > maxSpeed) {
            maxSpeed = currentPaceSpeed
            findViewById<TextView>(R.id.tv_max_speed).apply {
                visibility = View.VISIBLE
                text = String.format(Locale.getDefault(), "最高 %.1f km/h", maxSpeed)
            }
        }
    }

    private fun hasMeaningfulRunningPaceFallback(): Boolean {
        return elapsedSeconds >= 10 && totalDistance >= 0.02
    }

    private fun hasMeaningfulCyclingSpeedFallback(): Boolean {
        return elapsedSeconds >= 10 && totalDistance >= 0.03
    }

    private fun averagePrimaryMetric(): Double {
        if (elapsedSeconds <= 0 || totalDistance <= 0.0) {
            return 0.0
        }
        return if (config.isRunning()) {
            elapsedSeconds / totalDistance
        } else {
            (totalDistance / elapsedSeconds.toDouble()) * 3600.0
        }
    }

    private fun averagePrimaryMetricForDisplay(): String {
        val averageMetric = averagePrimaryMetric()
        if (averageMetric <= 0.0 || averageMetric.isNaN() || averageMetric.isInfinite()) {
            return if (config.isRunning()) "--'--\"" else "--.-"
        }
        return config.primaryFormat(averageMetric)
    }

    // ============================================================
    // 分段计时 (Lap)
    // ============================================================
    private fun recordLap() {
        lapCount++
        val lapTime = formatTime(lapSeconds)
        lapSeconds = 0
        showAppFeedback("第 $lapCount 段: $lapTime  总计 ${formatTime(elapsedSeconds)}", FeedbackType.INFO)
    }

    // ============================================================
    // 节拍器
    // ============================================================
    private fun toggleMetronome() {
        val bpmOptions = intArrayOf(160, 170, 180)
        val optionLabels = bpmOptions.map { "$it BPM" }.toTypedArray()
        var selectedIndex = bpmOptions.indexOf(metronomeBpm).coerceAtLeast(0)

        val dialog = createAppAlertDialogBuilder()
            .setTitle("选择节拍器")
            .setSingleChoiceItems(optionLabels, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(if (isMetronomeEnabled) "应用" else "开启") { _, _ ->
                metronomeBpm = bpmOptions[selectedIndex]
                startMetronome()
            }
            .setNegativeButton("取消", null)
            .apply {
                if (isMetronomeEnabled) {
                    setNeutralButton("关闭") { _, _ ->
                        stopMetronome(showToast = true)
                    }
                }
            }
            .create()

        dialog.show()
        dialog.applyAppDialogStyling(this)
    }

    private fun startMetronome(showToast: Boolean = true) {
        if (isPaused || !isTimerRunning) {
            showAppFeedback("请先继续运动，再开启节拍器", FeedbackType.WARNING)
            return
        }
        handler.removeCallbacks(metronomeRunnable)
        isMetronomeEnabled = true
        isMetronomeOn = true
        updateMetronomeChipState()
        metronomeToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
        handler.postDelayed(metronomeRunnable, metronomeIntervalMs())
        if (showToast) {
            showAppFeedback("节拍器已开启 (${metronomeBpm} BPM)", FeedbackType.SUCCESS)
        }
    }

    private fun suspendMetronome() {
        handler.removeCallbacks(metronomeRunnable)
        isMetronomeOn = false
        updateMetronomeChipState()
    }

    private fun stopMetronome(showToast: Boolean = false) {
        handler.removeCallbacks(metronomeRunnable)
        val wasEnabled = isMetronomeEnabled
        isMetronomeOn = false
        isMetronomeEnabled = false
        updateMetronomeChipState()
        if (showToast && wasEnabled) {
            showAppFeedback("节拍器已关闭", FeedbackType.INFO)
        }
    }

    private fun updateMetronomeChipState() {
        val chip = findViewById<TextView>(R.id.chip_metronome)
        if (isMetronomeEnabled) {
            chip.setBackgroundResource(R.drawable.tracking_chip_active_bg)
            chip.text = "节拍器 ${metronomeBpm}"
            chip.setTextColor(getColor(R.color.tracking_chip_active_text))
        } else {
            chip.setBackgroundResource(R.drawable.tracking_chip_bg)
            chip.text = "节拍器 ${metronomeBpm}"
            chip.setTextColor(getColor(R.color.tracking_on_primary))
        }
    }

    private fun metronomeIntervalMs(): Long {
        return (60_000L / metronomeBpm).coerceAtLeast(200L)
    }

    // ============================================================
    // 自动暂停
    // ============================================================
    private fun toggleAutoPause() {
        isAutoPauseOn = !isAutoPauseOn
        updateAutoPauseButtonState()
    }

    private fun setLockState(lock: Boolean) {
        isLocked = lock
        findViewById<View>(R.id.layout_lock_overlay).isVisible = lock
        if (lock) {
            showAppFeedback("已锁屏，长按屏幕中央解锁", FeedbackType.WARNING)
        } else {
            showAppFeedback("已解锁", FeedbackType.SUCCESS)
        }
    }

    private fun updateAutoPauseButtonState() {
        val btn = findViewById<TextView>(R.id.btn_auto_pause)
        if (isAutoPauseOn) {
            btn.setBackgroundResource(R.drawable.tracking_chip_active_bg)
            btn.text = "自动暂停: 开"
            btn.setTextColor(getColor(R.color.tracking_chip_active_text))
        } else {
            btn.setBackgroundResource(R.drawable.tracking_chip_bg)
            btn.text = "自动暂停: 关"
            btn.setTextColor(getColor(R.color.tracking_on_primary))
        }
    }

    // ============================================================
    // 结束确认弹窗
    // ============================================================
    private fun showStopConfirmDialog(resumeOnCancel: Boolean = isTimerRunning) {
        if (isTimerRunning) {
            pauseTimer()
        }

        createAppAlertDialogBuilder()
            .setTitle("结束运动")
            .setMessage("确定要结束本次运动吗？")
            .setPositiveButton("确定") { _, _ ->
                showSummarySheet()
            }
            .setNegativeButton("取消") { _, _ ->
                if (resumeOnCancel && isPaused) {
                    resumeTimer()
                }
            }
            .create()
            .also {
                it.show()
                it.applyAppDialogStyling(this)
            }
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
            "配速: ${averagePrimaryMetricForDisplay()}"
        } else {
            "速度: ${averagePrimaryMetricForDisplay()} km/h"
        }

        val titleView = view.findViewById<TextView>(R.id.summary_tv_title)
        val subtitleView = view.findViewById<TextView>(R.id.summary_tv_subtitle)
        val saveButton = view.findViewById<TextView>(R.id.summary_btn_save)
        val saveGuardHint = view.findViewById<TextView>(R.id.summary_tv_save_guard_hint)
        view.findViewById<TextView>(R.id.summary_tv_duration).text = duration
        view.findViewById<TextView>(R.id.summary_tv_distance).text = "$distance 公里"
        view.findViewById<TextView>(R.id.summary_tv_calories).text = "$totalCalories kcal"
        view.findViewById<TextView>(R.id.summary_tv_avg_pace).text = primaryStr
        bindCourseSummary(view, titleView, subtitleView)

        val saveGuardMessage = currentWorkoutSaveGuardMessage()
        val canSaveRecord = saveGuardMessage == null
        saveButton.isEnabled = canSaveRecord
        saveButton.isClickable = canSaveRecord
        saveButton.isFocusable = canSaveRecord
        saveButton.alpha = if (canSaveRecord) 1f else 0.45f
        saveButton.text = if (canSaveRecord) "保存记录" else "暂不可保存"
        saveGuardHint.isVisible = !saveGuardMessage.isNullOrBlank()
        saveGuardHint.text = saveGuardMessage

        saveButton.setOnClickListener {
            if (!canSaveCurrentWorkoutRecord()) {
                return@setOnClickListener
            }
            saveWorkoutRecord {
                bottomSheet.dismiss()
                showAppFeedback("记录已保存", FeedbackType.SUCCESS)
                onFinishWorkout(announceCompletion = true)
            }
        }

        view.findViewById<View>(R.id.summary_btn_discard).setOnClickListener {
            bottomSheet.dismiss()
            showAppFeedback("已放弃本次记录", FeedbackType.WARNING)
            onFinishWorkout(announceCompletion = false)
        }

        bottomSheet.setContentView(view)
        bottomSheet.setCancelable(false)
        announceWorkoutEndedIfNeeded()
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
    private fun saveWorkoutRecord(onSaved: (() -> Unit)? = null) {
        if (!canSaveCurrentWorkoutRecord()) {
            return
        }

        val sportType = if (config.isRunning()) "RUN" else "CYCLING"
        val iconRes = WorkoutRecordHelper.getIconRes(sportType)
        val timestamp = WorkoutRecordHelper.nowTimestamp()
        val date = WorkoutRecordHelper.todayDate()
        val paceStr = averagePrimaryMetricForDisplay()

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
        viewModel.saveRecord(record) {
            onSaved?.invoke()
        }
    }

    private fun canSaveCurrentWorkoutRecord(): Boolean {
        val guardMessage = currentWorkoutSaveGuardMessage()
        if (guardMessage == null) {
            return true
        }

        showAppFeedback(guardMessage, FeedbackType.WARNING)
        return false
    }

    private fun currentWorkoutSaveGuardMessage(): String? {
        if (config.isRunning()) {
            if (totalDistance >= MIN_RUN_SAVE_DISTANCE_KM && elapsedSeconds >= MIN_RUN_SAVE_DURATION_SECONDS) {
                return null
            }
            return "本次跑步未达到保存条件，至少需要 ${formatDistanceForSaveGuard(MIN_RUN_SAVE_DISTANCE_KM)} 公里，且时长不少于 ${MIN_RUN_SAVE_DURATION_SECONDS} 秒。"
        }

        if (config.isCycling()) {
            if (totalDistance >= MIN_CYCLING_SAVE_DISTANCE_KM && elapsedSeconds >= MIN_CYCLING_SAVE_DURATION_SECONDS) {
                return null
            }
            return "本次骑行未达到保存条件，至少需要 ${formatDistanceForSaveGuard(MIN_CYCLING_SAVE_DISTANCE_KM)} 公里，且时长不少于 ${MIN_CYCLING_SAVE_DURATION_SECONDS} 秒。"
        }

        return null
    }

    private fun formatDistanceForSaveGuard(distanceKm: Double): String {
        return String.format(Locale.getDefault(), "%.2f", distanceKm)
    }

    private fun onFinishWorkout(announceCompletion: Boolean) {
        shouldPersistCourseSession = false
        ActiveCourseSessionStore(this).clear(CourseNavigator.courseIdOf(intent))
        if (announceCompletion && config.isRunning() && isVoiceBroadcastEnabled && isVoiceEngineReady && !hasAnnouncedWorkoutEnd) {
            speakVoice(
                "运动已结束，本次运动消耗卡路里 ${totalCalories}",
                flush = true,
                allowWhenPaused = true
            )
            handler.postDelayed({ finish() }, 2400L)
        } else {
            finish()
        }
    }

    private fun announceWorkoutEndedIfNeeded() {
        if (!config.isRunning() || !isVoiceBroadcastEnabled || !isVoiceEngineReady || hasAnnouncedWorkoutEnd) {
            return
        }
        hasAnnouncedWorkoutEnd = true
        speakVoice("运动已结束", flush = true, allowWhenPaused = true)
    }

    private fun handleBackPressed() {
        if (isLocked) {
            showAppFeedback("请先长按解锁后再退出", FeedbackType.WARNING)
            return
        }

        if (elapsedSeconds > 0 && isPaused) {
            showStopConfirmDialog(resumeOnCancel = false)
            return
        }

        if (elapsedSeconds > 0 && isTimerRunning) {
            pauseTimer()
            createAppAlertDialogBuilder()
                .setTitle("放弃运动")
                .setMessage("当前运动数据将不会保存，确定退出吗？")
                .setPositiveButton("退出") { _, _ ->
                    stopTimer()
                    finish()
                }
                .setNegativeButton("继续运动") { _, _ ->
                    resumeTimer()
                }
                .create()
                .also {
                    it.show()
                    it.applyAppDialogStyling(this)
                }
        } else {
            stopTimer()
            finish()
        }
    }

    override fun onDestroy() {
        if (shouldPersistCourseSession) {
            persistCourseSessionProgress()
        }
        stopTimer()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        metronomeToneGenerator.release()
        locationService.destroy()
        currentLocationMarker?.remove()
        currentLocationMarker = null
        routePolyline?.remove()
        routePolyline = null
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
        mapView.onResume()
    }

    override fun onPause() {
        isActivityVisible = false
        textToSpeech?.stop()
        mapView.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun stopTimer() {
        isTimerRunning = false
        handler.removeCallbacks(timerRunnable)
        stopMetronome()
        textToSpeech?.stop()
        locationService.stopTracking()
        stepService.stopTracking()
    }

    companion object {
        const val EXTRA_SPORT_NAME = "sport_name"
        const val EXTRA_SPORT_ICON = "sport_icon"
        const val EXTRA_SPORT_TYPE = "sport_type" // RUN / CYCLING
        private const val PREFS_NAME = "fitness_prefs"
        private const val PREF_VOICE_BROADCAST_PREFIX = "running_voice_broadcast_enabled"
        private const val VOICE_ANNOUNCEMENT_INTERVAL_SECONDS = 1800
        private const val MIN_RUN_SAVE_DISTANCE_KM = 0.05
        private const val MIN_RUN_SAVE_DURATION_SECONDS = 30
        private const val MIN_CYCLING_SAVE_DISTANCE_KM = 0.10
        private const val MIN_CYCLING_SAVE_DURATION_SECONDS = 30
    }
}
